import { Pool } from 'pg';
import { Room, CreateRoomInput, UpdateRoomInput } from '../models/room';
import { IRoomRepository } from '../domain/repositories/IRoomRepository';

/**
 * Room repository for database operations
 * Implements IRoomRepository interface
 */
export class RoomRepository implements IRoomRepository {
  constructor(private pool: Pool) {}

  /**
   * Find room by ID and user ID
   */
  async findById(id: number, userId: number): Promise<Room | null> {
    const result = await this.pool.query(
      'SELECT id, user_id, home_id, name, created_at, updated_at FROM rooms WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    if (result.rows.length === 0) {
      return null;
    }
    return this.mapRowToRoom(result.rows[0]);
  }
  
  /**
   * Map database row to Room model
   */
  private mapRowToRoom(row: any): Room {
    return {
      id: row.id,
      userId: row.user_id,
      homeId: row.home_id,
      name: row.name,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  /**
   * Find all rooms for a user
   */
  async findAll(userId: number, homeId?: number): Promise<Room[]> {
    let result;
    if (homeId !== undefined) {
      result = await this.pool.query(
        'SELECT id, user_id, home_id, name, created_at, updated_at FROM rooms WHERE user_id = $1 AND home_id = $2 ORDER BY name ASC',
        [userId, homeId]
      );
    } else {
      result = await this.pool.query(
        'SELECT id, user_id, home_id, name, created_at, updated_at FROM rooms WHERE user_id = $1 ORDER BY name ASC',
        [userId]
      );
    }
    return result.rows.map(row => this.mapRowToRoom(row));
  }

  /**
   * Find room by name and user ID
   */
  async findByName(name: string, userId: number): Promise<Room | null> {
    const result = await this.pool.query(
      'SELECT id, user_id, home_id, name, created_at, updated_at FROM rooms WHERE name = $1 AND user_id = $2',
      [name, userId]
    );
    if (result.rows.length === 0) {
      return null;
    }
    return this.mapRowToRoom(result.rows[0]);
  }

  /**
   * Create new room
   */
  async create(userId: number, input: CreateRoomInput): Promise<Room> {
    const result = await this.pool.query(
      'INSERT INTO rooms (user_id, name, home_id) VALUES ($1, $2, $3) RETURNING id, user_id, home_id, name, created_at, updated_at',
      [userId, input.name, input.homeId || null]
    );
    return this.mapRowToRoom(result.rows[0]);
  }

  /**
   * Update room
   */
  async update(id: number, userId: number, input: UpdateRoomInput): Promise<Room> {
    const updates: string[] = [];
    const values: unknown[] = [];
    let paramCount = 1;

    if (input.name !== undefined) {
      updates.push(`name = $${paramCount++}`);
      values.push(input.name);
    }
    if (input.homeId !== undefined) {
      updates.push(`home_id = $${paramCount++}`);
      values.push(input.homeId);
    }

    if (updates.length === 0) {
      // No updates, return existing room
      const existing = await this.findById(id, userId);
      if (!existing) {
        throw new Error('Room not found');
      }
      return existing;
    }

    updates.push(`updated_at = NOW()`);
    values.push(id, userId);
    const result = await this.pool.query(
      `UPDATE rooms SET ${updates.join(', ')} WHERE id = $${paramCount++} AND user_id = $${paramCount++} RETURNING id, user_id, home_id, name, created_at, updated_at`,
      values
    );

    if (result.rows.length === 0) {
      throw new Error('Room not found');
    }

    return this.mapRowToRoom(result.rows[0]);
  }

  /**
   * Delete room
   */
  async delete(id: number, userId: number): Promise<void> {
    const result = await this.pool.query(
      'DELETE FROM rooms WHERE id = $1 AND user_id = $2',
      [id, userId]
    );

    if (result.rowCount === 0) {
      throw new Error('Room not found');
    }
  }
}

