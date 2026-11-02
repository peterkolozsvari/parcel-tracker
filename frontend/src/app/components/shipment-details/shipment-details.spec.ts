import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter, Router} from '@angular/router';
import {BehaviorSubject, Observable, of, Subject, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {ShipmentDetails} from './shipment-details';
import {ShipmentDetails as ShipmentDetailsModel} from '../../models/shipment.model';
import {ShipmentService} from '../../services/shipment.service';

describe('ShipmentDetails', () => {
  const address = {country: 'Magyarország', postalCode: '1211', city: 'Budapest',
    street: 'Teszt utca', houseNumber: '12', door: null};
  const details: ShipmentDetailsModel = {
    id: 1, trackingNumber: 'TEST123456', status: 'REGISTERED',
    sender: {id: 11, fullName: 'Teszt Feladó', email: 'test@example.test', phoneNumber: null,
      role: 'CUSTOMER', active: true}, courier: null, receiverName: 'Teszt Címzett',
    pickupAddress: address, deliveryAddress: {...address, city: 'Szeged', postalCode: '6720'},
    parcels: [{id: 3, weight: 1.5, description: null, fragile: true}],
    createdAt: '2026-11-02T10:00:00Z', updatedAt: '2026-11-02T10:00:00Z'
  };

  async function setup(response: Observable<ShipmentDetailsModel> = of(details), id = '1') {
    const params = new BehaviorSubject(convertToParamMap({id}));
    const service = {getShipmentById: vi.fn((_id: number) => response)};
    await TestBed.configureTestingModule({
      imports: [ShipmentDetails],
      providers: [provideRouter([]), {provide: ActivatedRoute, useValue: {paramMap: params}},
        {provide: ShipmentService, useValue: service}]
    }).compileComponents();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(ShipmentDetails);
    fixture.detectChanges();
    return {fixture, component: fixture.componentInstance, service, params, navigate};
  }

  it('shows receiver, both addresses, status and parcel information', async () => {
    const {fixture, service} = await setup();
    expect(service.getShipmentById).toHaveBeenCalledWith(1);
    const text = fixture.nativeElement.textContent;
    for (const value of ['TEST123456', 'Teszt Feladó', 'Teszt Címzett', 'Budapest', 'Szeged',
      'Rögzítve', '1.50 kg', 'Igen', 'Nincs megadva.']) {
      expect(text).toContain(value);
    }
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/my-shipments');
  });

  it('does not send invalid identifiers to the backend', async () => {
    const {fixture, service} = await setup(of(details), 'abc');
    expect(service.getShipmentById).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Érvénytelen küldeményazonosító.');
  });

  it.each([
    [403, 'Nincs jogosultságod'],
    [404, 'nem található']
  ])('shows the corresponding message for HTTP %s', async (status, message) => {
    const {fixture} = await setup(throwError(() => ({status})));
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(message);
    expect(fixture.nativeElement.querySelector('.summary-panel')).toBeNull();
  });

  it('redirects to login on an expired session', async () => {
    const {navigate} = await setup(throwError(() => ({status: 401})));
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('retries a failed request without leaving the page', async () => {
    const {fixture, component, service} = await setup(throwError(() => ({status: 500})));
    service.getShipmentById.mockReturnValue(of(details));
    const retry: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    retry.click();
    expect(component.shipment()).toEqual(details);
    expect(component.errorMessage()).toBe('');
    expect(service.getShipmentById).toHaveBeenCalledTimes(2);
  });

  it('loads a new route ID and ignores the previous request response', async () => {
    const previous = new Subject<ShipmentDetailsModel>();
    const {fixture, component, service, params} = await setup(previous);
    expect(fixture.nativeElement.textContent).toContain('Küldemény betöltése');
    service.getShipmentById.mockReturnValue(of({...details, id: 2, trackingNumber: 'SECOND1234'}));
    params.next(convertToParamMap({id: '2'}));
    previous.next(details);
    expect(component.shipment()?.trackingNumber).toBe('SECOND1234');
  });
});
