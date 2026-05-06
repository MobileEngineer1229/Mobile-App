import { database } from '../config/database';

export interface UserActionLog {
  id: number;
  user_id?: number;
  action_type: string;
  action_description?: string;
  resource_type?: string;
  resource_id?: number;
  ip_address?: string;
  user_agent?: string;
  request_method?: string;
  request_path?: string;
  status_code?: number;
  error_message?: string;
  metadata?: any;
  created_at: Date;
}

export class UserActionLogRepository {
  async create(logData: Partial<UserActionLog>): Promise<UserActionLog> {
    const result = await database.query(
      `INSERT INTO user_action_logs 
       (user_id, action_type, action_description, resource_type, resource_id, 
        ip_address, user_agent, request_method, request_path, status_code, 
        error_message, metadata)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
       RETURNING *`,
      [
        logData.user_id || null,
        logData.action_type,
        logData.action_description || null,
        logData.resource_type || null,
        logData.resource_id || null,
        logData.ip_address || null,
        logData.user_agent || null,
        logData.request_method || null,
        logData.request_path || null,
        logData.status_code || null,
        logData.error_message || null,
        logData.metadata ? JSON.stringify(logData.metadata) : null,
      ]
    );
    return result.rows[0];
  }

  async findByUserId(userId: number, limit: number = 100, offset: number = 0): Promise<UserActionLog[]> {
    const result = await database.query(
      `SELECT * FROM user_action_logs 
       WHERE user_id = $1 
       ORDER BY created_at DESC 
       LIMIT $2 OFFSET $3`,
      [userId, limit, offset]
    );
    return result.rows;
  }

  async findByActionType(actionType: string, limit: number = 100, offset: number = 0): Promise<UserActionLog[]> {
    const result = await database.query(
      `SELECT * FROM user_action_logs 
       WHERE action_type = $1 
       ORDER BY created_at DESC 
       LIMIT $2 OFFSET $3`,
      [actionType, limit, offset]
    );
    return result.rows;
  }

  async findByResource(resourceType: string, resourceId: number): Promise<UserActionLog[]> {
    const result = await database.query(
      `SELECT * FROM user_action_logs 
       WHERE resource_type = $1 AND resource_id = $2 
       ORDER BY created_at DESC`,
      [resourceType, resourceId]
    );
    return result.rows;
  }

  async findByDateRange(startDate: Date, endDate: Date, limit: number = 1000): Promise<UserActionLog[]> {
    const result = await database.query(
      `SELECT * FROM user_action_logs 
       WHERE created_at >= $1 AND created_at <= $2 
       ORDER BY created_at DESC 
       LIMIT $3`,
      [startDate, endDate, limit]
    );
    return result.rows;
  }

  async getStatistics(userId?: number, startDate?: Date, endDate?: Date): Promise<any> {
    let query = `
      SELECT 
        action_type,
        COUNT(*) as count,
        COUNT(DISTINCT user_id) as unique_users
      FROM user_action_logs
      WHERE 1=1
    `;
    const params: any[] = [];
    let paramIndex = 1;

    if (userId) {
      query += ` AND user_id = $${paramIndex}`;
      params.push(userId);
      paramIndex++;
    }

    if (startDate) {
      query += ` AND created_at >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND created_at <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' GROUP BY action_type ORDER BY count DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async deleteOldLogs(daysToKeep: number = 90): Promise<number> {
    const result = await database.query(
      `DELETE FROM user_action_logs 
       WHERE created_at < NOW() - INTERVAL '${daysToKeep} days'`
    );
    return result.rowCount || 0;
  }
}
