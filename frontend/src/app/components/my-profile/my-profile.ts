import {Component, inject, OnInit} from '@angular/core';
import {FormBuilder, ReactiveFormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {UserService} from '../../services/user.service';

@Component({
  selector: 'app-my-profile',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './my-profile.html',
  styleUrl: './my-profile.css',
})
export class MyProfile implements OnInit{


  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  protected errorMessage = '';
  protected successMessage = '';
  protected isSaving = false;

  ngOnInit(): void {
    throw new Error("Method not implemented.");
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
    this.errorMessage = '';

    if (this.profileForm.invalid) {
      return;
    }

  }
}
