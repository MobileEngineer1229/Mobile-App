/**
 * Notification type enum
 */
export type NotificationType =
  | 'general'
  | 'security'
  | 'system'
  | 'feature'
  | 'reminder'
  | 'alert'
  | 'info';

/**
 * Notification category enum
 */
export type NotificationCategory = 'general' | 'smart_home';

/**
 * Notification model interface
 */
export interface Notification {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: NotificationType;
  category?: NotificationCategory | null;
  icon?: string | null;
  isRead: boolean;
  readAt?: Date | null;
  metadata?: Record<string, unknown> | null;
  createdAt: Date;
}

/**
 * Create notification input
 */
export interface CreateNotificationInput {
  userId: number;
  title: string;
  message: string;
  type?: NotificationType;
  category?: NotificationCategory;
  icon?: string;
  metadata?: Record<string, unknown>;
}

/**
 * Update notification input
 */
export interface UpdateNotificationInput {
  isRead?: boolean;
  metadata?: Record<string, unknown>;
}

/**
 * Notification query parameters
 */
export interface NotificationQuery {
  page?: number;
  limit?: number;
  type?: NotificationType;
  category?: NotificationCategory;
  isRead?: boolean;
  startDate?: string;
  endDate?: string;
}

/**
 * Notification response
 */
export interface NotificationResponse {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: NotificationType;
  category?: NotificationCategory | null;
  icon?: string | null;
  isRead: boolean;
  readAt?: Date | null;
  metadata?: Record<string, unknown> | null;
  createdAt: Date;
}

/**
 * Notification statistics
 */
export interface NotificationStats {
  total: number;
  unread: number;
  byType: Record<NotificationType, number>;
}

