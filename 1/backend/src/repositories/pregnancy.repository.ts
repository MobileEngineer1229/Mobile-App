import { database } from '../config/database';

export interface Pregnancy {
  id: number;
  user_id: number;
  due_date: Date;
  lmp_date?: Date;
  conception_date?: Date;
  ultrasound_date?: Date;
  current_week?: number;
  current_day?: number;
  notes?: string;
  created_at: Date;
  updated_at: Date;
}

export class PregnancyRepository {
  async findByUserId(userId: number): Promise<Pregnancy | null> {
    const result = await database.query(
      'SELECT * FROM pregnancies WHERE user_id = $1 ORDER BY created_at DESC LIMIT 1',
      [userId]
    );
    return result.rows[0] || null;
  }

  async findById(id: number, userId: number): Promise<Pregnancy | null> {
    const result = await database.query(
      'SELECT * FROM pregnancies WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    return result.rows[0] || null;
  }

  async create(pregnancyData: Partial<Pregnancy>): Promise<Pregnancy> {
    // Calculate current week and day
    const dueDate = new Date(pregnancyData.due_date!);
    const today = new Date();
    const daysUntilDue = Math.floor((dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    const weeksPregnant = 40 - Math.ceil(daysUntilDue / 7);
    const daysPregnant = daysUntilDue % 7;

    const result = await database.query(
      `INSERT INTO pregnancies (user_id, due_date, lmp_date, conception_date, ultrasound_date, current_week, current_day, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        pregnancyData.user_id,
        pregnancyData.due_date,
        pregnancyData.lmp_date || null,
        pregnancyData.conception_date || null,
        pregnancyData.ultrasound_date || null,
        weeksPregnant,
        daysPregnant,
        pregnancyData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, userId: number, updates: Partial<Pregnancy>): Promise<Pregnancy> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['due_date', 'lmp_date', 'conception_date', 'ultrasound_date', 'notes'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Pregnancy]);
        paramIndex++;
      }
    });

    // Recalculate week and day if due_date changed
    if (updates.due_date) {
      const dueDate = new Date(updates.due_date);
      const today = new Date();
      const daysUntilDue = Math.floor((dueDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
      const weeksPregnant = 40 - Math.ceil(daysUntilDue / 7);
      const daysPregnant = daysUntilDue % 7;
      fields.push(`current_week = $${paramIndex}`);
      fields.push(`current_day = $${paramIndex + 1}`);
      values.push(weeksPregnant);
      values.push(daysPregnant);
      paramIndex += 2;
    }

    if (fields.length === 0) {
      const pregnancy = await this.findById(id, userId);
      if (!pregnancy) throw new Error('Pregnancy not found');
      return pregnancy;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id, userId);

    const result = await database.query(
      `UPDATE pregnancies SET ${fields.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number, userId: number): Promise<void> {
    const result = await database.query(
      'DELETE FROM pregnancies WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    if (result.rowCount === 0) {
      throw new Error('Pregnancy not found');
    }
  }
}
