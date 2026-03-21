export interface MaintenanceType {
  id: number;
  vehicle_id: number;
  name: string;
  interval_miles: number;
}

export interface MaintenanceTypeCreate {
  name: string;
  interval_miles: number;
}

export type MaintenanceTypeUpdate = MaintenanceTypeCreate;
