import { database } from '../config/database';

export interface Reminder {
  id: number;
  user_id: number;
  baby_id?: number;
  reminder_type: string; // 'feeding', 'sleep', 'diaper', 'appointment', 'vaccination', 'activity', 'custom'
  title: string;
  description?: string;
  reminder_time: Date;
  reminder_date?: Date;
  is_recurring: boolean;
  recurrence_pattern?: string; // 'daily', 'weekly', 'custom'
  is_active: boolean;
  created_at: Date;
  updated_at: Date;
}

export class ReminderRepository {
  async findByUserId(userId: number, isActive?: boolean): Promise<Reminder[]> {
    let query = 'SELECT * FROM reminders WHERE user_id = $1';
    const params: any[] = [userId];
    let paramIndex = 2;

    if (isActive !== undefined) {
      query += ` AND is_active = $${paramIndex}`;
      params.push(isActive);
      paramIndex++;
    }

    query += ' ORDER BY reminder_time ASC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findByBabyId(babyId: number): Promise<Reminder[]> {
    const result = await database.query(
      'SELECT * FROM reminders WHERE baby_id = $1 AND is_active = true ORDER BY reminder_time ASC',
      [babyId]
    );
    return result.rows;
  }

  async findById(id: number, userId: number): Promise<Reminder | null> {
    const result = await database.query(
      'SELECT * FROM reminders WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    return result.rows[0] || null;
  }

  async create(reminderData: Partial<Reminder>): Promise<Reminder> {
    const result = await database.query(
      `INSERT INTO reminders 
       (user_id, baby_id, reminder_type, title, description, reminder_time, reminder_date, 
        is_recurring, recurrence_pattern, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [
        reminderData.user_id,
        reminderData.baby_id || null,
        reminderData.reminder_type,
        reminderData.title,
        reminderData.description || null,
        reminderData.reminder_time,
        reminderData.reminder_date || null,
        reminderData.is_recurring || false,
        reminderData.recurrence_pattern || null,
        reminderData.is_active !== undefined ? reminderData.is_active : true,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, userId: number, updates: Partial<Reminder>): Promise<Reminder> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = [
      'reminder_type',
      'title',
      'description',
      'reminder_time',
      'reminder_date',
      'is_recurring',
      'recurrence_pattern',
      'is_active',
    ];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Reminder]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const reminder = await this.findById(id, userId);
      if (!reminder) throw new Error('Reminder not found');
      return reminder;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id, userId);

    const result = await database.query(
      `UPDATE reminders SET ${fields.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number, userId: number): Promise<void> {
    const result = await database.query(
      'DELETE FROM reminders WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    if (result.rowCount === 0) {
      throw new Error('Reminder not found');
    }
  }

  async getUpcomingReminders(userId: number, limit: number = 10): Promise<Reminder[]> {
    const result = await database.query(
      `SELECT * FROM reminders 
       WHERE user_id = $1 AND is_active = true 
       AND (reminder_date IS NULL OR reminder_date <= CURRENT_DATE)
       AND reminder_time >= CURRENT_TIME
       ORDER BY reminder_time ASC
       LIMIT $2`,
      [userId, limit]
    );
    return result.rows;
  }
}
