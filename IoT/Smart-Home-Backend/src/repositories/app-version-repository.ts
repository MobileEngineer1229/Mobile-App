import { Pool } from 'pg';
import { AppVersion, CreateAppVersionInput, UpdateAppVersionInput } from '../models/app-version';
import { NotFoundError } from '../utils/errors';

/**
 * App Version repository for database operations
 */
export class AppVersionRepository {
  constructor(private pool: Pool) {}

  /**
   * Map database row to AppVersion model
   */
  private mapRowToAppVersion(row: any): AppVersion {
    return {
      id: row.id,
      platform: row.platform,
      versionName: row.version_name,
      versionCode: row.version_code,
      minimumRequiredVersion: row.minimum_required_version,
      updateUrl: row.update_url || undefined,
      forceUpdate: row.force_update,
      releaseNotes: row.release_notes || undefined,
      isActive: row.is_active,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  /**
   * Compare version strings (e.g., "1.0.0" vs "1.0.1")
   * Returns: -1 if v1 < v2, 0 if v1 == v2, 1 if v1 > v2
   */
  private compareVersions(v1: string, v2: string): number {
    const parts1 = v1.split('.').map(Number);
    const parts2 = v2.split('.').map(Number);
    const maxLength = Math.max(parts1.length, parts2.length);

    for (let i = 0; i < maxLength; i++) {
      const part1 = parts1[i] || 0;
      const part2 = parts2[i] || 0;
      if (part1 < part2) return -1;
      if (part1 > part2) return 1;
    }
    return 0;
  }

  /**
   * Get active version for platform
   */
  async getActiveVersion(platform: 'android' | 'ios'): Promise<AppVersion | null> {
    const result = await this.pool.query(
      `SELECT * FROM app_versions 
       WHERE platform = $1 AND is_active = true 
       ORDER BY version_code DESC, created_at DESC 
       LIMIT 1`,
      [platform]
    );

    if (result.rows.length === 0) {
      return null;
    }
    return this.mapRowToAppVersion(result.rows[0]);
  }

  /**
   * Check version and return update status
   */
  async checkVersion(
    platform: 'android' | 'ios',
    clientVersionName: string,
    clientVersionCode: number
  ): Promise<{
    currentVersion: AppVersion | null;
    updateAvailable: boolean;
    updateRequired: boolean;
    message?: string;
  }> {
    const activeVersion = await this.getActiveVersion(platform);

    if (!activeVersion) {
      return {
        currentVersion: null,
        updateAvailable: false,
        updateRequired: false,
        message: 'No version information available',
      };
    }

    // Compare version codes (integer comparison)
    const versionCodeDiff = clientVersionCode - activeVersion.versionCode;

    // Compare version names (semantic version comparison)
    const versionNameDiff = this.compareVersions(clientVersionName, activeVersion.versionName);

    // Check if update is available (client version is older)
    const updateAvailable = versionCodeDiff < 0 || (versionCodeDiff === 0 && versionNameDiff < 0);

    // Check if update is required (client version is below minimum required)
    const minimumVersionDiff = this.compareVersions(clientVersionName, activeVersion.minimumRequiredVersion);
    const updateRequired = minimumVersionDiff < 0 || activeVersion.forceUpdate;

    let message: string | undefined;
    if (updateRequired) {
      message = 'A critical update is required to continue using the app.';
    } else if (updateAvailable) {
      message = 'A new version is available. Update recommended for better experience.';
    }

    return {
      currentVersion: activeVersion,
      updateAvailable,
      updateRequired,
      message,
    };
  }

  /**
   * Get all versions for platform
   */
  async getAllVersions(platform: 'android' | 'ios'): Promise<AppVersion[]> {
    const result = await this.pool.query(
      `SELECT * FROM app_versions 
       WHERE platform = $1 
       ORDER BY version_code DESC, created_at DESC`,
      [platform]
    );

    return result.rows.map((row) => this.mapRowToAppVersion(row));
  }

  /**
   * Get version by ID
   */
  async findById(id: number): Promise<AppVersion | null> {
    const result = await this.pool.query('SELECT * FROM app_versions WHERE id = $1', [id]);

    if (result.rows.length === 0) {
      return null;
    }
    return this.mapRowToAppVersion(result.rows[0]);
  }

  /**
   * Create new version
   */
  async create(input: CreateAppVersionInput): Promise<AppVersion> {
    // Deactivate all other versions for this platform
    if (input.isActive !== false) {
      await this.pool.query(
        'UPDATE app_versions SET is_active = false WHERE platform = $1',
        [input.platform]
      );
    }

    const result = await this.pool.query(
      `INSERT INTO app_versions 
       (platform, version_name, version_code, minimum_required_version, update_url, force_update, release_notes, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
       RETURNING *`,
      [
        input.platform,
        input.versionName,
        input.versionCode,
        input.minimumRequiredVersion,
        input.updateUrl || null,
        input.forceUpdate || false,
        input.releaseNotes || null,
        input.isActive !== false,
      ]
    );

    return this.mapRowToAppVersion(result.rows[0]);
  }

  /**
   * Update version
   */
  async update(id: number, input: UpdateAppVersionInput): Promise<AppVersion> {
    const existing = await this.findById(id);
    if (!existing) {
      throw new NotFoundError('App version not found');
    }

    // If setting this version as active, deactivate others
    if (input.isActive === true) {
      await this.pool.query(
        'UPDATE app_versions SET is_active = false WHERE platform = $1 AND id != $2',
        [existing.platform, id]
      );
    }

    const updates: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    if (input.versionName !== undefined) {
      updates.push(`version_name = $${paramIndex++}`);
      values.push(input.versionName);
    }
    if (input.versionCode !== undefined) {
      updates.push(`version_code = $${paramIndex++}`);
      values.push(input.versionCode);
    }
    if (input.minimumRequiredVersion !== undefined) {
      updates.push(`minimum_required_version = $${paramIndex++}`);
      values.push(input.minimumRequiredVersion);
    }
    if (input.updateUrl !== undefined) {
      updates.push(`update_url = $${paramIndex++}`);
      values.push(input.updateUrl || null);
    }
    if (input.forceUpdate !== undefined) {
      updates.push(`force_update = $${paramIndex++}`);
      values.push(input.forceUpdate);
    }
    if (input.releaseNotes !== undefined) {
      updates.push(`release_notes = $${paramIndex++}`);
      values.push(input.releaseNotes || null);
    }
    if (input.isActive !== undefined) {
      updates.push(`is_active = $${paramIndex++}`);
      values.push(input.isActive);
    }

    if (updates.length === 0) {
      return existing;
    }

    updates.push(`updated_at = CURRENT_TIMESTAMP`);
    values.push(id);

    const result = await this.pool.query(
      `UPDATE app_versions 
       SET ${updates.join(', ')} 
       WHERE id = $${paramIndex} 
       RETURNING *`,
      values
    );

    return this.mapRowToAppVersion(result.rows[0]);
  }

  /**
   * Delete version
   */
  async delete(id: number): Promise<void> {
    const result = await this.pool.query('DELETE FROM app_versions WHERE id = $1', [id]);
    if (result.rowCount === 0) {
      throw new NotFoundError('App version not found');
    }
  }
}
