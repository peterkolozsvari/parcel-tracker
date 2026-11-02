import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {ShipmentService} from './shipment.service';
import {API_ENDPOINTS} from '../config/api.config';

describe('ShipmentService own shipments', () => {
  let service: ShipmentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    service = TestBed.inject(ShipmentService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

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
