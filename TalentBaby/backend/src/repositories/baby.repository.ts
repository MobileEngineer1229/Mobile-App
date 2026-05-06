import { database } from '../config/database';

export interface Baby {
  id: number;
  user_id: number;
  name: string;
  birth_date: Date;
  gender?: string;
  birth_weight_kg?: number;
  birth_height_cm?: number;
  profile_picture_url?: string;
  avatar_url?: string;
  created_at: Date;
  updated_at: Date;
}

export class BabyRepository {
  async findByUserId(userId: number): Promise<Baby[]> {
    const result = await database.query(
      'SELECT * FROM babies WHERE user_id = $1 ORDER BY created_at DESC',
      [userId]
    );
    return result.rows;
  }

  async findAll(): Promise<Baby[]> {
    const result = await database.query(
      'SELECT * FROM babies ORDER BY created_at DESC'
    );
    return result.rows;
  }

  async findById(id: number, userId?: number): Promise<Baby | null> {
    let query = 'SELECT * FROM babies WHERE id = $1';
    const params: any[] = [id];

    if (userId) {
      query += ' AND user_id = $2';
      params.push(userId);
    }

    const result = await database.query(query, params);
    return result.rows[0] || null;
  }

  async create(babyData: {
    user_id: number;
    name: string;
    birth_date: Date;
    gender?: string;
    birth_weight_kg?: number;
    birth_height_cm?: number;
    profile_picture_url?: string;
    avatar_url?: string;
  }): Promise<Baby> {
    const result = await database.query(
      `INSERT INTO babies (user_id, name, birth_date, gender, birth_weight_kg, birth_height_cm, profile_picture_url, avatar_url)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        babyData.user_id,
        babyData.name,
        babyData.birth_date,
        babyData.gender || null,
        babyData.birth_weight_kg || null,
        babyData.birth_height_cm || null,
        babyData.profile_picture_url || null,
        babyData.avatar_url || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, userId: number, updates: Partial<Baby>): Promise<Baby> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = [
      'name',
      'birth_date',
      'gender',
      'birth_weight_kg',
      'birth_height_cm',
      'profile_picture_url',
      'avatar_url',
    ];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Baby]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const baby = await this.findById(id, userId);
      if (!baby) throw new Error('Baby not found');
      return baby;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id, userId);

    const result = await database.query(
      `UPDATE babies SET ${fields.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1} RETURNING *`,
      values
    );

    if (result.rows.length === 0) {
      throw new Error('Baby not found');
    }

    return result.rows[0];
  }

  async delete(id: number, userId: number): Promise<void> {
    const result = await database.query(
      'DELETE FROM babies WHERE id = $1 AND user_id = $2',
      [id, userId]
    );

    if (result.rowCount === 0) {
      throw new Error('Baby not found');
    }
  }
}
