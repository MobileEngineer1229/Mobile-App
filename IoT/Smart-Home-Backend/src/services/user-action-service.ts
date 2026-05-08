import { UserActionRepository } from '../repositories/user-action-repository';
import { CreateUserActionInput, UserActionQuery } from '../models/user-action';
import logger from '../utils/logger';

export class UserActionService {
  constructor(private userActionRepository: UserActionRepository) {}

  /**
   * Log a user action
   */
  async logAction(input: CreateUserActionInput): Promise<void> {
    try {
      // Validate required fields
      if (!input.userId || input.userId <= 0) {
        logger.warn('Skipping user action log - invalid userId', {
          userId: input.userId,
          endpoint: input.endpoint,
          actionType: input.actionType,
        });
        return;
      }

      // Verify table exists (only log warning, don't fail)
      const tableExists = await this.userActionRepository.verifyTableExists();
      if (!tableExists) {
        logger.error('User actions table does not exist. Please run migration: migration_add_user_actions.sql', {
          endpoint: input.endpoint,
          actionType: input.actionType,
        });
        return;
      }

      await this.userActionRepository.create(input);
      logger.infoWithEmoji('📝', `User action logged: ${input.actionType}`, 'USER_ACTION', {
        userId: input.userId,
        endpoint: input.endpoint,
        actionType: input.actionType,
      });
    } catch (error) {
      // Don't throw error - logging should not break the application
      // But log detailed error for debugging
      logger.error('Failed to log user action in service', {
        error: error instanceof Error ? error.message : String(error),
        errorStack: error instanceof Error ? error.stack : undefined,
        userId: input.userId,
        endpoint: input.endpoint,
        actionType: input.actionType,
      });
    }
  }

  /**
   * Get user actions with filters
   */
  async getUserActions(query: UserActionQuery) {
    const result = await this.userActionRepository.findAll(query);
    logger.infoWithEmoji('📊', `Retrieved ${result.actions.length} user actions`, 'USER_ACTION', {
      total: result.total,
      query,
    });
    return result;
  }

  /**
   * Get user action by ID
   */
  async getUserActionById(id: number) {
    const action = await this.userActionRepository.findById(id);
    if (!action) {
      logger.warn('User action not found', { id });
    }
    return action;
  }

  /**
   * Get action statistics for a user
   */
  async getStatistics(userId: number, startDate?: string, endDate?: string) {
    const stats = await this.userActionRepository.getStatistics(userId, startDate, endDate);
    logger.infoWithEmoji('📈', `Retrieved action statistics for user ${userId}`, 'USER_ACTION');
    return stats;
  }
}
