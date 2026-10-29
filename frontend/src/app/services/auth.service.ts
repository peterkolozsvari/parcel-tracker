import {inject, Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable, switchMap, tap} from 'rxjs';
import {API_ENDPOINTS} from '../config/api.config';
import {LoginCommand} from '../models/login-command.model';
import {CsrfService} from './csrf.service';


interface CurrentUser {
  id: number;
  fullName: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly csrfService = inject(CsrfService);

  private readonly currentUserState = signal<CurrentUser | null>(null);
  private readonly sessionCheckedState = signal(false);

  readonly currentUser = this.currentUserState.asReadonly();
  readonly sessionChecked = this.sessionCheckedState.asReadonly();

  constructor() {
    this.loadCurrentUser().subscribe({
      next: () => {
        this.sessionCheckedState.set(true);
      },
      error: () => {
        this.sessionCheckedState.set(true);
      }
    });
  }

  login(command: LoginCommand): Observable<void> {
    return this.csrfService.getToken().pipe(
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
      ),
      switchMap(() => this.loadCurrentUser()),
      tap(() => this.sessionCheckedState.set(true)),
      map(() => void 0)
    );
  }

  private loadCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(API_ENDPOINTS.currentUser, {withCredentials: true}
    ).pipe(tap(user => this.currentUserState.set(user)));
  }

  logout(): Observable<void> {
    return this.csrfService.getToken().pipe(
      switchMap(csrf =>
        this.http.post<void>(
          API_ENDPOINTS.logout,
          null,
          {
            withCredentials: true,
            headers: {
              [csrf.headerName]: csrf.token
            }
          }
        )
      ),
      tap(() => {
        this.currentUserState.set(null);
        this.sessionCheckedState.set(true);
      })
    );
  }
}
