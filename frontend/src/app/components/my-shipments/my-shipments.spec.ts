import {TestBed} from '@angular/core/testing';
import {provideRouter, Router} from '@angular/router';
import {Observable, of, Subject, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {MyShipments} from './my-shipments';
import {ShipmentService} from '../../services/shipment.service';
import {PageResponse} from '../../models/page-response.model';
import {ShipmentListItem, ShipmentStatus} from '../../models/shipment.model';

describe('MyShipments', () => {
  const page: PageResponse<ShipmentListItem> = {
    content: [{id: 1, trackingNumber: 'TEST123456', receiverName: 'Teszt Címzett',
      status: 'REGISTERED', createdAt: '2026-11-02T10:00:00Z'}],
    number: 0, size: 10, totalElements: 11, totalPages: 2
  };

  async function setup(response: Observable<PageResponse<ShipmentListItem>> = of(page)) {
    const service = {getMyShipments: vi.fn((_page: number, _status: ShipmentStatus | '') => response)};
    await TestBed.configureTestingModule({
      imports: [MyShipments],
      providers: [provideRouter([]), {provide: ShipmentService, useValue: service}]
    }).compileComponents();
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(MyShipments);
    fixture.detectChanges();
    return {fixture, component: fixture.componentInstance, service, navigate};
  }

  it('shows shipments, Hungarian status labels and the first page controls', async () => {
    const {fixture, service} = await setup();
    expect(service.getMyShipments).toHaveBeenCalledWith(0, '');
    const view: HTMLElement = fixture.nativeElement;
    expect(view.textContent).toContain('TEST123456');
    expect(view.textContent).toContain('Teszt Címzett');
    expect(view.textContent).toContain('Rögzítve');
    expect(view.querySelector<HTMLButtonElement>('.pagination-bar button')?.disabled).toBe(true);
    const next = [...view.querySelectorAll<HTMLButtonElement>('.pagination-bar button')][1];
    next.click();
    expect(service.getMyShipments).toHaveBeenLastCalledWith(1, '');
  });

  it('resets to the first page when filtering by status', async () => {
    const {fixture, component, service} = await setup();
    component.loadShipments(1);
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    select.value = 'DELIVERED';
    select.dispatchEvent(new Event('change'));
    expect(service.getMyShipments).toHaveBeenLastCalledWith(0, 'DELIVERED');
    expect(component.selectedStatus()).toBe('DELIVERED');
  });

  it('shows an empty state without pagination', async () => {
    const {fixture} = await setup(of({...page, content: [], totalElements: 0, totalPages: 0}));
    expect(fixture.nativeElement.textContent).toContain('Még nincs feladott küldeményed.');
    expect(fixture.nativeElement.querySelector('.pagination-bar')).toBeNull();
  });

  it('distinguishes an empty filter result from a user without shipments', async () => {
    const {fixture, component, service} = await setup();
    service.getMyShipments.mockReturnValue(of({...page, content: [], totalElements: 0, totalPages: 0}));
    component.selectedStatus.set('DELIVERED');
    component.loadShipments();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nincs ilyen státuszú küldeményed.');
  });

  it('shows loading, cancels an older request and keeps the latest result', async () => {
    const oldRequest = new Subject<PageResponse<ShipmentListItem>>();
    const {fixture, component, service} = await setup(oldRequest);
    expect(fixture.nativeElement.textContent).toContain('Küldemények betöltése');
    service.getMyShipments.mockReturnValue(of(page));
    component.loadShipments();
    oldRequest.next({...page, content: []});
    expect(component.shipments()?.content[0].trackingNumber).toBe('TEST123456');
    expect(component.loading()).toBe(false);
  });

  it('shows an error and can retry successfully', async () => {
    const {fixture, component, service} = await setup(throwError(() => ({status: 500})));
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    service.getMyShipments.mockReturnValue(of(page));
    const retry: HTMLButtonElement = fixture.nativeElement.querySelector('button');
    retry.click();
    expect(component.errorMessage()).toBe('');
    expect(component.shipments()?.content.length).toBe(1);
  });

  it('redirects to login when the session has expired', async () => {
    const {navigate} = await setup(throwError(() => ({status: 401})));
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});
