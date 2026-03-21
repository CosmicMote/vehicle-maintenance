export type DueStatus = 'ok' | 'upcoming' | 'due' | 'overdue' | 'never_performed';

export interface DueStatusItem {
  maintenance_type_id: number;
  name: string;
  interval_miles: number;
  last_performed_miles: number | null;
  last_performed_date: string | null;
  next_due_miles: number | null;
  current_miles: number;
  miles_until_due: number;
  status: DueStatus;
  estimated_miles_used: boolean;
}

export interface DueStatusResponse {
  vehicle_id: number;
  current_miles: number;
  estimated_miles_used: boolean;
  items: DueStatusItem[];
}
