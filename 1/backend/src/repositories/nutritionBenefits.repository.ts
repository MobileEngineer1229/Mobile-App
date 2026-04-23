import { database } from '../config/database';

export interface NutritionCategory {
  id: number;
  name: string;
  description?: string;
  icon_url?: string;
  sort_order: number;
}

export interface NutritionFood {
  id: number;
  category_id: number;
  category_name?: string;
  name: string;
  description?: string;
  image_url?: string;
  is_vegetarian: boolean;
  age_groups?: string;   // stored as comma-separated string
  sort_order: number;
}

export interface NutritionCategoryWithFoods extends NutritionCategory {
  foods: NutritionFood[];
}

export class NutritionBenefitsRepository {

  async getAllCategories(lang?: string): Promise<NutritionCategory[]> {
    const query = lang
      ? 'SELECT * FROM nutrition_categories WHERE language = $1 ORDER BY sort_order ASC'
      : 'SELECT * FROM nutrition_categories ORDER BY sort_order ASC';
    const result = await database.query(query, lang ? [lang] : []);
    return result.rows;
  }

  async getCategoryById(id: number): Promise<NutritionCategory | null> {
    const result = await database.query(
      'SELECT * FROM nutrition_categories WHERE id = $1', [id]
    );
    return result.rows[0] || null;
  }

  async getFoodsByCategory(categoryId: number, filters?: { ageGroup?: string; vegetarianOnly?: boolean; lang?: string }): Promise<NutritionFood[]> {
    const lang = filters?.lang || null;
    let query = `SELECT nf.*, nc.name AS category_name
                 FROM nutrition_foods nf
                 JOIN nutrition_categories nc ON nc.id = nf.category_id
                 WHERE nf.category_id = $1`;
    const params: any[] = [categoryId];
    let i = 2;
    if (lang) { query += ` AND nf.language = $${i}`; params.push(lang); i++; }

    if (filters?.ageGroup) {
      query += ` AND (nf.age_groups IS NULL OR nf.age_groups LIKE $${i})`;
      params.push(`%${filters.ageGroup}%`);
      i++;
    }
    if (filters?.vegetarianOnly) {
      query += ` AND nf.is_vegetarian = TRUE`;
    }

    query += ' ORDER BY nf.sort_order ASC';
    const result = await database.query(query, params);
    return result.rows;
  }

  async getAllCategoriesWithFoods(filters?: { ageGroup?: string; vegetarianOnly?: boolean; lang?: string }): Promise<NutritionCategoryWithFoods[]> {
    const lang = filters?.lang || null;
    let foodQuery = `
      SELECT nf.*, nc.name AS category_name
      FROM nutrition_foods nf
      JOIN nutrition_categories nc ON nc.id = nf.category_id
      WHERE 1=1
    `;
    const params: any[] = [];
    let i = 1;

    if (lang) { foodQuery += ` AND nf.language = $${i}`; params.push(lang); i++; }
    if (filters?.ageGroup) {
      foodQuery += ` AND (nf.age_groups IS NULL OR nf.age_groups LIKE $${i})`;
      params.push(`%${filters.ageGroup}%`);
      i++;
    }
    if (filters?.vegetarianOnly) {
      foodQuery += ` AND nf.is_vegetarian = TRUE`;
    }
    foodQuery += ' ORDER BY nf.category_id ASC, nf.sort_order ASC';

    const catQuery = lang
      ? 'SELECT * FROM nutrition_categories WHERE language = $1 ORDER BY sort_order ASC'
      : 'SELECT * FROM nutrition_categories ORDER BY sort_order ASC';
    const [catResult, foodResult] = await Promise.all([
      database.query(catQuery, lang ? [lang] : []),
      database.query(foodQuery, params),
    ]);

    const foodsByCategory: Record<number, NutritionFood[]> = {};
    for (const food of foodResult.rows) {
      if (!foodsByCategory[food.category_id]) foodsByCategory[food.category_id] = [];
      foodsByCategory[food.category_id].push(food);
    }

    return catResult.rows.map((cat: NutritionCategory) => ({
      ...cat,
      foods: foodsByCategory[cat.id] ?? [],
    }));
  }

  async getFoodById(id: number): Promise<NutritionFood | null> {
    const result = await database.query(
      `SELECT nf.*, nc.name AS category_name
       FROM nutrition_foods nf
       JOIN nutrition_categories nc ON nc.id = nf.category_id
       WHERE nf.id = $1`,
      [id]
    );
    return result.rows[0] || null;
  }

  async searchFoods(query: string, filters?: { ageGroup?: string; vegetarianOnly?: boolean }): Promise<NutritionFood[]> {
    let sql = `
      SELECT nf.*, nc.name AS category_name
      FROM nutrition_foods nf
      JOIN nutrition_categories nc ON nc.id = nf.category_id
      WHERE nf.name ILIKE $1
    `;
    const params: any[] = [`%${query}%`];
    let i = 2;

    if (filters?.ageGroup) {
      sql += ` AND (nf.age_groups IS NULL OR nf.age_groups LIKE $${i})`;
      params.push(`%${filters.ageGroup}%`);
      i++;
    }
    if (filters?.vegetarianOnly) {
      sql += ` AND nf.is_vegetarian = TRUE`;
    }

    sql += ' ORDER BY nf.name ASC';
    const result = await database.query(sql, params);
    return result.rows;
  }

  // ─── Admin CRUD ────────────────────────────────────────────────────────────

  async createFood(data: {
    category_id: number; name: string; description?: string;
    image_url?: string; is_vegetarian?: boolean; age_groups?: string; sort_order?: number; language?: string;
  }): Promise<NutritionFood> {
    const result = await database.query(
      `INSERT INTO nutrition_foods (category_id, name, description, image_url, is_vegetarian, age_groups, sort_order, language)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
      [
        data.category_id, data.name, data.description ?? null, data.image_url ?? null,
        data.is_vegetarian ?? false, data.age_groups ?? null, data.sort_order ?? 99,
        data.language ?? 'en',
      ]
    );
    return result.rows[0];
  }

  async updateFood(id: number, data: Partial<{
    category_id: number; name: string; description: string;
    image_url: string; is_vegetarian: boolean; age_groups: string; sort_order: number;
  }>): Promise<NutritionFood | null> {
    const sets: string[] = [];
    const params: any[] = [];
    let i = 1;
    const fields: Record<string, any> = {
      category_id: data.category_id, name: data.name, description: data.description,
      image_url: data.image_url, is_vegetarian: data.is_vegetarian,
      age_groups: data.age_groups, sort_order: data.sort_order,
    };
    for (const [key, val] of Object.entries(fields)) {
      if (val !== undefined) { sets.push(`${key} = $${i++}`); params.push(val); }
    }
    if (!sets.length) return this.getFoodById(id);
    params.push(id);
    const result = await database.query(
      `UPDATE nutrition_foods SET ${sets.join(', ')} WHERE id = $${i} RETURNING *`,
      params
    );
    return result.rows[0] || null;
  }

  async deleteFood(id: number): Promise<boolean> {
    const result = await database.query('DELETE FROM nutrition_foods WHERE id = $1', [id]);
    return (result.rowCount ?? 0) > 0;
  }
}
