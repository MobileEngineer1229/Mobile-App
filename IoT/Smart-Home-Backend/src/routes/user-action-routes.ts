import { Router } from 'express';
import { UserActionController } from '../controllers/user-action-controller';
import { authenticate } from '../middleware/auth';

const router: Router = Router();

// Initialize controller
const userActionController = new UserActionController();

// All routes require authentication
router.use(authenticate);

/**
 * @swagger
 * /user-actions:
 *   get:
 *     summary: Get user action logs
 *     tags: [User Actions]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: actionType
 *         schema:
 *           type: string
 *         description: Filter by action type
 *       - in: query
 *         name: actionCategory
 *         schema:
 *           type: string
 *         description: Filter by action category
 *       - in: query
 *         name: endpoint
 *         schema:
 *           type: string
 *         description: Filter by endpoint (partial match)
 *       - in: query
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date
 *         description: Start date filter (ISO format)
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date
 *         description: End date filter (ISO format)
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *         description: Page number
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 50
 *         description: Items per page
 *     responses:
 *       200:
 *         description: List of user actions
 *       401:
 *         description: Unauthorized
 */
router.get('/', userActionController.getUserActions);

/**
 * @swagger
 * /user-actions/statistics:
 *   get:
 *     summary: Get user action statistics
 *     tags: [User Actions]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date
 *         description: Start date filter (ISO format)
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date
 *         description: End date filter (ISO format)
 *     responses:
 *       200:
 *         description: User action statistics
 *       401:
 *         description: Unauthorized
 */
router.get('/statistics', userActionController.getStatistics);

/**
 * @swagger
 * /user-actions/{id}:
 *   get:
 *     summary: Get user action by ID
 *     tags: [User Actions]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Action ID
 *     responses:
 *       200:
 *         description: User action details
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden (action doesn't belong to user)
 *       404:
 *         description: Action not found
 */
router.get('/:id', userActionController.getUserActionById);

export default router;
