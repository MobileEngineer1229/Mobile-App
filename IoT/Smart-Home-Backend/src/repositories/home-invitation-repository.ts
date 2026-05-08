import { Pool } from 'pg';
import { HomeInvitation, CreateHomeInvitationInput } from '../models/home-invitation';
import { IHomeInvitationRepository } from '../domain/repositories/IHomeInvitationRepository';

/**
 * Home invitation repository for database operations
 * Implements IHomeInvitationRepository interface
 */
export class HomeInvitationRepository implements IHomeInvitationRepository {
  constructor(private pool: Pool) {}

  /**
   * Find invitation by code
   */
  async findByCode(code: string): Promise<HomeInvitation | null> {
    const result = await this.pool.query<HomeInvitation>(
      'SELECT * FROM home_invitations WHERE code = $1',
      [code]
    );
    return result.rows[0] || null;
  }

  /**
   * Find all invitations for a home
   */
  async findByHomeId(homeId: number): Promise<HomeInvitation[]> {
    const result = await this.pool.query<HomeInvitation>(
      'SELECT * FROM home_invitations WHERE home_id = $1 ORDER BY created_at DESC',
      [homeId]
    );
    return result.rows;
  }

  /**
   * Find active invitation by code
   */
  async findActiveByCode(code: string): Promise<HomeInvitation | null> {
    const result = await this.pool.query<HomeInvitation>(
      `SELECT * FROM home_invitations
       WHERE code = $1
         AND is_active = true
         AND (expires_at IS NULL OR expires_at > NOW())
         AND (max_uses = 0 OR current_uses < max_uses)`,
      [code]
    );
    return result.rows[0] || null;
  }

  /**
   * Create new invitation
   */
  async create(
    homeId: number,
    code: string,
    createdBy: number,
    input: CreateHomeInvitationInput
  ): Promise<HomeInvitation> {
    const result = await this.pool.query<HomeInvitation>(
      `INSERT INTO home_invitations (home_id, code, created_by, expires_at, max_uses)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [
        homeId,
        code,
        createdBy,
        input.expiresAt ? new Date(input.expiresAt) : null,
        input.maxUses ?? 1,
      ]
    );
    return result.rows[0];
  }

  /**
   * Increment invitation usage count
   */
  async incrementUses(code: string): Promise<void> {
    await this.pool.query(
      'UPDATE home_invitations SET current_uses = current_uses + 1 WHERE code = $1',
      [code]
    );
  }

  /**
   * Deactivate invitation
   */
  async deactivate(code: string): Promise<void> {
    await this.pool.query('UPDATE home_invitations SET is_active = false WHERE code = $1', [
      code,
    ]);
  }

  /**
   * Delete invitation
   */
  async delete(code: string): Promise<void> {
    await this.pool.query('DELETE FROM home_invitations WHERE code = $1', [code]);
  }

  /**
   * Generate unique invitation code
   */
  async generateUniqueCode(): Promise<string> {
    const characters = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Exclude confusing characters
    let code: string;
    let exists = true;

    // Try up to 10 times to generate unique code
    for (let i = 0; i < 10; i++) {
      code = '';
      for (let j = 0; j < 6; j++) {
        code += characters.charAt(Math.floor(Math.random() * characters.length));
      }

      const existing = await this.findByCode(code);
      if (!existing) {
        exists = false;
        break;
      }
    }

    if (exists) {
      throw new Error('Failed to generate unique invitation code');
    }

    return code!;
  }
}
