import { Pool } from 'pg';
import { DeviceTypeTemplate } from '../models/device-type';

/**
 * Device type repository for database operations
 */
export class DeviceTypeRepository {
  constructor(private pool: Pool) {}

  /**
   * Map database row to DeviceTypeTemplate model
   */
  private mapRowToTemplate(row: any): DeviceTypeTemplate {
    return {
      id: row.template_id,
      name: row.name,
      category: row.category,
      description: row.description || undefined,
      icon: row.icon || undefined,
      defaultType: row.default_type,
      metadata: row.metadata || undefined,
    };
  }

  /**
   * Find all device types, optionally filtered by category
   */
  async findAll(category?: string): Promise<DeviceTypeTemplate[]> {
    let query = `
      SELECT 
        template_id,
        name,
        category,
        description,
        icon,
        default_type,
        metadata
      FROM device_types
      WHERE is_active = true
    `;
    const params: unknown[] = [];

    if (category) {
      query += ' AND category = $1';
      params.push(category);
    }

    query += ' ORDER BY category, display_order, name';

    const result = await this.pool.query(query, params);
    return result.rows.map((row) => this.mapRowToTemplate(row));
  }

  /**
   * Get all unique categories
   */
  async getCategories(): Promise<string[]> {
    const result = await this.pool.query(
      'SELECT DISTINCT category FROM device_types WHERE is_active = true ORDER BY category'
    );
    return result.rows.map((row) => row.category);
  }

  /**
   * Find device type by template ID
   */
  async findByTemplateId(templateId: string): Promise<DeviceTypeTemplate | null> {
    const result = await this.pool.query(
      `SELECT 
        template_id,
        name,
        category,
        description,
        icon,
        default_type,
        metadata
      FROM device_types
      WHERE template_id = $1 AND is_active = true`,
      [templateId]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return this.mapRowToTemplate(result.rows[0]);
  }
}
