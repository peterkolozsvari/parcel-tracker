import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CreateUserCommand, UpdateUserProfile, UserDetails} from '../models/user.model';
import {API_ENDPOINTS} from '../config/api.config';

const BASE_URL = API_ENDPOINTS.users;

@Injectable({
  providedIn: 'root',
})
export class UserService {

  private readonly http = inject(HttpClient);


  createUser(command: CreateUserCommand): Observable<void> {
    return this.http.post<void>(BASE_URL, command);
  }


  getUserById(userId: number): Observable<UserDetails> {
    return this.http.get<UserDetails>(`${BASE_URL}/${userId}`);
  }

  updateUser(userId: number, command: UpdateUserProfile) {
    return this.http.patch<void>(`${BASE_URL}/${userId}`, command);
  }

}
