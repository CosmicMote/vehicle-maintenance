export interface MaintenanceType {
  id: number;
  vehicle_id: number;
  name: string;
  interval_miles: number;
  interval_months: number | null;
  notes: string | null;
}

export interface MaintenanceTypeCreate {
  name: string;
  interval_miles: number;
  interval_months?: number | null;
  notes?: string | null;
}

export type MaintenanceTypeUpdate = MaintenanceTypeCreate;
