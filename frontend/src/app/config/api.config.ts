const BASE_URL = 'http://localhost:8080/api';

export const API_ENDPOINTS = {
  users: `${BASE_URL}/users`,
  shipments: `${BASE_URL}/shipments`,
} as const;
