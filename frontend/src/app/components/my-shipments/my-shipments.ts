import {TranslatePipe} from '../../i18n/translate.pipe';
import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Subscription} from 'rxjs';
import {ShipmentService} from '../../services/shipment.service';
import {SHIPMENT_STATUS_LABELS, ShipmentListItem, ShipmentStatus} from '../../models/shipment.model';
import {PageResponse} from '../../models/page-response.model';

@Component({
  selector: 'app-my-shipments',
  imports: [TranslatePipe, DatePipe, RouterLink],
  templateUrl: './my-shipments.html',
  styleUrl: './my-shipments.css'
})
export class MyShipments implements OnInit {
  private readonly shipmentService = inject(ShipmentService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;

  readonly shipments = signal<PageResponse<ShipmentListItem> | null>(null);
  readonly selectedStatus = signal<ShipmentStatus | ''>('');
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly statusLabels = SHIPMENT_STATUS_LABELS;
  readonly statusOptions = Object.entries(this.statusLabels);

  ngOnInit(): void {
    this.loadShipments();
  }

  changeStatus(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    if (value !== '' && !Object.hasOwn(this.statusLabels, value)) {
      return;
    }
    this.selectedStatus.set(value as ShipmentStatus | '');
    this.loadShipments();
  }

  loadShipments(page = 0): void {
    if (page < 0) {
      return;
    }
    // A previous response must not overwrite a newer filter or page selection.
    this.request?.unsubscribe();
    this.loading.set(true);
    this.errorMessage.set('');

    this.request = this.shipmentService.getMyShipments(page, this.selectedStatus())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.shipments.set(result);
          this.loading.set(false);
        },
        error: error => {
          this.shipments.set(null);
          this.loading.set(false);
          this.errorMessage.set('A küldemények betöltése nem sikerült. Próbáld újra.');
          if (error.status === 401) {
            this.router.navigate(['/login']);
          }
        }
      });
  }
}
