"""
Maintenance reminder emails.

Settings live in the email_settings table (edited from the Admin page), except
the SMTP password, which comes from the environment:
  SMTP_PASSWORD — password (or Gmail app password) for the SMTP account

A background loop in main.py calls run_scheduled_check() every
CHECK_INTERVAL_SECONDS; it sends one consolidated email when reminders are
enabled, the configured frequency has elapsed since the last email, and at
least one item matches the configured threshold.
"""

import logging
import os
import smtplib
import ssl
from dataclasses import dataclass
from datetime import date, datetime, timedelta, timezone
from email.message import EmailMessage
from html import escape
from typing import Optional

from sqlalchemy.orm import Session

from ..database import SessionLocal
from ..models import EmailSettings, Vehicle
from ..schemas import DueStatusItem, DueStatusResponse, split_addresses
from .vehicle_status import get_vehicle_status

logger = logging.getLogger("uvicorn.error")

SMTP_PASSWORD = os.environ.get("SMTP_PASSWORD", "")

CHECK_INTERVAL_SECONDS = 3600

# Statuses included in the email for each threshold setting. These are the same
# statuses shown in the Status Report (see services/due_calculator.py).
THRESHOLD_STATUSES = {
    "overdue": {"overdue"},
    "due": {"overdue", "due"},
}

# Badge colors match the Status Report's status-badge component.
_BADGE_STYLES = {
    "ok": ("#e8f5e9", "#2e7d32", "OK"),
    "upcoming": ("#fff8e1", "#f57f17", "Upcoming"),
    "due": ("#fff3e0", "#e65100", "Due"),
    "overdue": ("#ffebee", "#c62828", "Overdue"),
    "never_performed": ("#eeeeee", "#616161", "Never Done"),
}


@dataclass
class VehicleReminder:
    vehicle: Vehicle
    status: DueStatusResponse
    items: list[DueStatusItem]


class ReminderError(Exception):
    """Raised when a reminder cannot be sent due to configuration or SMTP problems."""


def get_settings(db: Session) -> EmailSettings:
    """Return the singleton settings row, creating it with defaults if missing."""
    settings = db.get(EmailSettings, 1)
    if settings is None:
        settings = EmailSettings(id=1)
        db.add(settings)
        db.commit()
        db.refresh(settings)
    return settings


def password_configured() -> bool:
    return bool(SMTP_PASSWORD)


def collect_reminders(db: Session, threshold: str) -> list[VehicleReminder]:
    """Return each vehicle's items matching the threshold, omitting vehicles with none."""
    statuses = THRESHOLD_STATUSES[threshold]
    reminders = []
    for vehicle in db.query(Vehicle).order_by(Vehicle.name).all():
        status = get_vehicle_status(db, vehicle)
        items = sorted(
            (i for i in status.items if i.status in statuses),
            key=lambda i: i.miles_until_due,
        )
        if items:
            reminders.append(VehicleReminder(vehicle, status, items))
    return reminders


# --- Rendering ---

def _fmt_date(d: date) -> str:
    """Match Angular's 'mediumDate' format, e.g. "Oct 31, 2026"."""
    return f"{d:%b} {d.day}, {d.year}"


def _days_until(d: date) -> int:
    return (d - date.today()).days


def _vehicle_subtitle(v: Vehicle) -> str:
    return " ".join(str(p) for p in (v.year, v.make, v.model) if p)


def _count_by_status(reminders: list[VehicleReminder]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for r in reminders:
        for item in r.items:
            counts[item.status] = counts.get(item.status, 0) + 1
    return counts


def build_subject(reminders: list[VehicleReminder]) -> str:
    counts = _count_by_status(reminders)
    parts = []
    if counts.get("overdue"):
        parts.append(f"{counts['overdue']} overdue")
    if counts.get("due"):
        parts.append(f"{counts['due']} due soon")
    return "Vehicle maintenance reminder: " + ", ".join(parts)


def _until_due_html(item: DueStatusItem) -> str:
    """Mirror the Status Report's "Miles and Days Until Due" column."""
    overdue = 'style="color:#d32f2f;font-weight:500"'
    if item.miles_until_due <= 0:
        miles = f'<span {overdue}>{item.miles_until_due:,} mi overdue</span>'
    else:
        miles = f"{item.miles_until_due:,} mi"
    if not (item.interval_months and item.next_due_date):
        return miles
    days = _days_until(item.next_due_date)
    if days <= 0:
        days_html = f'<span {overdue}>{days:,} days overdue</span>'
    else:
        days_html = f"{days:,} days"
    return f"{miles}, {days_html}"


def _item_row_html(item: DueStatusItem) -> str:
    bg, fg, label = _BADGE_STYLES[item.status]
    badge = (
        f'<span style="display:inline-block;padding:3px 10px;border-radius:12px;'
        f'font-size:12px;font-weight:500;text-transform:uppercase;letter-spacing:0.5px;'
        f'background:{bg};color:{fg}">{label}</span>'
    )
    if item.last_performed_date:
        last_done = f"{_fmt_date(item.last_performed_date)} @ {item.last_performed_miles:,} mi"
    else:
        last_done = "—"
    if item.next_due_miles is not None:
        next_due = f"{item.next_due_miles:,} mi"
        if item.next_due_date:
            next_due += f" or {_fmt_date(item.next_due_date)}"
    else:
        next_due = "—"

    td = 'style="padding:10px 12px;border-bottom:1px solid #e0e0e0;font-size:14px"'
    return (
        "<tr>"
        f"<td {td}>{badge}</td>"
        f"<td {td}>{escape(item.name)}</td>"
        f"<td {td}>{last_done}</td>"
        f"<td {td}>{next_due}</td>"
        f"<td {td}>{_until_due_html(item)}</td>"
        "</tr>"
    )


def _vehicle_section_html(r: VehicleReminder) -> str:
    subtitle = _vehicle_subtitle(r.vehicle)
    subtitle_html = (
        f' <span style="color:#757575;font-size:14px;font-weight:400">{escape(subtitle)}</span>'
        if subtitle else ""
    )
    estimate_html = (
        f'<p style="color:#757575;font-style:italic;margin:0 0 12px">'
        f"Estimated current mileage: {r.status.current_miles:,} mi (based on avg miles/year)</p>"
        if r.status.estimated_miles_used else ""
    )
    th = (
        'style="text-align:left;padding:10px 12px;border-bottom:1px solid #e0e0e0;'
        'font-size:12px;font-weight:500;color:#616161"'
    )
    headers = "".join(
        f"<th {th}>{h}</th>"
        for h in ("Status", "Type", "Last Done", "Next Due", "Miles and Days Until Due")
    )
    rows = "".join(_item_row_html(i) for i in r.items)
    return (
        f'<h2 style="font-size:20px;font-weight:500;margin:28px 0 8px">'
        f"{escape(r.vehicle.name)}{subtitle_html}</h2>"
        f"{estimate_html}"
        '<table role="presentation" cellspacing="0" cellpadding="0" '
        'style="width:100%;border-collapse:collapse;background:#ffffff">'
        f"<thead><tr>{headers}</tr></thead><tbody>{rows}</tbody></table>"
    )


def _wrap_html(title: str, body: str) -> str:
    return (
        "<!doctype html><html><head><meta charset=\"utf-8\">"
        f"<title>{escape(title)}</title></head>"
        '<body style="margin:0;padding:24px;background:#f5f5f5;'
        "font-family:Roboto,'Helvetica Neue',Arial,sans-serif;color:#212121\">"
        '<div style="max-width:900px;margin:0 auto;background:#ffffff;padding:24px;border-radius:8px">'
        f"{body}</div></body></html>"
    )


def render_email_html(reminders: list[VehicleReminder], threshold: str) -> str:
    scope = "overdue" if threshold == "overdue" else "overdue or due soon"
    total = sum(len(r.items) for r in reminders)
    intro = (
        '<h1 style="font-size:24px;font-weight:500;margin:0 0 8px">Vehicle Maintenance Reminder</h1>'
        f'<p style="margin:0;color:#424242">{total} maintenance item{"s are" if total != 1 else " is"} '
        f"{scope} as of {_fmt_date(date.today())}.</p>"
    )
    sections = "".join(_vehicle_section_html(r) for r in reminders)
    return _wrap_html(build_subject(reminders), intro + sections)


def render_empty_preview_html(threshold: str) -> str:
    scope = "overdue" if threshold == "overdue" else "overdue or due soon"
    return _wrap_html(
        "No reminder to send",
        '<h1 style="font-size:24px;font-weight:500;margin:0 0 8px">No reminder to send</h1>'
        f'<p style="margin:0;color:#424242">No maintenance items are currently {scope}, '
        "so no reminder email would be sent.</p>",
    )


def render_email_text(reminders: list[VehicleReminder]) -> str:
    """Plain-text alternative for clients that don't render HTML."""
    lines = [build_subject(reminders), ""]
    for r in reminders:
        subtitle = _vehicle_subtitle(r.vehicle)
        lines.append(f"{r.vehicle.name}" + (f" ({subtitle})" if subtitle else ""))
        for item in r.items:
            label = _BADGE_STYLES[item.status][2].upper()
            detail = f"{item.miles_until_due:,} mi"
            if item.interval_months and item.next_due_date:
                detail += f", {_days_until(item.next_due_date):,} days"
            lines.append(f"  [{label}] {item.name} — {detail} until due")
        lines.append("")
    return "\n".join(lines)


# --- Sending ---

def _send_email(settings: EmailSettings, recipients: list[str], subject: str, html: str, text: str) -> None:
    msg = EmailMessage()
    msg["Subject"] = subject
    msg["From"] = settings.from_address
    msg["To"] = ", ".join(recipients)
    msg.set_content(text)
    msg.add_alternative(html, subtype="html")

    try:
        if settings.smtp_security == "ssl":
            server = smtplib.SMTP_SSL(
                settings.smtp_host, settings.smtp_port,
                context=ssl.create_default_context(), timeout=30,
            )
        else:
            server = smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=30)
        with server:
            if settings.smtp_security == "starttls":
                server.starttls(context=ssl.create_default_context())
            if settings.smtp_username:
                server.login(settings.smtp_username, SMTP_PASSWORD)
            server.send_message(msg)
    except (smtplib.SMTPException, OSError) as exc:
        raise ReminderError(f"Failed to send email: {exc}") from exc


def send_reminder(db: Session, settings: EmailSettings) -> tuple[bool, int, list[str]]:
    """
    Send the consolidated reminder email if any items match the threshold.
    Returns (sent, item_count, recipients). Raises ReminderError on failure.
    Does not update last_sent_at; callers decide whether a send affects the schedule.
    """
    recipients = split_addresses(settings.to_addresses)
    if not settings.smtp_host or not settings.from_address or not recipients:
        raise ReminderError("SMTP host, from address and at least one recipient are required.")
    if settings.smtp_username and not SMTP_PASSWORD:
        raise ReminderError("SMTP_PASSWORD environment variable is not set.")

    reminders = collect_reminders(db, settings.threshold)
    item_count = sum(len(r.items) for r in reminders)
    if not reminders:
        return False, 0, recipients

    _send_email(
        settings,
        recipients,
        build_subject(reminders),
        render_email_html(reminders, settings.threshold),
        render_email_text(reminders),
    )
    return True, item_count, recipients


def _utcnow() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)


def is_send_due(settings: EmailSettings, now: Optional[datetime] = None) -> bool:
    if not settings.enabled:
        return False
    if settings.last_sent_at is None:
        return True
    now = now or _utcnow()
    # Allow one check interval of slack so a daily email sent at 9:05 doesn't
    # drift to 10:05 the next day when the hourly check lands a few seconds early.
    period = timedelta(days=settings.frequency_days) - timedelta(seconds=CHECK_INTERVAL_SECONDS)
    return now - settings.last_sent_at >= period


def run_scheduled_check() -> None:
    """Send a reminder if one is due. Safe to call from a background thread."""
    db = SessionLocal()
    try:
        settings = get_settings(db)
        if not is_send_due(settings):
            return
        sent, item_count, recipients = send_reminder(db, settings)
        if sent:
            settings.last_sent_at = _utcnow()
            db.commit()
            logger.info(
                "Sent maintenance reminder (%d item(s)) to %s", item_count, ", ".join(recipients)
            )
        else:
            logger.debug("Maintenance reminder check: no items match threshold.")
    except ReminderError as exc:
        logger.error("Maintenance reminder failed: %s", exc)
    except Exception:
        logger.exception("Maintenance reminder check failed")
    finally:
        db.close()
