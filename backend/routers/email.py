from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import HTMLResponse
from sqlalchemy.orm import Session

from ..database import get_db
from ..schemas import EmailSettingsResponse, EmailSettingsUpdate, SendReminderResponse
from ..services import reminders

router = APIRouter(prefix="/api/admin/email", tags=["admin"])


def _to_response(settings) -> EmailSettingsResponse:
    return EmailSettingsResponse.model_validate(
        {
            **{c.name: getattr(settings, c.name) for c in settings.__table__.columns},
            "password_configured": reminders.password_configured(),
        }
    )


@router.get("/settings", response_model=EmailSettingsResponse)
def get_email_settings(db: Session = Depends(get_db)):
    return _to_response(reminders.get_settings(db))


@router.put("/settings", response_model=EmailSettingsResponse)
def update_email_settings(payload: EmailSettingsUpdate, db: Session = Depends(get_db)):
    settings = reminders.get_settings(db)
    for field, value in payload.model_dump().items():
        setattr(settings, field, value)
    db.commit()
    db.refresh(settings)
    return _to_response(settings)


@router.get("/preview", response_class=HTMLResponse)
def preview_email(db: Session = Depends(get_db)):
    """Render the reminder email that would be sent now, using the saved threshold."""
    settings = reminders.get_settings(db)
    items = reminders.collect_reminders(db, settings.threshold)
    if not items:
        return HTMLResponse(reminders.render_empty_preview_html(settings.threshold))
    return HTMLResponse(reminders.render_email_html(items, settings.threshold))


@router.post("/send", response_model=SendReminderResponse)
def send_email_now(db: Session = Depends(get_db)):
    """
    Run the reminder logic immediately (for testing). Sends only if items match the
    threshold. Works even when scheduled reminders are disabled, and does not reset
    the reminder schedule.
    """
    settings = reminders.get_settings(db)
    try:
        sent, item_count, recipients = reminders.send_reminder(db, settings)
    except reminders.ReminderError as exc:
        raise HTTPException(status_code=502, detail=str(exc))

    if sent:
        message = f"Reminder sent to {', '.join(recipients)} ({item_count} item{'s' if item_count != 1 else ''})."
    else:
        message = "No maintenance items match the reminder threshold — no email sent."
    return SendReminderResponse(sent=sent, item_count=item_count, recipients=recipients, message=message)
