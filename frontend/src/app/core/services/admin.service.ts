import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

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
}
