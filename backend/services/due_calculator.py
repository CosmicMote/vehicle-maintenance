from datetime import date
from typing import Optional

from ..schemas import DueStatusItem, DueStatus

# Thresholds (in miles) for status classification
OVERDUE_THRESHOLD = 0       # at or past due mileage
DUE_THRESHOLD = 500         # within 500 miles of due
UPCOMING_THRESHOLD = 1000   # within 1000 miles of due


def estimate_current_miles(
    last_performed_miles: int,
    last_performed_date: date,
    avg_miles_per_year: int,
    as_of: Optional[date] = None,
) -> int:
    if as_of is None:
        as_of = date.today()
    days_elapsed = (as_of - last_performed_date).days
    miles_driven = avg_miles_per_year * (days_elapsed / 365.25)
    return last_performed_miles + int(miles_driven)


def classify_status(miles_until_due: int) -> DueStatus:
    if miles_until_due <= OVERDUE_THRESHOLD:
        return "overdue"
    if miles_until_due <= DUE_THRESHOLD:
        return "due"
    if miles_until_due <= UPCOMING_THRESHOLD:
        return "upcoming"
    return "ok"


def calculate_due_status(
    vehicle_id: int,
    avg_miles_per_year: Optional[int],
    maintenance_types: list,          # list of MaintenanceType ORM objects
    latest_record_by_type: dict,      # {maintenance_type_id: MaintenanceRecord}
    supplied_current_miles: Optional[int],
) -> tuple[int, bool, list[DueStatusItem]]:
    """
    Returns (current_miles, estimated_miles_used, list[DueStatusItem]).
    estimated_miles_used is True when current_miles was derived from avg_miles_per_year.
    """
    estimated_miles_used = False

    # Determine current mileage
    if supplied_current_miles is not None:
        current_miles = supplied_current_miles
    elif avg_miles_per_year:
        # Find the most recent record across all types to use as the anchor
        all_records = [r for r in latest_record_by_type.values() if r is not None]
        if all_records:
            anchor = max(all_records, key=lambda r: r.performed_date)
            current_miles = estimate_current_miles(
                anchor.performed_miles,
                anchor.performed_date,
                avg_miles_per_year,
            )
        else:
            # No records at all — estimate from 0
            days_since_epoch = (date.today() - date(date.today().year, 1, 1)).days
            current_miles = int(avg_miles_per_year * (days_since_epoch / 365.25))
        estimated_miles_used = True
    else:
        current_miles = 0

    items: list[DueStatusItem] = []
    for mtype in maintenance_types:
        record = latest_record_by_type.get(mtype.id)

        if record is None:
            next_due_miles = mtype.interval_miles
            miles_until_due = next_due_miles - current_miles
            items.append(DueStatusItem(
                maintenance_type_id=mtype.id,
                name=mtype.name,
                interval_miles=mtype.interval_miles,
                last_performed_miles=None,
                last_performed_date=None,
                next_due_miles=next_due_miles,
                current_miles=current_miles,
                miles_until_due=miles_until_due,
                status="never_performed",
                estimated_miles_used=estimated_miles_used,
            ))
            continue

        next_due_miles = record.performed_miles + mtype.interval_miles
        miles_until_due = next_due_miles - current_miles
        status = classify_status(miles_until_due)

        items.append(DueStatusItem(
            maintenance_type_id=mtype.id,
            name=mtype.name,
            interval_miles=mtype.interval_miles,
            last_performed_miles=record.performed_miles,
            last_performed_date=record.performed_date,
            next_due_miles=next_due_miles,
            current_miles=current_miles,
            miles_until_due=miles_until_due,
            status=status,
            estimated_miles_used=estimated_miles_used,
        ))

    # Sort: overdue -> due -> upcoming -> never_performed -> ok
    status_order = {"overdue": 0, "due": 1, "upcoming": 2, "never_performed": 3, "ok": 4}
    items.sort(key=lambda i: status_order[i.status])

    return current_miles, estimated_miles_used, items
