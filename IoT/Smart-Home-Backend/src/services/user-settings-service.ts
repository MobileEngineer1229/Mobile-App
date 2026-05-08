import { UserSettingsRepository } from '../repositories/user-settings-repository';
import logger from '../utils/logger';

export class UserSettingsService {
  constructor(private userSettingsRepository: UserSettingsRepository) {}

  /**
   * Get notification preferences
   */
  async getNotificationPreferences(userId: number) {
    const preferences = await this.userSettingsRepository.getNotificationPreferences(userId);
    logger.infoWithEmoji('🔔', `Retrieved notification preferences for user ${userId}`, 'SETTINGS');
    return preferences;
  }

  /**
   * Update notification preference
   */
  async updateNotificationPreference(
    userId: number,
    notificationType: string,
    enabled: boolean
  ) {
    const preference = await this.userSettingsRepository.updateNotificationPreference(
      userId,
      notificationType,
      enabled
    );
    logger.infoWithEmoji('🔔', `Updated notification preference: ${notificationType} for user ${userId}`, 'SETTINGS', {
      enabled,
    });
    return preference;
  }

  /**
   * Bulk update notification preferences
   */
  async updateNotificationPreferences(
    userId: number,
    preferences: Array<{ type: string; enabled: boolean }>
  ) {
    const updated = await this.userSettingsRepository.updateNotificationPreferences(
      userId,
      preferences
    );
    logger.infoWithEmoji('🔔', `Bulk updated ${preferences.length} notification preferences for user ${userId}`, 'SETTINGS');
    return updated;
  }

  /**
   * Get security settings
   */
  async getSecuritySettings(userId: number) {
    const settings = await this.userSettingsRepository.getSecuritySettings(userId);
    logger.infoWithEmoji('🔒', `Retrieved security settings for user ${userId}`, 'SETTINGS');
    return settings;
  }

  /**
   * Update security setting
   */
  async updateSecuritySetting(
    userId: number,
    settingType: string,
    enabled: boolean,
    metadata?: Record<string, any>
  ) {
    const setting = await this.userSettingsRepository.updateSecuritySetting(
      userId,
      settingType,
      enabled,
      metadata
    );
    logger.infoWithEmoji('🔒', `Updated security setting: ${settingType} for user ${userId}`, 'SETTINGS', {
      enabled,
    });
    return setting;
  }

  /**
   * Bulk update security settings
   */
  async updateSecuritySettings(
    userId: number,
    settings: Array<{ type: string; enabled: boolean; metadata?: Record<string, any> }>
  ) {
    const updated = await this.userSettingsRepository.updateSecuritySettings(
      userId,
      settings
    );
    logger.infoWithEmoji('🔒', `Bulk updated ${settings.length} security settings for user ${userId}`, 'SETTINGS');
    return updated;
  }

  /**
   * Get profile metadata
   */
  async getProfileMetadata(userId: number) {
    const metadata = await this.userSettingsRepository.getProfileMetadata(userId);
    return metadata;
  }

  /**
   * Update profile metadata
   */
  async updateProfileMetadata(
    userId: number,
    data: {
      gender?: string;
      birthdate?: string;
      profilePictureUrl?: string;
      metadata?: Record<string, any>;
    }
  ) {
    const updated = await this.userSettingsRepository.updateProfileMetadata(userId, data);
    logger.infoWithEmoji('👤', `Updated profile metadata for user ${userId}`, 'SETTINGS');
    return updated;
  }

  /**
   * Get app appearance settings
   */
  async getAppAppearance(userId: number) {
    const appearance = await this.userSettingsRepository.getAppAppearance(userId);
    logger.infoWithEmoji('🎨', `Retrieved app appearance for user ${userId}`, 'SETTINGS');
    return appearance;
  }

  /**
   * Update app appearance settings
   */
  async updateAppAppearance(
    userId: number,
    settings: { theme?: string; language?: string }
  ) {
    const updated = await this.userSettingsRepository.updateAppAppearance(userId, settings);
    logger.infoWithEmoji('🎨', `Updated app appearance for user ${userId}`, 'SETTINGS', settings);
    return updated;
  }
}

