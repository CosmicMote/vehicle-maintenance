import re
from datetime import date, datetime, timezone
from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


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


# --- Email Reminder Settings ---

# "overdue" = overdue items only; "due" = overdue items plus items that are due soon
ReminderThreshold = Literal["overdue", "due"]
SmtpSecurity = Literal["starttls", "ssl", "none"]

_EMAIL_RE = re.compile(r"^[^@\s,;]+@[^@\s,;]+\.[^@\s,;]+$")


def split_addresses(raw: str) -> list[str]:
    """Split a comma/semicolon/whitespace-separated address list."""
    return [a for a in re.split(r"[,;\s]+", raw or "") if a]


class EmailSettingsBase(BaseModel):
    enabled: bool = False
    smtp_host: str = ""
    smtp_port: int = Field(587, ge=1, le=65535)
    smtp_security: SmtpSecurity = "starttls"
    smtp_username: str = ""
    from_address: str = ""
    to_addresses: str = ""
    threshold: ReminderThreshold = "due"
    frequency_days: int = Field(7, ge=1, le=30)


class EmailSettingsUpdate(EmailSettingsBase):
    @field_validator("smtp_host", "smtp_username", "from_address")
    @classmethod
    def _strip(cls, v: str) -> str:
        return v.strip()

    @field_validator("to_addresses")
    @classmethod
    def _normalize_recipients(cls, v: str) -> str:
        return ", ".join(split_addresses(v))

    @model_validator(mode="after")
    def _check_addresses(self):
        if self.from_address and not _EMAIL_RE.match(self.from_address):
            raise ValueError(f"Invalid from address: {self.from_address}")
        for addr in split_addresses(self.to_addresses):
            if not _EMAIL_RE.match(addr):
                raise ValueError(f"Invalid recipient address: {addr}")
        if self.enabled:
            missing = [
                label for label, value in (
                    ("SMTP host", self.smtp_host),
                    ("from address", self.from_address),
                    ("recipient address", self.to_addresses),
                ) if not value
            ]
            if missing:
                raise ValueError(
                    "Reminders cannot be enabled without: " + ", ".join(missing)
                )
        return self


class EmailSettingsResponse(EmailSettingsBase):
    password_configured: bool
    last_sent_at: Optional[datetime] = None
    model_config = ConfigDict(from_attributes=True)

    @field_validator("last_sent_at")
    @classmethod
    def _mark_utc(cls, v: Optional[datetime]) -> Optional[datetime]:
        # Stored as naive UTC; tag it so clients convert to local time correctly.
        return v.replace(tzinfo=timezone.utc) if v and v.tzinfo is None else v


class SendReminderResponse(BaseModel):
    sent: bool
    item_count: int
    recipients: list[str]
    message: str
