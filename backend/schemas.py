from datetime import date, datetime
from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict


# --- Vehicle ---

class VehicleBase(BaseModel):
    name: str
    make: Optional[str] = None
    model: Optional[str] = None
    year: Optional[int] = None
    avg_miles_per_year: Optional[int] = None


class VehicleCreate(VehicleBase):
    pass


class VehicleUpdate(VehicleBase):
    pass


class VehicleResponse(VehicleBase):
    id: int
    created_at: datetime
    estimated_current_miles: Optional[int] = None
    model_config = ConfigDict(from_attributes=True)


# --- Maintenance Type ---

class MaintenanceTypeBase(BaseModel):
    name: str
    interval_miles: int
    interval_months: Optional[int] = None
    notes: Optional[str] = None


class MaintenanceTypeCreate(MaintenanceTypeBase):
    pass


class MaintenanceTypeUpdate(MaintenanceTypeBase):
    pass


class MaintenanceTypeResponse(MaintenanceTypeBase):
    id: int
    vehicle_id: int
    model_config = ConfigDict(from_attributes=True)


# --- Maintenance Record ---

class MaintenanceRecordBase(BaseModel):
    maintenance_type_id: int
    performed_date: date
    performed_miles: int
    notes: Optional[str] = None


class MaintenanceRecordCreate(MaintenanceRecordBase):
    pass


class MaintenanceRecordResponse(MaintenanceRecordBase):
    id: int
    vehicle_id: int
    created_at: datetime
    maintenance_type_name: Optional[str] = None
    model_config = ConfigDict(from_attributes=True)


# --- Mileage Record ---

class MileageRecordCreate(BaseModel):
    recorded_date: date
    miles: int
    notes: Optional[str] = None


class MileageRecordResponse(MileageRecordCreate):
    id: int
    vehicle_id: int
    created_at: datetime
    model_config = ConfigDict(from_attributes=True)


# --- Due Status ---

DueStatus = Literal["ok", "upcoming", "due", "overdue", "never_performed"]


class DueStatusItem(BaseModel):
    maintenance_type_id: int
    name: str
    interval_miles: int
    interval_months: Optional[int] = None
    last_performed_miles: Optional[int] = None
    last_performed_date: Optional[date] = None
    next_due_miles: Optional[int] = None
    next_due_date: Optional[date] = None
    current_miles: int
    miles_until_due: int
    status: DueStatus
    estimated_miles_used: bool


class DueStatusResponse(BaseModel):
    vehicle_id: int
    current_miles: int
    estimated_miles_used: bool
    items: list[DueStatusItem]
