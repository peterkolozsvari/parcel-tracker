export interface CreateParcel {
  weight: number;
  description?: string | null;
  fragile: boolean;
}

export interface ParcelDetails {
  id: number;
  weight: number;
  description: string | null;
  fragile: boolean;
}

