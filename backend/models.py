from datetime import datetime

from sqlalchemy import Column, Date, DateTime, ForeignKey, Integer, String, Text, func
from sqlalchemy.orm import relationship

from .database import Base


class Vehicle(Base):
    __tablename__ = "vehicles"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, nullable=False)
    make = Column(String)
    model = Column(String)
    year = Column(Integer)
    avg_miles_per_year = Column(Integer)
    created_at = Column(DateTime, default=func.now())

    maintenance_types = relationship(
        "MaintenanceType", back_populates="vehicle", cascade="all, delete-orphan"
    )
    records = relationship(
        "MaintenanceRecord", back_populates="vehicle", cascade="all, delete-orphan"
    )


class MaintenanceType(Base):
    __tablename__ = "maintenance_types"

    id = Column(Integer, primary_key=True, autoincrement=True)
    vehicle_id = Column(Integer, ForeignKey("vehicles.id", ondelete="CASCADE"), nullable=False)
    name = Column(String, nullable=False)
    interval_miles = Column(Integer, nullable=False)
    notes = Column(Text)

    vehicle = relationship("Vehicle", back_populates="maintenance_types")
    records = relationship(
        "MaintenanceRecord", back_populates="maintenance_type", cascade="all, delete-orphan"
    )


class MaintenanceRecord(Base):
    __tablename__ = "maintenance_records"

    id = Column(Integer, primary_key=True, autoincrement=True)
    vehicle_id = Column(Integer, ForeignKey("vehicles.id", ondelete="CASCADE"), nullable=False)
    maintenance_type_id = Column(
        Integer, ForeignKey("maintenance_types.id", ondelete="CASCADE"), nullable=False
    )
    performed_date = Column(Date, nullable=False)
    performed_miles = Column(Integer, nullable=False)
    notes = Column(Text)
    created_at = Column(DateTime, default=func.now())

    vehicle = relationship("Vehicle", back_populates="records")
    maintenance_type = relationship("MaintenanceType", back_populates="records")
