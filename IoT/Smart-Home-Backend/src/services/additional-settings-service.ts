import { AdditionalSettingsRepository } from '../repositories/additional-settings-repository';
import logger from '../utils/logger';

export class AdditionalSettingsService {
  constructor(private additionalSettingsRepository: AdditionalSettingsRepository) {}

  /**
   * Get all additional settings
   */
  async getAdditionalSettings(userId: number) {
    const settings = await this.additionalSettingsRepository.findByUserId(userId);
    logger.infoWithEmoji('⚙️', `Retrieved additional settings for user ${userId}`, 'SETTINGS');
    return settings;
  }

  /**
   * Update a setting
   */
  async updateSetting(
    userId: number,
    settingKey: string,
    settingValue: string,
    metadata?: Record<string, any>
  ) {
    const setting = await this.additionalSettingsRepository.upsertSetting(
      userId,
      settingKey,
      settingValue,
      metadata
    );
    logger.infoWithEmoji('⚙️', `Updated setting: ${settingKey} for user ${userId}`, 'SETTINGS');
    return setting;
  }

  /**
   * Bulk update settings
   */
  async bulkUpdateSettings(
    userId: number,
    settings: Array<{ key: string; value: string; metadata?: Record<string, any> }>
  ) {
    const updated = await this.additionalSettingsRepository.bulkUpdateSettings(userId, settings);
    logger.infoWithEmoji('⚙️', `Bulk updated ${settings.length} settings for user ${userId}`, 'SETTINGS');
    return updated;
  }
}

