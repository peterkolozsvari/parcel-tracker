import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, switchMap} from 'rxjs';
import {CreateShipment, CreateShipmentResponse} from '../models/shipment.model';
import {API_ENDPOINTS} from '../config/api.config';
import {CsrfService} from './csrf.service';

const BASE_URL = API_ENDPOINTS.shipments;

@Injectable({
  providedIn: 'root',
})
export class ShipmentService {

  private readonly http = inject(HttpClient);
  private readonly csrfService = inject(CsrfService);

  createShipment(
    command: CreateShipment
  ): Observable<CreateShipmentResponse> {
    return this.csrfService.getToken().pipe(
      switchMap(csrf =>
        this.http.post<CreateShipmentResponse>(
          BASE_URL,
          command,
          {
            withCredentials: true,
            headers: {
              [csrf.headerName]: csrf.token
            }
          }
        )
      )
    );
  }
}
