import { Pool, QueryResult } from 'pg';
import { Notification, CreateNotificationInput, UpdateNotificationInput, NotificationQuery, NotificationStats } from '../models/notification';
import logger from '../utils/logger';

export class NotificationRepository {
  constructor(private pool: Pool) {}

  /**
   * Find notification by ID
   */
  async findById(id: number, userId: number): Promise<Notification | null> {
    const query = `
      SELECT * FROM notifications
      WHERE id = $1 AND user_id = $2;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [id, userId]);
      if (result.rows.length === 0) {
        return null;
      }

      return this.mapRowToNotification(result.rows[0]);
    } catch (error) {
      logger.error('Error finding notification by ID', {
        error: error instanceof Error ? error.message : String(error),
        id,
        userId,
      });
      throw error;
    }
  }

  /**
   * Find all notifications for a user
   */
  async findAll(userId: number, query: NotificationQuery): Promise<{ notifications: Notification[]; total: number }> {
    const page = query.page || 1;
    const limit = query.limit || 20;
    const offset = (page - 1) * limit;

    let whereClause = 'WHERE user_id = $1';
    const params: (number | string | boolean)[] = [userId];
    let paramIndex = 2;

    if (query.type) {
      params.push(query.type);
      whereClause += ` AND type = $${paramIndex}`;
      paramIndex++;
    }

    if (query.category) {
      params.push(query.category);
      whereClause += ` AND category = $${paramIndex}`;
      paramIndex++;
    }

    if (query.isRead !== undefined) {
      params.push(query.isRead);
      whereClause += ` AND is_read = $${paramIndex}`;
      paramIndex++;
    }

    if (query.startDate) {
      params.push(query.startDate);
      whereClause += ` AND created_at >= $${paramIndex}`;
      paramIndex++;
    }

    if (query.endDate) {
      params.push(query.endDate);
      whereClause += ` AND created_at <= $${paramIndex}`;
      paramIndex++;
    }

    const countQuery = `SELECT COUNT(*) as total FROM notifications ${whereClause}`;
    const dataQuery = `
      SELECT * FROM notifications
      ${whereClause}
      ORDER BY created_at DESC
      LIMIT $${paramIndex} OFFSET $${paramIndex + 1};
    `;

    params.push(limit, offset);

    try {
      const [countResult, dataResult]: [QueryResult, QueryResult] = await Promise.all([
        this.pool.query(countQuery, params.slice(0, -2)), // Exclude limit and offset for count
        this.pool.query(dataQuery, params),
      ]);

      const total = parseInt(countResult.rows[0].total, 10);
      const notifications = dataResult.rows.map((row) => this.mapRowToNotification(row));

      return { notifications, total };
    } catch (error) {
      logger.error('Error finding notifications', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        query,
      });
      throw error;
    }
  }

  /**
   * Create notification
   */
  async create(input: CreateNotificationInput): Promise<Notification> {
    // Auto-determine category based on type if not provided
    let category = input.category;
    if (!category && input.type) {
      category = (input.type === 'security' || input.type === 'alert') ? 'smart_home' : 'general';
    }
    category = category || 'general';

    const query = `
      INSERT INTO notifications (user_id, title, message, type, category, icon, metadata)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [
        input.userId,
        input.title,
        input.message,
        input.type || 'general',
        category,
        input.icon || null,
        input.metadata ? JSON.stringify(input.metadata) : null,
      ]);

      return this.mapRowToNotification(result.rows[0]);
    } catch (error) {
      logger.error('Error creating notification', {
        error: error instanceof Error ? error.message : String(error),
        input,
      });
      throw error;
    }
  }

  /**
   * Update notification
   */
  async update(id: number, userId: number, input: UpdateNotificationInput): Promise<Notification> {
    const updates: string[] = [];
    const values: (boolean | string | number)[] = [];
    let paramIndex = 1;

    if (input.isRead !== undefined) {
      updates.push(`is_read = $${paramIndex}`);
      values.push(input.isRead);
      paramIndex++;

      if (input.isRead) {
        updates.push(`read_at = CURRENT_TIMESTAMP`);
      } else {
        updates.push(`read_at = NULL`);
      }
    }

    if (input.metadata !== undefined) {
      updates.push(`metadata = $${paramIndex}`);
      values.push(JSON.stringify(input.metadata));
      paramIndex++;
    }

    if (updates.length === 0) {
      const notification = await this.findById(id, userId);
      if (!notification) {
        throw new Error('Notification not found');
      }
      return notification;
    }

    values.push(id, userId);
    const query = `
      UPDATE notifications
      SET ${updates.join(', ')}
      WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1}
      RETURNING *;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, values);
      if (result.rows.length === 0) {
        throw new Error('Notification not found');
      }

      return this.mapRowToNotification(result.rows[0]);
    } catch (error) {
      logger.error('Error updating notification', {
        error: error instanceof Error ? error.message : String(error),
        id,
        userId,
        input,
      });
      throw error;
    }
  }

  /**
   * Mark all notifications as read
   */
  async markAllAsRead(userId: number): Promise<number> {
    const query = `
      UPDATE notifications
      SET is_read = TRUE, read_at = CURRENT_TIMESTAMP
      WHERE user_id = $1 AND is_read = FALSE
      RETURNING id;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [userId]);
      return result.rowCount || 0;
    } catch (error) {
      logger.error('Error marking all notifications as read', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Delete notification
   */
  async delete(id: number, userId: number): Promise<void> {
    const query = `
      DELETE FROM notifications
      WHERE id = $1 AND user_id = $2;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [id, userId]);
      if (result.rowCount === 0) {
        throw new Error('Notification not found');
      }
    } catch (error) {
      logger.error('Error deleting notification', {
        error: error instanceof Error ? error.message : String(error),
        id,
        userId,
      });
      throw error;
    }
  }

  /**
   * Get notification statistics
   */
  async getStats(userId: number): Promise<NotificationStats> {
    // Fixed query - properly structure the stats query
    const query = `
      WITH notification_stats AS (
        SELECT 
          COUNT(*) as total,
          COUNT(*) FILTER (WHERE is_read = FALSE) as unread
        FROM notifications
        WHERE user_id = $1
      ),
      type_counts AS (
        SELECT 
          type,
          COUNT(*) as type_count
        FROM notifications
        WHERE user_id = $1
        GROUP BY type
      )
      SELECT 
        ns.total,
        ns.unread,
        COALESCE(json_object_agg(tc.type, tc.type_count) FILTER (WHERE tc.type IS NOT NULL), '{}'::json) as by_type
      FROM notification_stats ns
      LEFT JOIN type_counts tc ON true
      GROUP BY ns.total, ns.unread;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [userId]);
      const row = result.rows[0];

      if (!row) {
        return { total: 0, unread: 0, byType: {} as Record<string, number> };
      }

      const byType: Record<string, number> = {};
      if (row.by_type && typeof row.by_type === 'object') {
        Object.entries(row.by_type).forEach(([type, count]) => {
          byType[type] = parseInt(count as string, 10);
        });
      }

      return {
        total: parseInt(row.total || '0', 10),
        unread: parseInt(row.unread || '0', 10),
        byType: byType as Record<string, number>,
      };
    } catch (error) {
      logger.error('Error getting notification stats', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Map database row to Notification model
   */
  private mapRowToNotification(row: any): Notification {
    // Map snake_case to camelCase
    return {
      id: row.id,
      userId: row.user_id,
      title: row.title,
      message: row.message,
      type: row.type,
      category: row.category || null,
      icon: row.icon,
      isRead: row.is_read,
      readAt: row.read_at ? new Date(row.read_at) : null,
      metadata: row.metadata ? (typeof row.metadata === 'string' ? JSON.parse(row.metadata) : row.metadata) : null,
      createdAt: new Date(row.created_at),
    };
  }
}

