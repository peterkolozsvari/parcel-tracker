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
