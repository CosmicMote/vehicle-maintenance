"""
Dropbox backup module.

Configuration is via environment variables (see .env.example):
  DROPBOX_APP_KEY             — Dropbox app key
  DROPBOX_APP_SECRET          — Dropbox app secret
  DROPBOX_REFRESH_TOKEN       — OAuth2 offline refresh token (from scripts/setup_dropbox.py)
  DROPBOX_BACKUP_PATH         — Destination path inside Dropbox      (default: /vehicle-maintenance/vehicle-maintenance.db)
                                Each backup is uploaded alongside this path with a
                                timestamp appended to the filename, e.g.
                                vehicle-maintenance-2026-09-26_14-30-00.db
  DROPBOX_BACKUP_RETENTION_COUNT — Number of timestamped backups to keep (default: 5)
                                Older backups beyond this count are deleted after each
                                successful upload. Set to 0 to disable pruning.
  BACKUP_INTERVAL_HOURS       — How often to run the backup          (default: 24)
"""

import logging
import os
import sqlite3
import tempfile
from datetime import datetime
from pathlib import Path, PurePosixPath

logger = logging.getLogger("uvicorn.error")

DROPBOX_APP_KEY = os.environ.get("DROPBOX_APP_KEY", "")
DROPBOX_APP_SECRET = os.environ.get("DROPBOX_APP_SECRET", "")
DROPBOX_REFRESH_TOKEN = os.environ.get("DROPBOX_REFRESH_TOKEN", "")
DROPBOX_BACKUP_PATH = os.environ.get(
    "DROPBOX_BACKUP_PATH", "/vehicle-maintenance/vehicle-maintenance.db"
)
DROPBOX_BACKUP_RETENTION_COUNT = int(
    os.environ.get("DROPBOX_BACKUP_RETENTION_COUNT", "5")
)
BACKUP_INTERVAL_HOURS = float(os.environ.get("BACKUP_INTERVAL_HOURS", "24"))


def is_configured() -> bool:
    """Return True when all required Dropbox env vars are present."""
    return bool(DROPBOX_APP_KEY and DROPBOX_APP_SECRET and DROPBOX_REFRESH_TOKEN)


def _backup_folder_and_stem() -> tuple[str, str, str]:
    """Split DROPBOX_BACKUP_PATH into (folder, filename stem, extension)."""
    p = PurePosixPath(DROPBOX_BACKUP_PATH)
    folder = str(p.parent)
    if folder == "/":
        folder = ""
    return folder, p.stem, p.suffix


def _timestamped_backup_path() -> str:
    folder, stem, ext = _backup_folder_and_stem()
    timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
    return f"{folder}/{stem}-{timestamp}{ext}"


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

        dest_path = _timestamped_backup_path()
        data = _create_backup_bytes()
        dbx.files_upload(data, dest_path, mode=WriteMode.add)
        logger.info("Dropbox backup complete: %s (%d bytes)", dest_path, len(data))

        _prune_old_backups(dbx)
    except Exception:
        logger.exception("Dropbox backup failed")


def _prune_old_backups(dbx) -> None:
    """Delete old timestamped backups beyond DROPBOX_BACKUP_RETENTION_COUNT."""
    if DROPBOX_BACKUP_RETENTION_COUNT <= 0:
        return

    import dropbox

    folder, stem, ext = _backup_folder_and_stem()
    prefix = f"{stem}-"

    entries = []
    result = dbx.files_list_folder(folder)
    while True:
        entries.extend(
            entry
            for entry in result.entries
            if isinstance(entry, dropbox.files.FileMetadata)
            and entry.name.startswith(prefix)
            and entry.name.endswith(ext)
        )
        if not result.has_more:
            break
        result = dbx.files_list_folder_continue(result.cursor)

    # Timestamp format sorts lexicographically in chronological order.
    entries.sort(key=lambda entry: entry.name, reverse=True)

    for stale in entries[DROPBOX_BACKUP_RETENTION_COUNT:]:
        try:
            dbx.files_delete_v2(stale.path_lower)
            logger.info("Deleted old Dropbox backup: %s", stale.path_display)
        except Exception:
            logger.exception("Failed to delete old Dropbox backup: %s", stale.path_display)
