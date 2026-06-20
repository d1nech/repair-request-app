export type Role = 'USER' | 'ADMIN' | 'MASTER' | 'OPERATOR';
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
  categoryId: number | null;
  categoryName: string | null;
  assignedMasterId: number | null;
  assignedMasterEmail: string | null;
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

export interface Category {
  id: number;
  name: string;
  description: string | null;
}

export interface Comment {
  id: number;
  requestId: number;
  authorId: number;
  authorEmail: string;
  message: string;
  createdAt: string;
}

export interface Attachment {
  id: number;
  requestId: number;
  fileName: string;
  fileUrl: string;
  mimeType: string;
  uploadedAt: string;
}

export interface StatusHistoryEntry {
  id: number;
  requestId: number;
  changedById: number | null;
  changedByEmail: string | null;
  oldStatus: Status | null;
  newStatus: Status;
  comment: string | null;
  changedAt: string;
}

export interface MasterOption {
  id: number;
  email: string;
  fullName: string;
}
