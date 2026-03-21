import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { MaintenanceType, MaintenanceTypeCreate, MaintenanceTypeUpdate } from '../models/maintenance-type.model';

@Injectable({ providedIn: 'root' })
export class MaintenanceTypeService {
  private http = inject(HttpClient);

  private base(vehicleId: number) {
    return `/api/vehicles/${vehicleId}/maintenance-types`;
  }

  list(vehicleId: number): Observable<MaintenanceType[]> {
    return this.http.get<MaintenanceType[]>(this.base(vehicleId));
  }

  create(vehicleId: number, payload: MaintenanceTypeCreate): Observable<MaintenanceType> {
    return this.http.post<MaintenanceType>(this.base(vehicleId), payload);
  }

  update(vehicleId: number, typeId: number, payload: MaintenanceTypeUpdate): Observable<MaintenanceType> {
    return this.http.put<MaintenanceType>(`${this.base(vehicleId)}/${typeId}`, payload);
  }

  delete(vehicleId: number, typeId: number): Observable<void> {
    return this.http.delete<void>(`${this.base(vehicleId)}/${typeId}`);
  }
}
