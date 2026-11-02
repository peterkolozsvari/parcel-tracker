import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, provideRouter} from '@angular/router';
import {of, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {ShipmentManagement} from './shipment-management';
import {ShipmentService} from '../../services/shipment.service';

describe('ShipmentManagement', () => {
  async function setup(role: 'ADMIN' | 'COURIER') {
    const page = {content: [], number: 0, size: 10, totalElements: 0, totalPages: 0};
    const service = {getAllShipments: vi.fn(() => of(page)), getAssignedShipments: vi.fn(() => of(page))};
    await TestBed.configureTestingModule({imports: [ShipmentManagement], providers: [provideRouter([]),
      {provide: ActivatedRoute, useValue: {snapshot: {data: {role}}}},
      {provide: ShipmentService, useValue: service}]
    }).compileComponents();
    const fixture = TestBed.createComponent(ShipmentManagement);
    fixture.detectChanges();
    return {fixture, component: fixture.componentInstance, service};
  }

  it('admin loads the global list', async () => {
    const {fixture, service} = await setup('ADMIN');
    expect(service.getAllShipments).toHaveBeenCalledWith(0, '');
    expect(service.getAssignedShipments).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Küldeménykezelés');
  });

  it('courier loads only assigned shipments and can filter', async () => {
    const {fixture, service} = await setup('COURIER');
    expect(service.getAssignedShipments).toHaveBeenCalledWith(0, '');
    expect(service.getAllShipments).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Még nincs hozzád rendelt');
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    select.value = 'IN_TRANSIT'; select.dispatchEvent(new Event('change'));
    expect(service.getAssignedShipments).toHaveBeenLastCalledWith(0, 'IN_TRANSIT');
  });

  it('shows forbidden access instead of an empty list', async () => {
    const {fixture, component, service} = await setup('ADMIN');
    service.getAllShipments.mockReturnValue(throwError(() => ({status: 403})));
    component.loadShipments(); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Nincs jogosultságod');
  });
});
