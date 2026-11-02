import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, switchMap} from 'rxjs';
import {CourierListItem, CreateUserCommand, Role, UpdateUserProfile, UserDetails} from '../models/user.model';
import {PageResponse} from '../models/page-response.model';
import {API_ENDPOINTS} from '../config/api.config';
import {CsrfService} from './csrf.service';

const BASE_URL = API_ENDPOINTS.users;

@Injectable({
  providedIn: 'root',
})
export class UserService {

  private readonly http = inject(HttpClient);
  private readonly csrfService = inject(CsrfService);

  getUsers(page = 0, role: Role | '' = '', active = ''): Observable<PageResponse<UserDetails>> {
    const params: Record<string, string | number> = {page, size: 10};
    if (role) params['role'] = role;
    if (active) params['active'] = active;
    return this.http.get<PageResponse<UserDetails>>(API_ENDPOINTS.adminUsers,
      {withCredentials: true, params});
  }

  updateUserAccess(id: number, role: Role, active: boolean): Observable<UserDetails> {
    return this.csrfService.getToken().pipe(switchMap(csrf =>
      this.http.patch<UserDetails>(`${API_ENDPOINTS.adminUsers}/${id}/access`,
        {role, active}, {withCredentials: true, headers: {[csrf.headerName]: csrf.token}})));
  }

  getActiveCouriers(): Observable<CourierListItem[]> {
    return this.http.get<CourierListItem[]>(`${BASE_URL}/couriers`, {withCredentials: true});
  }


  createUser(command: CreateUserCommand): Observable<void> {
    return this.csrfService.getToken().pipe(
      switchMap(csrf =>
        this.http.post<void>(
          BASE_URL,
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


  getUserById(userId: number): Observable<UserDetails> {
    return this.http.get<UserDetails>(`${BASE_URL}/${userId}`, {withCredentials: true});
  }

  updateUser(
    userId: number,
    command: UpdateUserProfile
  ): Observable<void> {
    return this.csrfService.getToken().pipe(
      switchMap(csrf =>
        this.http.patch<void>(
          `${BASE_URL}/${userId}`,
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

  getCurrentUser(): Observable<UserDetails> {
    return this.http.get<UserDetails>(
      API_ENDPOINTS.currentUser,
      {withCredentials: true}
    );
  }
}

