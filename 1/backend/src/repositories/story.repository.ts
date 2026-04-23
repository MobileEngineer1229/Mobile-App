import { Pool } from 'pg';
import { database } from '../config/database';

export class StoryRepository {
  private pool: Pool;

  constructor() {
    this.pool = database.getPool();
  }

  async getStories(filters?: { category?: string; ageMonths?: number; search?: string }) {
    let query = 'SELECT * FROM bedtime_stories WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (filters?.category) {
      query += ` AND category = $${paramIndex}`;
      params.push(filters.category);
      paramIndex++;
    }

    if (filters?.ageMonths) {
      query += ` AND (age_min_months IS NULL OR age_min_months <= $${paramIndex})
                 AND (age_max_months IS NULL OR age_max_months >= $${paramIndex})`;
      params.push(filters.ageMonths);
      paramIndex++;
    }

    if (filters?.search) {
      query += ` AND (title ILIKE $${paramIndex} OR description ILIKE $${paramIndex} OR content ILIKE $${paramIndex})`;
      const searchTerm = `%${filters.search}%`;
      params.push(searchTerm, searchTerm, searchTerm);
      paramIndex += 3;
    }

    query += ' ORDER BY created_at DESC';

    const result = await this.pool.query(query, params);
    return result.rows;
  }

  async getStoryById(id: number) {
    const query = 'SELECT * FROM bedtime_stories WHERE id = $1';
    const result = await this.pool.query(query, [id]);
    return result.rows[0] || null;
  }

  async getStoryCategories() {
    const query = 'SELECT * FROM story_categories ORDER BY name ASC';
    const result = await this.pool.query(query);
    return result.rows;
  }

  async addStoryToFavorites(userId: number, storyId: number) {
    const query = `
      INSERT INTO story_favorites (user_id, story_id)
      VALUES ($1, $2)
      ON CONFLICT (user_id, story_id) DO NOTHING
      RETURNING *
    `;
    const result = await this.pool.query(query, [userId, storyId]);
    return result.rows[0] || null;
  }

  async removeStoryFromFavorites(userId: number, storyId: number) {
    const query = 'DELETE FROM story_favorites WHERE user_id = $1 AND story_id = $2';
    await this.pool.query(query, [userId, storyId]);
  }

  async getFavoriteStories(userId: number) {
    const query = `
      SELECT s.* FROM bedtime_stories s
      INNER JOIN story_favorites sf ON s.id = sf.story_id
      WHERE sf.user_id = $1
      ORDER BY sf.created_at DESC
    `;
    const result = await this.pool.query(query, [userId]);
    return result.rows;
  }

  async isStoryFavorite(userId: number, storyId: number): Promise<boolean> {
    const query = 'SELECT 1 FROM story_favorites WHERE user_id = $1 AND story_id = $2';
    const result = await this.pool.query(query, [userId, storyId]);
    return result.rows.length > 0;
  }
}
