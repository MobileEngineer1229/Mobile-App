import { database } from '../config/database';

export interface NutritionGuide {
  id: number;
  title: string;
  description: string;
  trimester: number; // 1, 2, or 3
  content: string;
  meal_suggestions: string[];
  recipes: string[];
  image_url?: string;
  created_at: Date;
  updated_at: Date;
}

export class NutritionRepository {
  async findByTrimester(trimester: number): Promise<NutritionGuide[]> {
    const result = await database.query(
      'SELECT * FROM nutrition_guides WHERE trimester = $1 ORDER BY title ASC',
      [trimester]
    );
    return result.rows;
  }

  async findAll(): Promise<NutritionGuide[]> {
    const result = await database.query('SELECT * FROM nutrition_guides ORDER BY trimester ASC, title ASC');
    return result.rows;
  }

  async findById(id: number): Promise<NutritionGuide | null> {
    const result = await database.query('SELECT * FROM nutrition_guides WHERE id = $1', [id]);
    return result.rows[0] || null;
  }
}
