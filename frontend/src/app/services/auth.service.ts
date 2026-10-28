import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, switchMap} from 'rxjs';
import {API_ENDPOINTS} from '../config/api.config';
import {LoginCommand} from '../models/login-command.model';

interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private readonly http = inject(HttpClient);

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
