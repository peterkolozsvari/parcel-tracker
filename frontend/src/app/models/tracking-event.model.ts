import {ShipmentStatus} from './shipment.model';

export interface TrackingEvent {
  id: number;
  status: ShipmentStatus;
  description: string | null;
  occurredAt: string;
}
