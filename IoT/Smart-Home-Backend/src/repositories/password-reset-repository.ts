import { Pool, QueryResult } from 'pg';
import { PasswordResetOTP } from '../models/password-reset';
import logger from '../utils/logger';

export class PasswordResetRepository {
  constructor(private pool: Pool) {}

  /**
   * Create a new OTP record
   */
  async createOTP(
    userId: number,
    email: string,
    otpCode: string,
    expiresAt: Date
  ): Promise<PasswordResetOTP> {
    const query = `
      INSERT INTO password_reset_otp (user_id, email, otp_code, expires_at)
      VALUES ($1, $2, $3, $4)
      RETURNING *;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [
        userId,
        email,
        otpCode,
        expiresAt,
      ]);

      const row = result.rows[0];
      return {
        id: row.id,
        userId: row.user_id,
        email: row.email,
        otpCode: row.otp_code,
        expiresAt: new Date(row.expires_at),
        used: row.used,
        createdAt: new Date(row.created_at),
      };
    } catch (error) {
      logger.error('Error creating OTP', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        email,
      });
      throw error;
    }
  }

  /**
   * Find valid OTP by email and code
   */
  async findValidOTP(email: string, otpCode: string): Promise<PasswordResetOTP | null> {
    const query = `
      SELECT * FROM password_reset_otp
      WHERE email = $1
        AND otp_code = $2
        AND used = FALSE
        AND expires_at > CURRENT_TIMESTAMP
      ORDER BY created_at DESC
      LIMIT 1;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [email, otpCode]);

      if (result.rows.length === 0) {
        return null;
      }

      const row = result.rows[0];
      return {
        id: row.id,
        userId: row.user_id,
        email: row.email,
        otpCode: row.otp_code,
        expiresAt: new Date(row.expires_at),
        used: row.used,
        createdAt: new Date(row.created_at),
      };
    } catch (error) {
      logger.error('Error finding OTP', {
        error: error instanceof Error ? error.message : String(error),
        email,
      });
      throw error;
    }
  }

  /**
   * Mark OTP as used
   */
  async markOTPAsUsed(otpId: number): Promise<void> {
    const query = `
      UPDATE password_reset_otp
      SET used = TRUE
      WHERE id = $1;
    `;

    try {
      await this.pool.query(query, [otpId]);
    } catch (error) {
      logger.error('Error marking OTP as used', {
        error: error instanceof Error ? error.message : String(error),
        otpId,
      });
      throw error;
    }
  }

  /**
   * Get recent OTP count for email (to prevent spam)
   */
  async getRecentOTPCount(email: string, minutes: number = 10): Promise<number> {
    const query = `
      SELECT COUNT(*) as count
      FROM password_reset_otp
      WHERE email = $1
        AND created_at > CURRENT_TIMESTAMP - INTERVAL '${minutes} minutes';
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [email]);
      return parseInt(result.rows[0].count, 10);
    } catch (error) {
      logger.error('Error getting recent OTP count', {
        error: error instanceof Error ? error.message : String(error),
        email,
      });
      return 0;
    }
  }

  /**
   * Invalidate all OTPs for a user (when password is reset)
   */
  async invalidateUserOTPs(userId: number): Promise<void> {
    const query = `
      UPDATE password_reset_otp
      SET used = TRUE
      WHERE user_id = $1 AND used = FALSE;
    `;

    try {
      await this.pool.query(query, [userId]);
    } catch (error) {
      logger.error('Error invalidating user OTPs', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Clean up expired OTPs (can be called by scheduled job)
   */
  async cleanupExpiredOTPs(): Promise<number> {
    const query = `
      DELETE FROM password_reset_otp
      WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 day';
    `;

    try {
      const result: QueryResult = await this.pool.query(query);
      return result.rowCount || 0;
    } catch (error) {
      logger.error('Error cleaning up expired OTPs', {
        error: error instanceof Error ? error.message : String(error),
      });
      return 0;
    }
  }
}

