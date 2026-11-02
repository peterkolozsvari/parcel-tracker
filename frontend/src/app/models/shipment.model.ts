import { CreateAddress } from './address.model';
import { CreateParcel } from './parcel.model';

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

