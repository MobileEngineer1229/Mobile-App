import { database } from '../config/database';

export interface ExpertClass {
  id: number;
  title: string;
  description?: string;
  instructor_name?: string;
  class_type: string; // 'live', 'on_demand'
  scheduled_time?: Date;
  video_url?: string;
  thumbnail_url?: string;
  duration_minutes?: number;
  category: string;
  is_premium: boolean;
  created_at: Date;
  updated_at: Date;
}

export class ExpertClassRepository {
  async findAll(classType?: string, category?: string, isPremium?: boolean): Promise<ExpertClass[]> {
    let query = 'SELECT * FROM expert_classes WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (classType) {
      query += ` AND class_type = $${paramIndex}`;
      params.push(classType);
      paramIndex++;
    }

    if (category) {
      query += ` AND category = $${paramIndex}`;
      params.push(category);
      paramIndex++;
    }

    if (isPremium !== undefined) {
      query += ` AND is_premium = $${paramIndex}`;
      params.push(isPremium);
      paramIndex++;
    }

    query += ' ORDER BY scheduled_time DESC, created_at DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<ExpertClass | null> {
    const result = await database.query('SELECT * FROM expert_classes WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async enrollUser(userId: number, classId: number): Promise<void> {
    await database.query(
      `INSERT INTO class_enrollments (user_id, class_id)
       VALUES ($1, $2)
       ON CONFLICT (user_id, class_id) DO NOTHING`,
      [userId, classId]
    );
  }

  async getEnrolledClasses(userId: number): Promise<ExpertClass[]> {
    const result = await database.query(
      `SELECT ec.* FROM expert_classes ec
       JOIN class_enrollments ce ON ec.id = ce.class_id
       WHERE ce.user_id = $1
       ORDER BY ce.enrolled_at DESC`,
      [userId]
    );
    return result.rows;
  }

  async markCompleted(userId: number, classId: number): Promise<void> {
    await database.query(
      `UPDATE class_enrollments 
       SET completed_at = CURRENT_TIMESTAMP 
       WHERE user_id = $1 AND class_id = $2`,
      [userId, classId]
    );
  }
}
