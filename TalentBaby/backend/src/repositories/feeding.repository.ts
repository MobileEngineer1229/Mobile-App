import { database } from '../config/database';

export interface Feeding {
  id: number;
  baby_id: number;
  feeding_type: string; // 'breastfeeding', 'formula', 'solid', 'mixed'
  feeding_date: Date;
  start_time?: Date;
  end_time?: Date;
  duration_minutes?: number;
  amount_ml?: number;
  amount_oz?: number;
  side?: string; // 'left', 'right', 'both'
  notes?: string;
  created_at: Date;
  updated_at: Date;
}

export class FeedingRepository {
  async findByBabyId(babyId: number, startDate?: Date, endDate?: Date): Promise<Feeding[]> {
    let query = 'SELECT * FROM feedings WHERE baby_id = $1';
    const params: any[] = [babyId];
    let paramIndex = 2;

    if (startDate) {
      query += ` AND feeding_date >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND feeding_date <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' ORDER BY feeding_date DESC, start_time DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<Feeding | null> {
    const result = await database.query('SELECT * FROM feedings WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(feedingData: Partial<Feeding>): Promise<Feeding> {
    const result = await database.query(
      `INSERT INTO feedings 
       (baby_id, feeding_type, feeding_date, start_time, end_time, duration_minutes, 
        amount_ml, amount_oz, side, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [
        feedingData.baby_id,
        feedingData.feeding_type,
        feedingData.feeding_date,
        feedingData.start_time || null,
        feedingData.end_time || null,
        feedingData.duration_minutes || null,
        feedingData.amount_ml || null,
        feedingData.amount_oz || null,
        feedingData.side || null,
        feedingData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, updates: Partial<Feeding>): Promise<Feeding> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = [
      'feeding_type',
      'feeding_date',
      'start_time',
      'end_time',
      'duration_minutes',
      'amount_ml',
      'amount_oz',
      'side',
      'notes',
    ];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Feeding]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const feeding = await this.findById(id);
      if (!feeding) throw new Error('Feeding record not found');
      return feeding;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);

    const result = await database.query(
      `UPDATE feedings SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number): Promise<void> {
    const result = await database.query('DELETE FROM feedings WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new Error('Feeding record not found');
    }
  }

  async getStatistics(babyId: number, startDate: Date, endDate: Date): Promise<any> {
    const result = await database.query(
      `SELECT 
        feeding_type,
        COUNT(*) as total_feedings,
        AVG(duration_minutes) as avg_duration,
        SUM(amount_ml) as total_amount_ml,
        SUM(amount_oz) as total_amount_oz
       FROM feedings
       WHERE baby_id = $1 AND feeding_date >= $2 AND feeding_date <= $3
       GROUP BY feeding_type`,
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
