import {Component, inject} from '@angular/core';
import {FormBuilder, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {AuthService} from '../../services/auth.service';
import {LoginCommand} from '../../models/login-command.model';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {

  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  protected errorMessage = '';

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  protected onSubmit() {
    this.loginForm.markAllAsTouched();
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      return;
    }

    const formValue = this.loginForm.getRawValue();

    const command: LoginCommand = {
      email: formValue.email,
      password: formValue.password,
    };

    this.authService.login(command).subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: error => {
        this.errorMessage = error.status === 401
          ? 'Hibás email-cím vagy jelszó, vagy a fiók nem elérhető.'
          : 'A bejelentkezés most nem sikerült. Próbáld újra később.';
      }
    });
  }
}
