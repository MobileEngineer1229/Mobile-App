import { database } from '../config/database';

export interface Subscription {
  id: number;
  user_id: number;
  plan_type: string; // 'free', 'premium', 'family'
  status: string; // 'active', 'cancelled', 'expired'
  start_date: Date;
  end_date?: Date;
  payment_provider?: string;
  payment_id?: string;
  created_at: Date;
  updated_at: Date;
}

export class SubscriptionRepository {
  async findByUserId(userId: number): Promise<Subscription | null> {
    const result = await database.query(
      'SELECT * FROM subscriptions WHERE user_id = $1 ORDER BY created_at DESC LIMIT 1',
      [userId]
    );
    return result.rows[0] || null;
  }

  async create(subscriptionData: Partial<Subscription>): Promise<Subscription> {
    const result = await database.query(
      `INSERT INTO subscriptions 
       (user_id, plan_type, status, start_date, end_date, payment_provider, payment_id)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [
        subscriptionData.user_id,
        subscriptionData.plan_type,
        subscriptionData.status || 'active',
        subscriptionData.start_date,
        subscriptionData.end_date || null,
        subscriptionData.payment_provider || null,
        subscriptionData.payment_id || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, userId: number, updates: Partial<Subscription>): Promise<Subscription> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['plan_type', 'status', 'end_date', 'payment_provider', 'payment_id'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Subscription]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const result = await database.query(
        'SELECT * FROM subscriptions WHERE id = $1 AND user_id = $2',
        [id, userId]
      );
      if (result.rows.length === 0) throw new Error('Subscription not found');
      return result.rows[0];
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id, userId);

    const result = await database.query(
      `UPDATE subscriptions SET ${fields.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async cancel(id: number, userId: number): Promise<Subscription> {
    const result = await database.query(
      `UPDATE subscriptions 
       SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP 
       WHERE id = $1 AND user_id = $2 
       RETURNING *`,
      [id, userId]
    );

    if (result.rows.length === 0) {
      throw new Error('Subscription not found');
    }

    return result.rows[0];
  }
}
