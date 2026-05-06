import { database } from '../config/database';

export interface PregnancyWeek {
  id: number;
  week_number: number;
  title: string;
  fetal_development: string;
  baby_size: string;
  baby_weight: string;
  body_changes: string;
  symptoms: string[];
  tips: string[];
  image_url?: string;
  created_at: Date;
  updated_at: Date;
}

export class PregnancyWeekRepository {
  async findByWeek(weekNumber: number): Promise<PregnancyWeek | null> {
    const result = await database.query(
      'SELECT * FROM pregnancy_weeks WHERE week_number = $1',
      [weekNumber]
    );
    return result.rows[0] || null;
  }

  async findAll(): Promise<PregnancyWeek[]> {
    const result = await database.query(
      'SELECT * FROM pregnancy_weeks ORDER BY week_number ASC'
    );
    return result.rows;
  }

  async findByWeekRange(startWeek: number, endWeek: number): Promise<PregnancyWeek[]> {
    const result = await database.query(
      'SELECT * FROM pregnancy_weeks WHERE week_number >= $1 AND week_number <= $2 ORDER BY week_number ASC',
      [startWeek, endWeek]
    );
    return result.rows;
  }

  async create(weekData: Partial<PregnancyWeek>): Promise<PregnancyWeek> {
    const result = await database.query(
      `INSERT INTO pregnancy_weeks 
       (week_number, title, fetal_development, baby_size, baby_weight, body_changes, symptoms, tips, image_url)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING *`,
      [
        weekData.week_number,
        weekData.title,
        weekData.fetal_development,
        weekData.baby_size,
        weekData.baby_weight,
        weekData.body_changes,
        weekData.symptoms || [],
        weekData.tips || [],
        weekData.image_url || null,
      ]
    );
    return result.rows[0];
  }
}
