import { NotificationRepository } from '../repositories/notification-repository';
import {
  Notification,
  CreateNotificationInput,
  UpdateNotificationInput,
  NotificationQuery,
  NotificationStats,
  NotificationResponse,
} from '../models/notification';
import { NotFoundError } from '../utils/errors';
import logger from '../utils/logger';

export class NotificationService {
  constructor(private notificationRepository: NotificationRepository) {}

  /**
   * Get notification by ID
   */
  async getNotificationById(id: number, userId: number): Promise<NotificationResponse> {
    const notification = await this.notificationRepository.findById(id, userId);
    if (!notification) {
      throw new NotFoundError('Notification');
    }
    return this.mapToResponse(notification);
  }

  /**
   * Get all notifications for a user
   */
  async getNotifications(userId: number, query: NotificationQuery): Promise<{
    notifications: NotificationResponse[];
    total: number;
    page: number;
    limit: number;
    totalPages: number;
  }> {
    const page = query.page || 1;
    const limit = query.limit || 20;

    const { notifications, total } = await this.notificationRepository.findAll(userId, query);
    const totalPages = Math.ceil(total / limit);

    return {
      notifications: notifications.map((n) => this.mapToResponse(n)),
      total,
      page,
      limit,
      totalPages,
    };
  }

  /**
   * Create notification
   */
  async createNotification(input: CreateNotificationInput): Promise<NotificationResponse> {
    const notification = await this.notificationRepository.create(input);
    logger.info('Notification created', {
      notificationId: notification.id,
      userId: notification.userId,
      type: notification.type,
    });
    return this.mapToResponse(notification);
  }

  /**
   * Update notification
   */
  async updateNotification(
    id: number,
    userId: number,
    input: UpdateNotificationInput
  ): Promise<NotificationResponse> {
    const notification = await this.notificationRepository.update(id, userId, input);
    return this.mapToResponse(notification);
  }

  /**
   * Mark notification as read
   */
  async markAsRead(id: number, userId: number): Promise<NotificationResponse> {
    return this.updateNotification(id, userId, { isRead: true });
  }

  /**
   * Mark all notifications as read
   */
  async markAllAsRead(userId: number): Promise<{ count: number }> {
    const count = await this.notificationRepository.markAllAsRead(userId);
    logger.info('All notifications marked as read', { userId, count });
    return { count };
  }

  /**
   * Delete notification
   */
  async deleteNotification(id: number, userId: number): Promise<void> {
    await this.notificationRepository.delete(id, userId);
    logger.info('Notification deleted', { notificationId: id, userId });
  }

  /**
   * Get notification statistics
   */
  async getStats(userId: number): Promise<NotificationStats> {
    return this.notificationRepository.getStats(userId);
  }

  /**
   * Map Notification to NotificationResponse
   */
  private mapToResponse(notification: Notification): NotificationResponse {
    return {
      id: notification.id,
      userId: notification.userId,
      title: notification.title,
      message: notification.message,
      type: notification.type,
      icon: notification.icon,
      isRead: notification.isRead,
      readAt: notification.readAt,
      metadata: notification.metadata,
      createdAt: notification.createdAt,
    };
  }
}

