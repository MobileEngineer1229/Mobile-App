import { database } from '../config/database';

export interface BumpPhoto {
  id: number;
  pregnancy_id: number;
  photo_url: string;
  thumbnail_url?: string;
  week_number: number;
  photo_date: Date;
  notes?: string;
  created_at: Date;
  updated_at: Date;
}

export class BumpPhotoRepository {
  async findByPregnancyId(pregnancyId: number): Promise<BumpPhoto[]> {
    const result = await database.query(
      'SELECT * FROM bump_photos WHERE pregnancy_id = $1 ORDER BY week_number ASC, photo_date ASC',
      [pregnancyId]
    );
    return result.rows;
  }

  async findById(id: number): Promise<BumpPhoto | null> {
    const result = await database.query('SELECT * FROM bump_photos WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async create(photoData: Partial<BumpPhoto>): Promise<BumpPhoto> {
    const result = await database.query(
      `INSERT INTO bump_photos (pregnancy_id, photo_url, thumbnail_url, week_number, photo_date, notes)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [
        photoData.pregnancy_id,
        photoData.photo_url,
        photoData.thumbnail_url || null,
        photoData.week_number,
        photoData.photo_date,
        photoData.notes || null,
      ]
    );
    return result.rows[0];
  }

  async delete(id: number): Promise<void> {
    const result = await database.query('DELETE FROM bump_photos WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new Error('Bump photo not found');
    }
  }
}
