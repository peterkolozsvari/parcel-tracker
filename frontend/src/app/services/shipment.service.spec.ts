import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {ShipmentService} from './shipment.service';
import {API_ENDPOINTS} from '../config/api.config';
import {UserService} from './user.service';

describe('ShipmentService own shipments', () => {
  let service: ShipmentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    service = TestBed.inject(ShipmentService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('includes the session cookie when loading a user profile by ID', () => {
    TestBed.inject(UserService).getUserById(1).subscribe();
    const request = http.expectOne(`${API_ENDPOINTS.users}/1`);
    expect(request.request.withCredentials).toBe(true);
    request.flush({id: 1});
  });

  it('loads the active courier directory with the session cookie', () => {
    TestBed.inject(UserService).getActiveCouriers().subscribe();
    const request = http.expectOne(`${API_ENDPOINTS.users}/couriers`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush([]);
  });

  it('loads separate admin and courier endpoints without a client supplied courier ID', () => {
    service.getAllShipments(1, 'REGISTERED').subscribe();
    let request = http.expectOne(req => req.url === API_ENDPOINTS.shipments);
    expect(request.request.params.get('status')).toBe('REGISTERED');
    expect(request.request.withCredentials).toBe(true);
    request.flush({content: []});
    service.getAssignedShipments().subscribe();
    request = http.expectOne(req => req.url === `${API_ENDPOINTS.shipments}/assigned`);
    expect(request.request.params.has('courierId')).toBe(false);
    expect(request.request.withCredentials).toBe(true);
    request.flush({content: []});
  });

  it('assigns courier with CSRF token and session cookie', () => {
    service.assignCourier(2, 7).subscribe();
    http.expectOne(API_ENDPOINTS.csrf).flush({token: 'test-token', headerName: 'X-CSRF-TOKEN'});
    const request = http.expectOne(`${API_ENDPOINTS.shipments}/2/courier`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('test-token');
    expect(request.request.body).toEqual({courierId: 7});
    request.flush(null);
  });

  it('updates status with CSRF token and optional description', () => {
    service.updateStatus(2, 'PICKED_UP', 'Átvettem').subscribe();
    http.expectOne(API_ENDPOINTS.csrf).flush({token: 'test-token', headerName: 'X-CSRF-TOKEN'});
    const request = http.expectOne(`${API_ENDPOINTS.shipments}/2/status`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('test-token');
    expect(request.request.body).toEqual({status: 'PICKED_UP', description: 'Átvettem'});
    request.flush(null);
  });

  it('loads tracking events with the session cookie', () => {
    service.getTrackingEvents(25).subscribe();
    const request = http.expectOne(`${API_ENDPOINTS.shipments}/25/tracking-events`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush([]);
  });

  it('loads shipment details with the session cookie', () => {
    service.getShipmentById(25).subscribe();
    const request = http.expectOne(`${API_ENDPOINTS.shipments}/25`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush({id: 25});
  });

  it('sends the session cookie and omits sender and status when unfiltered', () => {
    service.getMyShipments().subscribe();
    const request = http.expectOne(req => req.url === `${API_ENDPOINTS.shipments}/my`);
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('10');
    expect(request.request.params.has('status')).toBe(false);
    expect(request.request.params.has('senderId')).toBe(false);
    request.flush({content: [], number: 0, size: 10, totalElements: 0, totalPages: 0});
  });

  it('sends the selected status and zero based page', () => {
    service.getMyShipments(1, 'DELIVERED').subscribe();
    const request = http.expectOne(req => req.url === `${API_ENDPOINTS.shipments}/my`);
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('status')).toBe('DELIVERED');
    request.flush({content: [], number: 1, size: 10, totalElements: 11, totalPages: 2});
  });
});
