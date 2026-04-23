import { database } from '../config/database';

export interface VideoTag {
  id: number;
  memory_id: number;
  tag_name: string;
  created_at: Date;
}

export class VideoTagRepository {
  async addTag(memoryId: number, tagName: string): Promise<VideoTag> {
    const result = await database.query(
      `INSERT INTO video_tags (memory_id, tag_name)
       VALUES ($1, $2)
       ON CONFLICT (memory_id, tag_name) DO NOTHING
       RETURNING *`,
      [memoryId, tagName]
    );
    return result.rows[0];
  }

  async removeTag(memoryId: number, tagName: string): Promise<void> {
    await database.query('DELETE FROM video_tags WHERE memory_id = $1 AND tag_name = $2', [memoryId, tagName]);
  }

  async getTagsByMemory(memoryId: number): Promise<VideoTag[]> {
    const result = await database.query(
      'SELECT * FROM video_tags WHERE memory_id = $1 ORDER BY tag_name ASC',
      [memoryId]
    );
    return result.rows;
  }

  async getMemoriesByTag(tagName: string, babyId?: number): Promise<any[]> {
    let query = `
      SELECT m.* FROM memories m
      JOIN video_tags vt ON m.id = vt.memory_id
      WHERE vt.tag_name = $1 AND m.memory_type = 'video'
    `;
    const params: any[] = [tagName];
    let paramIndex = 2;

    if (babyId) {
      query += ` AND m.baby_id = $${paramIndex}`;
      params.push(babyId);
    }

    query += ' ORDER BY m.memory_date DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async getAllTags(babyId?: number): Promise<string[]> {
    let query = `
      SELECT DISTINCT vt.tag_name FROM video_tags vt
      JOIN memories m ON vt.memory_id = m.id
      WHERE m.memory_type = 'video'
    `;
    const params: any[] = [];

    if (babyId) {
      query += ' AND m.baby_id = $1';
      params.push(babyId);
    }

    query += ' ORDER BY vt.tag_name ASC';

    const result = await database.query(query, params);
    return result.rows.map((row: any) => row.tag_name);
  }
}
