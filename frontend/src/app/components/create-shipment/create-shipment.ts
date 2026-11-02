import { Component, inject, OnInit, signal } from '@angular/core';
import {FormArray, FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {ShipmentService} from '../../services/shipment.service';
import { CreateShipment as CreateShipmentCommand } from '../../models/shipment.model';
import {Router} from '@angular/router';
import {UserService} from '../../services/user.service';
import {CreateAddress} from '../../models/address.model';

@Component({
  selector: 'app-create-shipment',
  imports: [ReactiveFormsModule],
  templateUrl: './create-shipment.html',
  styleUrl: './create-shipment.css'
})
export class CreateShipment implements OnInit {
  private userId: number | null = null;
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly shipmentService = inject(ShipmentService);

  readonly isProfileLoading = signal(true);
  readonly profileLoadError = signal('');
  readonly savedPickupAddress = signal<CreateAddress | null>(null);
  readonly editingPickupAddress = signal(false);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');

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

  ngOnInit(): void {
    this.loadUserProfile();
  }

  loadUserProfile(): void {
    this.isProfileLoading.set(true);
    this.profileLoadError.set('');

    this.userService.getCurrentUser().subscribe({
      next: user => {
        this.userId = user.id;
        const address = user.address;

        if (address) {
          this.shipmentForm.controls.pickupAddress.patchValue({
            country: address.country,
            postalCode: address.postalCode,
            city: address.city,
            street: address.street,
            houseNumber: address.houseNumber,
            door: address.door ?? ''
          });
        }

        const hasValidAddress = !!address && this.shipmentForm.controls.pickupAddress.valid;
        this.savedPickupAddress.set(hasValidAddress
          ? this.shipmentForm.controls.pickupAddress.getRawValue()
          : null);
        this.editingPickupAddress.set(!hasValidAddress);
        this.isProfileLoading.set(false);
      },
      error: error => {
        this.isProfileLoading.set(false);
        this.profileLoadError.set('A mentett cím betöltése nem sikerült. Próbáld újra.');
        if (error.status === 401) {
          this.router.navigate(['/login']);
        }
      }
    });
  }

  useSavedPickupAddress(): void {
    const address = this.savedPickupAddress();
    if (!address) {
      return;
    }

    this.shipmentForm.controls.pickupAddress.reset({
      ...address,
      door: address.door ?? ''
    });
    this.editingPickupAddress.set(false);
  }

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
    if (this.isProfileLoading() || this.profileLoadError() || this.isSubmitting()) {
      return;
    }

    this.errorMessage.set('');
    this.shipmentForm.markAllAsTouched();

    if (this.shipmentForm.controls.pickupAddress.invalid) {
      this.editingPickupAddress.set(true);
    }

    if (this.shipmentForm.invalid) {
      return;
    }

    if (this.userId === null) {
      this.router.navigate(['/login']);
      return;
    }

    const formValue = this.shipmentForm.getRawValue();

    const command: CreateShipmentCommand = {
      senderId: this.userId,
      receiverName: formValue.receiverName,
      pickupAddress: formValue.pickupAddress,
      deliveryAddress: formValue.deliveryAddress,
      parcels: formValue.parcels.map(parcel => ({
        weight: parcel.weight!,
        description: parcel.description,
        fragile: parcel.fragile
      }))
    };

    this.isSubmitting.set(true);
    this.shipmentService.createShipment(command).subscribe({
      next: response => {
        this.isSubmitting.set(false);
        this.router.navigate(['/shipments/success'], {
          queryParams: {trackingNumber: response.trackingNumber}
        });
      },
      error: () => {
        this.isSubmitting.set(false);
        this.errorMessage.set('A küldemény feladása nem sikerült. Próbáld újra később.');
      }
    });
  }

}

