import { Request, Response, NextFunction } from 'express';
import { UserActionService } from '../services/user-action-service';
import { UserActionRepository } from '../repositories/user-action-repository';
import { getPool } from '../config/database';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger';

// Initialize service
const userActionRepository = new UserActionRepository(getPool());
const userActionService = new UserActionService(userActionRepository);

export class UserActionController {
  /**
   * Get user actions with filters
   * GET /api/v1/user-actions
   */
  getUserActions = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const query = {
        userId: req.user!.id,
        actionType: req.query.actionType as string | undefined,
        actionCategory: req.query.actionCategory as string | undefined,
        endpoint: req.query.endpoint as string | undefined,
        startDate: req.query.startDate as string | undefined,
        endDate: req.query.endDate as string | undefined,
        page: req.query.page ? parseInt(req.query.page as string, 10) : 1,
        limit: req.query.limit ? parseInt(req.query.limit as string, 10) : 50,
      };

      const result = await userActionService.getUserActions(query);
      sendSuccess(res, result, 200);
    } catch (error) {
      logger.error('Error in user action controller', {
        method: 'getUserActions',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Get user action by ID
   * GET /api/v1/user-actions/:id
   */
  getUserActionById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'BAD_REQUEST', 'Invalid action ID', 400);
        return;
      }

      const action = await userActionService.getUserActionById(id);
      if (!action) {
        sendError(res, 'NOT_FOUND', 'User action not found', 404);
        return;
      }

      // Verify the action belongs to the user
      if (action.userId !== req.user!.id) {
        sendError(res, 'FORBIDDEN', 'Access denied', 403);
        return;
      }

      sendSuccess(res, action, 200);
    } catch (error) {
      logger.error('Error in user action controller', {
        method: 'getUserActionById',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Get user action statistics
   * GET /api/v1/user-actions/statistics
   */
  getStatistics = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const startDate = req.query.startDate as string | undefined;
      const endDate = req.query.endDate as string | undefined;

      const stats = await userActionService.getStatistics(req.user!.id, startDate, endDate);
      sendSuccess(res, stats, 200);
    } catch (error) {
      logger.error('Error in user action controller', {
        method: 'getStatistics',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };
}
