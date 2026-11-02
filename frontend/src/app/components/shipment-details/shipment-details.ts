import {Component, DestroyRef, inject, OnInit, signal} from '@angular/core';
import {DatePipe, DecimalPipe} from '@angular/common';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {EMPTY, Subject, Subscription, catchError, combineLatest, startWith, switchMap} from 'rxjs';
import {ShipmentService} from '../../services/shipment.service';
import {SHIPMENT_STATUS_LABELS, ShipmentDetails as ShipmentDetailsModel} from '../../models/shipment.model';
import {TrackingEvent} from '../../models/tracking-event.model';

@Component({
  selector: 'app-shipment-details',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './shipment-details.html',
  styleUrl: './shipment-details.css'
})
export class ShipmentDetails implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly shipmentService = inject(ShipmentService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();
  private historyRequest?: Subscription;

  readonly trackingEvents = signal<TrackingEvent[]>([]);
  readonly historyLoading = signal(false);
  readonly historyError = signal('');

  readonly shipment = signal<ShipmentDetailsModel | null>(null);
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly canRetry = signal(false);
  readonly statusLabels = SHIPMENT_STATUS_LABELS;

  ngOnInit(): void {
    // switchMap cancels an older request when the route's shipment ID changes.
    combineLatest([this.route.paramMap, this.reload.pipe(startWith(undefined))]).pipe(
      switchMap(([params]) => {
        this.historyRequest?.unsubscribe();
        this.trackingEvents.set([]);
        this.historyError.set('');
        this.historyLoading.set(false);
        this.shipment.set(null);
        this.errorMessage.set('');
        this.canRetry.set(false);
        this.loading.set(true);
        const rawId = params.get('id');
        const id = Number(rawId);
        if (!rawId || !/^[1-9]\d*$/.test(rawId) || !Number.isSafeInteger(id)) {
          this.loading.set(false);
          this.errorMessage.set('Érvénytelen küldeményazonosító.');
          return EMPTY;
        }
        return this.shipmentService.getShipmentById(id).pipe(
          catchError(error => {
            this.loading.set(false);
            if (error.status === 401) {
              this.errorMessage.set('A megtekintéshez jelentkezz be.');
              this.router.navigate(['/login']);
            } else if (error.status === 403) {
              this.errorMessage.set('Nincs jogosultságod a küldemény megtekintéséhez.');
            } else if (error.status === 404) {
              this.errorMessage.set('A keresett küldemény nem található.');
            } else {
              this.errorMessage.set('A küldemény betöltése nem sikerült. Próbáld újra.');
              this.canRetry.set(true);
            }
            return EMPTY;
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(shipment => {
      this.shipment.set(shipment);
      this.loading.set(false);
      this.loadTrackingEvents();
    });
  }

  loadTrackingEvents(): void {
    const shipment = this.shipment();
    if (!shipment) {
      return;
    }
    this.historyRequest?.unsubscribe();
    this.historyLoading.set(true);
    this.historyError.set('');
    this.historyRequest = this.shipmentService.getTrackingEvents(shipment.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: events => {
          this.trackingEvents.set(events);
          this.historyLoading.set(false);
        },
        error: error => {
          this.historyLoading.set(false);
          if (error.status === 401) {
            this.historyError.set('A megtekintéshez jelentkezz be újra.');
            this.router.navigate(['/login']);
          } else if (error.status === 403 || error.status === 404) {
            this.historyError.set('A küldemény státusztörténete nem érhető el.');
          } else {
            this.historyError.set('A státusztörténet betöltése nem sikerült. Próbáld újra.');
          }
        }
      });
  }

  eventDescription(event: TrackingEvent): string | null {
    return event.description === 'Shipment registered' ? 'Küldemény rögzítve.' : event.description;
  }

  retry(): void {
    this.reload.next();
  }
}
