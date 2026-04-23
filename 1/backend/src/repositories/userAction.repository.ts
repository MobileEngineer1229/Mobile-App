import { database } from '../config/database';

export interface UserAction {
  id: number;
  user_id: number | null;
  action_type: string; // 'login', 'logout', 'create', 'update', 'delete', 'view', 'search', etc.
  action_name: string; // Descriptive name like 'Create Baby Profile', 'View Growth Chart'
  resource_type?: string; // 'baby', 'feeding', 'milestone', etc.
  resource_id?: number;
  details: any; // JSONB for additional details
  ip_address?: string;
  user_agent?: string;
  created_at: Date;
}

export class UserActionRepository {
  async create(actionData: Partial<UserAction>): Promise<UserAction> {
    const result = await database.query(
      `INSERT INTO user_actions 
       (user_id, action_type, action_name, resource_type, resource_id, details, ip_address, user_agent)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        actionData.user_id || null,
        actionData.action_type,
        actionData.action_name,
        actionData.resource_type || null,
        actionData.resource_id || null,
        JSON.stringify(actionData.details || {}),
        actionData.ip_address || null,
        actionData.user_agent || null,
      ]
    );
    return result.rows[0];
  }

  async findByUserId(userId: number, limit: number = 100, offset: number = 0): Promise<UserAction[]> {
    const result = await database.query(
      `SELECT * FROM user_actions 
       WHERE user_id = $1 
       ORDER BY created_at DESC 
       LIMIT $2 OFFSET $3`,
      [userId, limit, offset]
    );
    return result.rows;
  }

  async findByActionType(actionType: string, limit: number = 100): Promise<UserAction[]> {
    const result = await database.query(
      `SELECT * FROM user_actions 
       WHERE action_type = $1 
       ORDER BY created_at DESC 
       LIMIT $2`,
      [actionType, limit]
    );
    return result.rows;
  }

  async findByResource(resourceType: string, resourceId: number): Promise<UserAction[]> {
    const result = await database.query(
      `SELECT * FROM user_actions 
       WHERE resource_type = $1 AND resource_id = $2 
       ORDER BY created_at DESC`,
      [resourceType, resourceId]
    );
    return result.rows;
  }

  async getStatistics(userId?: number, startDate?: Date, endDate?: Date): Promise<any> {
    let query = `
      SELECT 
        action_type,
        COUNT(*) as count,
        COUNT(DISTINCT user_id) as unique_users
      FROM user_actions
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

    query += ` GROUP BY action_type ORDER BY count DESC`;

    const result = await database.query(query, params);
    return result.rows;
  }

  async getRecentActions(limit: number = 50): Promise<UserAction[]> {
    const result = await database.query(
      `SELECT * FROM user_actions 
       ORDER BY created_at DESC 
       LIMIT $1`,
      [limit]
    );
    return result.rows;
  }
}
