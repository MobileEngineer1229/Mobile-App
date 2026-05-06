import { database } from '../config/database';

export type MilestoneStatus = 'yes' | 'no' | 'almost';

export interface BabyMilestone {
  id: number;
  baby_id: number;
  milestone_definition_id: number;
  status: MilestoneStatus;
  achieved_date: string | null;
  notes: string | null;
  created_at: Date;
  updated_at: Date;
}

export interface BabyMilestoneWithDefinition extends BabyMilestone {
  month: number;
  milestone_type: string;
  title: string;
  description: string;
  question: string | null;
  related_activity: string | null;
  display_order: number;
}

export interface BabyMilestoneStatusRow {
  milestone_definition_id: number;
  status: MilestoneStatus;
  achieved_date: string | null;
  notes: string | null;
}

export class BabyMilestoneRepository {
  async findByBabyId(babyId: number, month?: number): Promise<BabyMilestoneWithDefinition[]> {
    const params: unknown[] = [babyId];
    let monthFilter = '';
    if (month !== undefined) {
      params.push(month);
      monthFilter = `AND md.month = $${params.length}`;
    }

    const result = await database.query(
      `SELECT bm.*, md.month, md.milestone_type, md.title, md.description,
              md.question, md.related_activity, md.display_order
       FROM baby_milestones bm
       JOIN milestone_definitions md ON md.id = bm.milestone_definition_id
       WHERE bm.baby_id = $1 ${monthFilter}
       ORDER BY md.month, md.milestone_type, md.display_order`,
      params
    );
    return result.rows;
  }

  async findStatusesByBabyAndMonth(babyId: number, month: number): Promise<BabyMilestoneStatusRow[]> {
    const result = await database.query(
      `SELECT bm.milestone_definition_id, bm.status, bm.achieved_date, bm.notes
       FROM baby_milestones bm
       JOIN milestone_definitions md ON md.id = bm.milestone_definition_id
       WHERE bm.baby_id = $1 AND md.month = $2`,
      [babyId, month]
    );
    return result.rows;
  }

  async findBabyBirthDate(babyId: number): Promise<string | null> {
    const result = await database.query(
      'SELECT birth_date FROM babies WHERE id = $1',
      [babyId]
    );
    return result.rows[0]?.birth_date ?? null;
  }

  async upsert(
    babyId: number,
    milestoneDefinitionId: number,
    status: MilestoneStatus,
    achievedDate?: string | null,
    notes?: string | null
  ): Promise<BabyMilestone> {
    const result = await database.query(
      `INSERT INTO baby_milestones (baby_id, milestone_definition_id, status, achieved_date, notes)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (baby_id, milestone_definition_id)
       DO UPDATE SET
         status        = EXCLUDED.status,
         achieved_date = EXCLUDED.achieved_date,
         notes         = EXCLUDED.notes,
         updated_at    = CURRENT_TIMESTAMP
       RETURNING *`,
      [babyId, milestoneDefinitionId, status, achievedDate ?? null, notes ?? null]
    );
    return result.rows[0];
  }

  async delete(id: number): Promise<void> {
    await database.query('DELETE FROM baby_milestones WHERE id = $1', [id]);
  }
}
