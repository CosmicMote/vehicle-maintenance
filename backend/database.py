from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

import os

DATABASE_URL = os.environ.get("DATABASE_URL", "sqlite:///./vehicle_maintenance.db")

engine = create_engine(
    DATABASE_URL, connect_args={"check_same_thread": False}
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def create_tables():
    Base.metadata.create_all(bind=engine)


def run_migrations():
    """Apply incremental schema changes that create_all cannot handle."""
    with engine.connect() as conn:
        existing = {
            row[1]
            for row in conn.execute(
                __import__("sqlalchemy").text("PRAGMA table_info(maintenance_types)")
            )
        }
        if "notes" not in existing:
            conn.execute(
                __import__("sqlalchemy").text(
                    "ALTER TABLE maintenance_types ADD COLUMN notes TEXT"
                )
            )
            conn.commit()
