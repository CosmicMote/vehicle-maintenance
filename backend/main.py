import asyncio
import logging
import os
from contextlib import asynccontextmanager
from pathlib import Path

from dotenv import load_dotenv
load_dotenv()  # Must run before any module-level os.environ.get() calls

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse

from .backup import BACKUP_INTERVAL_HOURS, is_configured, run_backup
from .database import create_tables, run_migrations
from .routers import admin, maintenance_types, mileage, records, status, vehicles

logger = logging.getLogger("uvicorn.error")


async def _backup_loop() -> None:
    """Run backups on a fixed interval using asyncio — more reliable than a daemon thread."""
    logger.info(
        "Dropbox backup scheduler started — running every %g hour(s).",
        BACKUP_INTERVAL_HOURS,
    )
    while True:
        await asyncio.sleep(BACKUP_INTERVAL_HOURS * 3600)
        try:
            await asyncio.to_thread(run_backup)
        except Exception:
            # run_backup already logs its own exceptions; this outer guard
            # ensures the loop never exits due to an unexpected error.
            logger.exception("Unexpected error in backup loop — continuing.")


@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
    run_migrations()

    task: asyncio.Task | None = None
    if is_configured():
        task = asyncio.create_task(_backup_loop())
    else:
        logger.info("Dropbox backup not configured — scheduler not started.")

    yield

    if task is not None:
        task.cancel()
        try:
            await task
        except asyncio.CancelledError:
            pass


app = FastAPI(title="Vehicle Maintenance API", lifespan=lifespan)

cors_origins = os.environ.get("CORS_ORIGINS", "http://localhost:4200").split(",")
app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(admin.router)
app.include_router(vehicles.router)
app.include_router(maintenance_types.router)
app.include_router(mileage.router)
app.include_router(records.router)
app.include_router(status.router)

# Serve Angular frontend when static assets are present (i.e. inside Docker).
_STATIC_DIR = Path(__file__).parent.parent / "frontend-dist"
if _STATIC_DIR.is_dir():
    @app.get("/{full_path:path}", include_in_schema=False)
    async def spa_fallback(full_path: str):
        candidate = _STATIC_DIR / full_path
        if candidate.is_file():
            return FileResponse(candidate)
        return FileResponse(_STATIC_DIR / "index.html")
