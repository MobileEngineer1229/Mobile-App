import { database } from '../config/database';

export interface Food {
  id: number;
  name: string;
  name_ko: string;
  category: string;
  calories_per_100g: number;
  protein_per_100g: number;
  fat_per_100g: number;
  carbs_per_100g: number;
  fiber_per_100g: number;
  sodium_per_100g: number;
  calcium_per_100g: number;
  iron_per_100g: number;
  is_baby_food: boolean;
  min_age_months: number | null;
  allergens: string[];
}

export interface IntakeLog {
  id: number;
  baby_id: number | null;
  user_id: number;
  food_id: number;
  log_date: string;
  meal_type: string;
  amount_g: number;
  calories: number;
  notes?: string;
  created_at: string;
  // joined
  food_name?: string;
  food_name_ko?: string;
  food_category?: string;
}

export interface NutrientSummary {
  total_calories: number;
  total_protein: number;
  total_fat: number;
  total_carbs: number;
  total_fiber: number;
  total_calcium: number;
  total_iron: number;
}

export interface MealBreakdown extends NutrientSummary {
  meal_type: string;
  item_count: number;
}

export class CalorieRepository {
  // ─── Food DB ────────────────────────────────────────────────────────────

  async searchFoods(search?: string, isBabyFood?: boolean, category?: string, minAgeMonths?: number) {
    const conditions: string[] = ['1=1'];
    const params: unknown[] = [];
    let i = 1;

    if (search) {
      conditions.push(`(name ILIKE $${i} OR name_ko ILIKE $${i})`);
      params.push(`%${search}%`);
      i++;
    }
    if (isBabyFood !== undefined) {
      conditions.push(`is_baby_food = $${i++}`);
      params.push(isBabyFood);
    }
    if (category) {
      conditions.push(`category = $${i++}`);
      params.push(category);
    }
    if (minAgeMonths !== undefined) {
      conditions.push(`(min_age_months IS NULL OR min_age_months <= $${i++})`);
      params.push(minAgeMonths);
    }

    const rows = await database.query(
      `SELECT * FROM foods WHERE ${conditions.join(' AND ')} ORDER BY is_baby_food DESC, name LIMIT 100`,
      params
    );
    return rows.rows as Food[];
  }

  async findFoodById(id: number) {
    const row = await database.query(`SELECT * FROM foods WHERE id = $1`, [id]);
    return row.rows[0] as Food | undefined;
  }

  // ─── Intake Logs ─────────────────────────────────────────────────────────

  async getLogs(babyId: number | null, userId: number, date: string) {
    const rows = await database.query(
      `SELECT il.*, f.name AS food_name, f.name_ko AS food_name_ko, f.category AS food_category,
              f.calories_per_100g, f.protein_per_100g, f.fat_per_100g,
              f.carbs_per_100g, f.fiber_per_100g, f.calcium_per_100g, f.iron_per_100g
       FROM food_intake_logs il
       JOIN foods f ON f.id = il.food_id
       WHERE il.user_id = $1
         AND ($2::integer IS NULL OR il.baby_id = $2)
         AND il.log_date = $3
       ORDER BY il.meal_type, il.created_at`,
      [userId, babyId, date]
    );
    return rows.rows as IntakeLog[];
  }

  async addLog(data: {
    baby_id?: number;
    user_id: number;
    food_id: number;
    log_date: string;
    meal_type: string;
    amount_g: number;
    notes?: string;
  }) {
    // Calculate calories from food table
    const food = await this.findFoodById(data.food_id);
    if (!food) throw { status: 404, message: 'Food not found.' };

    const calories = parseFloat(((data.amount_g / 100) * food.calories_per_100g).toFixed(2));

    const row = await database.query(
      `INSERT INTO food_intake_logs (baby_id, user_id, food_id, log_date, meal_type, amount_g, calories, notes)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
      [data.baby_id ?? null, data.user_id, data.food_id, data.log_date, data.meal_type, data.amount_g, calories, data.notes ?? null]
    );
    return row.rows[0] as IntakeLog;
  }

  async deleteLog(logId: number, userId: number) {
    const row = await database.query(
      `DELETE FROM food_intake_logs WHERE id = $1 AND user_id = $2 RETURNING id`,
      [logId, userId]
    );
    return row.rowCount && row.rowCount > 0;
  }

  // ─── Daily Analysis ───────────────────────────────────────────────────────

  async getDailySummary(babyId: number | null, userId: number, date: string): Promise<NutrientSummary> {
    const row = await database.query(
      `SELECT
         COALESCE(SUM(il.amount_g / 100.0 * f.calories_per_100g), 0) AS total_calories,
         COALESCE(SUM(il.amount_g / 100.0 * f.protein_per_100g),  0) AS total_protein,
         COALESCE(SUM(il.amount_g / 100.0 * f.fat_per_100g),      0) AS total_fat,
         COALESCE(SUM(il.amount_g / 100.0 * f.carbs_per_100g),    0) AS total_carbs,
         COALESCE(SUM(il.amount_g / 100.0 * f.fiber_per_100g),    0) AS total_fiber,
         COALESCE(SUM(il.amount_g / 100.0 * f.calcium_per_100g),  0) AS total_calcium,
         COALESCE(SUM(il.amount_g / 100.0 * f.iron_per_100g),     0) AS total_iron
       FROM food_intake_logs il
       JOIN foods f ON f.id = il.food_id
       WHERE il.user_id = $1
         AND ($2::integer IS NULL OR il.baby_id = $2)
         AND il.log_date = $3`,
      [userId, babyId, date]
    );
    return row.rows[0] as NutrientSummary;
  }

  async getMealBreakdown(babyId: number | null, userId: number, date: string): Promise<MealBreakdown[]> {
    const rows = await database.query(
      `SELECT
         il.meal_type,
         COUNT(*) AS item_count,
         COALESCE(SUM(il.amount_g / 100.0 * f.calories_per_100g), 0) AS total_calories,
         COALESCE(SUM(il.amount_g / 100.0 * f.protein_per_100g),  0) AS total_protein,
         COALESCE(SUM(il.amount_g / 100.0 * f.fat_per_100g),      0) AS total_fat,
         COALESCE(SUM(il.amount_g / 100.0 * f.carbs_per_100g),    0) AS total_carbs,
         COALESCE(SUM(il.amount_g / 100.0 * f.fiber_per_100g),    0) AS total_fiber,
         COALESCE(SUM(il.amount_g / 100.0 * f.calcium_per_100g),  0) AS total_calcium,
         COALESCE(SUM(il.amount_g / 100.0 * f.iron_per_100g),     0) AS total_iron
       FROM food_intake_logs il
       JOIN foods f ON f.id = il.food_id
       WHERE il.user_id = $1
         AND ($2::integer IS NULL OR il.baby_id = $2)
         AND il.log_date = $3
       GROUP BY il.meal_type
       ORDER BY il.meal_type`,
      [userId, babyId, date]
    );
    return rows.rows as MealBreakdown[];
  }

  // ─── Weekly Trend ─────────────────────────────────────────────────────────

  async getWeeklyTrend(babyId: number | null, userId: number, startDate: string, endDate: string) {
    const rows = await database.query(
      `SELECT
         il.log_date::text AS date,
         COALESCE(SUM(il.amount_g / 100.0 * f.calories_per_100g), 0) AS total_calories,
         COALESCE(SUM(il.amount_g / 100.0 * f.protein_per_100g),  0) AS total_protein,
         COALESCE(SUM(il.amount_g / 100.0 * f.fat_per_100g),      0) AS total_fat,
         COALESCE(SUM(il.amount_g / 100.0 * f.carbs_per_100g),    0) AS total_carbs
       FROM food_intake_logs il
       JOIN foods f ON f.id = il.food_id
       WHERE il.user_id = $1
         AND ($2::integer IS NULL OR il.baby_id = $2)
         AND il.log_date BETWEEN $3 AND $4
       GROUP BY il.log_date
       ORDER BY il.log_date`,
      [userId, babyId, startDate, endDate]
    );
    return rows.rows;
  }

  // ─── Recommendation ───────────────────────────────────────────────────────

  async getRecommendation(ageMonths: number, gender: string = 'all') {
    const row = await database.query(
      `SELECT * FROM calorie_recommendations
       WHERE age_min_months <= $1 AND age_max_months >= $1
         AND (gender = $2 OR gender = 'all')
       ORDER BY CASE WHEN gender = $2 THEN 0 ELSE 1 END
       LIMIT 1`,
      [ageMonths, gender]
    );
    return row.rows[0] || null;
  }

  // ─── Meal Plan Recommendations ────────────────────────────────────────────

  async getMealPlan(ageMonths: number) {
    // Fetch all meal templates and food list for the given age in months
    const templates = await database.query(
      `SELECT t.id, t.meal_type, t.title, t.description,
              t.total_kcal_approx, t.stage_label, t.sort_order,
              t.age_min_months, t.age_max_months
       FROM meal_plan_templates t
       WHERE t.age_min_months <= $1 AND t.age_max_months >= $1
       ORDER BY t.sort_order`,
      [ageMonths]
    );

    if (!templates.rows.length) return null;

    // Fetch food details for each template
    const templateIds = templates.rows.map((t: { id: number }) => t.id);
    const foods = await database.query(
      `SELECT mpf.template_id, mpf.recommended_g, mpf.unit, mpf.notes AS serving_note,
              f.id AS food_id, f.name, f.name_ko, f.category,
              f.calories_per_100g, f.protein_per_100g, f.fat_per_100g,
              f.carbs_per_100g, f.fiber_per_100g, f.calcium_per_100g, f.iron_per_100g,
              f.allergens, f.min_age_months,
              -- Nutrition calculated based on recommended serving size
              ROUND((mpf.recommended_g / 100.0 * f.calories_per_100g)::numeric, 1) AS calories,
              ROUND((mpf.recommended_g / 100.0 * f.protein_per_100g)::numeric,  1) AS protein,
              ROUND((mpf.recommended_g / 100.0 * f.fat_per_100g)::numeric,      1) AS fat,
              ROUND((mpf.recommended_g / 100.0 * f.carbs_per_100g)::numeric,    1) AS carbs,
              ROUND((mpf.recommended_g / 100.0 * f.calcium_per_100g)::numeric,  1) AS calcium,
              ROUND((mpf.recommended_g / 100.0 * f.iron_per_100g)::numeric,     3) AS iron
       FROM meal_plan_foods mpf
       JOIN foods f ON f.id = mpf.food_id
       WHERE mpf.template_id = ANY($1::int[])
       ORDER BY mpf.id`,
      [templateIds]
    );

    // Merge food list into each template
    const foodsByTemplate = new Map<number, unknown[]>();
    foods.rows.forEach((row: { template_id: number }) => {
      if (!foodsByTemplate.has(row.template_id)) foodsByTemplate.set(row.template_id, []);
      foodsByTemplate.get(row.template_id)!.push(row);
    });

    return templates.rows.map((t: {
      id: number; meal_type: string; title: string; description: string;
      total_kcal_approx: number; stage_label: string; sort_order: number;
      age_min_months: number; age_max_months: number;
    }) => ({
      meal_type:          t.meal_type,
      title:              t.title,
      description:        t.description,
      total_kcal_approx:  t.total_kcal_approx,
      stage_label:        t.stage_label,
      foods:              foodsByTemplate.get(t.id) ?? [],
    }));
  }
}
