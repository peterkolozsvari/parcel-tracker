export interface CreateAddress {
  country: string;
  postalCode: string;
  city: string;
  street: string;
  houseNumber: string;
  door?: string | null;
}
