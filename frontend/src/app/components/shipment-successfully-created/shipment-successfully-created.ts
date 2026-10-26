import {Component, inject, CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';

@Component({
  selector: 'app-shipment-successfully-created',
  imports: [
    RouterLink
  ],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './shipment-successfully-created.html',
  styleUrl: './shipment-successfully-created.css',
})
export class ShipmentSuccessfullyCreated {

  private readonly route = inject(ActivatedRoute);


  readonly trackingNumber = this.route.snapshot.queryParamMap.get('trackingNumber');

  async copyTrackingNumber(): Promise<void> {
    if (!this.trackingNumber) {
      return;
    }

    try {
      await navigator.clipboard.writeText(this.trackingNumber);
    } catch (error) {
      console.error('Nem sikerült a vágólapra másolni:', error);
    }
  }
}
