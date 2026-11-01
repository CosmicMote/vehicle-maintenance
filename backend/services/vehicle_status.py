from typing import Optional

from sqlalchemy.orm import Session

from ..models import MaintenanceRecord, MaintenanceType, MileageRecord, Vehicle
from ..schemas import DueStatusResponse
from .due_calculator import calculate_due_status


def get_vehicle_status(
    db: Session,
    vehicle: Vehicle,
    current_miles: Optional[int] = None,
) -> DueStatusResponse:
    """Build the due-status report for a vehicle (shared by the status API and reminder emails)."""
    maintenance_types = (
        db.query(MaintenanceType)
        .filter(MaintenanceType.vehicle_id == vehicle.id)
        .all()
    )

    # For each maintenance type, fetch the most recent record
    latest_record_by_type: dict = {}
    for mtype in maintenance_types:
        latest = (
            db.query(MaintenanceRecord)
            .filter(
                MaintenanceRecord.vehicle_id == vehicle.id,
                MaintenanceRecord.maintenance_type_id == mtype.id,
            )
            .order_by(MaintenanceRecord.performed_date.desc(), MaintenanceRecord.performed_miles.desc())
            .first()
        )
        latest_record_by_type[mtype.id] = latest

    latest_mileage_record = (
        db.query(MileageRecord)
        .filter(MileageRecord.vehicle_id == vehicle.id)
        .order_by(MileageRecord.miles.desc(), MileageRecord.recorded_date.desc())
        .first()
    )

    resolved_miles, estimated, items = calculate_due_status(
        vehicle_id=vehicle.id,
        avg_miles_per_year=vehicle.avg_miles_per_year,
        maintenance_types=maintenance_types,
        latest_record_by_type=latest_record_by_type,
        supplied_current_miles=current_miles,
        latest_mileage_record=latest_mileage_record,
    )

    return DueStatusResponse(
        vehicle_id=vehicle.id,
        current_miles=resolved_miles,
        estimated_miles_used=estimated,
        items=items,
    )
