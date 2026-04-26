import calendar
from datetime import date
from typing import Optional

from ..schemas import DueStatus, DueStatusItem

# Mileage thresholds for status classification
OVERDUE_THRESHOLD = 0       # at or past due mileage
DUE_THRESHOLD = 500         # within 500 miles of due
UPCOMING_THRESHOLD = 1000   # within 1000 miles of due

# Day thresholds for time-based status classification
DUE_DAYS = 30
UPCOMING_DAYS = 60

# Priority order for merging statuses (lower = worse)
_STATUS_PRIORITY: dict[DueStatus, int] = {
    "overdue": 0,
    "due": 1,
    "upcoming": 2,
    "never_performed": 3,
    "ok": 4,
}


def add_months(d: date, months: int) -> date:
    """Return d + months calendar months, clamping to the last day of the month."""
    total_months = d.month - 1 + months
    year = d.year + total_months // 12
    month = total_months % 12 + 1
    day = min(d.day, calendar.monthrange(year, month)[1])
    return date(year, month, day)


def estimate_current_miles(
    last_miles: int,
    last_date: date,
    avg_miles_per_year: int,
    as_of: Optional[date] = None,
) -> int:
    if as_of is None:
        as_of = date.today()
    days_elapsed = (as_of - last_date).days
    miles_driven = avg_miles_per_year * (days_elapsed / 365.25)
    return last_miles + int(miles_driven)


def classify_miles_status(miles_until_due: int) -> DueStatus:
    if miles_until_due <= OVERDUE_THRESHOLD:
        return "overdue"
    if miles_until_due <= DUE_THRESHOLD:
        return "due"
    if miles_until_due <= UPCOMING_THRESHOLD:
        return "upcoming"
    return "ok"


def classify_date_status(next_due_date: date) -> DueStatus:
    days = (next_due_date - date.today()).days
    if days <= 0:
        return "overdue"
    if days <= DUE_DAYS:
        return "due"
    if days <= UPCOMING_DAYS:
        return "upcoming"
    return "ok"


def worse_status(a: DueStatus, b: DueStatus) -> DueStatus:
    """Return whichever status has higher severity."""
    return a if _STATUS_PRIORITY[a] <= _STATUS_PRIORITY[b] else b


def calculate_due_status(
    vehicle_id: int,
    avg_miles_per_year: Optional[int],
    maintenance_types: list,          # list of MaintenanceType ORM objects
    latest_record_by_type: dict,      # {maintenance_type_id: MaintenanceRecord}
    supplied_current_miles: Optional[int],
    latest_mileage_record=None,       # MileageRecord ORM object or None
) -> tuple[int, bool, list[DueStatusItem]]:
    """
    Returns (current_miles, estimated_miles_used, list[DueStatusItem]).
    estimated_miles_used is True when current_miles was derived from avg_miles_per_year.
    """
    estimated_miles_used = False

    # Resolve current mileage
    if supplied_current_miles is not None:
        current_miles = supplied_current_miles
    elif avg_miles_per_year and latest_mileage_record is not None:
        current_miles = estimate_current_miles(
            latest_mileage_record.miles,
            latest_mileage_record.recorded_date,
            avg_miles_per_year,
        )
        estimated_miles_used = True
    else:
        current_miles = 0

    items: list[DueStatusItem] = []

    for mtype in maintenance_types:
        record = latest_record_by_type.get(mtype.id)

        if record is None:
            # Never performed — can calculate next_due_miles but not next_due_date
            next_due_miles = mtype.interval_miles
            miles_until_due = next_due_miles - current_miles
            items.append(DueStatusItem(
                maintenance_type_id=mtype.id,
                name=mtype.name,
                interval_miles=mtype.interval_miles,
                interval_months=mtype.interval_months,
                last_performed_miles=None,
                last_performed_date=None,
                next_due_miles=next_due_miles,
                next_due_date=None,
                current_miles=current_miles,
                miles_until_due=miles_until_due,
                status="never_performed",
                estimated_miles_used=estimated_miles_used,
            ))
            continue

        # Calculate mileage-based due info
        next_due_miles = record.performed_miles + mtype.interval_miles
        miles_until_due = next_due_miles - current_miles
        status = classify_miles_status(miles_until_due)

        # Calculate date-based due info (if interval_months is set)
        next_due_date: Optional[date] = None
        if mtype.interval_months:
            next_due_date = add_months(record.performed_date, mtype.interval_months)
            date_status = classify_date_status(next_due_date)
            status = worse_status(status, date_status)

        items.append(DueStatusItem(
            maintenance_type_id=mtype.id,
            name=mtype.name,
            interval_miles=mtype.interval_miles,
            interval_months=mtype.interval_months,
            last_performed_miles=record.performed_miles,
            last_performed_date=record.performed_date,
            next_due_miles=next_due_miles,
            next_due_date=next_due_date,
            current_miles=current_miles,
            miles_until_due=miles_until_due,
            status=status,
            estimated_miles_used=estimated_miles_used,
        ))

    return current_miles, estimated_miles_used, items
