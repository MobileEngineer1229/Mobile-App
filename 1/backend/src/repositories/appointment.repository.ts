import { database } from '../config/database';

export interface Appointment {
  id: number;
  user_id?: number;
  baby_id?: number;
  pregnancy_id?: number;
  appointment_type: string; // 'doctor', 'ultrasound', 'test', 'other'
  title: string;
  description?: string;
  appointment_date: Date;
  location?: string;
  reminder_sent: boolean;
  created_at: Date;
  updated_at: Date;
}

export class AppointmentRepository {
  async findByUserId(userId: number, startDate?: Date, endDate?: Date): Promise<Appointment[]> {
    let query = 'SELECT * FROM appointments WHERE user_id = $1';
    const params: any[] = [userId];
    let paramIndex = 2;

    if (startDate) {
      query += ` AND appointment_date >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND appointment_date <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' ORDER BY appointment_date ASC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number, userId: number): Promise<Appointment | null> {
    const result = await database.query(
      'SELECT * FROM appointments WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    return result.rows[0] || null;
  }

  async create(appointmentData: Partial<Appointment>): Promise<Appointment> {
    const result = await database.query(
      `INSERT INTO appointments 
       (user_id, baby_id, pregnancy_id, appointment_type, title, description, appointment_date, location)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        appointmentData.user_id || null,
        appointmentData.baby_id || null,
        appointmentData.pregnancy_id || null,
        appointmentData.appointment_type,
        appointmentData.title,
        appointmentData.description || null,
        appointmentData.appointment_date,
        appointmentData.location || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, userId: number, updates: Partial<Appointment>): Promise<Appointment> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['appointment_type', 'title', 'description', 'appointment_date', 'location'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Appointment]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const appointment = await this.findById(id, userId);
      if (!appointment) throw new Error('Appointment not found');
      return appointment;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id, userId);

    const result = await database.query(
      `UPDATE appointments SET ${fields.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number, userId: number): Promise<void> {
    const result = await database.query(
      'DELETE FROM appointments WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    if (result.rowCount === 0) {
      throw new Error('Appointment not found');
    }
  }
}
