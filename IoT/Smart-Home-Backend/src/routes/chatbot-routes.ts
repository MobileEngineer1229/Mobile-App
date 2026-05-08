import { Router } from 'express';
import { ChatbotController } from '../controllers/chatbot-controller';
import { ChatbotRepository } from '../repositories/chatbot-repository';
import { ChatbotService } from '../services/chatbot-service';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router = Router();
const chatbotRepository = new ChatbotRepository(getPool());
const chatbotService = new ChatbotService(chatbotRepository);
const chatbotController = new ChatbotController(chatbotService);

/**
 * @swagger
 * /chatbot/message:
 *   post:
 *     summary: Send message to chatbot
 *     tags: [Chatbot]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - message
 *             properties:
 *               message:
 *                 type: string
 *                 maxLength: 1000
 *               conversationId:
 *                 type: string
 *     responses:
 *       200:
 *         description: Chatbot response
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 */
router.post('/message', authenticate, chatbotController.sendMessage);

/**
 * @swagger
 * /chatbot/history:
 *   get:
 *     summary: Get chat history
 *     tags: [Chatbot]
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
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date-time
 *         description: Start date filter
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date-time
 *         description: End date filter
 *     responses:
 *       200:
 *         description: Chat history
 *       401:
 *         description: Unauthorized
 */
router.get('/history', authenticate, chatbotController.getHistory);

/**
 * @swagger
 * /chatbot/history:
 *   delete:
 *     summary: Clear chat history
 *     tags: [Chatbot]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Chat history cleared
 *       401:
 *         description: Unauthorized
 */
router.delete('/history', authenticate, chatbotController.clearHistory);

export default router;

