export interface MaintenanceRecord {
  id: number;
  vehicle_id: number;
  maintenance_type_id: number;
  maintenance_type_name: string | null;
  performed_date: string;
  performed_miles: number;
  notes: string | null;
  created_at: string;
}

export interface MaintenanceRecordCreate {
  maintenance_type_id: number;
  performed_date: string;
  performed_miles: number;
  notes?: string | null;
}
