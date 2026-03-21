from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import MileageRecord, Vehicle
from ..schemas import VehicleCreate, VehicleResponse, VehicleUpdate
from ..services.due_calculator import estimate_current_miles

router = APIRouter(prefix="/api/vehicles", tags=["vehicles"])


def get_vehicle_or_404(vehicle_id: int, db: Session) -> Vehicle:
    vehicle = db.query(Vehicle).filter(Vehicle.id == vehicle_id).first()
    if not vehicle:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    return vehicle


def build_vehicle_response(vehicle: Vehicle, db: Session) -> VehicleResponse:
    resp = VehicleResponse.model_validate(vehicle)
    if vehicle.avg_miles_per_year:
        latest = (
            db.query(MileageRecord)
            .filter(MileageRecord.vehicle_id == vehicle.id)
            .order_by(MileageRecord.miles.desc(), MileageRecord.recorded_date.desc())
            .first()
        )
        if latest:
            resp.estimated_current_miles = estimate_current_miles(
                latest.miles, latest.recorded_date, vehicle.avg_miles_per_year
            )
    return resp


@router.get("", response_model=list[VehicleResponse])
def list_vehicles(db: Session = Depends(get_db)):
    vehicles = db.query(Vehicle).order_by(Vehicle.name).all()
    return [build_vehicle_response(v, db) for v in vehicles]


@router.post("", response_model=VehicleResponse, status_code=201)
def create_vehicle(payload: VehicleCreate, db: Session = Depends(get_db)):
    vehicle = Vehicle(**payload.model_dump())
    db.add(vehicle)
    db.commit()
    db.refresh(vehicle)
    return build_vehicle_response(vehicle, db)


@router.get("/{vehicle_id}", response_model=VehicleResponse)
def get_vehicle(vehicle_id: int, db: Session = Depends(get_db)):
    return build_vehicle_response(get_vehicle_or_404(vehicle_id, db), db)


@router.put("/{vehicle_id}", response_model=VehicleResponse)
def update_vehicle(vehicle_id: int, payload: VehicleUpdate, db: Session = Depends(get_db)):
    vehicle = get_vehicle_or_404(vehicle_id, db)
    for field, value in payload.model_dump().items():
        setattr(vehicle, field, value)
    db.commit()
    db.refresh(vehicle)
    return build_vehicle_response(vehicle, db)


@router.delete("/{vehicle_id}", status_code=204)
def delete_vehicle(vehicle_id: int, db: Session = Depends(get_db)):
    vehicle = get_vehicle_or_404(vehicle_id, db)
    db.delete(vehicle)
    db.commit()
