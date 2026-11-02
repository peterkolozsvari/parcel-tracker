import {inject, Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, switchMap} from 'rxjs';
import {CreateShipment, CreateShipmentResponse, ShipmentDetails, ShipmentListItem, ShipmentStatus} from '../models/shipment.model';
import {PageResponse} from '../models/page-response.model';
import {TrackingEvent} from '../models/tracking-event.model';
import {API_ENDPOINTS} from '../config/api.config';
import {CsrfService} from './csrf.service';

const BASE_URL = API_ENDPOINTS.shipments;

@Injectable({
  providedIn: 'root',
})
export class ShipmentService {

  private readonly http = inject(HttpClient);
  private readonly csrfService = inject(CsrfService);

  getAllShipments(page = 0, status: ShipmentStatus | '' = ''): Observable<PageResponse<ShipmentListItem>> {
    return this.getShipmentPage(BASE_URL, page, status);
  }

  getAssignedShipments(page = 0, status: ShipmentStatus | '' = ''): Observable<PageResponse<ShipmentListItem>> {
    return this.getShipmentPage(`${BASE_URL}/assigned`, page, status);
  }

  private getShipmentPage(url: string, page: number, status: ShipmentStatus | ''): Observable<PageResponse<ShipmentListItem>> {
    let params = new HttpParams().set('page', page).set('size', 10);
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<ShipmentListItem>>(url, {params, withCredentials: true});
  }

  assignCourier(shipmentId: number, courierId: number): Observable<void> {
    return this.patchShipment(`${BASE_URL}/${shipmentId}/courier`, {courierId});
  }

  updateStatus(shipmentId: number, status: ShipmentStatus, description = ''): Observable<void> {
    return this.patchShipment(`${BASE_URL}/${shipmentId}/status`, {status, description});
  }

  private patchShipment(url: string, command: object): Observable<void> {
    return this.csrfService.getToken().pipe(switchMap(csrf => this.http.patch<void>(url, command, {
      withCredentials: true,
      headers: {[csrf.headerName]: csrf.token}
    })));
  }

  getTrackingEvents(shipmentId: number): Observable<TrackingEvent[]> {
    return this.http.get<TrackingEvent[]>(`${BASE_URL}/${shipmentId}/tracking-events`, {
      withCredentials: true
    });
  }

  getShipmentById(id: number): Observable<ShipmentDetails> {
    return this.http.get<ShipmentDetails>(`${BASE_URL}/${id}`, {withCredentials: true});
  }

  getMyShipments(page = 0, status: ShipmentStatus | '' = ''): Observable<PageResponse<ShipmentListItem>> {
    let params = new HttpParams().set('page', page).set('size', 10);
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PageResponse<ShipmentListItem>>(`${BASE_URL}/my`, {
      params,
      withCredentials: true
    });
  }

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

