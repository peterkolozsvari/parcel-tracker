import { CreateAddress } from './address.model';
import { CreateParcel, ParcelDetails } from './parcel.model';
import { UserDetails } from './user.model';

export interface CreateShipment {
  senderId: number;
  pickupAddress: CreateAddress;
  receiverName: string;
  deliveryAddress: CreateAddress;
  parcels: CreateParcel[];
}

export interface CreateShipmentResponse {
  id: number;
  trackingNumber: string;
}

export type ShipmentStatus =
  | 'REGISTERED'
  | 'COURIER_ASSIGNED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'DELIVERED'
  | 'DELIVERY_FAILED'
  | 'CANCELLED';

export interface ShipmentListItem {
  id: number;
  trackingNumber: string;
  receiverName: string;
  status: ShipmentStatus;
  createdAt: string;
}

export interface ShipmentDetails {
  id: number;
  trackingNumber: string;
  status: ShipmentStatus;
  sender: UserDetails;
  courier: UserDetails | null;
  receiverName: string;
  pickupAddress: CreateAddress;
  deliveryAddress: CreateAddress;
  parcels: ParcelDetails[];
  createdAt: string;
  updatedAt: string;
}

export const SHIPMENT_STATUS_LABELS: Record<ShipmentStatus, string> = {
  REGISTERED: 'Rögzítve',
  COURIER_ASSIGNED: 'Futár hozzárendelve',
  PICKED_UP: 'Felvéve',
  IN_TRANSIT: 'Szállítás alatt',
  DELIVERED: 'Kézbesítve',
  DELIVERY_FAILED: 'Sikertelen kézbesítés',
  CANCELLED: 'Törölve'
};

