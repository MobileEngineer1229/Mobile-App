import { Pool } from 'pg';
import { Home, CreateHomeInput, UpdateHomeInput } from '../models/home';
import { IHomeRepository } from '../domain/repositories/IHomeRepository';

/**
 * Home repository for database operations
 * Implements IHomeRepository interface
 */
export class HomeRepository implements IHomeRepository {
  constructor(private pool: Pool) {}

  /**
   * Find home by ID and user ID (checks both owner and members)
   */
  async findById(id: number, userId: number): Promise<Home | null> {
    const result = await this.pool.query<Home>(
      `SELECT h.* FROM homes h
       WHERE h.id = $1 
         AND (h.user_id = $2 OR EXISTS (
           SELECT 1 FROM home_members hm 
           WHERE hm.home_id = h.id AND hm.user_id = $2
         ))`,
      [id, userId]
    );
    return result.rows[0] || null;
  }

  /**
   * Find home by ID (no user check - for internal use)
   */
  async findByIdOnly(id: number): Promise<Home | null> {
    const result = await this.pool.query<Home>('SELECT * FROM homes WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  /**
   * Find all homes for a user (including homes where user is a member)
   */
  async findAll(userId: number): Promise<Home[]> {
    const result = await this.pool.query<Home>(
      `SELECT DISTINCT h.* FROM homes h
       LEFT JOIN home_members hm ON h.id = hm.home_id
       WHERE h.user_id = $1 OR hm.user_id = $1
       ORDER BY h.is_primary DESC, h.name ASC`,
      [userId]
    );
    return result.rows;
  }

  /**
   * Find primary home for a user
   */
  async findPrimary(userId: number): Promise<Home | null> {
    const result = await this.pool.query<Home>(
      'SELECT * FROM homes WHERE user_id = $1 AND is_primary = true LIMIT 1',
      [userId]
    );
    return result.rows[0] || null;
  }

  /**
   * Find home by name and user ID
   */
  async findByName(name: string, userId: number): Promise<Home | null> {
    const result = await this.pool.query<Home>(
      'SELECT * FROM homes WHERE name = $1 AND user_id = $2',
      [name, userId]
    );
    return result.rows[0] || null;
  }

  /**
   * Create new home
   */
  async create(userId: number, input: CreateHomeInput): Promise<Home> {
    // If this is set as primary, unset other primary homes
    if (input.isPrimary) {
      await this.pool.query(
        'UPDATE homes SET is_primary = false WHERE user_id = $1',
        [userId]
      );
    }

    const result = await this.pool.query<Home>(
      `INSERT INTO homes (user_id, name, address, latitude, longitude, country, is_primary) 
       VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *`,
      [
        userId,
        input.name,
        input.address || null,
        input.latitude || null,
        input.longitude || null,
        input.country || null,
        input.isPrimary || false,
      ]
    );
    return result.rows[0];
  }

  /**
   * Update home
   */
  async update(id: number, userId: number, input: UpdateHomeInput): Promise<Home> {
    // If setting as primary, unset other primary homes
    if (input.isPrimary === true) {
      await this.pool.query(
        'UPDATE homes SET is_primary = false WHERE user_id = $1 AND id != $2',
        [userId, id]
      );
    }

    const updates: string[] = [];
    const values: unknown[] = [];
    let paramCount = 1;

    if (input.name !== undefined) {
      updates.push(`name = $${paramCount++}`);
      values.push(input.name);
    }
    if (input.address !== undefined) {
      updates.push(`address = $${paramCount++}`);
      values.push(input.address);
    }
    if (input.latitude !== undefined) {
      updates.push(`latitude = $${paramCount++}`);
      values.push(input.latitude);
    }
    if (input.longitude !== undefined) {
      updates.push(`longitude = $${paramCount++}`);
      values.push(input.longitude);
    }
    if (input.country !== undefined) {
      updates.push(`country = $${paramCount++}`);
      values.push(input.country);
    }
    if (input.isPrimary !== undefined) {
      updates.push(`is_primary = $${paramCount++}`);
      values.push(input.isPrimary);
    }

    if (updates.length === 0) {
      // No updates, return existing home
      const existing = await this.findById(id, userId);
      if (!existing) {
        throw new Error('Home not found');
      }
      return existing;
    }

    updates.push(`updated_at = NOW()`);
    values.push(id, userId);
    const result = await this.pool.query<Home>(
      `UPDATE homes SET ${updates.join(', ')} WHERE id = $${paramCount++} AND user_id = $${paramCount++} RETURNING *`,
      values
    );

    if (result.rows.length === 0) {
      throw new Error('Home not found');
    }

    return result.rows[0];
  }

  /**
   * Delete home
   */
  async delete(id: number, userId: number): Promise<void> {
    const result = await this.pool.query(
      'DELETE FROM homes WHERE id = $1 AND user_id = $2',
      [id, userId]
    );

    if (result.rowCount === 0) {
      throw new Error('Home not found');
    }
  }
}
