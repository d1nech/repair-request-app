import axios from 'axios';
import { AuthResponse, RepairRequest, RequestForm, Status } from './types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api'
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { email, password });
  return data;
}

export async function register(email: string, fullName: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/register', { email, fullName, password });
  return data;
}

export async function fetchRequests(): Promise<RepairRequest[]> {
  const { data } = await api.get<RepairRequest[]>('/requests');
  return data;
}

export async function createRequest(payload: RequestForm): Promise<RepairRequest> {
  const { data } = await api.post<RepairRequest>('/requests', payload);
  return data;
}

export async function updateRequest(id: number, payload: RequestForm): Promise<RepairRequest> {
  const { data } = await api.put<RepairRequest>(`/requests/${id}`, payload);
  return data;
}

export async function updateStatus(id: number, status: Status): Promise<RepairRequest> {
  const { data } = await api.patch<RepairRequest>(`/requests/${id}/status`, null, { params: { status } });
  return data;
}

export async function deleteRequest(id: number): Promise<void> {
  await api.delete(`/requests/${id}`);
}

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const response = error.response?.data as any;
    if (response?.validationErrors) {
      return Object.values(response.validationErrors).join('; ');
    }
    return response?.message || error.message;
  }
  return 'Неизвестная ошибка';
}
