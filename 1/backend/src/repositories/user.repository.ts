import { database } from '../config/database';

export interface User {
  id: number;
  email: string;
  password_hash: string;
  full_name?: string;
  phone_number?: string;
  gender?: string;
  birthdate?: Date;
  profile_picture_url?: string;
  language_preference?: string;
  role?: string;
  is_premium?: boolean;
  relation_to_baby?: string;
  active_baby_id?: number | null;
  created_at: Date;
  updated_at: Date;
}

export interface UserWithBabies extends Omit<User, 'password_hash'> {
  babies: {
    id: number;
    name: string;
    birth_date: string;
    gender?: string;
    birth_weight_kg?: number;
    birth_height_cm?: number;
    profile_picture_url?: string;
  }[];
}

export class UserRepository {
  async findByEmail(email: string): Promise<User | null> {
    const result = await database.query(
      'SELECT * FROM users WHERE email = $1',
      [email]
    );
    return result.rows[0] || null;
  }

  async findById(id: number): Promise<User | null> {
    const result = await database.query('SELECT * FROM users WHERE id = $1', [
      id,
    ]);
    return result.rows[0] || null;
  }

  async create(userData: {
    email: string;
    password_hash: string;
    full_name?: string;
  }): Promise<User> {
    const result = await database.query(
      `INSERT INTO users (email, password_hash, full_name)
       VALUES ($1, $2, $3)
       RETURNING *`,
      [userData.email, userData.password_hash, userData.full_name || null]
    );
    return result.rows[0];
  }

  async createAdmin(userData: {
    email: string;
    password_hash: string;
    full_name?: string;
    role: string;
    is_premium?: boolean;
    relation_to_baby?: string;
  }): Promise<User> {
    const result = await database.query(
      `INSERT INTO users (email, password_hash, full_name, role, is_premium, relation_to_baby)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [
        userData.email,
        userData.password_hash,
        userData.full_name || null,
        userData.role || 'user',
        userData.is_premium ?? false,
        userData.relation_to_baby || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, updates: Partial<User>): Promise<User> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    Object.keys(updates).forEach((key) => {
      if (key !== 'id' && key !== 'created_at') {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof User]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const user = await this.findById(id);
      if (!user) throw new Error('User not found');
      return user;
    }

    fields.push(`updated_at = CURRENT_TIMESTAMP`);
    values.push(id);

    const result = await database.query(
      `UPDATE users SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async findAll(): Promise<UserWithBabies[]> {
    const result = await database.query(`
      SELECT
        u.id, u.email, u.full_name, u.phone_number, u.gender,
        u.birthdate, u.profile_picture_url, u.language_preference,
        u.role, u.is_premium, u.relation_to_baby,
        u.active_baby_id, u.created_at, u.updated_at,
        COALESCE(
          json_agg(
            json_build_object(
              'id',                  b.id,
              'name',                b.name,
              'birth_date',          b.birth_date,
              'gender',              b.gender,
              'birth_weight_kg',     b.birth_weight_kg,
              'birth_height_cm',     b.birth_height_cm,
              'profile_picture_url', b.profile_picture_url
            ) ORDER BY b.created_at
          ) FILTER (WHERE b.id IS NOT NULL),
          '[]'::json
        ) AS babies
      FROM users u
      LEFT JOIN babies b ON b.user_id = u.id
      GROUP BY u.id
      ORDER BY u.created_at DESC
    `);
    return result.rows;
  }

  async setActiveBaby(userId: number, babyId: number | null): Promise<void> {
    await database.query(
      'UPDATE users SET active_baby_id = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2',
      [babyId, userId]
    );
  }

  async createNotificationPreferences(userId: number): Promise<void> {
    await database.query(
      `INSERT INTO user_notification_preferences (user_id)
       VALUES ($1)
       ON CONFLICT (user_id) DO NOTHING`,
      [userId]
    );
  }
}
