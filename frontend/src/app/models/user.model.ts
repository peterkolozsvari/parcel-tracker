import {CreateAddress} from './address.model';

export type Role = 'CUSTOMER' | 'COURIER' | 'ADMIN';

export interface UserDetails {
  id: number;
  fullName: string;
  email: string;
  phoneNumber: string | null;
  role: Role;
  active: boolean;
  address?: CreateAddress | null;
}

export interface CreateUserCommand {
  fullName: string;
  email: string;
  password: string;
  phoneNumber?: string | null;
}

export interface UpdateUserProfile {
  fullName?: string | null;
  phoneNumber?: string | null;
}
