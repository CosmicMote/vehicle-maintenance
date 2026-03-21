import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DueStatusResponse } from '../models/due-status.model';

@Injectable({ providedIn: 'root' })
export class StatusService {
  private http = inject(HttpClient);

  getStatus(vehicleId: number, currentMiles?: number): Observable<DueStatusResponse> {
    let params = new HttpParams();
    if (currentMiles !== undefined && currentMiles !== null) {
      params = params.set('current_miles', currentMiles.toString());
    }
    return this.http.get<DueStatusResponse>(`/api/vehicles/${vehicleId}/status`, { params });
  }
}
