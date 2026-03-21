from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import MaintenanceRecord, MaintenanceType, Vehicle
from ..schemas import MaintenanceRecordCreate, MaintenanceRecordResponse

router = APIRouter(prefix="/api/vehicles/{vehicle_id}/records", tags=["records"])


def get_vehicle_or_404(vehicle_id: int, db: Session) -> Vehicle:
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    return vehicle


@router.get("", response_model=list[MaintenanceRecordResponse])
def list_records(vehicle_id: int, db: Session = Depends(get_db)):
    get_vehicle_or_404(vehicle_id, db)
    records = (
        db.query(MaintenanceRecord)
        .filter(MaintenanceRecord.vehicle_id == vehicle_id)
        .order_by(MaintenanceRecord.performed_date.desc())
        .all()
    )
    # Attach maintenance_type_name for the response
    result = []
    for r in records:
        resp = MaintenanceRecordResponse.model_validate(r)
        resp.maintenance_type_name = r.maintenance_type.name if r.maintenance_type else None
        result.append(resp)
    return result


@router.post("", response_model=MaintenanceRecordResponse, status_code=201)
def create_record(
    vehicle_id: int, payload: MaintenanceRecordCreate, db: Session = Depends(get_db)
):
    get_vehicle_or_404(vehicle_id, db)
    # Verify the maintenance type belongs to this vehicle
    mtype = (
        db.query(MaintenanceType)
        .filter(
            MaintenanceType.id == payload.maintenance_type_id,
            MaintenanceType.vehicle_id == vehicle_id,
        )
        .first()
    )
    if not mtype:
        raise HTTPException(status_code=404, detail="Maintenance type not found for this vehicle")

    record = MaintenanceRecord(vehicle_id=vehicle_id, **payload.model_dump())
    db.add(record)
    db.commit()
    db.refresh(record)

    resp = MaintenanceRecordResponse.model_validate(record)
    resp.maintenance_type_name = mtype.name
    return resp


@router.get("/{record_id}", response_model=MaintenanceRecordResponse)
def get_record(vehicle_id: int, record_id: int, db: Session = Depends(get_db)):
    record = (
        db.query(MaintenanceRecord)
        .filter(
            MaintenanceRecord.id == record_id,
            MaintenanceRecord.vehicle_id == vehicle_id,
        )
        .first()
    )
    if not record:
        raise HTTPException(status_code=404, detail="Record not found")
    resp = MaintenanceRecordResponse.model_validate(record)
    resp.maintenance_type_name = record.maintenance_type.name if record.maintenance_type else None
    return resp


@router.delete("/{record_id}", status_code=204)
def delete_record(vehicle_id: int, record_id: int, db: Session = Depends(get_db)):
    record = (
        db.query(MaintenanceRecord)
        .filter(
            MaintenanceRecord.id == record_id,
            MaintenanceRecord.vehicle_id == vehicle_id,
        )
        .first()
    )
    if not record:
        raise HTTPException(status_code=404, detail="Record not found")
    db.delete(record)
    db.commit()
