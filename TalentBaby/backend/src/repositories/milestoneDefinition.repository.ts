import { database } from '../config/database';

export type MilestoneTypeValue = 'physical' | 'cognitive' | 'communication' | 'social_emotional';

export interface MilestoneDefinition {
  id: number;
  month: number;
  milestone_type: MilestoneTypeValue;
  title: string;
  description: string;
  question: string | null;
  related_activity: string | null;
  display_order: number;
  doctor_verified: boolean;
  created_at: Date;
  updated_at: Date;
}

export interface CreateMilestoneDefinitionInput {
  month: number;
  milestone_type: string;
  title: string;
  description: string;
  question?: string | null;
  related_activity?: string | null;
  display_order?: number;
  doctor_verified?: boolean;
}

export class MilestoneDefinitionRepository {
  async findByMonth(month: number): Promise<MilestoneDefinition[]> {
    const result = await database.query(
      `SELECT * FROM milestone_definitions
       WHERE month = $1
       ORDER BY milestone_type, display_order`,
      [month]
    );
    return result.rows;
  }

  async findAll(month?: number, milestoneType?: string): Promise<MilestoneDefinition[]> {
    const conditions: string[] = [];
    const params: unknown[] = [];

    if (month !== undefined) {
      params.push(month);
      conditions.push(`month = $${params.length}`);
    }
    if (milestoneType) {
      params.push(milestoneType);
      conditions.push(`milestone_type = $${params.length}`);
    }

    const where = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
    const result = await database.query(
      `SELECT * FROM milestone_definitions ${where} ORDER BY month, milestone_type, display_order`,
      params
    );
    return result.rows;
  }

  async findById(id: number): Promise<MilestoneDefinition | null> {
    const result = await database.query(
      'SELECT * FROM milestone_definitions WHERE id = $1',
      [id]
    );
    return result.rows[0] ?? null;
  }

  async create(input: CreateMilestoneDefinitionInput): Promise<MilestoneDefinition> {
    const result = await database.query(
      `INSERT INTO milestone_definitions
         (month, milestone_type, title, description, question, related_activity, display_order, doctor_verified)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        input.month,
        input.milestone_type,
        input.title,
        input.description,
        input.question ?? null,
        input.related_activity ?? null,
        input.display_order ?? 0,
        input.doctor_verified ?? false,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, input: Partial<CreateMilestoneDefinitionInput>): Promise<MilestoneDefinition | null> {
    const result = await database.query(
      `UPDATE milestone_definitions
       SET month = $1, milestone_type = $2, title = $3, description = $4,
           question = $5, related_activity = $6, display_order = $7,
           doctor_verified = $8, updated_at = CURRENT_TIMESTAMP
       WHERE id = $9
       RETURNING *`,
      [
        input.month,
        input.milestone_type,
        input.title,
        input.description,
        input.question ?? null,
        input.related_activity ?? null,
        input.display_order ?? 0,
        input.doctor_verified ?? false,
        id,
      ]
    );
    return result.rows[0] ?? null;
  }

  async delete(id: number): Promise<void> {
    await database.query('DELETE FROM milestone_definitions WHERE id = $1', [id]);
  }
}
