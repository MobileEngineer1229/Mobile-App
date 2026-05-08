import { Router } from 'express';
import { NotificationController } from '../controllers/notification-controller';
import { NotificationRepository } from '../repositories/notification-repository';
import { NotificationService } from '../services/notification-service';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router = Router();
const notificationRepository = new NotificationRepository(getPool());
const notificationService = new NotificationService(notificationRepository);
const notificationController = new NotificationController(notificationService);

/**
 * @swagger
 * /notifications:
 *   get:
 *     summary: Get all notifications
 *     tags: [Notifications]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *         description: Page number
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *         description: Items per page
     *       - in: query
     *         name: type
     *         schema:
     *           type: string
     *           enum: [general, security, system, feature, reminder, alert, info]
     *         description: Filter by notification type
     *       - in: query
     *         name: category
     *         schema:
     *           type: string
     *           enum: [general, smart_home]
     *         description: Filter by notification category (General or Smart Home)
     *       - in: query
     *         name: isRead
     *         schema:
     *           type: boolean
     *         description: Filter by read status
 *     responses:
 *       200:
 *         description: List of notifications
 *       401:
 *         description: Unauthorized
 */
router.get('/', authenticate, notificationController.getNotifications);

/**
 * @swagger
 * /notifications/stats:
 *   get:
 *     summary: Get notification statistics
 *     tags: [Notifications]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Notification statistics
 *       401:
 *         description: Unauthorized
 */
router.get('/stats', authenticate, notificationController.getStats);

/**
 * @swagger
 * /notifications/read-all:
 *   put:
 *     summary: Mark all notifications as read
 *     tags: [Notifications]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: All notifications marked as read
 *       401:
 *         description: Unauthorized
 */
router.put('/read-all', authenticate, notificationController.markAllAsRead);

/**
 * @swagger
 * /notifications/{id}:
 *   get:
 *     summary: Get notification by ID
 *     tags: [Notifications]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Notification details
 *       404:
 *         description: Notification not found
 *       401:
 *         description: Unauthorized
 */
router.get('/:id', authenticate, notificationController.getNotificationById);

/**
 * @swagger
 * /notifications/{id}/read:
 *   put:
 *     summary: Mark notification as read
 *     tags: [Notifications]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Notification marked as read
 *       404:
 *         description: Notification not found
 *       401:
 *         description: Unauthorized
 */
router.put('/:id/read', authenticate, notificationController.markAsRead);

/**
 * @swagger
 * /notifications/{id}:
 *   delete:
 *     summary: Delete notification
 *     tags: [Notifications]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Notification deleted
 *       404:
 *         description: Notification not found
 *       401:
 *         description: Unauthorized
 */
router.delete('/:id', authenticate, notificationController.deleteNotification);

export default router;

