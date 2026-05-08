import { Request, Response } from 'express';
import { ChatbotService } from '../services/chatbot-service';
import { SendMessageRequest, ChatHistoryQuery } from '../models/chatbot';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger';

export class ChatbotController {
  constructor(private chatbotService: ChatbotService) {}

  /**
   * Send message to chatbot
   * POST /api/v1/chatbot/message
   */
  sendMessage = async (req: Request, res: Response): Promise<void> => {
    try {
      const request: SendMessageRequest = {
        message: req.body.message,
        conversationId: req.body.conversationId,
      };

      // Validation
      if (!request.message || request.message.trim().length === 0) {
        sendError(res, 'VALIDATION_ERROR', 'Message is required', 400);
        return;
      }

      if (request.message.length > 1000) {
        sendError(res, 'VALIDATION_ERROR', 'Message is too long (max 1000 characters)', 400);
        return;
      }

      const response = await this.chatbotService.sendMessage(req.user!.id, request);
      sendSuccess(res, response, 200);
    } catch (error) {
      logger.error('Error in sendMessage controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to process message', 500);
    }
  };

  /**
   * Get chat history
   * GET /api/v1/chatbot/history
   */
  getHistory = async (req: Request, res: Response): Promise<void> => {
    try {
      const query: ChatHistoryQuery = {
        page: req.query.page ? parseInt(req.query.page as string, 10) : undefined,
        limit: req.query.limit ? parseInt(req.query.limit as string, 10) : undefined,
        startDate: req.query.startDate as string | undefined,
        endDate: req.query.endDate as string | undefined,
      };

      const history = await this.chatbotService.getHistory(req.user!.id, query);
      sendSuccess(res, history, 200);
    } catch (error) {
      logger.error('Error in getHistory controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to fetch chat history', 500);
    }
  };

  /**
   * Clear chat history
   * DELETE /api/v1/chatbot/history
   */
  clearHistory = async (req: Request, res: Response): Promise<void> => {
    try {
      const result = await this.chatbotService.clearHistory(req.user!.id);
      sendSuccess(res, { message: 'Chat history cleared successfully', ...result }, 200);
    } catch (error) {
      logger.error('Error in clearHistory controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to clear chat history', 500);
    }
  };
}

