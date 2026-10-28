const BASE_URL = 'http://localhost:8080/api';

export const API_ENDPOINTS = {
  users: `${BASE_URL}/users`,
  shipments: `${BASE_URL}/shipments`,
  login: `${BASE_URL}/login`,
  csrf: `${BASE_URL}/auth/csrf`,
} as const;
