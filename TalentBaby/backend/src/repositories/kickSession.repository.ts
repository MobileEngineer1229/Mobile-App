import { database } from '../config/database';

export interface KickSession {
  id: number;
  pregnancy_id: number;
  session_date: Date;
  start_time: Date;
  end_time?: Date;
  kick_count: number;
  duration_minutes?: number;
  notes?: string;
  created_at: Date;
}

export class KickSessionRepository {
  async findByPregnancyId(pregnancyId: number): Promise<KickSession[]> {
    const result = await database.query(
      'SELECT * FROM kick_sessions WHERE pregnancy_id = $1 ORDER BY session_date DESC, start_time DESC',
      [pregnancyId]
    );
    return result.rows;
  }

  async findById(id: number): Promise<KickSession | null> {
    const result = await database.query('SELECT * FROM kick_sessions WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(sessionData: Partial<KickSession>): Promise<KickSession> {
    // Calculate duration if start and end times are provided
    let duration_minutes: number | null = null;
    if (sessionData.start_time && sessionData.end_time) {
      const start = new Date(sessionData.start_time);
      const end = new Date(sessionData.end_time);
      duration_minutes = Math.floor((end.getTime() - start.getTime()) / (1000 * 60));
    }

    const result = await database.query(
      `INSERT INTO kick_sessions 
       (pregnancy_id, session_date, start_time, end_time, kick_count, duration_minutes, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [
        sessionData.pregnancy_id,
        sessionData.session_date,
        sessionData.start_time,
        sessionData.end_time || null,
        sessionData.kick_count || 0,
        duration_minutes,
        sessionData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, updates: Partial<KickSession>): Promise<KickSession> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['start_time', 'end_time', 'kick_count', 'duration_minutes', 'notes'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof KickSession]);
        paramIndex++;
      }
    });

    // Recalculate duration if start_time or end_time changed
    if (updates.start_time || updates.end_time) {
      const existing = await this.findById(id);
      if (existing) {
        const start = updates.start_time ? new Date(updates.start_time) : new Date(existing.start_time);
        const end = updates.end_time ? new Date(updates.end_time) : existing.end_time ? new Date(existing.end_time) : null;
        if (end) {
          const duration = Math.floor((end.getTime() - start.getTime()) / (1000 * 60));
          fields.push(`duration_minutes = $${paramIndex}`);
          values.push(duration);
          paramIndex++;
        }
      }
    }

    if (fields.length === 0) {
      const session = await this.findById(id);
      if (!session) throw new Error('Kick session not found');
      return session;
    }

    const result = await database.query(
      `UPDATE kick_sessions SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      [...values, id]
    );

    return result.rows[0];
  }

  async delete(id: number): Promise<void> {
    const result = await database.query('DELETE FROM kick_sessions WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new Error('Kick session not found');
    }
  }
}
