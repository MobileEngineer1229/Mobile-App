import { database } from '../config/database';

export interface Recipe {
  id: number;
  title: string;
  description: string;
  target: 'baby' | 'mum';
  meal_slot?: string;
  baby_age_group?: string;
  age_range_min_months: number;
  age_range_max_months: number;
  recipe_type: string;
  ingredients: string[];
  instructions: string[];
  nutrition_info?: any;
  prep_time_minutes?: number;
  cooking_time_minutes?: number;
  image_url?: string;
  created_at: Date;
  updated_at: Date;
}

export interface DailyMealPlan {
  id: number;
  user_id: number;
  baby_id?: number;
  plan_type: 'baby' | 'mum';
  plan_date: string;
  meal_slot: string;
  recipe_id?: number;
  notes?: string;
  recipe?: Recipe;
}

// ─── Baby age group constants ──────────────────────────────────────────────
export const BABY_AGE_GROUPS = [
  { group: '0-6',   min: 0,  max: 6  },
  { group: '7-11',  min: 7,  max: 11 },
  { group: '12-17', min: 12, max: 17 },
  { group: '18-24', min: 18, max: 24 },
  { group: '25-30', min: 25, max: 30 },
  { group: '31-36', min: 31, max: 36 },
];

export const BABY_MEAL_SLOTS  = ['breakfast', 'lunch', 'dinner', 'snack'] as const;
export const MUM_MEAL_SLOTS   = ['early_morning', 'breakfast', 'mid_morning', 'lunch', 'evening', 'dinner', 'bedtime'] as const;

export class RecipeRepository {
  // ─── Existing ─────────────────────────────────────────────────────────────

  async findByAge(ageInMonths: number): Promise<Recipe[]> {
    const result = await database.query(
      `SELECT * FROM recipes
       WHERE age_range_min_months <= $1 AND age_range_max_months >= $1
       ORDER BY RANDOM()`,
      [ageInMonths]
    );
    return result.rows;
  }

  async findByType(recipeType: string, ageInMonths?: number): Promise<Recipe[]> {
    let query = 'SELECT * FROM recipes WHERE recipe_type = $1';
    const params: any[] = [recipeType];
    if (ageInMonths) {
      query += ' AND age_range_min_months <= $2 AND age_range_max_months >= $2';
      params.push(ageInMonths);
    }
    query += ' ORDER BY title ASC';
    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number): Promise<Recipe | null> {
    const result = await database.query('SELECT * FROM recipes WHERE id = $1', [id]);
    return result.rows[0] || null;
  }

  async getDailyRecipeTip(ageInMonths: number): Promise<Recipe | null> {
    const result = await database.query(
      `SELECT * FROM recipes
       WHERE age_range_min_months <= $1 AND age_range_max_months >= $1
       ORDER BY RANDOM() LIMIT 1`,
      [ageInMonths]
    );
    return result.rows[0] || null;
  }

  async findAll(filters?: { age_months?: number; category?: string; target?: string; lang?: string }): Promise<Recipe[]> {
    const lang = filters?.lang || null;
    let query = 'SELECT * FROM recipes WHERE 1=1';
    const params: any[] = [];
    let i = 1;

    if (lang) {
      query += ` AND language = $${i}`;
      params.push(lang); i++;
    }
    if (filters?.age_months) {
      query += ` AND age_range_min_months <= $${i} AND age_range_max_months >= $${i}`;
      params.push(filters.age_months); i++;
    }
    if (filters?.category) {
      query += ` AND recipe_type = $${i}`;
      params.push(filters.category); i++;
    }
    if (filters?.target) {
      query += ` AND target = $${i}`;
      params.push(filters.target); i++;
    }

    query += ' ORDER BY title ASC';
    const result = await database.query(query, params);
    return result.rows;
  }

  async addToFavorites(userId: number, recipeId: number): Promise<void> {
    await database.query(
      `INSERT INTO recipe_favorites (user_id, recipe_id)
       VALUES ($1, $2) ON CONFLICT (user_id, recipe_id) DO NOTHING`,
      [userId, recipeId]
    );
  }

  async removeFromFavorites(userId: number, recipeId: number): Promise<void> {
    await database.query(
      'DELETE FROM recipe_favorites WHERE user_id = $1 AND recipe_id = $2',
      [userId, recipeId]
    );
  }

  async getFavorites(userId: number): Promise<Recipe[]> {
    const result = await database.query(
      `SELECT r.* FROM recipes r
       INNER JOIN recipe_favorites rf ON r.id = rf.recipe_id
       WHERE rf.user_id = $1
       ORDER BY rf.created_at DESC`,
      [userId]
    );
    return result.rows;
  }

  async isFavorite(userId: number, recipeId: number): Promise<boolean> {
    const result = await database.query(
      'SELECT 1 FROM recipe_favorites WHERE user_id = $1 AND recipe_id = $2',
      [userId, recipeId]
    );
    return result.rows.length > 0;
  }

  // ─── Baby recipes ──────────────────────────────────────────────────────────

  async findBabyByAgeGroup(ageGroup: string, mealSlot?: string, lang?: string): Promise<Recipe[]> {
    let query = `SELECT * FROM recipes WHERE target = 'baby' AND baby_age_group = $1`;
    const params: any[] = [ageGroup];
    let i = 2;
    if (lang) { query += ` AND language = $${i}`; params.push(lang); i++; }
    if (mealSlot) { query += ` AND meal_slot = $${i}`; params.push(mealSlot); }
    query += ' ORDER BY title ASC';
    const result = await database.query(query, params);
    return result.rows;
  }

  async findBabyByAgeMonths(ageInMonths: number, mealSlot?: string): Promise<Recipe[]> {
    const group = BABY_AGE_GROUPS.find(g => ageInMonths >= g.min && ageInMonths <= g.max);
    const ageGroup = group?.group ?? '0-6';
    return this.findBabyByAgeGroup(ageGroup, mealSlot);
  }

  // ─── Mum recipes ───────────────────────────────────────────────────────────

  async findMumRecipes(mealSlot?: string, lang?: string): Promise<Recipe[]> {
    let query = `SELECT * FROM recipes WHERE target = 'mum'`;
    const params: any[] = [];
    let i = 1;
    if (lang) { query += ` AND language = $${i}`; params.push(lang); i++; }
    if (mealSlot) { query += ` AND meal_slot = $${i}`; params.push(mealSlot); }
    query += ' ORDER BY title ASC';
    const result = await database.query(query, params);
    return result.rows;
  }

  // ─── Daily meal plan ───────────────────────────────────────────────────────

  async getDailyPlan(userId: number, planType: 'baby' | 'mum', planDate: string, babyId?: number): Promise<DailyMealPlan[]> {
    const result = await database.query(
      `SELECT dmp.*, r.title as recipe_title, r.description as recipe_description,
              r.image_url, r.prep_time_minutes, r.cooking_time_minutes,
              r.ingredients, r.instructions, r.nutrition_info,
              r.meal_slot as recipe_meal_slot, r.target, r.baby_age_group
       FROM daily_meal_plans dmp
       LEFT JOIN recipes r ON r.id = dmp.recipe_id
       WHERE dmp.user_id = $1
         AND dmp.plan_type = $2
         AND dmp.plan_date = $3
         ${babyId ? 'AND dmp.baby_id = $4' : 'AND dmp.baby_id IS NULL'}
       ORDER BY dmp.meal_slot`,
      babyId ? [userId, planType, planDate, babyId] : [userId, planType, planDate]
    );
    return result.rows;
  }

  async upsertPlanSlot(data: {
    userId: number;
    babyId?: number;
    planType: 'baby' | 'mum';
    planDate: string;
    mealSlot: string;
    recipeId?: number;
    notes?: string;
  }): Promise<DailyMealPlan> {
    const result = await database.query(
      `INSERT INTO daily_meal_plans (user_id, baby_id, plan_type, plan_date, meal_slot, recipe_id, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       ON CONFLICT (user_id, COALESCE(baby_id, -1), plan_type, plan_date, meal_slot)
       DO UPDATE SET recipe_id = EXCLUDED.recipe_id, notes = EXCLUDED.notes
       RETURNING *`,
      [data.userId, data.babyId ?? null, data.planType, data.planDate, data.mealSlot, data.recipeId ?? null, data.notes ?? null]
    );
    return result.rows[0];
  }

  async generateBabyDailyPlan(userId: number, babyId: number, ageGroup: string, planDate: string): Promise<DailyMealPlan[]> {
    const plans: DailyMealPlan[] = [];
    for (const slot of BABY_MEAL_SLOTS) {
      const recipes = await this.findBabyByAgeGroup(ageGroup, slot);
      const recipe = recipes[Math.floor(Math.random() * recipes.length)];
      const plan = await this.upsertPlanSlot({
        userId, babyId, planType: 'baby', planDate,
        mealSlot: slot, recipeId: recipe?.id,
      });
      plans.push(plan);
    }
    return plans;
  }

  async generateMumDailyPlan(userId: number, planDate: string): Promise<DailyMealPlan[]> {
    const plans: DailyMealPlan[] = [];
    for (const slot of MUM_MEAL_SLOTS) {
      const recipes = await this.findMumRecipes(slot);
      const recipe = recipes[Math.floor(Math.random() * recipes.length)];
      const plan = await this.upsertPlanSlot({
        userId, planType: 'mum', planDate,
        mealSlot: slot, recipeId: recipe?.id,
      });
      plans.push(plan);
    }
    return plans;
  }

  // ─── Admin CRUD ────────────────────────────────────────────────────────────

  async create(data: {
    title: string; description?: string; target: 'baby' | 'mum';
    meal_slot?: string; baby_age_group?: string;
    age_range_min_months: number; age_range_max_months: number;
    recipe_type: string; ingredients?: string[]; instructions?: string[];
    nutrition_info?: any; prep_time_minutes?: number; cooking_time_minutes?: number;
    image_url?: string; language?: string;
  }): Promise<Recipe> {
    const result = await database.query(
      `INSERT INTO recipes
         (title, description, target, meal_slot, baby_age_group,
          age_range_min_months, age_range_max_months, recipe_type,
          ingredients, instructions, nutrition_info,
          prep_time_minutes, cooking_time_minutes, image_url, language)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15)
       RETURNING *`,
      [
        data.title, data.description ?? null, data.target, data.meal_slot ?? null, data.baby_age_group ?? null,
        data.age_range_min_months, data.age_range_max_months, data.recipe_type,
        data.ingredients ? JSON.stringify(data.ingredients) : null,
        data.instructions ? JSON.stringify(data.instructions) : null,
        data.nutrition_info ? JSON.stringify(data.nutrition_info) : null,
        data.prep_time_minutes ?? null, data.cooking_time_minutes ?? null, data.image_url ?? null,
        data.language ?? 'en',
      ]
    );
    return result.rows[0];
  }

  async update(id: number, data: Partial<{
    title: string; description: string; target: 'baby' | 'mum';
    meal_slot: string; baby_age_group: string;
    age_range_min_months: number; age_range_max_months: number;
    recipe_type: string; ingredients: string[]; instructions: string[];
    nutrition_info: any; prep_time_minutes: number; cooking_time_minutes: number;
    image_url: string;
  }>): Promise<Recipe | null> {
    const sets: string[] = [];
    const params: any[] = [];
    let i = 1;
    const fields: Record<string, any> = {
      title: data.title, description: data.description, target: data.target,
      meal_slot: data.meal_slot, baby_age_group: data.baby_age_group,
      age_range_min_months: data.age_range_min_months,
      age_range_max_months: data.age_range_max_months,
      recipe_type: data.recipe_type,
      ingredients: data.ingredients ? JSON.stringify(data.ingredients) : undefined,
      instructions: data.instructions ? JSON.stringify(data.instructions) : undefined,
      nutrition_info: data.nutrition_info !== undefined ? JSON.stringify(data.nutrition_info) : undefined,
      prep_time_minutes: data.prep_time_minutes, cooking_time_minutes: data.cooking_time_minutes,
      image_url: data.image_url,
    };
    for (const [key, val] of Object.entries(fields)) {
      if (val !== undefined) { sets.push(`${key} = $${i++}`); params.push(val); }
    }
    if (!sets.length) return this.findById(id);
    params.push(id);
    const result = await database.query(
      `UPDATE recipes SET ${sets.join(', ')}, updated_at = NOW() WHERE id = $${i} RETURNING *`,
      params
    );
    return result.rows[0] || null;
  }

  async delete(id: number): Promise<boolean> {
    const result = await database.query('DELETE FROM recipes WHERE id = $1', [id]);
    return (result.rowCount ?? 0) > 0;
  }
}
