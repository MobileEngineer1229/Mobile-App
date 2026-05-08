import { Request, Response, NextFunction } from 'express';
import { UserService } from '../services/user-service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateUserInput, UpdateUserInput, LoginInput } from '../models/user';
import logger from '../utils/logger';

/**
 * User controller
 */
export class UserController {
  constructor(private userService: UserService) {}

  /**
   * Sign up new user
   */
  signup = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const input: CreateUserInput = req.body;
      const user = await this.userService.signup(input);
      sendSuccess(res, user, 201);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'signup',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Login user
   */
  login = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const input: LoginInput = req.body;
      const result = await this.userService.login(input);
      sendSuccess(res, result, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'login',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Get current user profile
   */
  getProfile = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const user = await this.userService.getUserById(req.user!.id);
      sendSuccess(res, user, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'updateProfile',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Update user profile
   */
  updateProfile = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const input: UpdateUserInput = req.body;
      const user = await this.userService.updateProfile(req.user!.id, input);
      sendSuccess(res, user, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'updateProfile',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Get user settings
   */
  getSettings = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const settings = await this.userService.getSettings(req.user!.id);
      sendSuccess(res, settings, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'getSettings',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Update user settings
   */
  updateSettings = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const settings = req.body;
      const updatedSettings = await this.userService.updateSettings(req.user!.id, settings);
      sendSuccess(res, updatedSettings, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'updateSettings',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Logout user
   */
  logout = async (_req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      // In a stateless JWT system, logout is handled client-side by removing the token
      // This endpoint can be used for logging/logging out events or token blacklisting if needed
      sendSuccess(res, { message: 'Logged out successfully' }, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'logout',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Change password
   */
  changePassword = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { currentPassword, newPassword } = req.body;
      if (!currentPassword || !newPassword) {
        sendError(res, 'BAD_REQUEST', 'Current password and new password are required', 400);
        return;
      }

      if (newPassword.length < 8) {
        sendError(res, 'BAD_REQUEST', 'New password must be at least 8 characters', 400);
        return;
      }

      // Verify current password and update
      await this.userService.changePassword(
        req.user!.id,
        currentPassword,
        newPassword
      );
      sendSuccess(res, { message: 'Password changed successfully' }, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'changePassword',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Deactivate account
   */
  deactivateAccount = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      await this.userService.deactivateAccount(req.user!.id);
      sendSuccess(res, { message: 'Account deactivated successfully' }, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'deactivateAccount',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };

  /**
   * Delete account
   */
  deleteAccount = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      await this.userService.deleteAccount(req.user!.id);
      sendSuccess(res, { message: 'Account deleted successfully' }, 200);
    } catch (error) {
      logger.error('Error in user controller', {
        method: 'deleteAccount',
        error: error instanceof Error ? error.message : String(error),
      });
      next(error);
    }
  };
}

