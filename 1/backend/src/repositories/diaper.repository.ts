import { database } from '../config/database';

export interface DiaperChange {
  id: number;
  baby_id: number;
  change_time: Date;
  diaper_type: string; // 'wet', 'dirty', 'both'
  notes?: string;
  created_at: Date;
  updated_at: Date;
}

export class DiaperRepository {
  async findByBabyId(babyId: number, startDate?: Date, endDate?: Date): Promise<DiaperChange[]> {
    let query = 'SELECT * FROM diaper_changes WHERE baby_id = $1';
    const params: any[] = [babyId];
    let paramIndex = 2;

    if (startDate) {
      query += ` AND change_time >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND change_time <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' ORDER BY change_time DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<DiaperChange | null> {
    const result = await database.query('SELECT * FROM diaper_changes WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(diaperData: Partial<DiaperChange>): Promise<DiaperChange> {
    const result = await database.query(
      `INSERT INTO diaper_changes (baby_id, change_time, diaper_type, notes)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [
        diaperData.baby_id,
        diaperData.change_time,
        diaperData.diaper_type,
        diaperData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, updates: Partial<DiaperChange>): Promise<DiaperChange> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['change_time', 'diaper_type', 'notes'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof DiaperChange]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const diaper = await this.findById(id);
      if (!diaper) throw new Error('Diaper change not found');
      return diaper;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);

    const result = await database.query(
      `UPDATE diaper_changes SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number): Promise<void> {
    const result = await database.query('DELETE FROM diaper_changes WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new Error('Diaper change not found');
    }
  }

  async getDailySummary(babyId: number, date: Date): Promise<any> {
    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(date);
    endOfDay.setHours(23, 59, 59, 999);

    const result = await database.query(
      `SELECT 
        COUNT(*) FILTER (WHERE diaper_type = 'wet') as wet_count,
        COUNT(*) FILTER (WHERE diaper_type = 'dirty') as dirty_count,
        COUNT(*) FILTER (WHERE diaper_type = 'both') as both_count,
        COUNT(*) as total_changes
       FROM diaper_changes
       WHERE baby_id = $1 AND change_time >= $2 AND change_time <= $3`,
      [babyId, startOfDay, endOfDay]
    );
    return result.rows[0] || { wet_count: 0, dirty_count: 0, both_count: 0, total_changes: 0 };
  }

  async getStatistics(babyId: number, startDate: Date, endDate: Date): Promise<any> {
    const result = await database.query(
      `SELECT 
        diaper_type,
        COUNT(*) as count
       FROM diaper_changes
       WHERE baby_id = $1 AND change_time >= $2 AND change_time <= $3
       GROUP BY diaper_type`,
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
