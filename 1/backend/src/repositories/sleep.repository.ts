import { database } from '../config/database';

export interface SleepSession {
  id: number;
  baby_id: number;
  sleep_type: string; // 'nap', 'night', 'bedtime'
  start_time: Date;
  end_time?: Date;
  duration_minutes?: number;
  quality_rating?: number; // 1-5
  notes?: string;
  created_at: Date;
  updated_at: Date;
}

export class SleepRepository {
  async findByBabyId(babyId: number, startDate?: Date, endDate?: Date): Promise<SleepSession[]> {
    let query = 'SELECT * FROM sleep_sessions WHERE baby_id = $1';
    const params: any[] = [babyId];
    let paramIndex = 2;

    if (startDate) {
      query += ` AND start_time >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND start_time <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' ORDER BY start_time DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<SleepSession | null> {
    const result = await database.query('SELECT * FROM sleep_sessions WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(sleepData: Partial<SleepSession>): Promise<SleepSession> {
    // Calculate duration if start and end times are provided
    let duration_minutes: number | null = null;
    if (sleepData.start_time && sleepData.end_time) {
      const start = new Date(sleepData.start_time);
      const end = new Date(sleepData.end_time);
      duration_minutes = Math.floor((end.getTime() - start.getTime()) / (1000 * 60));
    }

    const result = await database.query(
      `INSERT INTO sleep_sessions 
       (baby_id, sleep_type, start_time, end_time, duration_minutes, quality_rating, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [
        sleepData.baby_id,
        sleepData.sleep_type,
        sleepData.start_time,
        sleepData.end_time || null,
        duration_minutes || sleepData.duration_minutes || null,
        sleepData.quality_rating || null,
        sleepData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, updates: Partial<SleepSession>): Promise<SleepSession> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['sleep_type', 'start_time', 'end_time', 'duration_minutes', 'quality_rating', 'notes'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof SleepSession]);
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
      const sleep = await this.findById(id);
      if (!sleep) throw new Error('Sleep session not found');
      return sleep;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);

    const result = await database.query(
      `UPDATE sleep_sessions SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number): Promise<void> {
    const result = await database.query('DELETE FROM sleep_sessions WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new Error('Sleep session not found');
    }
  }

  async getStatistics(babyId: number, startDate: Date, endDate: Date): Promise<any> {
    const result = await database.query(
      `SELECT 
        sleep_type,
        COUNT(*) as total_sessions,
        AVG(duration_minutes) as avg_duration,
        SUM(duration_minutes) as total_duration,
        AVG(quality_rating) as avg_quality
       FROM sleep_sessions
       WHERE baby_id = $1 AND start_time >= $2 AND start_time <= $3
       GROUP BY sleep_type`,
      [babyId, startDate, endDate]
    );
    return result.rows;
  }

  async verifyBabyOwnership(babyId: number, userId: number): Promise<boolean> {
    const result = await database.query(
      'SELECT id FROM babies WHERE id = $1 AND user_id = $2',
      [babyId, userId]
    );
    return result.rows.length > 0;
  }
}
