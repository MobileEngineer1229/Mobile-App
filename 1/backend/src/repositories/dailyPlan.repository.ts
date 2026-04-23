import { database } from '../config/database';

export interface DailyPlan {
  id: number;
  baby_id: number;
  plan_date: Date;
  activities: number[]; // Array of activity IDs
  status: string; // 'pending', 'in_progress', 'completed'
  created_at: Date;
  updated_at: Date;
}

export class DailyPlanRepository {
  async findByBabyIdAndDate(babyId: number, date: Date): Promise<DailyPlan | null> {
    const result = await database.query(
      'SELECT * FROM daily_plans WHERE baby_id = $1 AND plan_date = $2',
      [babyId, date]
    );
    return result.rows[0] || null;
  }

  async findByBabyId(babyId: number, startDate?: Date, endDate?: Date): Promise<DailyPlan[]> {
    let query = 'SELECT * FROM daily_plans WHERE baby_id = $1';
    const params: any[] = [babyId];
    let paramIndex = 2;

    if (startDate) {
      query += ` AND plan_date >= $${paramIndex}`;
      params.push(startDate);
      paramIndex++;
    }

    if (endDate) {
      query += ` AND plan_date <= $${paramIndex}`;
      params.push(endDate);
      paramIndex++;
    }

    query += ' ORDER BY plan_date DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async create(planData: Partial<DailyPlan>): Promise<DailyPlan> {
    const result = await database.query(
      `INSERT INTO daily_plans (baby_id, plan_date, activities, status)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [
        planData.baby_id,
        planData.plan_date,
        planData.activities || [],
        planData.status || 'pending',
      ]
    );
    return result.rows[0];
  }

  async update(id: number, updates: Partial<DailyPlan>): Promise<DailyPlan> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['activities', 'status'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof DailyPlan]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const result = await database.query('SELECT * FROM daily_plans WHERE id = $1', [id]);
      if (result.rows.length === 0) throw new Error('Daily plan not found');
      return result.rows[0];
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id);

    const result = await database.query(
      `UPDATE daily_plans SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async generateDailyPlan(babyId: number, date: Date, activityIds: number[]): Promise<DailyPlan> {
    // Check if plan already exists
    const existing = await this.findByBabyIdAndDate(babyId, date);
    if (existing) {
      // Update existing plan
      return await this.update(existing.id, { activities: activityIds });
    } else {
      // Create new plan
      return await this.create({
        baby_id: babyId,
        plan_date: date,
        activities: activityIds,
        status: 'pending',
      });
    }
  }
}
