import { database } from '../config/database';

export interface BirthClub {
  id: number;
  birth_month: number;
  birth_year: number;
  club_name?: string;
  member_count: number;
  created_at: Date;
}

export interface CommunityPost {
  id: number;
  user_id: number;
  birth_club_id?: number;
  post_type: string; // 'discussion', 'question', 'experience', 'advice'
  title: string;
  content: string;
  image_url?: string;
  like_count: number;
  comment_count: number;
  is_pinned: boolean;
  created_at: Date;
  updated_at: Date;
}

export interface PostComment {
  id: number;
  post_id: number;
  user_id: number;
  content: string;
  like_count: number;
  created_at: Date;
  updated_at: Date;
}

export class CommunityRepository {
  // Birth Clubs
  async findOrCreateBirthClub(birthMonth: number, birthYear: number): Promise<BirthClub> {
    let result = await database.query(
      'SELECT * FROM birth_clubs WHERE birth_month = $1 AND birth_year = $2',
      [birthMonth, birthYear]
    );

    if (result.rows.length === 0) {
      result = await database.query(
        `INSERT INTO birth_clubs (birth_month, birth_year, club_name, member_count)
         VALUES ($1, $2, $3, 0)
         RETURNING *`,
        [birthMonth, birthYear, `Birth Club ${birthMonth}/${birthYear}`]
      );
    }

    return result.rows[0];
  }

  async joinBirthClub(userId: number, birthClubId: number): Promise<void> {
    await database.query(
      `INSERT INTO birth_club_memberships (user_id, birth_club_id)
       VALUES ($1, $2)
       ON CONFLICT (user_id, birth_club_id) DO NOTHING`,
      [userId, birthClubId]
    );

    // Update member count
    await database.query(
      'UPDATE birth_clubs SET member_count = (SELECT COUNT(*) FROM birth_club_memberships WHERE birth_club_id = $1) WHERE id = $1',
      [birthClubId]
    );
  }

  async getUserBirthClubs(userId: number): Promise<BirthClub[]> {
    const result = await database.query(
      `SELECT bc.* FROM birth_clubs bc
       JOIN birth_club_memberships bcm ON bc.id = bcm.birth_club_id
       WHERE bcm.user_id = $1`,
      [userId]
    );
    return result.rows;
  }

  // Community Posts
  async createPost(postData: Partial<CommunityPost>): Promise<CommunityPost> {
    const result = await database.query(
      `INSERT INTO community_posts 
       (user_id, birth_club_id, post_type, title, content, image_url)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [
        postData.user_id,
        postData.birth_club_id || null,
        postData.post_type || 'discussion',
        postData.title,
        postData.content,
        postData.image_url || null,
      ]
    );
    return result.rows[0];
  }

  async getPosts(birthClubId?: number, postType?: string, limit: number = 20, offset: number = 0): Promise<CommunityPost[]> {
    let query = 'SELECT * FROM community_posts WHERE 1=1';
    const params: any[] = [];
    let paramIndex = 1;

    if (birthClubId) {
      query += ` AND birth_club_id = $${paramIndex}`;
      params.push(birthClubId);
      paramIndex++;
    }

    if (postType) {
      query += ` AND post_type = $${paramIndex}`;
      params.push(postType);
      paramIndex++;
    }

    query += ' ORDER BY is_pinned DESC, created_at DESC';
    query += ` LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`;
    params.push(limit, offset);

    const result = await database.query(query, params);
    return result.rows;
  }

  async getPostById(id: number): Promise<CommunityPost | null> {
    const result = await database.query('SELECT * FROM community_posts WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async likePost(userId: number, postId: number): Promise<void> {
    await database.query(
      `INSERT INTO post_likes (post_id, user_id)
       VALUES ($1, $2)
       ON CONFLICT (post_id, user_id) DO NOTHING`,
      [postId, userId]
    );

    // Update like count
    await database.query(
      'UPDATE community_posts SET like_count = (SELECT COUNT(*) FROM post_likes WHERE post_id = $1) WHERE id = $1',
      [postId]
    );
  }

  async unlikePost(userId: number, postId: number): Promise<void> {
    await database.query('DELETE FROM post_likes WHERE post_id = $1 AND user_id = $2', [postId, userId]);

    // Update like count
    await database.query(
      'UPDATE community_posts SET like_count = (SELECT COUNT(*) FROM post_likes WHERE post_id = $1) WHERE id = $1',
      [postId]
    );
  }

  // Comments
  async createComment(commentData: Partial<PostComment>): Promise<PostComment> {
    const result = await database.query(
      `INSERT INTO post_comments (post_id, user_id, content)
       VALUES ($1, $2, $3)
       RETURNING *`,
      [commentData.post_id, commentData.user_id, commentData.content]
    );

    // Update comment count
    await database.query(
      'UPDATE community_posts SET comment_count = (SELECT COUNT(*) FROM post_comments WHERE post_id = $1) WHERE id = $1',
      [commentData.post_id]
    );

    return result.rows[0];
  }

  async getComments(postId: number): Promise<PostComment[]> {
    const result = await database.query(
      'SELECT * FROM post_comments WHERE post_id = $1 ORDER BY created_at ASC',
      [postId]
    );
    return result.rows;
  }
}
