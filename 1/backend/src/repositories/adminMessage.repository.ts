import { database } from '../config/database';

export interface AdminMessage {
  id: number;
  title: string;
  body: string;
  type: 'announcement' | 'alert' | 'tip' | 'update';
  priority: 'low' | 'normal' | 'high' | 'urgent';
  target_role: 'all' | 'user' | 'doctor' | 'agent';
  is_published: boolean;
  published_at: string | null;
  created_at: string;
  updated_at: string;
}

export class AdminMessageRepository {
  async findAll() {
    const result = await database.query(
      `SELECT * FROM admin_messages ORDER BY created_at DESC`
    );
    return result.rows as AdminMessage[];
  }

  async findPublished(role?: string) {
    const result = await database.query(
      `SELECT * FROM admin_messages
       WHERE is_published = true
         AND (target_role = 'all' OR target_role = $1)
       ORDER BY published_at DESC`,
      [role ?? 'user']
    );
    return result.rows as AdminMessage[];
  }

  async findById(id: number) {
    const result = await database.query(
      `SELECT * FROM admin_messages WHERE id = $1`,
      [id]
    );
    return result.rows[0] as AdminMessage | undefined;
  }

  async create(data: Omit<AdminMessage, 'id' | 'created_at' | 'updated_at'>) {
    const result = await database.query(
      `INSERT INTO admin_messages (title, body, type, priority, target_role, is_published, published_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [
        data.title,
        data.body,
        data.type,
        data.priority,
        data.target_role,
        data.is_published,
        data.is_published ? (data.published_at ?? new Date().toISOString()) : null,
      ]
    );
    return result.rows[0] as AdminMessage;
  }

  async update(id: number, data: Partial<Omit<AdminMessage, 'id' | 'created_at' | 'updated_at'>>) {
    const fields: string[] = [];
    const params: unknown[] = [];
    let i = 1;

    if (data.title       !== undefined) { fields.push(`title = $${i++}`);       params.push(data.title); }
    if (data.body        !== undefined) { fields.push(`body = $${i++}`);        params.push(data.body); }
    if (data.type        !== undefined) { fields.push(`type = $${i++}`);        params.push(data.type); }
    if (data.priority    !== undefined) { fields.push(`priority = $${i++}`);    params.push(data.priority); }
    if (data.target_role !== undefined) { fields.push(`target_role = $${i++}`); params.push(data.target_role); }
    if (data.is_published !== undefined) {
      fields.push(`is_published = $${i++}`);
      params.push(data.is_published);
      if (data.is_published && !data.published_at) {
        fields.push(`published_at = CURRENT_TIMESTAMP`);
      } else if (!data.is_published) {
        fields.push(`published_at = NULL`);
      }
    }
    if (data.published_at !== undefined) { fields.push(`published_at = $${i++}`); params.push(data.published_at); }

    if (!fields.length) return null;
    fields.push(`updated_at = CURRENT_TIMESTAMP`);
    params.push(id);

    const result = await database.query(
      `UPDATE admin_messages SET ${fields.join(', ')} WHERE id = $${i} RETURNING *`,
      params
    );
    return result.rows[0] as AdminMessage | undefined;
  }

  async delete(id: number) {
    const result = await database.query(
      `DELETE FROM admin_messages WHERE id = $1 RETURNING id`,
      [id]
    );
    return (result.rowCount ?? 0) > 0;
  }
}
