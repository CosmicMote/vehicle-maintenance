from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import MaintenanceRecord, MaintenanceType, MileageRecord, Vehicle
from ..schemas import DueStatusResponse
from ..services.due_calculator import calculate_due_status

router = APIRouter(prefix="/api/vehicles", tags=["status"])


@router.get("/{vehicle_id}/status", response_model=DueStatusResponse)
def get_due_status(
    vehicle_id: int,
    current_miles: Optional[int] = None,
    db: Session = Depends(get_db),
):
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")

    maintenance_types = (
        db.query(MaintenanceType)
        .filter(MaintenanceType.vehicle_id == vehicle_id)
        .all()
    )

    # For each maintenance type, fetch the most recent record
    latest_record_by_type: dict = {}
    for mtype in maintenance_types:
        latest = (
            db.query(MaintenanceRecord)
            .filter(
                MaintenanceRecord.vehicle_id == vehicle_id,
                MaintenanceRecord.maintenance_type_id == mtype.id,
            )
            .order_by(MaintenanceRecord.performed_date.desc(), MaintenanceRecord.performed_miles.desc())
            .first()
        )
        latest_record_by_type[mtype.id] = latest

    latest_mileage_record = (
        db.query(MileageRecord)
        .filter(MileageRecord.vehicle_id == vehicle_id)
        .order_by(MileageRecord.miles.desc(), MileageRecord.recorded_date.desc())
        .first()
    )

    resolved_miles, estimated, items = calculate_due_status(
        vehicle_id=vehicle_id,
        avg_miles_per_year=vehicle.avg_miles_per_year,
        maintenance_types=maintenance_types,
        latest_record_by_type=latest_record_by_type,
        supplied_current_miles=current_miles,
        latest_mileage_record=latest_mileage_record,
    )

    return DueStatusResponse(
        vehicle_id=vehicle_id,
        current_miles=resolved_miles,
        estimated_miles_used=estimated,
        items=items,
    )
