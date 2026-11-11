import {TranslatePipe} from '../../i18n/translate.pipe';
import {Component, computed, DestroyRef, effect, inject, input, output, signal, untracked} from '@angular/core';
import {FormControl, ReactiveFormsModule, Validators} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Router} from '@angular/router';
import {Observable} from 'rxjs';
import {AuthService} from '../../services/auth.service';
import {ShipmentService} from '../../services/shipment.service';
import {UserService} from '../../services/user.service';
import {ShipmentDetails, ShipmentStatus} from '../../models/shipment.model';
import {CourierListItem} from '../../models/user.model';

@Component({
  selector: 'app-shipment-actions',
  imports: [TranslatePipe, ReactiveFormsModule],
  templateUrl: './shipment-actions.html',
  styles: [`
    :host { display: block; }
    .actions-panel { border: 1px solid var(--border); border-radius: 12px; padding: 1.5rem; margin-bottom: 1.5rem; }
    h2 { font-size: 1.2rem; margin-bottom: 1rem; }
    .action-row { display: flex; flex-wrap: wrap; align-items: end; gap: .75rem; }
    .courier-select { flex: 1; min-width: min(100%, 240px); }
    .action-button { min-height: 44px; border: 1px solid var(--accent); border-radius: 8px; padding: .6rem 1rem; background: var(--accent); color: var(--on-accent); font-weight: 600; }
    .action-button:disabled { opacity: .5; }
    .cancel-button { background: transparent; border-color: #a12b20; color: #a12b20; }
    .error-message { color: #b42318; }
    button:focus-visible { outline: 3px solid var(--accent); outline-offset: 3px; }
  `]
})
export class ShipmentActions {
  readonly shipment = input.required<ShipmentDetails>();
  readonly shipmentUpdated = output<void>();
  private readonly auth = inject(AuthService);
  private readonly service = inject(ShipmentService);
  private readonly users = inject(UserService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  readonly isAdmin = computed(() => this.auth.currentUser()?.role === 'ADMIN');
  readonly couriers = signal<CourierListItem[]>([]);
  readonly couriersLoading = signal(false);
  readonly couriersError = signal('');
  readonly busy = signal(false);
  readonly errorMessage = signal('');
  readonly courierId = new FormControl<number | null>(null, Validators.required);
  readonly description = new FormControl('', {nonNullable: true, validators: Validators.maxLength(500)});
  readonly canAssign = computed(() => this.isAdmin() && this.shipment().status === 'REGISTERED' && !this.shipment().courier);
  readonly nextStatuses = computed<ShipmentStatus[]>(() => {
    const item = this.shipment();
    const user = this.auth.currentUser();
    const isAssigned = user?.role === 'COURIER' && item.courier?.id === user.id;
    if (!this.isAdmin() && !isAssigned) return [];
    switch (item.status) {
      case 'REGISTERED': return this.isAdmin() ? ['CANCELLED'] : [];
      case 'COURIER_ASSIGNED': return this.isAdmin() ? ['PICKED_UP', 'CANCELLED'] : ['PICKED_UP'];
      case 'PICKED_UP': return ['IN_TRANSIT'];
      case 'IN_TRANSIT': return ['DELIVERED', 'DELIVERY_FAILED'];
      case 'DELIVERY_FAILED': return ['IN_TRANSIT'];
      default: return [];
    }
  });
  readonly actionLabels: Partial<Record<ShipmentStatus, string>> = {
    PICKED_UP: 'Csomag felvéve', IN_TRANSIT: 'Szállítás megkezdése', DELIVERED: 'Kézbesítve',
    DELIVERY_FAILED: 'Sikertelen kézbesítés', CANCELLED: 'Küldemény törlése'
  };

  constructor() {
    effect(() => {
      if (this.canAssign()) untracked(() => this.loadCouriers());
    });
  }

  loadCouriers(): void {
    if (!this.canAssign() || this.couriersLoading()) return;
    this.couriersLoading.set(true);
    this.couriersError.set('');
    this.users.getActiveCouriers().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: couriers => { this.couriers.set(couriers); this.courierId.reset(); this.couriersLoading.set(false); },
      error: error => {
        this.couriersLoading.set(false);
        this.couriersError.set('A futárok betöltése nem sikerült.');
        if (error.status === 401) this.router.navigate(['/login']);
      }
    });
  }

  assignCourier(): void {
    if (!this.canAssign() || this.busy() || this.courierId.invalid || this.couriersLoading()) return;
    const courierId = this.courierId.value;
    if (courierId === null || !this.couriers().some(courier => courier.id === courierId)) return;
    this.run(this.service.assignCourier(this.shipment().id, courierId));
  }

  updateStatus(status: ShipmentStatus): void {
    if (this.busy() || this.description.invalid || !this.nextStatuses().includes(status)) return;
    this.run(this.service.updateStatus(this.shipment().id, status, this.description.value));
  }

  private run(request: Observable<void>): void {
    this.busy.set(true);
    this.errorMessage.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.busy.set(false); this.shipmentUpdated.emit(); },
      error: error => {
        this.busy.set(false);
        this.errorMessage.set(error.status === 409 ? 'A küldemény állapota közben megváltozott. Frissítsd az oldalt.'
          : error.status === 403 ? 'Nincs jogosultságod ehhez a művelethez.'
          : error.status === 400 ? 'A művelet adatai nem érvényesek. Ellenőrizd a kiválasztott futárt és a leírást.'
          : 'A módosítás nem sikerült. Próbáld újra később.');
        if (error.status === 401) this.router.navigate(['/login']);
      }
    });
  }
}
