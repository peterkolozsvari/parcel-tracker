import {Component, inject, OnInit} from '@angular/core';
import {FormBuilder, ReactiveFormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {UserService} from '../../services/user.service';
import {UpdateUserProfile} from '../../models/user.model';

@Component({
  selector: 'app-my-profile',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './my-profile.html',
  styleUrl: './my-profile.css',
})
export class MyProfile implements OnInit {


  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private userId: number | null = null;
  protected errorMessage = '';
  protected successMessage = '';
  protected isSaving = false;

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: user => {
        this.userId = user.id;
        this.profileForm.patchValue({
          fullName: user.fullName,
          email: user.email,
          phoneNumber: user.phoneNumber ?? '',
          address: {
            country: user.address?.country ?? '',
            postalCode: user.address?.postalCode ?? '',
            city: user.address?.city ?? '',
            street: user.address?.street ?? '',
            houseNumber: user.address?.houseNumber ?? '',
            door: user.address?.door ?? ''
          }
        });
      },
      error: () => {
        this.errorMessage = 'A profiladatokat nem sikerült betölteni.';
      }
    });
  }

  readonly profileForm = this.fb.nonNullable.group({
    fullName: [''],
    email: [''],
    phoneNumber: [''],
    address: this.fb.nonNullable.group({
      country: [''],
      postalCode: [''],
      city: [''],
      street: [''],
      houseNumber: [''],
      door: ['']
    })
  });

  protected onSubmit() {
    this.profileForm.markAllAsTouched();
    this.successMessage = '';
    this.errorMessage = '';

    if (this.profileForm.invalid) {
      return;
    }
    if (this.userId === null) {
      this.errorMessage = 'A profiladatok még nem töltődtek be.';
      return;
    }

    const formValue = this.profileForm.getRawValue();

    const command: UpdateUserProfile = {
      fullName: formValue.fullName,
      phoneNumber: formValue.phoneNumber,
      address: {
        country: formValue.address.country,
        postalCode: formValue.address.postalCode,
        city: formValue.address.city,
        street: formValue.address.street,
        houseNumber: formValue.address.houseNumber,
        door: formValue.address.door
      }
    };
    this.userService.updateUser(this.userId ,command).subscribe({
      next: () => {
        this.successMessage = 'A módosításokat sikeresen mentettük.';
      },
      error: () => {
        this.errorMessage = 'A regisztráció most nem sikerült. Próbáld újra később.';
      }
    });
  }
}
