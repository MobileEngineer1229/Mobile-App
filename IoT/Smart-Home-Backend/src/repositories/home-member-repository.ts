import { Pool } from 'pg';
import { HomeMember, HomeMemberRole, UpdateHomeMemberInput } from '../models/home-member';
import { IHomeMemberRepository } from '../domain/repositories/IHomeMemberRepository';

/**
 * Home member repository for database operations
 * Implements IHomeMemberRepository interface
 */
export class HomeMemberRepository implements IHomeMemberRepository {
  constructor(private pool: Pool) {}

  /**
   * Find all members of a home
   */
  async findByHomeId(homeId: number): Promise<HomeMember[]> {
    const result = await this.pool.query<HomeMember>(
      `SELECT hm.* FROM home_members hm
       WHERE hm.home_id = $1
       ORDER BY 
         CASE hm.role
           WHEN 'owner' THEN 1
           WHEN 'admin' THEN 2
           WHEN 'member' THEN 3
         END,
         hm.created_at ASC`,
      [homeId]
    );
    return result.rows;
  }

  /**
   * Find member by ID and home ID
   */
  async findById(memberId: number, homeId: number): Promise<HomeMember | null> {
    const result = await this.pool.query<HomeMember>(
      'SELECT * FROM home_members WHERE id = $1 AND home_id = $2',
      [memberId, homeId]
    );
    return result.rows[0] || null;
  }

  /**
   * Find member by user ID and home ID
   */
  async findByUserIdAndHomeId(userId: number, homeId: number): Promise<HomeMember | null> {
    const result = await this.pool.query<HomeMember>(
      'SELECT * FROM home_members WHERE user_id = $1 AND home_id = $2',
      [userId, homeId]
    );
    return result.rows[0] || null;
  }

  /**
   * Create new home member
   */
  async create(
    homeId: number,
    userId: number,
    role: HomeMemberRole,
    addedBy: number
  ): Promise<HomeMember> {
    const result = await this.pool.query<HomeMember>(
      `INSERT INTO home_members (home_id, user_id, role, added_by)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [homeId, userId, role, addedBy]
    );
    return result.rows[0];
  }

  /**
   * Update home member
   */
  async update(
    memberId: number,
    homeId: number,
    input: UpdateHomeMemberInput
  ): Promise<HomeMember> {
    const updates: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    if (input.role !== undefined) {
      updates.push(`role = $${paramCount++}`);
      values.push(input.role);
    }

    if (updates.length === 0) {
      // No updates, return existing member
      const existing = await this.findById(memberId, homeId);
      if (!existing) {
        throw new Error('Member not found');
      }
      return existing;
    }

    values.push(memberId, homeId);
    const result = await this.pool.query<HomeMember>(
      `UPDATE home_members
       SET ${updates.join(', ')}
       WHERE id = $${paramCount++} AND home_id = $${paramCount++}
       RETURNING *`,
      values
    );

    if (result.rows.length === 0) {
      throw new Error('Member not found');
    }

    return result.rows[0];
  }

  /**
   * Delete home member
   */
  async delete(memberId: number, homeId: number): Promise<void> {
    const result = await this.pool.query(
      'DELETE FROM home_members WHERE id = $1 AND home_id = $2',
      [memberId, homeId]
    );

    if (result.rowCount === 0) {
      throw new Error('Member not found');
    }
  }

  /**
   * Check if user is a member of home
   */
  async isMember(userId: number, homeId: number): Promise<boolean> {
    const result = await this.pool.query(
      'SELECT 1 FROM home_members WHERE user_id = $1 AND home_id = $2 LIMIT 1',
      [userId, homeId]
    );
    return result.rows.length > 0;
  }

  /**
   * Get member role
   */
  async getMemberRole(userId: number, homeId: number): Promise<HomeMemberRole | null> {
    const result = await this.pool.query<{ role: HomeMemberRole }>(
      'SELECT role FROM home_members WHERE user_id = $1 AND home_id = $2',
      [userId, homeId]
    );
    return result.rows[0]?.role || null;
  }
}
