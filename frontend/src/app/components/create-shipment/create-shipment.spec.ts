import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {NEVER, of, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {CreateShipment} from './create-shipment';
import {UserService} from '../../services/user.service';
import {ShipmentService} from '../../services/shipment.service';

describe('CreateShipment pickup address', () => {
  const address = {
    country: 'Magyarország', postalCode: '1211', city: 'Budapest',
    street: 'Teszt utca', houseNumber: '12', door: null
  };

  async function setup(response = of({id: 11, address})) {
    const userService = {getCurrentUser: vi.fn(() => response)};
    const shipmentService = {createShipment: vi.fn(() => NEVER)};
    await TestBed.configureTestingModule({
      imports: [CreateShipment],
      providers: [
        provideRouter([]),
        {provide: UserService, useValue: userService},
        {provide: ShipmentService, useValue: shipmentService}
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(CreateShipment);
    fixture.detectChanges();
    return {fixture, component: fixture.componentInstance, userService, shipmentService};
  }

  it('prefills the saved address and shows its summary instead of inputs', async () => {
    const {fixture, component} = await setup();
    expect(component.shipmentForm.controls.pickupAddress.getRawValue()).toEqual({...address, door: ''});
    expect(fixture.nativeElement.querySelector('address').textContent).toContain('Teszt utca');
    expect(fixture.nativeElement.querySelector('#pickupAddress-country')).toBeNull();
    expect(fixture.nativeElement.querySelector('#deliveryAddress-country')).not.toBeNull();
  });

  it('opens the editor and restores the saved address when requested', async () => {
    const {fixture, component} = await setup();
    const button = [...fixture.nativeElement.querySelectorAll('button')]
      .find((button: any) => button.textContent.includes('Másik felvételi cím')) as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#pickupAddress-country')).not.toBeNull();
    component.shipmentForm.controls.pickupAddress.controls.street.setValue('Másik utca');
    component.useSavedPickupAddress();
    expect(component.shipmentForm.controls.pickupAddress.controls.street.value).toBe('Teszt utca');
    expect(component.editingPickupAddress()).toBe(false);
    expect(address.street).toBe('Teszt utca');
  });

  it('opens editable fields when no saved address exists', async () => {
    const {fixture, component} = await setup(of({id: 11, address: null}) as any);
    expect(component.editingPickupAddress()).toBe(true);
    expect(fixture.nativeElement.querySelector('#pickupAddress-country')).not.toBeNull();
    expect(component.shipmentForm.controls.pickupAddress.invalid).toBe(true);
  });

  it('blocks submission after a profile loading error and allows retry', async () => {
    const {component, userService, shipmentService} = await setup(throwError(() => ({status: 500})));
    expect(component.profileLoadError()).not.toBe('');
    component.submit();
    expect(shipmentService.createShipment).not.toHaveBeenCalled();
    userService.getCurrentUser.mockReturnValue(of({id: 11, address}));
    component.loadUserProfile();
    expect(component.profileLoadError()).toBe('');
    expect(component.savedPickupAddress()?.street).toBe('Teszt utca');
  });

  it('sends the edited pickup address and prevents duplicate submissions', async () => {
    const {component, shipmentService} = await setup();
    component.editingPickupAddress.set(true);
    component.shipmentForm.patchValue({
      receiverName: 'Teszt Címzett',
      pickupAddress: {street: 'Másik utca'},
      deliveryAddress: {...address, door: ''},
      parcels: [{weight: 1}]
    });
    component.submit();
    component.submit();
    expect(shipmentService.createShipment).toHaveBeenCalledTimes(1);
    expect(shipmentService.createShipment.mock.calls[0]).toEqual([expect.objectContaining({
      senderId: 11,
      pickupAddress: {...address, street: 'Másik utca', door: ''}
    })]);
    expect(component.savedPickupAddress()?.street).toBe('Teszt utca');
  });
});
