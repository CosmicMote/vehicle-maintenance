import io
import os
import sqlite3
import tempfile
from pathlib import Path

from fastapi import APIRouter, HTTPException, UploadFile
from fastapi.responses import StreamingResponse

from ..database import engine

router = APIRouter(prefix="/api/admin", tags=["admin"])

SQLITE_MAGIC = b"SQLite format 3\x00"


def _db_path() -> Path:
    """Extract the filesystem path from the SQLAlchemy engine URL."""
    return Path(engine.url.database)


@router.get("/db/export")
def export_db():
    """Stream a consistent backup of the SQLite database as a file download."""
    db_path = _db_path()
    if not db_path.exists():
        raise HTTPException(status_code=404, detail="Database file not found")

    # Use sqlite3 backup API for a consistent snapshot while the DB may be in use.
    # Note: sqlite3 context managers do NOT close connections on exit (they only
    # commit/rollback), so we close explicitly — required on Windows to release
    # the file lock before reading and deleting the temp file.
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
        data = Path(tmp_path).read_bytes()
    finally:
        Path(tmp_path).unlink(missing_ok=True)

    return StreamingResponse(
        io.BytesIO(data),
        media_type="application/octet-stream",
        headers={"Content-Disposition": "attachment; filename=vehicle_maintenance.db"},
    )


@router.post("/db/import", status_code=204)
async def import_db(file: UploadFile):
    """Replace the current database with an uploaded SQLite file."""
    contents = await file.read()

    if not contents.startswith(SQLITE_MAGIC):
        raise HTTPException(
            status_code=400, detail="Uploaded file is not a valid SQLite database"
        )

    db_path = _db_path()
    tmp_path = db_path.with_suffix(".db.tmp")
    try:
        tmp_path.write_bytes(contents)
        engine.dispose()          # Close all pooled connections before replacing the file
        os.replace(tmp_path, db_path)  # Atomic replace
    except Exception as exc:
        tmp_path.unlink(missing_ok=True)
        raise HTTPException(status_code=500, detail=f"Import failed: {exc}")
