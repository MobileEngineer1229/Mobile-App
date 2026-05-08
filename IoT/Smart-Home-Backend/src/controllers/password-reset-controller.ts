import { Request, Response } from 'express';
import { PasswordResetService } from '../services/password-reset-service';
import {
  ForgotPasswordRequest,
  VerifyOTPRequest,
  ResetPasswordRequest,
} from '../models/password-reset';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger';

export class PasswordResetController {
  constructor(private passwordResetService: PasswordResetService) {}

  /**
   * Forgot password - send OTP
   * POST /api/v1/users/forgot-password
   */
  forgotPassword = async (req: Request, res: Response): Promise<void> => {
    try {
      const request: ForgotPasswordRequest = {
        email: req.body.email?.toLowerCase().trim(),
      };

      // Validation
      if (!request.email) {
        sendError(res, 'VALIDATION_ERROR', 'Email is required', 400);
        return;
      }

      // Basic email validation
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(request.email)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid email format', 400);
        return;
      }

      const response = await this.passwordResetService.forgotPassword(request);
      sendSuccess(res, response, 200);
    } catch (error) {
      logger.error('Error in forgotPassword controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to process forgot password request', 500);
    }
  };

  /**
   * Verify OTP code
   * POST /api/v1/users/verify-otp
   */
  verifyOTP = async (req: Request, res: Response): Promise<void> => {
    try {
      const request: VerifyOTPRequest = {
        email: req.body.email?.toLowerCase().trim(),
        otpCode: req.body.otpCode?.trim(),
      };

      // Validation
      if (!request.email) {
        sendError(res, 'VALIDATION_ERROR', 'Email is required', 400);
        return;
      }

      if (!request.otpCode || request.otpCode.length !== 6) {
        sendError(res, 'VALIDATION_ERROR', 'OTP code must be 6 digits', 400);
        return;
      }

      const response = await this.passwordResetService.verifyOTP(request);
      
      if (response.valid) {
        sendSuccess(res, response, 200);
      } else {
        sendError(res, 'INVALID_OTP', response.message, 400);
      }
    } catch (error) {
      logger.error('Error in verifyOTP controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to verify OTP', 500);
    }
  };

  /**
   * Reset password with OTP
   * POST /api/v1/users/reset-password
   */
  resetPassword = async (req: Request, res: Response): Promise<void> => {
    try {
      const request: ResetPasswordRequest = {
        email: req.body.email?.toLowerCase().trim(),
        otpCode: req.body.otpCode?.trim(),
        newPassword: req.body.newPassword,
      };

      // Validation
      if (!request.email) {
        sendError(res, 'VALIDATION_ERROR', 'Email is required', 400);
        return;
      }

      if (!request.otpCode || request.otpCode.length !== 6) {
        sendError(res, 'VALIDATION_ERROR', 'OTP code must be 6 digits', 400);
        return;
      }

      if (!request.newPassword) {
        sendError(res, 'VALIDATION_ERROR', 'New password is required', 400);
        return;
      }

      if (request.newPassword.length < 6) {
        sendError(res, 'VALIDATION_ERROR', 'Password must be at least 6 characters long', 400);
        return;
      }

      const response = await this.passwordResetService.resetPassword(request);

      if (response.success) {
        sendSuccess(res, response, 200);
      } else {
        sendError(res, 'RESET_FAILED', response.message, 400);
      }
    } catch (error) {
      logger.error('Error in resetPassword controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to reset password', 500);
    }
  };
}

