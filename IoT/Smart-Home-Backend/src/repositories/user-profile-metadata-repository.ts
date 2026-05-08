import { Pool } from 'pg';

/**
 * User profile metadata repository
 */
export class UserProfileMetadataRepository {
  constructor(private pool: Pool) {}

  /**
   * Get all profile metadata for a user
   */
  async findAll(userId: number): Promise<Record<string, any>> {
    const result = await this.pool.query(
      'SELECT metadata_key, metadata_value FROM user_profile_metadata WHERE user_id = $1',
      [userId]
    );

    const metadata: Record<string, any> = {};
    for (const row of result.rows) {
      metadata[row.metadata_key] = row.metadata_value;
    }
    return metadata;
  }

  /**
   * Get a specific metadata entry
   */
  async findOne(userId: number, key: string): Promise<any | null> {
    const result = await this.pool.query(
      'SELECT metadata_value FROM user_profile_metadata WHERE user_id = $1 AND metadata_key = $2',
      [userId, key]
    );
    return result.rows.length > 0 ? result.rows[0].metadata_value : null;
  }

  /**
   * Update or create a metadata entry
   */
  async upsert(userId: number, key: string, value: any): Promise<void> {
    await this.pool.query(
      `INSERT INTO user_profile_metadata (user_id, metadata_key, metadata_value)
       VALUES ($1, $2, $3)
       ON CONFLICT (user_id, metadata_key)
       DO UPDATE SET metadata_value = EXCLUDED.metadata_value, updated_at = CURRENT_TIMESTAMP`,
      [userId, key, JSON.stringify(value)]
    );
  }

  /**
   * Bulk update metadata
   */
  async bulkUpdate(userId: number, metadata: Record<string, any>): Promise<void> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      for (const [key, value] of Object.entries(metadata)) {
        await client.query(
          `INSERT INTO user_profile_metadata (user_id, metadata_key, metadata_value)
           VALUES ($1, $2, $3)
           ON CONFLICT (user_id, metadata_key)
           DO UPDATE SET metadata_value = EXCLUDED.metadata_value, updated_at = CURRENT_TIMESTAMP`,
          [userId, key, JSON.stringify(value)]
        );
      }
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  }

  /**
   * Delete a metadata entry
   */
  async delete(userId: number, key: string): Promise<void> {
    await this.pool.query(
      'DELETE FROM user_profile_metadata WHERE user_id = $1 AND metadata_key = $2',
      [userId, key]
    );
  }
}
