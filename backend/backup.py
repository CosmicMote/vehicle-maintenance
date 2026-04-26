"""
Dropbox backup module.

Configuration is via environment variables (see .env.example):
  DROPBOX_APP_KEY         — Dropbox app key
  DROPBOX_APP_SECRET      — Dropbox app secret
  DROPBOX_REFRESH_TOKEN   — OAuth2 offline refresh token (from scripts/setup_dropbox.py)
  DROPBOX_BACKUP_PATH     — Destination path inside Dropbox  (default: /vehicle-maintenance/vehicle_maintenance.db)
  BACKUP_INTERVAL_HOURS   — How often to run the backup      (default: 24)
"""

import logging
import os
import sqlite3
import tempfile
from pathlib import Path

logger = logging.getLogger("uvicorn.error")

DROPBOX_APP_KEY = os.environ.get("DROPBOX_APP_KEY", "")
DROPBOX_APP_SECRET = os.environ.get("DROPBOX_APP_SECRET", "")
DROPBOX_REFRESH_TOKEN = os.environ.get("DROPBOX_REFRESH_TOKEN", "")
DROPBOX_BACKUP_PATH = os.environ.get(
    "DROPBOX_BACKUP_PATH", "/vehicle-maintenance/vehicle_maintenance.db"
)
BACKUP_INTERVAL_HOURS = float(os.environ.get("BACKUP_INTERVAL_HOURS", "24"))


def is_configured() -> bool:
    """Return True when all required Dropbox env vars are present."""
    return bool(DROPBOX_APP_KEY and DROPBOX_APP_SECRET and DROPBOX_REFRESH_TOKEN)


def _db_path() -> Path:
    from .database import engine
    return Path(engine.url.database)


def _create_backup_bytes() -> bytes:
    """Return a consistent binary snapshot of the live SQLite database."""
    db_path = _db_path()
    tmp_fd, tmp_path = tempfile.mkstemp(suffix=".db")
    os.close(tmp_fd)
    try:
        src = sqlite3.connect(str(db_path))
        dst = sqlite3.connect(tmp_path)
        try:
            src.backup(dst)
        finally:
            dst.close()
            src.close()
        return Path(tmp_path).read_bytes()
    finally:
        Path(tmp_path).unlink(missing_ok=True)


def run_backup() -> None:
    """Create a database snapshot and upload it to Dropbox. Safe to call from a background thread."""
    if not is_configured():
        logger.debug("Dropbox backup skipped — DROPBOX_* env vars not set.")
        return

    logger.info("Starting Dropbox backup...")
    try:
        import dropbox
        from dropbox.files import WriteMode

        dbx = dropbox.Dropbox(
            app_key=DROPBOX_APP_KEY,
            app_secret=DROPBOX_APP_SECRET,
            oauth2_refresh_token=DROPBOX_REFRESH_TOKEN,
        )

        data = _create_backup_bytes()
        dbx.files_upload(data, DROPBOX_BACKUP_PATH, mode=WriteMode.overwrite)
        logger.info(
            "Dropbox backup complete: %s (%d bytes)", DROPBOX_BACKUP_PATH, len(data)
        )
    except Exception:
        logger.exception("Dropbox backup failed")
