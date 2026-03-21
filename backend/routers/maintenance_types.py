from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import MaintenanceType, Vehicle
from ..schemas import MaintenanceTypeCreate, MaintenanceTypeResponse, MaintenanceTypeUpdate

router = APIRouter(prefix="/api/vehicles/{vehicle_id}/maintenance-types", tags=["maintenance-types"])


def get_vehicle_or_404(vehicle_id: int, db: Session) -> Vehicle:
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    return vehicle


def get_type_or_404(vehicle_id: int, type_id: int, db: Session) -> MaintenanceType:
    mtype = (
        db.query(MaintenanceType)
        .filter(MaintenanceType.id == type_id, MaintenanceType.vehicle_id == vehicle_id)
        .first()
    )
    if not mtype:
        raise HTTPException(status_code=404, detail="Maintenance type not found")
    return mtype


@router.get("", response_model=list[MaintenanceTypeResponse])
def list_types(vehicle_id: int, db: Session = Depends(get_db)):
    get_vehicle_or_404(vehicle_id, db)
    return (
        db.query(MaintenanceType)
        .filter(MaintenanceType.vehicle_id == vehicle_id)
        .order_by(MaintenanceType.name)
        .all()
    )


@router.post("", response_model=MaintenanceTypeResponse, status_code=201)
def create_type(vehicle_id: int, payload: MaintenanceTypeCreate, db: Session = Depends(get_db)):
    get_vehicle_or_404(vehicle_id, db)
    mtype = MaintenanceType(vehicle_id=vehicle_id, **payload.model_dump())
    db.add(mtype)
    db.commit()
    db.refresh(mtype)
    return mtype


@router.put("/{type_id}", response_model=MaintenanceTypeResponse)
def update_type(
    vehicle_id: int, type_id: int, payload: MaintenanceTypeUpdate, db: Session = Depends(get_db)
):
    mtype = get_type_or_404(vehicle_id, type_id, db)
    for field, value in payload.model_dump().items():
        setattr(mtype, field, value)
    db.commit()
    db.refresh(mtype)
    return mtype


@router.delete("/{type_id}", status_code=204)
def delete_type(vehicle_id: int, type_id: int, db: Session = Depends(get_db)):
    mtype = get_type_or_404(vehicle_id, type_id, db)
    db.delete(mtype)
    db.commit()
