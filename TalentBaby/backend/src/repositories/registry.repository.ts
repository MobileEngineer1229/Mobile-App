import { database } from '../config/database';

export interface Registry {
  id: number;
  user_id: number;
  baby_id?: number;
  registry_name: string;
  is_public: boolean;
  share_code?: string;
  created_at: Date;
  updated_at: Date;
}

export interface RegistryItem {
  id: number;
  registry_id: number;
  material_id?: number;
  custom_item_name?: string;
  custom_item_description?: string;
  quantity: number;
  purchased: boolean;
  purchased_by?: string;
  created_at: Date;
  updated_at: Date;
}

export class RegistryRepository {
  async findByUserId(userId: number): Promise<Registry[]> {
    const result = await database.query(
      'SELECT * FROM registries WHERE user_id = $1 ORDER BY created_at DESC',
      [userId]
    );
    return result.rows;
  }

  async findById(id: number, userId: number): Promise<Registry | null> {
    const result = await database.query(
      'SELECT * FROM registries WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    return result.rows[0] || null;
  }

  async findByShareCode(shareCode: string): Promise<Registry | null> {
    const result = await database.query(
      'SELECT * FROM registries WHERE share_code = $1 AND is_public = true',
      [shareCode]
    );
    return result.rows[0] || null;
  }

  async create(registryData: Partial<Registry>): Promise<Registry> {
    // Generate share code
    const shareCode = this.generateShareCode();

    const result = await database.query(
      `INSERT INTO registries (user_id, baby_id, registry_name, is_public, share_code)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [
        registryData.user_id,
        registryData.baby_id || null,
        registryData.registry_name,
        registryData.is_public || false,
        shareCode,
      ]
    );
    return result.rows[0];
  }

  async update(id: number, userId: number, updates: Partial<Registry>): Promise<Registry> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['registry_name', 'is_public'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof Registry]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      const registry = await this.findById(id, userId);
      if (!registry) throw new Error('Registry not found');
      return registry;
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(id, userId);

    const result = await database.query(
      `UPDATE registries SET ${fields.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async delete(id: number, userId: number): Promise<void> {
    const result = await database.query(
      'DELETE FROM registries WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    if (result.rowCount === 0) {
      throw new Error('Registry not found');
    }
  }

  // Registry Items
  async getRegistryItems(registryId: number): Promise<RegistryItem[]> {
    const result = await database.query(
      'SELECT * FROM registry_items WHERE registry_id = $1 ORDER BY created_at DESC',
      [registryId]
    );
    return result.rows;
  }

  async addRegistryItem(itemData: Partial<RegistryItem>): Promise<RegistryItem> {
    const result = await database.query(
      `INSERT INTO registry_items 
       (registry_id, material_id, custom_item_name, custom_item_description, quantity)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [
        itemData.registry_id,
        itemData.material_id || null,
        itemData.custom_item_name || null,
        itemData.custom_item_description || null,
        itemData.quantity || 1,
      ]
    );
    return result.rows[0];
  }

  async updateRegistryItem(itemId: number, updates: Partial<RegistryItem>): Promise<RegistryItem> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    const allowedFields = ['quantity', 'purchased', 'purchased_by'];

    Object.keys(updates).forEach((key) => {
      if (allowedFields.includes(key)) {
        fields.push(`${key} = $${paramIndex}`);
        values.push(updates[key as keyof RegistryItem]);
        paramIndex++;
      }
    });

    if (fields.length === 0) {
      throw new Error('No valid fields to update');
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(itemId);

    const result = await database.query(
      `UPDATE registry_items SET ${fields.join(', ')} WHERE id = $${paramIndex} RETURNING *`,
      values
    );

    return result.rows[0];
  }

  async deleteRegistryItem(itemId: number): Promise<void> {
    const result = await database.query('DELETE FROM registry_items WHERE id = $1', [itemId]);
    if (result.rowCount === 0) {
      throw new Error('Registry item not found');
    }
  }

  private generateShareCode(): string {
    return Math.random().toString(36).substring(2, 10).toUpperCase();
  }
}
