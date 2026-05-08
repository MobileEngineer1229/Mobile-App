import { Pool } from 'pg';
import { User, CreateUserInput, UpdateUserInput } from '../models/user';
import { NotFoundError, ConflictError } from '../utils/errors';
import { IUserRepository } from '../domain/repositories/IUserRepository';

/**
 * User repository for database operations
 * Implements IUserRepository interface
 */
export class UserRepository implements IUserRepository {
  constructor(private pool: Pool) {}

  /**
   * Map database row to User model
   */
  private mapRowToUser(row: any): User {
    return {
      id: row.id,
      email: row.email,
      password: row.password,
      firstName: row.first_name,
      lastName: row.last_name,
      phone: row.phone || undefined,
      notificationPreferences: row.notification_preferences || {},
      securitySettings: row.security_settings || {},
      profileMetadata: row.profile_metadata || {},
      additionalSettings: row.additional_settings || {},
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  /**
   * Find user by ID
   */
  async findById(id: number): Promise<User | null> {
    const result = await this.pool.query(
      `SELECT 
        id, email, password, first_name, last_name, phone,
        notification_preferences, security_settings, profile_metadata, additional_settings,
        created_at, updated_at
      FROM users WHERE id = $1`,
      [id]
    );
    if (result.rows.length === 0) {
      return null;
    }
    return this.mapRowToUser(result.rows[0]);
  }

  /**
   * Find user by email
   */
  async findByEmail(email: string): Promise<User | null> {
    const result = await this.pool.query(
      `SELECT 
        id, email, password, first_name, last_name, phone,
        notification_preferences, security_settings, profile_metadata, additional_settings,
        created_at, updated_at
      FROM users WHERE email = $1`,
      [email]
    );
    if (result.rows.length === 0) {
      return null;
    }
    return this.mapRowToUser(result.rows[0]);
  }

  /**
   * Create new user
   */
  async create(input: CreateUserInput): Promise<User> {
    // Check if email already exists
    const existingUser = await this.findByEmail(input.email);
    if (existingUser) {
      throw new ConflictError('Email already exists');
    }

    const result = await this.pool.query(
      `INSERT INTO users (email, password, first_name, last_name, phone, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
       RETURNING id`,
      [input.email, input.password, input.firstName, input.lastName, input.phone || null]
    );

    const insertId = result.rows[0].id;
    const user = await this.findById(insertId);

    if (!user) {
      throw new Error('Failed to create user');
    }

    return user;
  }

  /**
   * Update user
   */
  async update(id: number, input: UpdateUserInput): Promise<User> {
    const updates: string[] = [];
    const values: unknown[] = [];
    let paramIndex = 1;

    if (input.firstName !== undefined) {
      updates.push(`first_name = $${paramIndex}`);
      values.push(input.firstName);
      paramIndex++;
    }
    if (input.lastName !== undefined) {
      updates.push(`last_name = $${paramIndex}`);
      values.push(input.lastName);
      paramIndex++;
    }
    if (input.phone !== undefined) {
      updates.push(`phone = $${paramIndex}`);
      values.push(input.phone);
      paramIndex++;
    }
    if ((input as any).password !== undefined) {
      updates.push(`password = $${paramIndex}`);
      values.push((input as any).password);
      paramIndex++;
    }

    if (updates.length === 0) {
      const user = await this.findById(id);
      if (!user) {
        throw new NotFoundError('User');
      }
      return user;
    }

    // updated_at is automatically updated by trigger
    values.push(id);

    await this.pool.query(
      `UPDATE users SET ${updates.join(', ')} WHERE id = $${paramIndex}`,
      values
    );

    const user = await this.findById(id);
    if (!user) {
      throw new NotFoundError('User');
    }

    return user;
  }

  /**
   * Update user password
   */
  async updatePassword(id: number, hashedPassword: string): Promise<void> {
    const result = await this.pool.query(
      'UPDATE users SET password = $1 WHERE id = $2',
      [hashedPassword, id]
    );

    if (result.rowCount === 0) {
      throw new NotFoundError('User');
    }
  }

  /**
   * Delete user
   */
  async delete(id: number): Promise<void> {
    const result = await this.pool.query('DELETE FROM users WHERE id = $1', [id]);
    const affectedRows = result.rowCount || 0;

    if (affectedRows === 0) {
      throw new NotFoundError('User');
    }
  }

  /**
   * Update notification preferences
   */
  async updateNotificationPreferences(
    id: number,
    preferences: Record<string, boolean>
  ): Promise<User> {
    const result = await this.pool.query(
      `UPDATE users 
       SET notification_preferences = $1, updated_at = CURRENT_TIMESTAMP
       WHERE id = $2
       RETURNING id`,
      [JSON.stringify(preferences), id]
    );

    if (result.rowCount === 0) {
      throw new NotFoundError('User');
    }

    const user = await this.findById(id);
    if (!user) {
      throw new NotFoundError('User');
    }
    return user;
  }

  /**
   * Update security settings
   */
  async updateSecuritySettings(
    id: number,
    settings: Record<string, boolean | Record<string, any>>
  ): Promise<User> {
    const result = await this.pool.query(
      `UPDATE users 
       SET security_settings = $1, updated_at = CURRENT_TIMESTAMP
       WHERE id = $2
       RETURNING id`,
      [JSON.stringify(settings), id]
    );

    if (result.rowCount === 0) {
      throw new NotFoundError('User');
    }

    const user = await this.findById(id);
    if (!user) {
      throw new NotFoundError('User');
    }
    return user;
  }

  /**
   * Update profile metadata
   */
  async updateProfileMetadata(
    id: number,
    metadata: Record<string, any>
  ): Promise<User> {
    const result = await this.pool.query(
      `UPDATE users 
       SET profile_metadata = $1, updated_at = CURRENT_TIMESTAMP
       WHERE id = $2
       RETURNING id`,
      [JSON.stringify(metadata), id]
    );

    if (result.rowCount === 0) {
      throw new NotFoundError('User');
    }

    const user = await this.findById(id);
    if (!user) {
      throw new NotFoundError('User');
    }
    return user;
  }

  /**
   * Update additional settings
   */
  async updateAdditionalSettings(
    id: number,
    settings: Record<string, any>
  ): Promise<User> {
    const result = await this.pool.query(
      `UPDATE users 
       SET additional_settings = $1, updated_at = CURRENT_TIMESTAMP
       WHERE id = $2
       RETURNING id`,
      [JSON.stringify(settings), id]
    );

    if (result.rowCount === 0) {
      throw new NotFoundError('User');
    }

    const user = await this.findById(id);
    if (!user) {
      throw new NotFoundError('User');
    }
    return user;
  }
}

