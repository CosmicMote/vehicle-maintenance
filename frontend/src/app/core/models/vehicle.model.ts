export interface Vehicle {
  id: number;
  name: string;
  make: string | null;
  model: string | null;
  year: number | null;
  avg_miles_per_year: number | null;
  created_at: string;
}

export interface VehicleCreate {
  name: string;
  make?: string | null;
  model?: string | null;
  year?: number | null;
  avg_miles_per_year?: number | null;
}

export type VehicleUpdate = VehicleCreate;
