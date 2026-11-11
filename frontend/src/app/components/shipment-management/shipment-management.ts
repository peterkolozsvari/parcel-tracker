import {TranslatePipe} from '../../i18n/translate.pipe';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Subscription} from 'rxjs';
import {ShipmentService} from '../../services/shipment.service';
import {SHIPMENT_STATUS_LABELS, ShipmentListItem, ShipmentStatus} from '../../models/shipment.model';
import {PageResponse} from '../../models/page-response.model';

@Component({
  selector: 'app-shipment-management',
  imports: [TranslatePipe, DatePipe, RouterLink],
  templateUrl: './shipment-management.html',
  styleUrl: '../my-shipments/my-shipments.css'
})
export class ShipmentManagement implements OnInit {
  private readonly service = inject(ShipmentService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly isAdmin = inject(ActivatedRoute).snapshot.data['role'] === 'ADMIN';
  readonly title = this.isAdmin ? 'Küldeménykezelés' : 'Futárfeladatok';
  readonly statusLabels = SHIPMENT_STATUS_LABELS;
  readonly statusOptions = Object.entries(this.statusLabels);
  readonly selectedStatus = signal<ShipmentStatus | ''>('');
  readonly shipments = signal<PageResponse<ShipmentListItem> | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  private request?: Subscription;

  ngOnInit(): void { this.loadShipments(); }

  changeStatus(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value !== '' && !Object.hasOwn(this.statusLabels, value)) return;
    this.selectedStatus.set(value as ShipmentStatus | '');
    this.loadShipments();
  }

  loadShipments(page = 0): void {
    if (page < 0) return;
    this.request?.unsubscribe();
    this.loading.set(true);
    this.errorMessage.set('');
    const request = this.isAdmin
      ? this.service.getAllShipments(page, this.selectedStatus())
      : this.service.getAssignedShipments(page, this.selectedStatus());
    this.request = request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => { this.shipments.set(result); this.loading.set(false); },
      error: error => {
        this.shipments.set(null);
        this.loading.set(false);
        this.errorMessage.set(error.status === 403 ? 'Nincs jogosultságod ehhez a listához.'
          : 'A küldemények betöltése nem sikerült. Próbáld újra.');
        if (error.status === 401) this.router.navigate(['/login']);
      }
    });
  }
}
