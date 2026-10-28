import {inject, Injectable} from '@angular/core';
import {API_ENDPOINTS} from '../config/api.config';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {LoginCommand} from '../models/login-command.model';

const BASE_URL = API_ENDPOINTS.users;

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private readonly http = inject(HttpClient);

  login(command: LoginCommand): Observable<void> {
    return this.http.post<void>(BASE_URL, command);
  }
}
