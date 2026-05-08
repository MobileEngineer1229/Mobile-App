import { Pool } from 'pg';

export interface LinkedAccount {
  id: number;
  userId: number;
  provider: string;
  providerUserId?: string;
  accessToken?: string;
  refreshToken?: string;
  expiresAt?: string;
  metadata?: Record<string, any>;
  connectedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export class LinkedAccountRepository {
  constructor(private pool: Pool) {}

  /**
   * Get all linked accounts for a user
   */
  async findByUserId(userId: number): Promise<LinkedAccount[]> {
    const query = `
      SELECT 
        id,
        user_id as "userId",
        provider,
        provider_user_id as "providerUserId",
        access_token as "accessToken",
        refresh_token as "refreshToken",
        expires_at as "expiresAt",
        metadata,
        connected_at as "connectedAt",
        created_at as "createdAt",
        updated_at as "updatedAt"
      FROM linked_accounts
      WHERE user_id = $1
      ORDER BY provider ASC
    `;
    const result = await this.pool.query(query, [userId]);
    return result.rows.map(row => ({
      ...row,
      metadata: row.metadata || {},
    }));
  }

  /**
   * Link an account
   */
  async linkAccount(
    userId: number,
    provider: string,
    data: {
      providerUserId?: string;
      accessToken?: string;
      refreshToken?: string;
      expiresAt?: string;
      metadata?: Record<string, any>;
    }
  ): Promise<LinkedAccount> {
    const query = `
      INSERT INTO linked_accounts (
        user_id, provider, provider_user_id, access_token, refresh_token, expires_at, metadata
      )
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      ON CONFLICT (user_id, provider)
      DO UPDATE SET
        provider_user_id = EXCLUDED.provider_user_id,
        access_token = EXCLUDED.access_token,
        refresh_token = EXCLUDED.refresh_token,
        expires_at = EXCLUDED.expires_at,
        metadata = EXCLUDED.metadata,
        connected_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
      RETURNING 
        id,
        user_id as "userId",
        provider,
        provider_user_id as "providerUserId",
        access_token as "accessToken",
        refresh_token as "refreshToken",
        expires_at as "expiresAt",
        metadata,
        connected_at as "connectedAt",
        created_at as "createdAt",
        updated_at as "updatedAt"
    `;
    const result = await this.pool.query(query, [
      userId,
      provider,
      data.providerUserId || null,
      data.accessToken || null,
      data.refreshToken || null,
      data.expiresAt || null,
      data.metadata ? JSON.stringify(data.metadata) : null,
    ]);
    return {
      ...result.rows[0],
      metadata: result.rows[0].metadata || {},
    };
  }

  /**
   * Unlink an account
   */
  async unlinkAccount(userId: number, accountId: number): Promise<void> {
    const query = `
      DELETE FROM linked_accounts
      WHERE id = $1 AND user_id = $2
    `;
    await this.pool.query(query, [accountId, userId]);
  }

  /**
   * Get all available providers
   */
  async getAvailableProviders(): Promise<string[]> {
    // Return standard providers
    return ['Google', 'Apple', 'Facebook', 'Twitter'];
  }
}

