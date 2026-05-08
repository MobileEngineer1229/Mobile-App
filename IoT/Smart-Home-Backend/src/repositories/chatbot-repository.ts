import { Pool, QueryResult } from 'pg';
import { ChatbotMessage, ChatHistoryQuery } from '../models/chatbot';
import logger from '../utils/logger';

export class ChatbotRepository {
  constructor(private pool: Pool) {}

  /**
   * Create a new message
   */
  async createMessage(
    userId: number,
    role: 'user' | 'assistant',
    message: string,
    metadata?: Record<string, unknown>
  ): Promise<ChatbotMessage> {
    const query = `
      INSERT INTO chatbot_messages (user_id, role, message, metadata)
      VALUES ($1, $2, $3, $4)
      RETURNING *;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [
        userId,
        role,
        message,
        metadata ? JSON.stringify(metadata) : null,
      ]);

      return this.mapRowToMessage(result.rows[0]);
    } catch (error) {
      logger.error('Error creating chatbot message', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        role,
      });
      throw error;
    }
  }

  /**
   * Get chat history
   */
  async getHistory(userId: number, query: ChatHistoryQuery): Promise<{ messages: ChatbotMessage[]; total: number }> {
    const page = query.page || 1;
    const limit = query.limit || 50;
    const offset = (page - 1) * limit;

    let whereClause = 'WHERE user_id = $1';
    const params: (number | string)[] = [userId];
    let paramIndex = 2;

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

    const countQuery = `SELECT COUNT(*) as total FROM chatbot_messages ${whereClause}`;
    const dataQuery = `
      SELECT * FROM chatbot_messages
      ${whereClause}
      ORDER BY created_at ASC
      LIMIT $${paramIndex} OFFSET $${paramIndex + 1};
    `;

    params.push(limit, offset);

    try {
      const [countResult, dataResult]: [QueryResult, QueryResult] = await Promise.all([
        this.pool.query(countQuery, params.slice(0, -2)),
        this.pool.query(dataQuery, params),
      ]);

      const total = parseInt(countResult.rows[0].total, 10);
      const messages = dataResult.rows.map((row) => this.mapRowToMessage(row));

      return { messages, total };
    } catch (error) {
      logger.error('Error getting chat history', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        query,
      });
      throw error;
    }
  }

  /**
   * Delete chat history
   */
  async deleteHistory(userId: number): Promise<number> {
    const query = `
      DELETE FROM chatbot_messages
      WHERE user_id = $1;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [userId]);
      return result.rowCount || 0;
    } catch (error) {
      logger.error('Error deleting chat history', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Get recent messages for context (last N messages)
   */
  async getRecentMessages(userId: number, limit: number = 10): Promise<ChatbotMessage[]> {
    const query = `
      SELECT * FROM chatbot_messages
      WHERE user_id = $1
      ORDER BY created_at DESC
      LIMIT $2;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [userId, limit]);
      return result.rows.map((row) => this.mapRowToMessage(row)).reverse(); // Reverse to get chronological order
    } catch (error) {
      logger.error('Error getting recent messages', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Map database row to ChatbotMessage model
   */
  private mapRowToMessage(row: any): ChatbotMessage {
    return {
      id: row.id,
      userId: row.user_id,
      role: row.role,
      message: row.message,
      metadata: row.metadata ? (typeof row.metadata === 'string' ? JSON.parse(row.metadata) : row.metadata) : null,
      createdAt: new Date(row.created_at),
    };
  }
}

