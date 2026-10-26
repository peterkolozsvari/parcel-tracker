import { Component, inject } from '@angular/core';
import {FormArray, FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ShipmentService} from '../../services/shipment.service';
import { CreateShipment as CreateShipmentCommand } from '../../models/shipment.model';
import {Router, RouterLink} from '@angular/router';
import {form} from '@angular/forms/signals';

@Component({
  selector: 'app-create-shipment',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './create-shipment.html',
  styleUrl: './create-shipment.css'
})
export class CreateShipment {
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly shipmentService = inject(ShipmentService);

  readonly addressSections = [
    { name: 'pickupAddress', title: 'Felvételi cím' },
    { name: 'deliveryAddress', title: 'Kézbesítési cím' }
  ];

  readonly addressFields = [
    { name: 'country', label: 'Ország', maxLength: 100 },
    { name: 'postalCode', label: 'Irányítószám', maxLength: 20 },
    { name: 'city', label: 'Város', maxLength: 100 },
    { name: 'street', label: 'Utca', maxLength: 150 },
    { name: 'houseNumber', label: 'Házszám', maxLength: 20 },
    { name: 'door', label: 'Emelet, ajtó (opcionális)', maxLength: 50 }
  ];

  readonly shipmentForm = this.fb.nonNullable.group({
    receiverName: ['', [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(100)
    ]],
    pickupAddress: this.createAddressGroup(),
    deliveryAddress: this.createAddressGroup(),
    parcels: this.fb.array([this.createParcelGroup()])
  });

  get parcels(): FormArray {
    return this.shipmentForm.controls.parcels;
  }

  private createAddressGroup() {
    const requiredText = (maxLength: number) => [
      Validators.required,
      Validators.pattern(/\S/),
      Validators.maxLength(maxLength)
    ];

    return this.fb.nonNullable.group({
      country: ['Magyarország', requiredText(100)],
      postalCode: ['', requiredText(20)],
      city: ['', requiredText(100)],
      street: ['', requiredText(150)],
      houseNumber: ['', requiredText(20)],
      door: ['', Validators.maxLength(50)]
    });
  }

  private createParcelGroup() {
    return this.fb.group({
      weight: this.fb.control<number | null>(null, [
        Validators.required,
        Validators.min(0.01),
        Validators.pattern(/^\d+(\.\d{1,2})?$/)
      ]),
      description: this.fb.nonNullable.control(''),
      fragile: this.fb.nonNullable.control(false)
    });
  }

  addParcel(): void {
    this.parcels.push(this.createParcelGroup());
  }

  removeParcel(index: number): void {
    if (this.parcels.length > 1) {
      this.parcels.removeAt(index);
    }
  }

  isInvalid(path: string): boolean {
    const control = this.shipmentForm.get(path);
    return !!control && control.invalid && control.touched;
  }

  submit(): void {
    this.shipmentForm.markAllAsTouched();

    if (this.shipmentForm.invalid) {
      return;
    }

    const formValue = this.shipmentForm.getRawValue();

    const command: CreateShipmentCommand = {
      senderId: 1,
      receiverName: formValue.receiverName,
      pickupAddress: formValue.pickupAddress,
      deliveryAddress: formValue.deliveryAddress,
      parcels: formValue.parcels.map(parcel => ({
        weight: parcel.weight!,
        description: parcel.description,
        fragile: parcel.fragile
      }))
    };

    this.shipmentService.createShipment(command).subscribe({
      next: response => {
        this.router.navigate(['/shipments/success'], {
          queryParams: {trackingNumber: response.trackingNumber}
        });
      },
      error: error => {
        console.error('Sikertelen küldeményfeladás:', error);
      }
    });
  }

  protected readonly form = form;
}
