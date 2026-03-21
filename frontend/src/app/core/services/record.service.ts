import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MaintenanceRecord, MaintenanceRecordCreate } from '../models/maintenance-record.model';

@Injectable({ providedIn: 'root' })
export class RecordService {
  private http = inject(HttpClient);

  private base(vehicleId: number) {
    return `/api/vehicles/${vehicleId}/records`;
  }

  list(vehicleId: number): Observable<MaintenanceRecord[]> {
    return this.http.get<MaintenanceRecord[]>(this.base(vehicleId));
  }

  create(vehicleId: number, payload: MaintenanceRecordCreate): Observable<MaintenanceRecord> {
    return this.http.post<MaintenanceRecord>(this.base(vehicleId), payload);
  }

  delete(vehicleId: number, recordId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(vehicleId)}/${recordId}`);
  }
}
