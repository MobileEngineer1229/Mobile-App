import { Request, Response } from 'express';
import { NotificationService } from '../services/notification-service';
import { NotificationQuery } from '../models/notification';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger';

export class NotificationController {
  constructor(private notificationService: NotificationService) {}

  /**
   * Get all notifications
   * GET /api/v1/notifications
   */
  getNotifications = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const query: NotificationQuery = {
        page: req.query.page ? parseInt(req.query.page as string, 10) : undefined,
        limit: req.query.limit ? parseInt(req.query.limit as string, 10) : undefined,
        type: req.query.type as any,
        category: req.query.category as any,
        isRead: req.query.isRead === 'true' ? true : req.query.isRead === 'false' ? false : undefined,
        startDate: req.query.startDate as string | undefined,
        endDate: req.query.endDate as string | undefined,
      };

      const result = await this.notificationService.getNotifications(userId, query);
      sendSuccess(res, result, 200);
    } catch (error) {
      logger.error('Error in getNotifications controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to fetch notifications', 500);
    }
  };

  /**
   * Get notification by ID
   * GET /api/v1/notifications/:id
   */
  getNotificationById = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid notification ID', 400);
        return;
      }

      const notification = await this.notificationService.getNotificationById(id, userId);
      sendSuccess(res, notification, 200);
    } catch (error) {
      if (error instanceof Error && error.message.includes('not found')) {
        sendError(res, 'NOT_FOUND', 'Notification not found', 404);
      } else {
        logger.error('Error in getNotificationById controller', {
          error: error instanceof Error ? error.message : String(error),
        });
        sendError(res, 'INTERNAL_ERROR', 'Failed to fetch notification', 500);
      }
    }
  };

  /**
   * Mark notification as read
   * PUT /api/v1/notifications/:id/read
   */
  markAsRead = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid notification ID', 400);
        return;
      }

      const notification = await this.notificationService.markAsRead(id, userId);
      sendSuccess(res, notification, 200);
    } catch (error) {
      if (error instanceof Error && error.message.includes('not found')) {
        sendError(res, 'NOT_FOUND', 'Notification not found', 404);
      } else {
        logger.error('Error in markAsRead controller', {
          error: error instanceof Error ? error.message : String(error),
        });
        sendError(res, 'INTERNAL_ERROR', 'Failed to mark notification as read', 500);
      }
    }
  };

  /**
   * Mark all notifications as read
   * PUT /api/v1/notifications/read-all
   */
  markAllAsRead = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const result = await this.notificationService.markAllAsRead(userId);
      sendSuccess(res, result, 200);
    } catch (error) {
      logger.error('Error in markAllAsRead controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to mark all notifications as read', 500);
    }
  };

  /**
   * Delete notification
   * DELETE /api/v1/notifications/:id
   */
  deleteNotification = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid notification ID', 400);
        return;
      }

      await this.notificationService.deleteNotification(id, userId);
      sendSuccess(res, { message: 'Notification deleted successfully' }, 200);
    } catch (error) {
      if (error instanceof Error && error.message.includes('not found')) {
        sendError(res, 'NOT_FOUND', 'Notification not found', 404);
      } else {
        logger.error('Error in deleteNotification controller', {
          error: error instanceof Error ? error.message : String(error),
        });
        sendError(res, 'INTERNAL_ERROR', 'Failed to delete notification', 500);
      }
    }
  };

  /**
   * Get notification statistics
   * GET /api/v1/notifications/stats
   */
  getStats = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const stats = await this.notificationService.getStats(userId);
      sendSuccess(res, stats, 200);
    } catch (error) {
      logger.error('Error in getStats controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to fetch notification statistics', 500);
    }
  };
}

