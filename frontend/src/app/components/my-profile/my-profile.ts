import {Component, inject} from '@angular/core';
import {FormBuilder, ReactiveFormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-my-profile',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './my-profile.html',
  styleUrl: './my-profile.css',
})
export class MyProfile {

  private readonly fb = inject(FormBuilder);
  protected errorMessage = '';
  protected successMessage = '';
  protected isSaving = false;

  readonly profileForm = this.fb.nonNullable.group({
    fullName: [''],
    email: [''],
    phoneNumber: [''],
    address: [''],
  });

  protected onSubmit() {
    this.profileForm.markAllAsTouched();
    this.errorMessage = '';

    if (this.profileForm.invalid) {
      return;
    }

  }
}
