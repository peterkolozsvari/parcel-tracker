import {Component, inject} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import {UserService} from '../../services/user.service';
import {CreateUserCommand} from '../../models/user.model';

@Component({
  selector: 'app-registration',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registration.html',
  styleUrl: './registration.css',
})
export class Registration {

  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  protected errorMessage = '';

  readonly registrationForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.pattern(/\S+/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    phoneNumber: ['', Validators.maxLength(30)],
  });

  protected onSubmit() {
    this.registrationForm.markAllAsTouched();
    this.errorMessage = '';

    if (this.registrationForm.invalid) {
      return;
    }

    const formValue = this.registrationForm.getRawValue();

    const command: CreateUserCommand = {
      fullName: formValue.fullName,
      email: formValue.email,
      password: formValue.password,
      phoneNumber: formValue.phoneNumber,
    };

    this.userService.createUser(command).subscribe({
      next: () => {
        this.router.navigate(['/home']);
      },
      error: () => {
        this.errorMessage = 'A regisztráció most nem sikerült. Próbáld újra később.';
      }
    });
  }
}
