import { database } from '../config/database';

export interface AssessmentQuestion {
  id: number;
  talent_category_id: number;
  question_text: string;
  question_type: string; // 'multiple_choice', 'scale', 'yes_no', 'text'
  options: any; // JSONB
  age_range_min_months?: number;
  age_range_max_months?: number;
  weight: number;
  created_at: Date;
}

export interface AssessmentAnswer {
  id: number;
  assessment_id: number;
  question_id: number;
  answer_value: string;
  created_at: Date;
}

export class AssessmentRepository {
  async getQuestionsByTalentCategory(
    talentCategoryId: number,
    ageInMonths: number
  ): Promise<AssessmentQuestion[]> {
    const result = await database.query(
      `SELECT * FROM assessment_questions 
       WHERE talent_category_id = $1 
       AND (age_range_min_months IS NULL OR age_range_min_months <= $2)
       AND (age_range_max_months IS NULL OR age_range_max_months >= $2)
       ORDER BY id`,
      [talentCategoryId, ageInMonths]
    );
    return result.rows;
  }

  async saveAnswers(assessmentId: number, answers: { question_id: number; answer_value: string }[]): Promise<void> {
    for (const answer of answers) {
      await database.query(
        `INSERT INTO assessment_answers (assessment_id, question_id, answer_value)
         VALUES ($1, $2, $3)
         ON CONFLICT DO NOTHING`,
        [assessmentId, answer.question_id, answer.answer_value]
      );
    }
  }

  async getAnswersByAssessment(assessmentId: number): Promise<AssessmentAnswer[]> {
    const result = await database.query(
      'SELECT * FROM assessment_answers WHERE assessment_id = $1',
      [assessmentId]
    );
    return result.rows;
  }

  async calculateScore(assessmentId: number): Promise<number> {
    // Get assessment and answers
    const assessmentResult = await database.query(
      `SELECT ta.*, tc.id as talent_category_id 
       FROM talent_assessments ta
       JOIN talent_categories tc ON ta.talent_category_id = tc.id
       WHERE ta.id = $1`,
      [assessmentId]
    );

    if (assessmentResult.rows.length === 0) {
      throw new Error('Assessment not found');
    }

    const assessment = assessmentResult.rows[0];
    const answers = await this.getAnswersByAssessment(assessmentId);

    // Get questions with weights
    const questionsResult = await database.query(
      `SELECT id, weight, question_type, options 
       FROM assessment_questions 
       WHERE talent_category_id = $1`,
      [assessment.talent_category_id]
    );

    let totalScore = 0;
    let totalWeight = 0;

    for (const question of questionsResult.rows) {
      const answer = answers.find((a) => a.question_id === question.id);
      if (answer) {
        let questionScore = 0;

        // Calculate score based on question type
        if (question.question_type === 'scale') {
          // Scale 1-5 or 1-10
          questionScore = parseInt(answer.answer_value) || 0;
        } else if (question.question_type === 'yes_no') {
          questionScore = answer.answer_value === 'yes' ? 5 : 0;
        } else if (question.question_type === 'multiple_choice') {
          // Each option has a score value
          const options = question.options || {};
          questionScore = options[answer.answer_value]?.score || 0;
        }

        totalScore += questionScore * question.weight;
        totalWeight += question.weight;
      }
    }

    // Calculate final score (0-100)
    const finalScore = totalWeight > 0 ? Math.round((totalScore / (totalWeight * 5)) * 100) : 0;
    return Math.min(100, Math.max(0, finalScore));
  }
}
