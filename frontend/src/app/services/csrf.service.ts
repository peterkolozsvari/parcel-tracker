import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {API_ENDPOINTS} from '../config/api.config';

interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

@Injectable({
  providedIn: 'root',
})
export class CsrfService {

  private readonly http = inject(HttpClient);

  getToken(): Observable<CsrfTokenResponse> {
    return this.http.get<CsrfTokenResponse>(
      API_ENDPOINTS.csrf,
      {withCredentials: true}
    );
  }
}
