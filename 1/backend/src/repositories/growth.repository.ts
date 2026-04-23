import { database } from '../config/database';

export interface GrowthRecord {
  id: number;
  baby_id: number;
  record_date: Date;
  age_in_days: number;
  weight_kg?: number;
  height_cm?: number;
  head_circumference_cm?: number;
  bmi?: number;
  percentile_weight?: number;
  percentile_height?: number;
  percentile_head_circumference?: number;
  notes?: string;
  created_at: Date;
  updated_at: Date;
}

export class GrowthRepository {
  async findByBabyId(babyId: number, startDate?: Date, endDate?: Date): Promise<GrowthRecord[]> {
    let query = 'SELECT * FROM growth_records WHERE baby_id = $1';
    const params: any[] = [babyId];
    let paramIndex = 2;

    if (startDate) {
      query += ` AND record_date >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND record_date <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' ORDER BY record_date ASC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<GrowthRecord | null> {
    const result = await database.query('SELECT * FROM growth_records WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(growthData: {
    baby_id: number;
    record_date: Date;
    age_in_days: number;
    weight_kg?: number;
    height_cm?: number;
    head_circumference_cm?: number;
    percentile_weight?: number;
    percentile_height?: number;
    percentile_head_circumference?: number;
    notes?: string;
  }): Promise<GrowthRecord> {
    // Calculate BMI if weight and height are provided
    let bmi: number | null = null;
    if (growthData.weight_kg && growthData.height_cm) {
      const heightInMeters = growthData.height_cm / 100;
      bmi = growthData.weight_kg / (heightInMeters * heightInMeters);
    }

    const result = await database.query(
      `INSERT INTO growth_records 
       (baby_id, record_date, age_in_days, weight_kg, height_cm, head_circumference_cm, bmi, 
        percentile_weight, percentile_height, percentile_head_circumference, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
       RETURNING *`,
      [
        growthData.baby_id,
        growthData.record_date,
        growthData.age_in_days,
        growthData.weight_kg || null,
        growthData.height_cm || null,
        growthData.head_circumference_cm || null,
        bmi,
        growthData.percentile_weight || null,
        growthData.percentile_height || null,
        growthData.percentile_head_circumference || null,
        growthData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, updates: Partial<GrowthRecord>): Promise<GrowthRecord> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    Object.keys(updates).forEach((key) => {
      if (key !== 'id' && key !== 'created_at' && key !== 'baby_id') {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof GrowthRecord]);
        paramIndex++;
      }
    });

    // Recalculate BMI if weight or height changed
    if (updates.weight_kg || updates.height_cm) {
      const existing = await this.findById(id);
      if (existing) {
        const weight = updates.weight_kg ?? existing.weight_kg;
        const height = updates.height_cm ?? existing.height_cm;
        if (weight && height) {
          const heightInMeters = height / 100;
          const bmi = weight / (heightInMeters * heightInMeters);
          fields.push(`bmi = $${paramIndex}`);
          values.push(bmi);
          paramIndex++;
        }
      }
    }

    if (fields.length === 0) {
      const record = await this.findById(id);
      if (!record) throw new Error('Growth record not found');
      return record;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);

    const result = await database.query(
      `UPDATE growth_records SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number): Promise<void> {
    const result = await database.query('DELETE FROM growth_records WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new Error('Growth record not found');
    }
  }

  async verifyBabyOwnership(babyId: number, userId: number): Promise<boolean> {
    const result = await database.query(
      'SELECT id FROM babies WHERE id = $1 AND user_id = $2',
      [babyId, userId]
    );
    return result.rows.length > 0;
  }
}
