import { Pool } from 'pg';
import { database } from '../config/database';

export class GuideRepository {
  private pool: Pool;

  constructor() {
    this.pool = database.getPool();
  }

  // Sleep Guide Methods
  async getSleepGuideByAge(ageMonths: number) {
    const query = `
      SELECT * FROM sleep_guides
      WHERE age_min_months <= $1
      AND (age_max_months IS NULL OR age_max_months >= $1)
      ORDER BY age_min_months DESC
      LIMIT 1
    `;
    const result = await this.pool.query(query, [ageMonths]);
    return result.rows[0] || null;
  }

  async getAllSleepGuides() {
    const query = 'SELECT * FROM sleep_guides ORDER BY age_min_months ASC';
    const result = await this.pool.query(query);
    return result.rows;
  }

  async getSleepTipsByAge(ageMonths: number) {
    const guide = await this.getSleepGuideByAge(ageMonths);
    return guide?.tips || [];
  }

  async getSleepScheduleRecommendations(ageMonths: number) {
    const guide = await this.getSleepGuideByAge(ageMonths);
    return guide?.schedule_recommendations || null;
  }

  // Feeding Guide Methods
  async getFeedingGuideByAge(ageMonths: number) {
    const query = `
      SELECT * FROM feeding_guides
      WHERE age_min_months <= $1
      AND (age_max_months IS NULL OR age_max_months >= $1)
      ORDER BY age_min_months DESC
      LIMIT 1
    `;
    const result = await this.pool.query(query, [ageMonths]);
    return result.rows[0] || null;
  }

  async getAllFeedingGuides() {
    const query = 'SELECT * FROM feeding_guides ORDER BY age_min_months ASC';
    const result = await this.pool.query(query);
    return result.rows;
  }

  async getFeedingTipsByAge(ageMonths: number) {
    const guide = await this.getFeedingGuideByAge(ageMonths);
    return guide?.tips || [];
  }

  async getFeedingScheduleRecommendations(ageMonths: number) {
    const guide = await this.getFeedingGuideByAge(ageMonths);
    return guide?.schedule_recommendations || null;
  }

  async getFoodIntroductionTimeline(ageMonths: number) {
    const guide = await this.getFeedingGuideByAge(ageMonths);
    return guide?.food_introduction_timeline || null;
  }

  async getFeedingAdequacyQuestions(ageMonths: number) {
    const guide = await this.getFeedingGuideByAge(ageMonths);
    return guide?.adequacy_assessment_questions || null;
  }
}
