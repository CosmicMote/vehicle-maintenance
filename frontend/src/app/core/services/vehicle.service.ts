import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Vehicle, VehicleCreate, VehicleUpdate } from '../models/vehicle.model';

@Injectable({ providedIn: 'root' })
export class VehicleService {
  private http = inject(HttpClient);
  private base = '/api/vehicles';

  list(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>(this.base);
  }

  get(id: number): Observable<Vehicle> {
    return this.http.get<Vehicle>(`${this.base}/${id}`);
  }

  create(payload: VehicleCreate): Observable<Vehicle> {
    return this.http.post<Vehicle>(this.base, payload);
  }

  update(id: number, payload: VehicleUpdate): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${this.base}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
