from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import MileageRecord, Vehicle
from ..schemas import MileageRecordCreate, MileageRecordResponse

router = APIRouter(prefix="/api/vehicles/{vehicle_id}/mileage", tags=["mileage"])


def get_vehicle_or_404(vehicle_id: int, db: Session) -> Vehicle:
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    return vehicle


@router.get("", response_model=list[MileageRecordResponse])
def list_mileage(vehicle_id: int, db: Session = Depends(get_db)):
    get_vehicle_or_404(vehicle_id, db)
    return (
        db.query(MileageRecord)
        .filter(MileageRecord.vehicle_id == vehicle_id)
        .order_by(MileageRecord.miles.desc(), MileageRecord.recorded_date.desc())
        .all()
    )


@router.post("", response_model=MileageRecordResponse, status_code=201)
def create_mileage(
    vehicle_id: int, payload: MileageRecordCreate, db: Session = Depends(get_db)
):
    get_vehicle_or_404(vehicle_id, db)
    record = MileageRecord(vehicle_id=vehicle_id, **payload.model_dump())
    db.add(record)
    db.commit()
    db.refresh(record)
    return record


@router.put("/{record_id}", response_model=MileageRecordResponse)
def update_mileage(
    vehicle_id: int, record_id: int, payload: MileageRecordCreate, db: Session = Depends(get_db)
):
    record = (
        db.query(MileageRecord)
        .filter(MileageRecord.id == record_id, MileageRecord.vehicle_id == vehicle_id)
        .first()
    )
    if not record:
        raise HTTPException(status_code=404, detail="Mileage record not found")
    for field, value in payload.model_dump().items():
        setattr(record, field, value)
    db.commit()
    db.refresh(record)
    return record


@router.delete("/{record_id}", status_code=204)
def delete_mileage(vehicle_id: int, record_id: int, db: Session = Depends(get_db)):
    record = (
        db.query(MileageRecord)
        .filter(MileageRecord.id == record_id, MileageRecord.vehicle_id == vehicle_id)
        .first()
    )
    if not record:
        raise HTTPException(status_code=404, detail="Mileage record not found")
    db.delete(record)
    db.commit()
