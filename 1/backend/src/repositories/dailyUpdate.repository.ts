import { database } from '../config/database';

export interface DailyUpdate {
  id: number;
  age_in_months: number;
  content_type: string; // 'tip', 'development', 'activity', 'safety'
  title?: string;
  content: string;
  image_url?: string;
  video_url?: string;
  created_at: Date;
  updated_at: Date;
}

export class DailyUpdateRepository {
  async findByAge(ageInMonths: number, contentType?: string): Promise<DailyUpdate[]> {
    let query = 'SELECT * FROM daily_updates_content WHERE age_in_months = $1';
    const params: any[] = [ageInMonths];

    if (contentType) {
      query += ' AND content_type = $2';
      params.push(contentType);
    }

    query += ' ORDER BY created_at DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<DailyUpdate | null> {
    const result = await database.query('SELECT * FROM daily_updates_content WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(updateData: Partial<DailyUpdate>): Promise<DailyUpdate> {
    const result = await database.query(
      `INSERT INTO daily_updates_content (age_in_months, content_type, title, content, image_url, video_url)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [
        updateData.age_in_months,
        updateData.content_type,
        updateData.title || null,
        updateData.content,
        updateData.image_url || null,
        updateData.video_url || null,
      ]
    );
    return result.rows[0];
  }

  async getRandomUpdate(ageInMonths: number, contentType?: string): Promise<DailyUpdate | null> {
    let query = 'SELECT * FROM daily_updates_content WHERE age_in_months = $1';
    const params: any[] = [ageInMonths];

    if (contentType) {
      query += ' AND content_type = $2';
      params.push(contentType);
    }

    query += ' ORDER BY RANDOM() LIMIT 1';

    const result = await database.query(query, params);
    return result.rows[0] || null;
  }
}
