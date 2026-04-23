import { Pool } from 'pg';
import { database } from '../config/database';

export class ContentTopicRepository {
  private pool: Pool;

  constructor() {
    this.pool = database.getPool();
  }

  async getAllTopics() {
    const query = 'SELECT * FROM content_topics ORDER BY name ASC';
    const result = await this.pool.query(query);
    return result.rows;
  }

  async getTopicById(id: number) {
    const query = 'SELECT * FROM content_topics WHERE id = $1';
    const result = await this.pool.query(query, [id]);
    return result.rows[0] || null;
  }

  async getContentByTopic(topicId: number, contentType?: string) {
    let query = `
      SELECT 
        cta.content_type,
        cta.content_id,
        cta.created_at
      FROM content_topic_associations cta
      WHERE cta.topic_id = $1
    `;
    const params: any[] = [topicId];

    if (contentType) {
      query += ' AND cta.content_type = $2';
      params.push(contentType);
    }

    query += ' ORDER BY cta.created_at DESC';

    const result = await this.pool.query(query, params);
    return result.rows;
  }

  async searchContent(searchTerm: string, contentType?: string) {
    let query = '';
    const params: any[] = [];
    const searchPattern = `%${searchTerm}%`;

    // Search across articles, stories, activities, recipes
    const contentTypes = contentType ? [contentType] : ['article', 'story', 'activity', 'recipe'];
    
    const queries: string[] = [];

    if (contentTypes.includes('article')) {
      queries.push(`
        SELECT 'article' as content_type, id as content_id, title, description
        FROM articles
        WHERE title ILIKE $1 OR description ILIKE $1 OR content ILIKE $1
      `);
      params.push(searchPattern);
    }

    if (contentTypes.includes('story')) {
      queries.push(`
        SELECT 'story' as content_type, id as content_id, title, description
        FROM bedtime_stories
        WHERE title ILIKE $${params.length + 1} OR description ILIKE $${params.length + 1} OR content ILIKE $${params.length + 1}
      `);
      params.push(searchPattern, searchPattern, searchPattern);
    }

    if (contentTypes.includes('activity')) {
      queries.push(`
        SELECT 'activity' as content_type, id as content_id, title, description
        FROM activities
        WHERE title ILIKE $${params.length + 1} OR description ILIKE $${params.length + 1}
      `);
      params.push(searchPattern, searchPattern);
    }

    if (contentTypes.includes('recipe')) {
      queries.push(`
        SELECT 'recipe' as content_type, id as content_id, title, description
        FROM recipes
        WHERE title ILIKE $${params.length + 1} OR description ILIKE $${params.length + 1}
      `);
      params.push(searchPattern, searchPattern);
    }

    query = queries.join(' UNION ALL ') + ' ORDER BY content_type, content_id';

    const result = await this.pool.query(query, params);
    return result.rows;
  }
}
