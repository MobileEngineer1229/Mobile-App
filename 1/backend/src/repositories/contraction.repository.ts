import { database } from '../config/database';

export interface Contraction {
  id: number;
  pregnancy_id: number;
  contraction_time: Date;
  duration_seconds: number;
  intensity: number; // 1-10
  notes?: string;
  created_at: Date;
}

export class ContractionRepository {
  async findByPregnancyId(pregnancyId: number): Promise<Contraction[]> {
    const result = await database.query(
      'SELECT * FROM contractions WHERE pregnancy_id = $1 ORDER BY contraction_time DESC',
      [pregnancyId]
    );
    return result.rows;
  }

  async create(contractionData: Partial<Contraction>): Promise<Contraction> {
    const result = await database.query(
      `INSERT INTO contractions (pregnancy_id, contraction_time, duration_seconds, intensity, notes)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [
        contractionData.pregnancy_id,
        contractionData.contraction_time,
        contractionData.duration_seconds,
        contractionData.intensity || 1,
        contractionData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async getContractionPatterns(pregnancyId: number): Promise<any> {
    // Get last 10 contractions
    const result = await database.query(
      `SELECT 
        contraction_time,
        duration_seconds,
        intensity,
        LAG(contraction_time) OVER (ORDER BY contraction_time) as previous_contraction_time
       FROM contractions
       WHERE pregnancy_id = $1
       ORDER BY contraction_time DESC
       LIMIT 10`,
      [pregnancyId]
    );

    // Calculate time between contractions
    const contractions = result.rows.map((row: any) => {
      let timeBetween = null;
      if (row.previous_contraction_time) {
        const current = new Date(row.contraction_time);
        const previous = new Date(row.previous_contraction_time);
        timeBetween = Math.floor((current.getTime() - previous.getTime()) / (1000 * 60)); // minutes
      }
      return {
        ...row,
        time_between_minutes: timeBetween,
      };
    });

    return contractions;
  }

  async delete(id: number): Promise<void> {
    const result = await database.query('DELETE FROM contractions WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new Error('Contraction not found');
    }
  }
}
