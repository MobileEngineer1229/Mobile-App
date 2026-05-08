import { Pool } from 'pg';
import { VoiceAssistant, LinkVoiceAssistantRequest } from '../models/voice-assistant';

export class VoiceAssistantRepository {
  constructor(private pool: Pool) {}

  /**
   * Get all available voice assistants
   */
  async findAll(): Promise<VoiceAssistant[]> {
    const query = `
      SELECT id, name, created_at as "createdAt", updated_at as "updatedAt"
      FROM voice_assistants
      ORDER BY name ASC
    `;
    const result = await this.pool.query(query);
    return result.rows.map(row => ({
      id: row.id,
      name: row.name,
      isLinked: false, // Will be set based on user's linked assistants
      createdAt: row.createdAt,
      updatedAt: row.updatedAt,
    }));
  }

  /**
   * Get voice assistants with linking status for a user
   */
  async findByUserId(userId: number): Promise<VoiceAssistant[]> {
    const query = `
      SELECT 
        va.id,
        va.name,
        va.created_at as "createdAt",
        va.updated_at as "updatedAt",
        COALESCE(ula.user_id IS NOT NULL, false) as "isLinked",
        ula.linked_at as "linkedAt"
      FROM voice_assistants va
      LEFT JOIN user_linked_assistants ula ON va.id = ula.assistant_id AND ula.user_id = $1
      ORDER BY va.name ASC
    `;
    const result = await this.pool.query(query, [userId]);
    return result.rows.map(row => ({
      id: row.id,
      name: row.name,
      isLinked: row.isLinked,
      userId: row.isLinked ? userId : undefined,
      linkedAt: row.linkedAt,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt,
    }));
  }

  /**
   * Link a voice assistant to a user
   */
  async linkAssistant(request: LinkVoiceAssistantRequest): Promise<VoiceAssistant> {
    const query = `
      INSERT INTO user_linked_assistants (user_id, assistant_id, access_token, refresh_token, metadata, linked_at)
      VALUES ($1, $2, $3, $4, $5, NOW())
      ON CONFLICT (user_id, assistant_id) 
      DO UPDATE SET 
        access_token = EXCLUDED.access_token,
        refresh_token = EXCLUDED.refresh_token,
        metadata = EXCLUDED.metadata,
        linked_at = NOW()
      RETURNING assistant_id as "assistantId", linked_at as "linkedAt"
    `;
    const result = await this.pool.query(query, [
      request.userId,
      request.assistantId,
      request.accessToken || null,
      request.refreshToken || null,
      request.metadata ? JSON.stringify(request.metadata) : null,
    ]);

    // Get the assistant details
    const assistantQuery = `
      SELECT id, name, created_at as "createdAt", updated_at as "updatedAt"
      FROM voice_assistants
      WHERE id = $1
    `;
    const assistantResult = await this.pool.query(assistantQuery, [request.assistantId]);
    const assistant = assistantResult.rows[0];

    return {
      id: assistant.id,
      name: assistant.name,
      isLinked: true,
      userId: request.userId,
      linkedAt: result.rows[0].linkedAt,
      createdAt: assistant.createdAt,
      updatedAt: assistant.updatedAt,
    };
  }

  /**
   * Unlink a voice assistant from a user
   */
  async unlinkAssistant(userId: number, assistantId: number): Promise<void> {
    const query = `
      DELETE FROM user_linked_assistants
      WHERE user_id = $1 AND assistant_id = $2
    `;
    await this.pool.query(query, [userId, assistantId]);
  }
}

