from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import Vehicle
from ..schemas import DueStatusResponse
from ..services.vehicle_status import get_vehicle_status

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

    return get_vehicle_status(db, vehicle, current_miles)
