# Vehicle Maintenance

A web application for tracking vehicle maintenance schedules. Add vehicles, define maintenance types with mileage intervals, log service history, and get a report of what's due or overdue.

## Prerequisites

### Local Development
- Python 3.13+
- Node.js 18+

### Docker
- Docker Desktop (or Docker Engine + Docker Compose on Linux)

---

## Running with Docker (Recommended)

Docker builds the frontend, bundles everything into a single image, and persists the database in a named volume — no local Python or Node installation required.

### Build and run with Docker Compose

```bash
docker compose up --build
```

The app will be available at `http://localhost:8000`.

To run in the background:

```bash
docker compose up --build -d
```

To stop:

```bash
docker compose down
```

### Build the image standalone (without Compose)

```bash
docker build -t vehicle-maintenance .
```

Run the standalone image:

```bash
docker run -p 8000:8000 -v vehicle_maintenance_data:/data vehicle-maintenance
```

### Database persistence

The SQLite database is stored in a Docker named volume (`vehicle_maintenance_data`). It persists across container restarts and rebuilds. To start fresh, remove the volume:

```bash
docker volume rm vehicle-maintenance_vehicle_maintenance_data
```

---

## Running Locally (Development)

### Running the Backend

From the project root:

```bash
venv/Scripts/uvicorn backend.main:app --reload
```

The API will be available at `http://localhost:8000`.
Interactive API docs (Swagger UI): `http://localhost:8000/docs`

The SQLite database (`vehicle_maintenance.db`) is created automatically on first run.

## Running the Frontend

```bash
cd frontend
npm install   # first time only
npm start
```

The app will be available at `http://localhost:4200`.

The dev server proxies all `/api` requests to the backend at `localhost:8000`, so both servers must be running.

---

---

## Dropbox Backups

The app can automatically back up the SQLite database to Dropbox on a configurable schedule. Backups are disabled by default and only activate when the required environment variables are set.

### One-time setup

**1. Create a Dropbox app**

Go to [https://www.dropbox.com/developers/apps](https://www.dropbox.com/developers/apps) and create a new app:
- Access type: **Full Dropbox** (or App folder for isolation)
- Under the **Permissions** tab, enable: `files.content.write`

Note the **App key** and **App secret** from the Settings tab.

**2. Obtain an OAuth2 refresh token**

From the project root, run the interactive setup script:

```bash
python scripts/setup_dropbox.py
```

Follow the prompts — it will open a Dropbox authorization URL, then print the three environment variables to add to your configuration.

**3. Configure the environment variables**

Copy `.env.example` to `.env` and fill in the Dropbox values:

```bash
DROPBOX_APP_KEY=your_app_key
DROPBOX_APP_SECRET=your_app_secret
DROPBOX_REFRESH_TOKEN=your_refresh_token

# Optional — defaults shown
DROPBOX_BACKUP_PATH=/vehicle-maintenance/vehicle_maintenance.db
BACKUP_INTERVAL_HOURS=24
```

`BACKUP_INTERVAL_HOURS` accepts fractional values (e.g. `0.5` for every 30 minutes).

### Docker

Pass the variables via a `.env` file or directly in `docker-compose.yml` under `environment:`. The backup runs inside the container on the configured schedule.

### How it works

- On startup, if the `DROPBOX_*` variables are present the scheduler starts automatically and logs the configured interval.
- Each backup creates a consistent snapshot of the live database using SQLite's backup API, then uploads it to the configured Dropbox path with overwrite mode.
- Dropbox's built-in version history retains previous backups, so older copies are recoverable from the Dropbox web interface even though only one file is kept at the destination path.
- If a backup fails (network error, expired token, etc.) the error is logged and the app continues running — a failed backup never crashes the server.

---

## Project Structure

```
vehicle-maintenance/
├── backend/
│   ├── main.py                  # FastAPI app entry point + scheduler lifecycle
│   ├── database.py              # SQLAlchemy engine and session
│   ├── models.py                # ORM models (Vehicle, MaintenanceType, MaintenanceRecord)
│   ├── schemas.py               # Pydantic request/response schemas
│   ├── backup.py                # Dropbox backup logic
│   ├── routers/
│   │   ├── vehicles.py          # GET/POST/PUT/DELETE /api/vehicles
│   │   ├── maintenance_types.py # CRUD for maintenance types per vehicle
│   │   ├── records.py           # CRUD for service records per vehicle
│   │   ├── mileage.py           # CRUD for mileage records per vehicle
│   │   ├── admin.py             # Database export/import endpoints
│   │   └── status.py            # GET /api/vehicles/{id}/status
│   └── services/
│       └── due_calculator.py    # Due/overdue business logic
├── scripts/
│   └── setup_dropbox.py         # One-time Dropbox OAuth2 setup helper
├── frontend/
│   └── src/app/
│       ├── features/
│       │   ├── vehicles/        # Vehicle list, form dialog, detail view
│       │   ├── maintenance-types/ # Maintenance type list and form
│       │   ├── records/         # Service record list and log form
│       │   └── status/          # Status report with odometer input
│       ├── core/
│       │   ├── models/          # TypeScript interfaces
│       │   ├── services/        # HTTP services
│       │   └── interceptors/    # Global error handler
│       └── shared/
│           └── components/      # StatusBadge, ConfirmDialog
├── Dockerfile                   # Multi-stage Docker build
├── docker-compose.yml           # Compose file with persistent volume
├── .dockerignore
├── venv/                        # Python virtual environment (local dev only)
└── vehicle_maintenance.db       # SQLite database (local dev only; Docker uses a volume)
```

## Features

- **Vehicles** — Add and manage multiple vehicles with make, model, year, and average annual mileage.
- **Maintenance Types** — Define maintenance tasks per vehicle (e.g. Oil Change every 5,000 miles).
- **Service Records** — Log when maintenance was performed and at what odometer reading.
- **Status Report** — Enter a current odometer reading (or let the app estimate from average annual mileage) to see which maintenance items are OK, upcoming, due, or overdue.

### Status Thresholds

| Status | Condition |
|--------|-----------|
| Overdue | Past the due mileage |
| Due | Within 500 miles of due |
| Upcoming | Within 1,000 miles of due |
| OK | More than 1,000 miles remaining |
| Never Done | No service record exists yet |
