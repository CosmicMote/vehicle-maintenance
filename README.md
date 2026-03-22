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

## Project Structure

```
vehicle-maintenance/
├── backend/
│   ├── main.py                  # FastAPI app entry point
│   ├── database.py              # SQLAlchemy engine and session
│   ├── models.py                # ORM models (Vehicle, MaintenanceType, MaintenanceRecord)
│   ├── schemas.py               # Pydantic request/response schemas
│   ├── routers/
│   │   ├── vehicles.py          # GET/POST/PUT/DELETE /api/vehicles
│   │   ├── maintenance_types.py # CRUD for maintenance types per vehicle
│   │   ├── records.py           # CRUD for service records per vehicle
│   │   └── status.py            # GET /api/vehicles/{id}/status
│   └── services/
│       └── due_calculator.py    # Due/overdue business logic
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
