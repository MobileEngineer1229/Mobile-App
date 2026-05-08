import { Pool } from 'pg';

/**
 * User notification preferences repository
 */
export class UserNotificationPreferencesRepository {
  constructor(private pool: Pool) {}

  /**
   * Get all notification preferences for a user
   */
  async findAll(userId: number): Promise<Record<string, any>> {
    const result = await this.pool.query(
      'SELECT preference_key, preference_value FROM user_notification_preferences WHERE user_id = $1',
      [userId]
    );

    const preferences: Record<string, any> = {};
    for (const row of result.rows) {
      preferences[row.preference_key] = row.preference_value;
    }
    return preferences;
  }

  /**
   * Get a specific preference
   */
  async findOne(userId: number, key: string): Promise<any | null> {
    const result = await this.pool.query(
      'SELECT preference_value FROM user_notification_preferences WHERE user_id = $1 AND preference_key = $2',
      [userId, key]
    );
    return result.rows.length > 0 ? result.rows[0].preference_value : null;
  }

  /**
   * Update or create a preference
   */
  async upsert(userId: number, key: string, value: any): Promise<void> {
    await this.pool.query(
      `INSERT INTO user_notification_preferences (user_id, preference_key, preference_value)
       VALUES ($1, $2, $3)
       ON CONFLICT (user_id, preference_key)
       DO UPDATE SET preference_value = EXCLUDED.preference_value, updated_at = CURRENT_TIMESTAMP`,
      [userId, key, JSON.stringify(value)]
    );
  }

  /**
   * Bulk update preferences
   */
  async bulkUpdate(userId: number, preferences: Record<string, any>): Promise<void> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      for (const [key, value] of Object.entries(preferences)) {
        await client.query(
          `INSERT INTO user_notification_preferences (user_id, preference_key, preference_value)
           VALUES ($1, $2, $3)
           ON CONFLICT (user_id, preference_key)
           DO UPDATE SET preference_value = EXCLUDED.preference_value, updated_at = CURRENT_TIMESTAMP`,
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
   * Delete a preference
   */
  async delete(userId: number, key: string): Promise<void> {
    await this.pool.query(
      'DELETE FROM user_notification_preferences WHERE user_id = $1 AND preference_key = $2',
      [userId, key]
    );
  }
}
