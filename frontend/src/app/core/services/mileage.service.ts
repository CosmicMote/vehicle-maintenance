import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MileageRecord, MileageRecordCreate } from '../models/mileage-record.model';

@Injectable({ providedIn: 'root' })
export class MileageService {
  private http = inject(HttpClient);

  private base(vehicleId: number) {
    return `/api/vehicles/${vehicleId}/mileage`;
  }

  list(vehicleId: number): Observable<MileageRecord[]> {
    return this.http.get<MileageRecord[]>(this.base(vehicleId));
  }

  create(vehicleId: number, payload: MileageRecordCreate): Observable<MileageRecord> {
    return this.http.post<MileageRecord>(this.base(vehicleId), payload);
  }

  update(vehicleId: number, recordId: number, payload: MileageRecordCreate): Observable<MileageRecord> {
    return this.http.put<MileageRecord>(`${this.base(vehicleId)}/${recordId}`, payload);
  }

  delete(vehicleId: number, recordId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(vehicleId)}/${recordId}`);
  }
}
