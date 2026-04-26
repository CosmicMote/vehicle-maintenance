export interface MileageRecord {
  id: number;
  vehicle_id: number;
  recorded_date: string;
  miles: number;
  notes: string | null;
  created_at: string;
}

export interface MileageRecordCreate {
  recorded_date: string;
  miles: number;
  notes?: string | null;
}
