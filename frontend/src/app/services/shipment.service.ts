import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {CreateShipment, CreateShipmentResponse} from '../models/shipment.model';
import {Observable} from 'rxjs';
import {API_ENDPOINTS} from '../config/api.config';

const BASE_URL = API_ENDPOINTS.shipments;

@Injectable({
  providedIn: 'root',
})
export class ShipmentService {

  private readonly http = inject(HttpClient);

  createShipment(
    command: CreateShipment): Observable<CreateShipmentResponse> {
    return this.http.post<CreateShipmentResponse>(BASE_URL, command);
  }
}
