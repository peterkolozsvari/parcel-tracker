const BASE_URL = 'http://localhost:8080/api';

export const API_ENDPOINTS = {
  users: `${BASE_URL}/users`,
  shipments: `${BASE_URL}/shipments`,
  login: `${BASE_URL}/auth/login`,
  logout: `${BASE_URL}/auth/logout`,
  csrf: `${BASE_URL}/auth/csrf`,
  currentUser: `${BASE_URL}/auth/me`,
} as const;
