import { Pool } from 'pg';

/**
 * User additional settings repository
 */
export class UserAdditionalSettingsRepository {
  constructor(private pool: Pool) {}

  /**
   * Get all additional settings for a user
   */
  async findAll(userId: number): Promise<Record<string, any>> {
    const result = await this.pool.query(
      'SELECT setting_key, setting_value FROM user_additional_settings WHERE user_id = $1',
      [userId]
    );

    const settings: Record<string, any> = {};
    for (const row of result.rows) {
      settings[row.setting_key] = row.setting_value;
    }
    return settings;
  }

  /**
   * Get a specific setting
   */
  async findOne(userId: number, key: string): Promise<any | null> {
    const result = await this.pool.query(
      'SELECT setting_value FROM user_additional_settings WHERE user_id = $1 AND setting_key = $2',
      [userId, key]
    );
    return result.rows.length > 0 ? result.rows[0].setting_value : null;
  }

  /**
   * Update or create a setting
   */
  async upsert(userId: number, key: string, value: any): Promise<void> {
    await this.pool.query(
      `INSERT INTO user_additional_settings (user_id, setting_key, setting_value)
       VALUES ($1, $2, $3)
       ON CONFLICT (user_id, setting_key)
       DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP`,
      [userId, key, JSON.stringify(value)]
    );
  }

  /**
   * Bulk update settings
   */
  async bulkUpdate(userId: number, settings: Record<string, any>): Promise<void> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      for (const [key, value] of Object.entries(settings)) {
        await client.query(
          `INSERT INTO user_additional_settings (user_id, setting_key, setting_value)
           VALUES ($1, $2, $3)
           ON CONFLICT (user_id, setting_key)
           DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = CURRENT_TIMESTAMP`,
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
   * Delete a setting
   */
  async delete(userId: number, key: string): Promise<void> {
    await this.pool.query(
      'DELETE FROM user_additional_settings WHERE user_id = $1 AND setting_key = $2',
      [userId, key]
    );
  }
}
