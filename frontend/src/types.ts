export type Role = 'USER' | 'ADMIN';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type Status = 'NEW' | 'IN_PROGRESS' | 'WAITING_PARTS' | 'DONE' | 'CANCELLED';

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface RepairRequest {
  id: number;
  title: string;
  description: string;
  equipmentType: string;
  location: string;
  priority: Priority;
  status: Status;
  userId: number;
  userEmail: string;
  createdAt: string;
  updatedAt: string;
}

export interface RequestForm {
  title: string;
  description: string;
  equipmentType: string;
  location: string;
  priority: Priority;
}
