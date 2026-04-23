import { database } from '../config/database';

export interface Checklist {
  id: number;
  user_id: number;
  checklist_type: string; // 'hospital_bag', 'birth_plan', 'custom'
  title: string;
  description?: string;
  created_at: Date;
  updated_at: Date;
}

export interface ChecklistItem {
  id: number;
  checklist_id: number;
  item_text: string;
  is_checked: boolean;
  item_order: number;
  created_at: Date;
  updated_at: Date;
}

export class ChecklistRepository {
  async findByUserId(userId: number, checklistType?: string): Promise<Checklist[]> {
    let query = 'SELECT * FROM checklists WHERE user_id = $1';
    const params: any[] = [userId];

    if (checklistType) {
      query += ' AND checklist_type = $2';
      params.push(checklistType);
    }

    query += ' ORDER BY created_at DESC';

    const result = await database.query(query, params);
    return result.rows;
  }

  async findById(id: number, userId: number): Promise<Checklist | null> {
    const result = await database.query(
      'SELECT * FROM checklists WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    return result.rows[0] || null;
  }

  async create(checklistData: Partial<Checklist>): Promise<Checklist> {
    const result = await database.query(
      `INSERT INTO checklists (user_id, checklist_type, title, description)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [
        checklistData.user_id,
        checklistData.checklist_type,
        checklistData.title,
        checklistData.description || null,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, userId: number, updates: Partial<Checklist>): Promise<Checklist> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['title', 'description'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Checklist]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const checklist = await this.findById(id, userId);
      if (!checklist) throw new Error('Checklist not found');
      return checklist;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id, userId);

    const result = await database.query(
      `UPDATE checklists SET ${fields.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number, userId: number): Promise<void> {
    const result = await database.query(
      'DELETE FROM checklists WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    if (result.rowCount === 0) {
      throw new Error('Checklist not found');
    }
  }

  // Checklist Items
  async getItemsByChecklist(checklistId: number): Promise<ChecklistItem[]> {
    const result = await database.query(
      'SELECT * FROM checklist_items WHERE checklist_id = $1 ORDER BY item_order ASC',
      [checklistId]
    );
    return result.rows;
  }

  async addItem(itemData: Partial<ChecklistItem>): Promise<ChecklistItem> {
    const result = await database.query(
      `INSERT INTO checklist_items (checklist_id, item_text, item_order)
       VALUES ($1, $2, $3)
       RETURNING *`,
      [
        itemData.checklist_id,
        itemData.item_text,
        itemData.item_order || 0,
      ]
    );
    return result.rows[0];
  }

  async updateItem(itemId: number, updates: Partial<ChecklistItem>): Promise<ChecklistItem> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['item_text', 'is_checked', 'item_order'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof ChecklistItem]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      throw new Error('No valid fields to update');
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(itemId);

    const result = await database.query(
      `UPDATE checklist_items SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async deleteItem(itemId: number): Promise<void> {
    const result = await database.query('DELETE FROM checklist_items WHERE id = $1', [itemId]);
    if (result.rowCount === 0) {
      throw new Error('Checklist item not found');
    }
  }
}
