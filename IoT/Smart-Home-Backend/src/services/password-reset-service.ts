import bcrypt from 'bcryptjs';
import { PasswordResetRepository } from '../repositories/password-reset-repository';
import { UserRepository } from '../repositories/user-repository';
import {
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  VerifyOTPRequest,
  VerifyOTPResponse,
  ResetPasswordRequest,
  ResetPasswordResponse,
} from '../models/password-reset';
import logger from '../utils/logger';

// OTP configuration
const OTP_EXPIRY_MINUTES = 15;
const MAX_OTP_ATTEMPTS_PER_10_MIN = 3; // Prevent spam

export class PasswordResetService {
  constructor(
    private passwordResetRepository: PasswordResetRepository,
    private userRepository: UserRepository
  ) {}

  /**
   * Generate a random 6-digit OTP
   */
  private generateOTP(): string {
    return Math.floor(100000 + Math.random() * 900000).toString();
  }

  /**
   * Send OTP email (mock implementation - replace with actual email service)
   */
  private async sendOTPEmail(email: string, otpCode: string): Promise<void> {
    // TODO: Integrate with email service (SendGrid, AWS SES, Nodemailer, etc.)
    // For now, log the OTP (in production, this should send an actual email)
    logger.info('OTP Code Generated', {
      email,
      otpCode,
      message: 'In production, this would be sent via email',
    });

    // Mock email sending - in production, use:
    // await emailService.send({
    //   to: email,
    //   subject: 'Password Reset OTP - Smartify',
    //   template: 'password-reset',
    //   data: { otpCode, expiresInMinutes: OTP_EXPIRY_MINUTES }
    // });
  }

  /**
   * Request password reset - send OTP to email
   */
  async forgotPassword(request: ForgotPasswordRequest): Promise<ForgotPasswordResponse> {
    try {
      const { email } = request;

      // Find user by email
      const user = await this.userRepository.findByEmail(email);
      if (!user) {
        // Don't reveal if email exists or not (security best practice)
        // Return success message even if user doesn't exist
        return {
          message: 'If the email exists, an OTP code has been sent.',
          otpSent: false,
          expiresInMinutes: OTP_EXPIRY_MINUTES,
        };
      }

      // Check for recent OTP requests (prevent spam)
      const recentOTPCount = await this.passwordResetRepository.getRecentOTPCount(email, 10);
      if (recentOTPCount >= MAX_OTP_ATTEMPTS_PER_10_MIN) {
        logger.warn('Too many OTP requests', { email, count: recentOTPCount });
        return {
          message: 'Too many requests. Please try again in 10 minutes.',
          otpSent: false,
          expiresInMinutes: OTP_EXPIRY_MINUTES,
        };
      }

      // Generate OTP
      const otpCode = this.generateOTP();
      const expiresAt = new Date();
      expiresAt.setMinutes(expiresAt.getMinutes() + OTP_EXPIRY_MINUTES);

      // Save OTP to database
      await this.passwordResetRepository.createOTP(user.id, email, otpCode, expiresAt);

      // Send OTP via email
      await this.sendOTPEmail(email, otpCode);

      logger.info('Password reset OTP sent', { email, userId: user.id });

      return {
        message: 'OTP code has been sent to your email.',
        otpSent: true,
        expiresInMinutes: OTP_EXPIRY_MINUTES,
      };
    } catch (error) {
      logger.error('Error in forgotPassword', {
        error: error instanceof Error ? error.message : String(error),
        email: request.email,
      });
      throw error;
    }
  }

  /**
   * Verify OTP code
   */
  async verifyOTP(request: VerifyOTPRequest): Promise<VerifyOTPResponse> {
    try {
      const { email, otpCode } = request;

      // Find valid OTP
      const otp = await this.passwordResetRepository.findValidOTP(email, otpCode);

      if (!otp) {
        return {
          valid: false,
          message: 'Invalid or expired OTP code.',
        };
      }

      // Generate temporary token for password reset (optional - can use OTP directly)
      // For simplicity, we'll just verify and allow password reset
      logger.info('OTP verified successfully', { email, otpId: otp.id });

      return {
        valid: true,
        message: 'OTP verified successfully.',
      };
    } catch (error) {
      logger.error('Error in verifyOTP', {
        error: error instanceof Error ? error.message : String(error),
        email: request.email,
      });
      throw error;
    }
  }

  /**
   * Reset password with OTP
   */
  async resetPassword(request: ResetPasswordRequest): Promise<ResetPasswordResponse> {
    try {
      const { email, otpCode, newPassword } = request;

      // Verify OTP
      const otp = await this.passwordResetRepository.findValidOTP(email, otpCode);
      if (!otp) {
        return {
          success: false,
          message: 'Invalid or expired OTP code.',
        };
      }

      // Find user
      const user = await this.userRepository.findByEmail(email);
      if (!user) {
        return {
          success: false,
          message: 'User not found.',
        };
      }

      // Validate new password
      if (!newPassword || newPassword.length < 6) {
        return {
          success: false,
          message: 'Password must be at least 6 characters long.',
        };
      }

      // Hash new password
      const saltRounds = 10;
      const hashedPassword = await bcrypt.hash(newPassword, saltRounds);

      // Update user password directly in database
      await this.userRepository.updatePassword(user.id, hashedPassword);

      // Mark OTP as used and invalidate all other OTPs for this user
      await this.passwordResetRepository.markOTPAsUsed(otp.id);
      await this.passwordResetRepository.invalidateUserOTPs(user.id);

      logger.info('Password reset successful', { email, userId: user.id });

      return {
        success: true,
        message: 'Password has been reset successfully.',
      };
    } catch (error) {
      logger.error('Error in resetPassword', {
        error: error instanceof Error ? error.message : String(error),
        email: request.email,
      });
      throw error;
    }
  }
}

