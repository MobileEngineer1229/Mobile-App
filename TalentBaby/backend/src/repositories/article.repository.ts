import { database } from '../config/database';

export interface Article {
  id: number;
  title: string;
  content: string;
  category: string;
  author?: string;
  image_url?: string;
  video_url?: string;
  reading_time_minutes?: number;
  is_featured: boolean;
  view_count: number;
  doctor_name?: string;
  doctor_credentials?: string;
  doctor_verified?: boolean;
  age_min_months?: number;
  age_max_months?: number;
  created_at: Date;
  updated_at: Date;
}

export interface CreateArticleInput {
  title: string;
  content: string;
  category: string;
  author?: string;
  image_url?: string;
  reading_time_minutes?: number;
  is_featured?: boolean;
  doctor_name?: string;
  doctor_credentials?: string;
  doctor_verified?: boolean;
  age_min_months?: number;
  age_max_months?: number;
}

export class ArticleRepository {
  async findAll(category?: string, isFeatured?: boolean, limit?: number, offset?: number, lang?: string, month?: number): Promise<Article[]> {
    let query = 'SELECT * FROM articles WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (lang) { query += ` AND language = $${paramIndex}`; params.push(lang); paramIndex++; }
    if (category) { query += ` AND category = $${paramIndex}`; params.push(category); paramIndex++; }
    if (isFeatured !== undefined) { query += ` AND is_featured = $${paramIndex}`; params.push(isFeatured); paramIndex++; }
    if (month !== undefined) {
      query += ` AND (age_min_months IS NULL OR age_min_months <= $${paramIndex})`;
      query += ` AND (age_max_months IS NULL OR age_max_months >= $${paramIndex})`;
      params.push(month);
      paramIndex++;
    }

    query += ' ORDER BY created_at DESC';

    if (limit) {
      query += ` LIMIT $${paramIndex}`; params.push(limit); paramIndex++;
      if (offset) { query += ` OFFSET $${paramIndex}`; params.push(offset); }
    }

    const result = await database.query(query, params);
    return result.rows;
  }

  async findByAgeRange(ageMonths: number, category?: string, lang: string = 'en'): Promise<Article[]> {
    let query = `
      SELECT * FROM articles
      WHERE language = $1
        AND age_min_months <= $2
        AND age_max_months >= $2
        AND doctor_name IS NOT NULL
    `;
    const params: any[] = [lang, ageMonths];
    let paramIndex = 3;

    if (category) {
      query += ` AND category = $${paramIndex}`;
      params.push(category);
      paramIndex++;
    }

    query += ' ORDER BY is_featured DESC, view_count DESC';
    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<Article | null> {
    const result = await database.query('SELECT * FROM articles WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(input: CreateArticleInput): Promise<Article> {
    const result = await database.query(
      `INSERT INTO articles
        (title, content, category, author, image_url, reading_time_minutes,
         is_featured, doctor_name, doctor_credentials, doctor_verified,
         age_min_months, age_max_months, language)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,'en')
       RETURNING *`,
      [
        input.title, input.content, input.category,
        input.author ?? 'BabyG Editorial Team',
        input.image_url ?? null,
        input.reading_time_minutes ?? null,
        input.is_featured ?? false,
        input.doctor_name ?? null,
        input.doctor_credentials ?? null,
        input.doctor_verified ?? false,
        input.age_min_months ?? 0,
        input.age_max_months ?? 60,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, input: Partial<CreateArticleInput>): Promise<Article | null> {
    const fields: string[] = [];
    const params: any[] = [];
    let i = 1;

    const allowed: (keyof CreateArticleInput)[] = [
      'title', 'content', 'category', 'author', 'image_url',
      'reading_time_minutes', 'is_featured', 'doctor_name',
      'doctor_credentials', 'doctor_verified', 'age_min_months', 'age_max_months',
    ];

    for (const key of allowed) {
      if (input[key] !== undefined) {
        fields.push(`${key} = $${i}`);
        params.push(input[key]);
        i++;
      }
    }

    if (fields.length === 0) return this.findById(id);

    fields.push(`updated_at = NOW()`);
    params.push(id);

    const result = await database.query(
      `UPDATE articles SET ${fields.join(', ')} WHERE id = $${i} RETURNING *`,
      params
    );
    return result.rows[0] || null;
  }

  async delete(id: number): Promise<boolean> {
    const result = await database.query('DELETE FROM articles WHERE id = $1', [id]);
    return (result.rowCount ?? 0) > 0;
  }

  async search(searchTerm: string, limit: number = 20): Promise<Article[]> {
    const result = await database.query(
      `SELECT * FROM articles
       WHERE title ILIKE $1 OR content ILIKE $1
       ORDER BY created_at DESC LIMIT $2`,
      [`%${searchTerm}%`, limit]
    );
    return result.rows;
  }

  async incrementViewCount(id: number): Promise<void> {
    await database.query('UPDATE articles SET view_count = view_count + 1 WHERE id = $1', [id]);
  }

  async bookmarkArticle(userId: number, articleId: number): Promise<void> {
    await database.query(
      `INSERT INTO article_bookmarks (user_id, article_id) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
      [userId, articleId]
    );
  }

  async unbookmarkArticle(userId: number, articleId: number): Promise<void> {
    await database.query(
      'DELETE FROM article_bookmarks WHERE user_id = $1 AND article_id = $2',
      [userId, articleId]
    );
  }

  async getBookmarkedArticles(userId: number): Promise<Article[]> {
    const result = await database.query(
      `SELECT a.* FROM articles a
       JOIN article_bookmarks ab ON a.id = ab.article_id
       WHERE ab.user_id = $1 ORDER BY ab.created_at DESC`,
      [userId]
    );
    return result.rows;
  }

  async recordReadingHistory(userId: number, articleId: number): Promise<void> {
    await database.query(
      `INSERT INTO article_reading_history (user_id, article_id) VALUES ($1, $2)`,
      [userId, articleId]
    );
  }
}
