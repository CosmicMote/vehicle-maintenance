# ── Stage 1: Build Angular frontend ──────────────────────────────────────────
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ── Stage 2: Python runtime ───────────────────────────────────────────────────
FROM python:3.13-slim
WORKDIR /app

# Install Python dependencies
COPY backend/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy backend source
COPY backend/ ./backend/

# Copy compiled Angular app (builder outputs to dist/frontend/browser/)
COPY --from=frontend-builder /app/frontend/dist/frontend/browser ./frontend-dist/

# Volume mount point for the SQLite database
RUN mkdir -p /data

EXPOSE 8000

ENV DATABASE_URL=sqlite:////data/vehicle_maintenance.db

CMD ["uvicorn", "backend.main:app", "--host", "0.0.0.0", "--port", "8000"]
