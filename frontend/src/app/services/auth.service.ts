import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, switchMap} from 'rxjs';
import {API_ENDPOINTS} from '../config/api.config';
import {LoginCommand} from '../models/login-command.model';

interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

interface CurrentUser {
  id: number;
  fullName: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly currentUserState = signal<CurrentUser | null>(null);
  private readonly sessionCheckedState = signal(false);
  readonly currentUser = this.currentUserState.asReadonly();
  readonly sessionChecked = this.sessionCheckedState.asReadonly();

  constructor() {
    this.http.get<CurrentUser>(
      `${API_ENDPOINTS.users.replace(/\/users$/, '')}/auth/me`,
      {withCredentials: true}
    ).subscribe({
      next: user => {
        this.currentUserState.set(user);
        this.sessionCheckedState.set(true);
      },
      error: () => {
        this.sessionCheckedState.set(true);
      }
    });
  }

  login(command: LoginCommand): Observable<void> {
    return this.http.get<CsrfTokenResponse>(
      API_ENDPOINTS.csrf,
      {withCredentials: true}
    ).pipe(
      switchMap(csrf =>
        this.http.post<void>(
          API_ENDPOINTS.login,
          command,
          {
            withCredentials: true,
            headers: {
              [csrf.headerName]: csrf.token
            }
          }
        )
      )
    );
  }
}
