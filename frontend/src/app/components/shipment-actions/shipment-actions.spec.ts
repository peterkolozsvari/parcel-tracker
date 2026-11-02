import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {provideRouter} from '@angular/router';
import {NEVER, of, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {ShipmentActions} from './shipment-actions';
import {AuthService} from '../../services/auth.service';
import {ShipmentService} from '../../services/shipment.service';
import {UserService} from '../../services/user.service';
import {Role} from '../../models/user.model';
import {ShipmentDetails, ShipmentStatus} from '../../models/shipment.model';

describe('ShipmentActions', () => {
  const user = {id: 7, fullName: 'Teszt Futár', email: 'courier@test.hu', phoneNumber: null,
    role: 'COURIER' as const, active: true};
  const address = {country: 'HU', postalCode: '1211', city: 'Budapest', street: 'Teszt', houseNumber: '1'};
  const shipment: ShipmentDetails = {id: 2, trackingNumber: 'TEST123456', status: 'REGISTERED', sender: user,
    courier: null, receiverName: 'Címzett', pickupAddress: address, deliveryAddress: address,
    parcels: [], createdAt: '', updatedAt: ''};

  async function setup(role: Role, status: ShipmentStatus = 'REGISTERED', courierId: number | null = null) {
    const currentUser = signal<{id: number; role: Role} | null>({id: role === 'ADMIN' ? 1 : 7, role});
    const service = {assignCourier: vi.fn(() => of(void 0)), updateStatus: vi.fn(() => of(void 0))};
    const users = {getActiveCouriers: vi.fn(() => of([{id: 7, fullName: 'Teszt Futár'}]))};
    await TestBed.configureTestingModule({imports: [ShipmentActions], providers: [provideRouter([]),
      {provide: AuthService, useValue: {currentUser}},
      {provide: ShipmentService, useValue: service}, {provide: UserService, useValue: users}]
    }).compileComponents();
    const fixture = TestBed.createComponent(ShipmentActions);
    fixture.componentRef.setInput('shipment', {...shipment, status, courier: courierId === null ? null : {...user, id: courierId}});
    fixture.detectChanges();
    return {fixture, component: fixture.componentInstance, service, users, currentUser};
  }

  it('admin selects an active courier and emits refresh after assignment', async () => {
    const {component, service} = await setup('ADMIN');
    const updated = vi.fn();
    component.shipmentUpdated.subscribe(updated);
    component.courierId.setValue(7);
    component.assignCourier();
    expect(service.assignCourier).toHaveBeenCalledWith(2, 7);
    expect(updated).toHaveBeenCalledOnce();
  });

  it('does not assign an ID outside the loaded courier list', async () => {
    const {component, service} = await setup('ADMIN');
    component.courierId.setValue(999);
    component.assignCourier();
    expect(service.assignCourier).not.toHaveBeenCalled();
  });

  it('loads couriers when session restoration reveals the admin role later', async () => {
    const {fixture, users, currentUser} = await setup('CUSTOMER');
    expect(users.getActiveCouriers).not.toHaveBeenCalled();
    currentUser.set({id: 1, role: 'ADMIN'});
    fixture.detectChanges();
    expect(users.getActiveCouriers).toHaveBeenCalledOnce();
  });

  it('hides all management actions from a customer', async () => {
    const {fixture, component, users} = await setup('CUSTOMER');
    expect(fixture.nativeElement.querySelector('section')).toBeNull();
    component.updateStatus('CANCELLED');
    expect(component.nextStatuses()).toEqual([]);
    expect(users.getActiveCouriers).not.toHaveBeenCalled();
  });

  it('assigned courier can pick up but cannot cancel', async () => {
    const {component, service} = await setup('COURIER', 'COURIER_ASSIGNED', 7);
    expect(component.nextStatuses()).toEqual(['PICKED_UP']);
    component.updateStatus('CANCELLED');
    expect(service.updateStatus).not.toHaveBeenCalled();
    component.description.setValue('Átvettem');
    component.updateStatus('PICKED_UP');
    expect(service.updateStatus).toHaveBeenCalledWith(2, 'PICKED_UP', 'Átvettem');
  });

  it('another courier has no actions', async () => {
    const {component} = await setup('COURIER', 'COURIER_ASSIGNED', 99);
    expect(component.nextStatuses()).toEqual([]);
  });

  it.each(['DELIVERED', 'CANCELLED'] as ShipmentStatus[])('has no actions for terminal status %s', async status => {
    const {component} = await setup('ADMIN', status, 7);
    expect(component.nextStatuses()).toEqual([]);
    expect(component.canAssign()).toBe(false);
  });

  it('offers retry after failed delivery', async () => {
    const {component} = await setup('COURIER', 'DELIVERY_FAILED', 7);
    expect(component.nextStatuses()).toEqual(['IN_TRANSIT']);
  });

  it('prevents duplicate updates while a request is pending', async () => {
    const {component, service} = await setup('COURIER', 'COURIER_ASSIGNED', 7);
    service.updateStatus.mockReturnValue(NEVER);
    component.updateStatus('PICKED_UP');
    component.updateStatus('PICKED_UP');
    expect(service.updateStatus).toHaveBeenCalledOnce();
    expect(component.busy()).toBe(true);
  });

  it('shows conflict without announcing a successful update', async () => {
    const {component, service} = await setup('COURIER', 'COURIER_ASSIGNED', 7);
    const updated = vi.fn();
    component.shipmentUpdated.subscribe(updated);
    service.updateStatus.mockReturnValue(throwError(() => ({status: 409})));
    component.updateStatus('PICKED_UP');
    expect(component.errorMessage()).toContain('közben megváltozott');
    expect(updated).not.toHaveBeenCalled();
    expect(component.busy()).toBe(false);
  });

  it('blocks descriptions longer than 500 characters', async () => {
    const {component, service} = await setup('COURIER', 'COURIER_ASSIGNED', 7);
    component.description.setValue('x'.repeat(501));
    component.updateStatus('PICKED_UP');
    expect(service.updateStatus).not.toHaveBeenCalled();
  });
});
