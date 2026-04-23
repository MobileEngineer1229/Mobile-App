import { database } from '../config/database';
import crypto from 'crypto';

export interface VideoShare {
  id: number;
  memory_id: number;
  shared_by_user_id: number;
  share_token: string;
  share_type: string; // 'link', 'family', 'public'
  expires_at?: Date;
  view_count: number;
  created_at: Date;
}

export class VideoShareRepository {
  async createShare(memoryId: number, userId: number, shareType: string = 'link', expiresInDays?: number): Promise<VideoShare> {
    const shareToken = crypto.randomBytes(32).toString('hex');
    const expiresAt = expiresInDays
      ? new Date(Date.now() + expiresInDays * 24 * 60 * 60 * 1000)
      : null;

    const result = await database.query(
      `INSERT INTO video_shares (memory_id, shared_by_user_id, share_token, share_type, expires_at)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [memoryId, userId, shareToken, shareType, expiresAt]
    );
    return result.rows[0];
  }

  async findByToken(shareToken: string): Promise<VideoShare | null> {
    const result = await database.query(
      'SELECT * FROM video_shares WHERE share_token = $1 AND (expires_at IS NULL OR expires_at > NOW())',
      [shareToken]
    );
    return result.rows[0] || null;
  }

  async incrementViewCount(shareId: number): Promise<void> {
    await database.query('UPDATE video_shares SET view_count = view_count + 1 WHERE id = $1', [shareId]);
  }

  async getSharesByMemory(memoryId: number): Promise<VideoShare[]> {
    const result = await database.query(
      'SELECT * FROM video_shares WHERE memory_id = $1 ORDER BY created_at DESC',
      [memoryId]
    );
    return result.rows;
  }

  async deleteShare(shareId: number, userId: number): Promise<void> {
    const result = await database.query(
      'DELETE FROM video_shares WHERE id = $1 AND shared_by_user_id = $2',
      [shareId, userId]
    );
    if (result.rowCount === 0) {
      throw new Error('Share not found');
    }
  }
}
