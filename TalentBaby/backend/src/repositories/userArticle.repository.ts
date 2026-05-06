import { database } from '../config/database';

export interface UserArticle {
  id: number;
  user_id: number;
  title: string;
  content: string;
  category: string;
  tags: string[];
  keywords: string[];
  thumbnail_url?: string;
  status: string;
  view_count: number;
  like_count: number;
  comment_count: number;
  created_at: string;
  updated_at: string;
  // joined
  author_name?: string;
  author_role?: string;
  doctor_grade?: string;
  doctor_specialty?: string;
  is_liked?: boolean;
}

export interface UserArticleComment {
  id: number;
  article_id: number;
  user_id: number;
  parent_id?: number;
  content: string;
  like_count: number;
  is_doctor_comment: boolean;
  created_at: string;
  updated_at: string;
  author_name?: string;
  doctor_grade?: string;
  doctor_specialty?: string;
  is_liked?: boolean;
  replies?: UserArticleComment[];
}

const POINT_RULES = {
  article_submit: 10,
  article_liked: 2,
  comment_submit: 3,
  comment_liked: 1,
} as const;

export class UserArticleRepository {
  // ─── Articles ────────────────────────────────────────────────────────────

  async findAll(filters: { category?: string; userId?: number; search?: string; doctor_only?: boolean; limit?: number; offset?: number } = {}) {
    const conditions: string[] = ['a.status = $1'];
    const params: unknown[] = ['published'];
    let i = 2;

    if (filters.category)    { conditions.push(`a.category = $${i++}`); params.push(filters.category); }
    if (filters.doctor_only) { conditions.push(`u.role = 'doctor'`); }
    if (filters.search) {
      conditions.push(`(a.title ILIKE $${i} OR a.content ILIKE $${i} OR EXISTS (SELECT 1 FROM unnest(a.keywords) k WHERE k ILIKE $${i}))`);
      params.push(`%${filters.search}%`);
      i++;
    }

    const where = conditions.join(' AND ');
    const limit = filters.limit ?? 20;
    const offset = filters.offset ?? 0;

    const likedSub = filters.userId
      ? `(SELECT 1 FROM user_article_likes l WHERE l.article_id = a.id AND l.user_id = ${Number(filters.userId)}) IS NOT NULL`
      : 'false';

    const rows = await database.query(
      `SELECT a.*, u.full_name AS author_name, u.role AS author_role,
              dp.grade AS doctor_grade, dp.specialty AS doctor_specialty,
              ${likedSub} AS is_liked
       FROM user_articles a
       JOIN users u ON u.id = a.user_id
       LEFT JOIN doctor_profiles dp ON dp.user_id = a.user_id
       WHERE ${where}
       ORDER BY a.created_at DESC
       LIMIT $${i++} OFFSET $${i++}`,
      [...params, limit, offset]
    );
    return rows.rows as UserArticle[];
  }

  async findById(id: number, requestingUserId?: number) {
    const likedSub = requestingUserId
      ? `(SELECT 1 FROM user_article_likes l WHERE l.article_id = a.id AND l.user_id = ${Number(requestingUserId)}) IS NOT NULL`
      : 'false';

    const row = await database.query(
      `SELECT a.*, u.full_name AS author_name, u.role AS author_role,
              dp.grade AS doctor_grade, dp.specialty AS doctor_specialty,
              ${likedSub} AS is_liked
       FROM user_articles a
       JOIN users u ON u.id = a.user_id
       LEFT JOIN doctor_profiles dp ON dp.user_id = a.user_id
       WHERE a.id = $1`,
      [id]
    );
    return row.rows[0] as UserArticle | undefined;
  }

  async create(data: { user_id: number; title: string; content: string; category?: string; tags?: string[]; keywords?: string[]; thumbnail_url?: string; status?: string }) {
    const row = await database.query(
      `INSERT INTO user_articles (user_id, title, content, category, tags, keywords, thumbnail_url, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
      [data.user_id, data.title, data.content, data.category ?? 'general', data.tags ?? [], data.keywords ?? [], data.thumbnail_url ?? null, data.status ?? 'published']
    );
    return row.rows[0] as UserArticle;
  }

  async update(id: number, userId: number, data: Partial<Pick<UserArticle, 'title' | 'content' | 'category' | 'tags' | 'thumbnail_url' | 'status'>>) {
    const fields: string[] = [];
    const params: unknown[] = [];
    let i = 1;

    if (data.title !== undefined) { fields.push(`title = $${i++}`); params.push(data.title); }
    if (data.content !== undefined) { fields.push(`content = $${i++}`); params.push(data.content); }
    if (data.category !== undefined) { fields.push(`category = $${i++}`); params.push(data.category); }
    if (data.tags !== undefined) { fields.push(`tags = $${i++}`); params.push(data.tags); }
    if (data.thumbnail_url !== undefined) { fields.push(`thumbnail_url = $${i++}`); params.push(data.thumbnail_url); }
    if (data.status !== undefined) { fields.push(`status = $${i++}`); params.push(data.status); }

    if (!fields.length) return null;

    fields.push(`updated_at = CURRENT_TIMESTAMP`);
    params.push(id, userId);

    const row = await database.query(
      `UPDATE user_articles SET ${fields.join(', ')} WHERE id = $${i++} AND user_id = $${i++} RETURNING *`,
      params
    );
    return row.rows[0] as UserArticle | undefined;
  }

  async delete(id: number, userId: number) {
    const row = await database.query(
      `DELETE FROM user_articles WHERE id = $1 AND user_id = $2 RETURNING id`,
      [id, userId]
    );
    return row.rowCount && row.rowCount > 0;
  }

  async incrementView(id: number) {
    await database.query(`UPDATE user_articles SET view_count = view_count + 1 WHERE id = $1`, [id]);
  }

  // ─── Likes ───────────────────────────────────────────────────────────────

  async toggleLike(articleId: number, userId: number): Promise<'liked' | 'unliked'> {
    const existing = await database.query(
      `SELECT 1 FROM user_article_likes WHERE article_id = $1 AND user_id = $2`,
      [articleId, userId]
    );

    if (existing.rows.length) {
      await database.query(`DELETE FROM user_article_likes WHERE article_id = $1 AND user_id = $2`, [articleId, userId]);
      await database.query(`UPDATE user_articles SET like_count = GREATEST(like_count - 1, 0) WHERE id = $1`, [articleId]);
      return 'unliked';
    } else {
      await database.query(`INSERT INTO user_article_likes (article_id, user_id) VALUES ($1, $2)`, [articleId, userId]);
      await database.query(`UPDATE user_articles SET like_count = like_count + 1 WHERE id = $1`, [articleId]);

      // Award points to article author
      const article = await database.query(`SELECT user_id FROM user_articles WHERE id = $1`, [articleId]);
      if (article.rows[0] && article.rows[0].user_id !== userId) {
        await this.awardPoints(article.rows[0].user_id, POINT_RULES.article_liked, 'article_liked', articleId, 'article', 'Article liked');
      }
      return 'liked';
    }
  }

  // ─── Comments ────────────────────────────────────────────────────────────

  async getComments(articleId: number, requestingUserId?: number) {
    const likedSub = requestingUserId
      ? `(SELECT 1 FROM user_article_comment_likes cl WHERE cl.comment_id = c.id AND cl.user_id = ${Number(requestingUserId)}) IS NOT NULL`
      : 'false';

    const rows = await database.query(
      `SELECT c.*, u.full_name AS author_name, u.role AS author_role,
              dp.grade AS doctor_grade, dp.specialty AS doctor_specialty,
              ${likedSub} AS is_liked
       FROM user_article_comments c
       JOIN users u ON u.id = c.user_id
       LEFT JOIN doctor_profiles dp ON dp.user_id = c.user_id
       WHERE c.article_id = $1
       ORDER BY c.parent_id NULLS FIRST, c.created_at ASC`,
      [articleId]
    );
    // Nest replies under parent comments
    const comments = rows.rows as UserArticleComment[];
    const map = new Map<number, UserArticleComment>();
    const roots: UserArticleComment[] = [];

    comments.forEach(c => { c.replies = []; map.set(c.id, c); });
    comments.forEach(c => {
      if (c.parent_id) {
        const parent = map.get(c.parent_id);
        if (parent) parent.replies!.push(c);
      } else {
        roots.push(c);
      }
    });
    return roots;
  }

  async createComment(data: { article_id: number; user_id: number; parent_id?: number; content: string; is_doctor_comment?: boolean }) {
    const row = await database.query(
      `INSERT INTO user_article_comments (article_id, user_id, parent_id, content, is_doctor_comment)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [data.article_id, data.user_id, data.parent_id ?? null, data.content, data.is_doctor_comment ?? false]
    );
    await database.query(`UPDATE user_articles SET comment_count = comment_count + 1 WHERE id = $1`, [data.article_id]);
    return row.rows[0] as UserArticleComment;
  }

  async deleteComment(commentId: number, userId: number) {
    const row = await database.query(
      `DELETE FROM user_article_comments WHERE id = $1 AND user_id = $2 RETURNING article_id`,
      [commentId, userId]
    );
    if (row.rows[0]) {
      await database.query(`UPDATE user_articles SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = $1`, [row.rows[0].article_id]);
    }
    return row.rowCount && row.rowCount > 0;
  }

  async toggleCommentLike(commentId: number, userId: number): Promise<'liked' | 'unliked'> {
    const existing = await database.query(
      `SELECT 1 FROM user_article_comment_likes WHERE comment_id = $1 AND user_id = $2`,
      [commentId, userId]
    );

    if (existing.rows.length) {
      await database.query(`DELETE FROM user_article_comment_likes WHERE comment_id = $1 AND user_id = $2`, [commentId, userId]);
      await database.query(`UPDATE user_article_comments SET like_count = GREATEST(like_count - 1, 0) WHERE id = $1`, [commentId]);
      return 'unliked';
    } else {
      await database.query(`INSERT INTO user_article_comment_likes (comment_id, user_id) VALUES ($1, $2)`, [commentId, userId]);
      await database.query(`UPDATE user_article_comments SET like_count = like_count + 1 WHERE id = $1`, [commentId]);

      const comment = await database.query(`SELECT user_id FROM user_article_comments WHERE id = $1`, [commentId]);
      if (comment.rows[0] && comment.rows[0].user_id !== userId) {
        await this.awardPoints(comment.rows[0].user_id, POINT_RULES.comment_liked, 'comment_liked', commentId, 'comment', 'Comment liked');
      }
      return 'liked';
    }
  }

  // ─── Points ──────────────────────────────────────────────────────────────

  async awardPoints(userId: number, points: number, reason: string, referenceId?: number, referenceType?: string, description?: string) {
    await database.query(
      `INSERT INTO user_points (user_id, total_points) VALUES ($1, $2)
       ON CONFLICT (user_id) DO UPDATE SET total_points = user_points.total_points + $2, updated_at = CURRENT_TIMESTAMP`,
      [userId, points]
    );
    await database.query(
      `INSERT INTO user_point_history (user_id, points, reason, reference_id, reference_type, description)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [userId, points, reason, referenceId ?? null, referenceType ?? null, description ?? null]
    );
  }

  async getPoints(userId: number) {
    const row = await database.query(`SELECT total_points FROM user_points WHERE user_id = $1`, [userId]);
    return (row.rows[0]?.total_points ?? 0) as number;
  }

  async getPointHistory(userId: number) {
    const rows = await database.query(
      `SELECT * FROM user_point_history WHERE user_id = $1 ORDER BY created_at DESC LIMIT 50`,
      [userId]
    );
    return rows.rows;
  }

  async getLeaderboard(limit = 20) {
    const rows = await database.query(
      `SELECT u.id, u.full_name, u.profile_picture_url, u.role,
              COALESCE(p.total_points, 0) AS total_points,
              dp.grade AS doctor_grade, dp.specialty AS doctor_specialty
       FROM users u
       LEFT JOIN user_points p ON p.user_id = u.id
       LEFT JOIN doctor_profiles dp ON dp.user_id = u.id
       ORDER BY total_points DESC
       LIMIT $1`,
      [limit]
    );
    return rows.rows;
  }

  // ─── Doctor Profiles ─────────────────────────────────────────────────────

  async findDoctorProfile(userId: number) {
    const row = await database.query(
      `SELECT dp.*, u.full_name, u.email, u.profile_picture_url
       FROM doctor_profiles dp
       JOIN users u ON u.id = dp.user_id
       WHERE dp.user_id = $1`,
      [userId]
    );
    return row.rows[0];
  }

  async findAllDoctors(filters: { specialty?: string; grade_level?: number; verified?: boolean } = {}) {
    const conditions: string[] = ['1=1'];
    const params: unknown[] = [];
    let i = 1;

    if (filters.specialty) { conditions.push(`dp.specialty = $${i++}`); params.push(filters.specialty); }
    if (filters.grade_level) { conditions.push(`dp.grade_level = $${i++}`); params.push(filters.grade_level); }
    if (filters.verified !== undefined) { conditions.push(`dp.verified = $${i++}`); params.push(filters.verified); }

    const rows = await database.query(
      `SELECT dp.*, u.full_name, u.email, u.profile_picture_url,
              COALESCE(pt.total_points, 0) AS total_points
       FROM doctor_profiles dp
       JOIN users u ON u.id = dp.user_id
       LEFT JOIN user_points pt ON pt.user_id = dp.user_id
       WHERE ${conditions.join(' AND ')}
       ORDER BY dp.grade_level DESC, dp.verified DESC`,
      params
    );
    return rows.rows;
  }

  async upsertDoctorProfile(userId: number, data: {
    specialty: string;
    grade: string;
    grade_level: number;
    license_number?: string;
    hospital?: string;
    bio?: string;
  }) {
    const gradeMap: Record<string, number> = { 'intern': 1, 'resident': 2, 'specialist': 3, 'professor': 4, 'honorary': 5 };
    const gradeLevel = gradeMap[data.grade] ?? data.grade_level ?? 1;

    const row = await database.query(
      `INSERT INTO doctor_profiles (user_id, specialty, grade, grade_level, license_number, hospital, bio)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       ON CONFLICT (user_id) DO UPDATE SET
         specialty = EXCLUDED.specialty,
         grade = EXCLUDED.grade,
         grade_level = EXCLUDED.grade_level,
         license_number = COALESCE(EXCLUDED.license_number, doctor_profiles.license_number),
         hospital = COALESCE(EXCLUDED.hospital, doctor_profiles.hospital),
         bio = COALESCE(EXCLUDED.bio, doctor_profiles.bio),
         updated_at = CURRENT_TIMESTAMP
       RETURNING *`,
      [userId, data.specialty, data.grade, gradeLevel, data.license_number ?? null, data.hospital ?? null, data.bio ?? null]
    );

    // Set user role to 'doctor'
    await database.query(`UPDATE users SET role = 'doctor' WHERE id = $1`, [userId]);

    return row.rows[0];
  }

  async verifyDoctor(userId: number) {
    const row = await database.query(
      `UPDATE doctor_profiles SET verified = true, verified_at = CURRENT_TIMESTAMP WHERE user_id = $1 RETURNING *`,
      [userId]
    );
    return row.rows[0];
  }
}
