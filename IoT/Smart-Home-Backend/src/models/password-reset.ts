/**
 * Password reset OTP model
 */
export interface PasswordResetOTP {
  id: number;
  userId: number;
  email: string;
  otpCode: string;
  expiresAt: Date;
  used: boolean;
  createdAt: Date;
}

/**
 * Forgot password request
 */
export interface ForgotPasswordRequest {
  email: string;
}

/**
 * Forgot password response
 */
export interface ForgotPasswordResponse {
  message: string;
  otpSent: boolean;
  expiresInMinutes: number;
}

/**
 * Verify OTP request
 */
export interface VerifyOTPRequest {
  email: string;
  otpCode: string;
}

/**
 * Verify OTP response
 */
export interface VerifyOTPResponse {
  valid: boolean;
  token?: string; // Temporary token for password reset
  message: string;
}

/**
 * Reset password request
 */
export interface ResetPasswordRequest {
  email: string;
  otpCode: string;
  newPassword: string;
}

/**
 * Reset password response
 */
export interface ResetPasswordResponse {
  success: boolean;
  message: string;
}

