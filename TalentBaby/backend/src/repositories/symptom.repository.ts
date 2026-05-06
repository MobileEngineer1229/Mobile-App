import { database } from '../config/database';

export interface Symptom {
  id: number;
  pregnancy_id: number;
  symptom_date: Date;
  symptom_type: string; // 'nausea', 'fatigue', 'mood', 'pain', 'cravings', etc.
  severity: number; // 1-5
  notes?: string;
  created_at: Date;
}

export class SymptomRepository {
  async findByPregnancyId(pregnancyId: number, startDate?: Date, endDate?: Date): Promise<Symptom[]> {
    let query = 'SELECT * FROM pregnancy_symptoms WHERE pregnancy_id = $1';
    const params: any[] = [pregnancyId];
    let paramIndex = 2;

    if (startDate) {
      query += ` AND symptom_date >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND symptom_date <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' ORDER BY symptom_date DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async create(symptomData: Partial<Symptom>): Promise<Symptom> {
    const result = await database.query(
      `INSERT INTO pregnancy_symptoms (pregnancy_id, symptom_date, symptom_type, severity, notes)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [
        symptomData.pregnancy_id,
        symptomData.symptom_date,
        symptomData.symptom_type,
        symptomData.severity || 1,
        symptomData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async getSymptomPatterns(pregnancyId: number): Promise<any> {
    const result = await database.query(
      `SELECT 
        symptom_type,
        AVG(severity) as avg_severity,
        COUNT(*) as frequency
       FROM pregnancy_symptoms
       WHERE pregnancy_id = $1
       GROUP BY symptom_type
       ORDER BY frequency DESC`,
      [pregnancyId]
    );
    return result.rows;
  }
}
