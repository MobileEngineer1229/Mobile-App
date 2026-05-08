import { Pool, QueryResult } from 'pg';
import { UserAction, CreateUserActionInput, UserActionQuery } from '../models/user-action';
import logger from '../utils/logger';

export class UserActionRepository {
  constructor(private pool: Pool) {}

  /**
   * Verify that the user_actions table exists
   */
  async verifyTableExists(): Promise<boolean> {
    try {
      const query = `
        SELECT EXISTS (
          SELECT FROM information_schema.tables 
          WHERE table_schema = 'public' 
          AND table_name = 'user_actions'
        );
      `;
      const result = await this.pool.query(query);
      return result.rows[0].exists;
    } catch (error) {
      logger.error('Error checking if user_actions table exists', {
        error: error instanceof Error ? error.message : String(error),
      });
      return false;
    }
  }

  /**
   * Create a new user action log
   */
  async create(input: CreateUserActionInput): Promise<UserAction> {
    const query = `
      INSERT INTO user_actions (
        user_id, action_type, action_category, endpoint, method,
        request_body, response_status, response_body, ip_address,
        user_agent, device_info, session_id, duration_ms, error_message, metadata
      )
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
      RETURNING *;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [
        input.userId,
        input.actionType,
        input.actionCategory,
        input.endpoint,
        input.method,
        input.requestBody ? JSON.stringify(input.requestBody) : null,
        input.responseStatus,
        input.responseBody ? JSON.stringify(input.responseBody) : null,
        input.ipAddress,
        input.userAgent,
        input.deviceInfo ? JSON.stringify(input.deviceInfo) : null,
        input.sessionId,
        input.durationMs,
        input.errorMessage,
        input.metadata ? JSON.stringify(input.metadata) : null,
      ]);

      return this.mapRowToUserAction(result.rows[0]);
    } catch (error) {
      logger.error('Error creating user action', {
        error: error instanceof Error ? error.message : String(error),
        input,
      });
      throw error;
    }
  }

  /**
   * Find user actions with query filters
   */
  async findAll(query: UserActionQuery): Promise<{ actions: UserAction[]; total: number }> {
    const page = query.page || 1;
    const limit = query.limit || 50;
    const offset = (page - 1) * limit;

    let whereClause = 'WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (query.userId) {
      params.push(query.userId);
      whereClause += ` AND user_id = $${paramIndex}`;
      paramIndex++;
    }

    if (query.actionType) {
      params.push(query.actionType);
      whereClause += ` AND action_type = $${paramIndex}`;
      paramIndex++;
    }

    if (query.actionCategory) {
      params.push(query.actionCategory);
      whereClause += ` AND action_category = $${paramIndex}`;
      paramIndex++;
    }

    if (query.endpoint) {
      params.push(`%${query.endpoint}%`);
      whereClause += ` AND endpoint LIKE $${paramIndex}`;
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

    // Get total count
    const countQuery = `SELECT COUNT(*) as total FROM user_actions ${whereClause}`;
    const countResult: QueryResult = await this.pool.query(countQuery, params);
    const total = parseInt(countResult.rows[0].total, 10);

    // Get paginated results
    const dataQuery = `
      SELECT * FROM user_actions
      ${whereClause}
      ORDER BY created_at DESC
      LIMIT $${paramIndex} OFFSET $${paramIndex + 1}
    `;
    params.push(limit, offset);

    try {
      const result: QueryResult = await this.pool.query(dataQuery, params);
      const actions = result.rows.map((row) => this.mapRowToUserAction(row));

      return { actions, total };
    } catch (error) {
      logger.error('Error finding user actions', {
        error: error instanceof Error ? error.message : String(error),
        query,
      });
      throw error;
    }
  }

  /**
   * Find user action by ID
   */
  async findById(id: number): Promise<UserAction | null> {
    const query = `SELECT * FROM user_actions WHERE id = $1`;

    try {
      const result: QueryResult = await this.pool.query(query, [id]);
      if (result.rows.length === 0) {
        return null;
      }

      return this.mapRowToUserAction(result.rows[0]);
    } catch (error) {
      logger.error('Error finding user action by ID', {
        error: error instanceof Error ? error.message : String(error),
        id,
      });
      throw error;
    }
  }

  /**
   * Get action statistics for a user
   */
  async getStatistics(userId: number, startDate?: string, endDate?: string): Promise<any> {
    let whereClause = 'WHERE user_id = $1';
    const params: any[] = [userId];
    let paramIndex = 2;

    if (startDate) {
      params.push(startDate);
      whereClause += ` AND created_at >= $${paramIndex}`;
      paramIndex++;
    }

    if (endDate) {
      params.push(endDate);
      whereClause += ` AND created_at <= $${paramIndex}`;
      paramIndex++;
    }

    const query = `
      SELECT
        COUNT(*) as total_actions,
        COUNT(DISTINCT action_type) as unique_action_types,
        COUNT(DISTINCT action_category) as unique_categories,
        COUNT(DISTINCT DATE(created_at)) as active_days,
        AVG(duration_ms) as avg_duration_ms,
        MAX(created_at) as last_action_at
      FROM user_actions
      ${whereClause}
    `;

    try {
      const result: QueryResult = await this.pool.query(query, params);
      return result.rows[0];
    } catch (error) {
      logger.error('Error getting user action statistics', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Map database row to UserAction model
   */
  private mapRowToUserAction(row: any): UserAction {
    return {
      id: row.id,
      userId: row.user_id,
      actionType: row.action_type,
      actionCategory: row.action_category,
      endpoint: row.endpoint,
      method: row.method,
      requestBody: row.request_body,
      responseStatus: row.response_status,
      responseBody: row.response_body,
      ipAddress: row.ip_address,
      userAgent: row.user_agent,
      deviceInfo: row.device_info,
      sessionId: row.session_id,
      durationMs: row.duration_ms,
      errorMessage: row.error_message,
      metadata: row.metadata,
      createdAt: row.created_at,
    };
  }
}
