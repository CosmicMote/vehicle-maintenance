import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EmailSettings, EmailSettingsUpdate, SendReminderResult } from '../models/email-settings.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  exportDb(): void {
    this.http.get('/api/admin/db/export', { responseType: 'blob' }).subscribe(blob => {
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'vehicle_maintenance.db';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    });
  }

  importDb(file: File): Observable<void> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<void>('/api/admin/db/import', formData);
  }

  getEmailSettings(): Observable<EmailSettings> {
    return this.http.get<EmailSettings>('/api/admin/email/settings');
  }

  updateEmailSettings(payload: EmailSettingsUpdate): Observable<EmailSettings> {
    return this.http.put<EmailSettings>('/api/admin/email/settings', payload);
  }

  sendReminderNow(): Observable<SendReminderResult> {
    return this.http.post<SendReminderResult>('/api/admin/email/send', {});
  }

  /** Opens the rendered reminder email in a new tab (resolved against <base href> for BASE_PATH). */
  openEmailPreview(): void {
    window.open(new URL('api/admin/email/preview', document.baseURI).href, '_blank');
  }
}
