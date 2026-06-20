import axios from 'axios';
import {
  Attachment,
  AuthResponse,
  Category,
  Comment,
  MasterOption,
  RepairRequest,
  RequestForm,
  Status,
  StatusHistoryEntry
} from './types';

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

export async function updateStatus(id: number, status: Status, comment?: string): Promise<RepairRequest> {
  const { data } = await api.patch<RepairRequest>(`/requests/${id}/status`, null, { params: { status, comment } });
  return data;
}

export async function assignMaster(id: number, masterId: number): Promise<RepairRequest> {
  const { data } = await api.patch<RepairRequest>(`/requests/${id}/assign`, { masterId });
  return data;
}

export async function classifyRequest(id: number, categoryId: number): Promise<RepairRequest> {
  const { data } = await api.patch<RepairRequest>(`/requests/${id}/classify`, { categoryId });
  return data;
}

export async function deleteRequest(id: number): Promise<void> {
  await api.delete(`/requests/${id}`);
}

export async function fetchCategories(): Promise<Category[]> {
  const { data } = await api.get<Category[]>('/categories');
  return data;
}

export async function fetchMasters(): Promise<MasterOption[]> {
  const { data } = await api.get<MasterOption[]>('/masters');
  return data;
}

export async function fetchComments(requestId: number): Promise<Comment[]> {
  const { data } = await api.get<Comment[]>(`/requests/${requestId}/comments`);
  return data;
}

export async function addComment(requestId: number, message: string): Promise<Comment> {
  const { data } = await api.post<Comment>(`/requests/${requestId}/comments`, { message });
  return data;
}

export async function fetchAttachments(requestId: number): Promise<Attachment[]> {
  const { data } = await api.get<Attachment[]>(`/requests/${requestId}/attachments`);
  return data;
}

export async function uploadAttachment(requestId: number, file: File): Promise<Attachment> {
  const formData = new FormData();
  formData.append('file', file);
  const { data } = await api.post<Attachment>(`/requests/${requestId}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return data;
}

export async function downloadAttachment(requestId: number, attachmentId: number): Promise<Blob> {
  const { data } = await api.get(`/requests/${requestId}/attachments/${attachmentId}/download`, {
    responseType: 'blob'
  });
  return data;
}

export async function fetchStatusHistory(requestId: number): Promise<StatusHistoryEntry[]> {
  const { data } = await api.get<StatusHistoryEntry[]>(`/requests/${requestId}/status-history`);
  return data;
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
