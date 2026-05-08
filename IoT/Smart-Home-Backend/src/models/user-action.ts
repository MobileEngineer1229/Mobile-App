/**
 * User Action Model
 */

export interface UserAction {
  id: number;
  userId: number;
  actionType: string;
  actionCategory: string;
  endpoint: string;
  method: string;
  requestBody?: Record<string, any> | null;
  responseStatus?: number | null;
  responseBody?: Record<string, any> | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  deviceInfo?: Record<string, any> | null;
  sessionId?: string | null;
  durationMs?: number | null;
  errorMessage?: string | null;
  metadata?: Record<string, any> | null;
  createdAt: Date;
}

export interface CreateUserActionInput {
  userId: number;
  actionType: string;
  actionCategory: string;
  endpoint: string;
  method: string;
  requestBody?: Record<string, any> | null;
  responseStatus?: number | null;
  responseBody?: Record<string, any> | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  deviceInfo?: Record<string, any> | null;
  sessionId?: string | null;
  durationMs?: number | null;
  errorMessage?: string | null;
  metadata?: Record<string, any> | null;
}

export interface UserActionQuery {
  userId?: number;
  actionType?: string;
  actionCategory?: string;
  endpoint?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  limit?: number;
}
