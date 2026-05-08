import { UserRepository } from './user-repository';
import { UserNotificationPreferencesRepository } from './user-notification-preferences-repository';
import { UserSecuritySettingsRepository } from './user-security-settings-repository';
import { UserProfileMetadataRepository } from './user-profile-metadata-repository';
import { getPool } from '../config/database';

export interface NotificationPreference {
  type: string;
  enabled: boolean;
}

export interface SecuritySetting {
  type: string;
  enabled: boolean;
  metadata?: Record<string, any>;
}

export interface ProfileMetadata {
  gender?: string;
  birthdate?: string;
  profilePictureUrl?: string;
  appAppearance?: {
    theme?: string;
    language?: string;
  };
  [key: string]: any;
}

export class UserSettingsRepository {
  private notificationPrefsRepo: UserNotificationPreferencesRepository;
  private securitySettingsRepo: UserSecuritySettingsRepository;
  private profileMetadataRepo: UserProfileMetadataRepository;

  constructor(
    private userRepository: UserRepository
  ) {
    const pool = getPool();
    this.notificationPrefsRepo = new UserNotificationPreferencesRepository(pool);
    this.securitySettingsRepo = new UserSecuritySettingsRepository(pool);
    this.profileMetadataRepo = new UserProfileMetadataRepository(pool);
  }

  /**
   * Get all notification preferences for a user
   */
  async getNotificationPreferences(userId: number): Promise<NotificationPreference[]> {
    // Try new separate table first, fallback to JSONB column
    try {
      const prefs = await this.notificationPrefsRepo.findAll(userId);
      if (Object.keys(prefs).length > 0) {
        return Object.entries(prefs).map(([type, value]) => ({
          type,
          enabled: typeof value === 'boolean' ? value : (value as any)?.enabled || false,
        }));
      }
    } catch (error) {
      // Fallback to JSONB column
    }

    // Fallback to JSONB column
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new Error('User not found');
    }

    const preferences = user.notificationPreferences || {};
    return Object.entries(preferences).map(([type, enabled]) => ({
      type,
      enabled: enabled as boolean,
    }));
  }

  /**
   * Update notification preference
   */
  async updateNotificationPreference(
    userId: number,
    notificationType: string,
    enabled: boolean
  ): Promise<NotificationPreference> {
    // Update in separate table
    await this.notificationPrefsRepo.upsert(userId, notificationType, enabled);
    
    // Also update JSONB column for backward compatibility
    const user = await this.userRepository.findById(userId);
    if (user) {
      const preferences = { ...(user.notificationPreferences || {}) };
      preferences[notificationType] = enabled;
      await this.userRepository.updateNotificationPreferences(userId, preferences);
    }

    return {
      type: notificationType,
      enabled,
    };
  }

  /**
   * Bulk update notification preferences
   */
  async updateNotificationPreferences(
    userId: number,
    preferences: Array<{ type: string; enabled: boolean }>
  ): Promise<NotificationPreference[]> {
    // Update in separate table
    const prefsMap: Record<string, any> = {};
    for (const pref of preferences) {
      prefsMap[pref.type] = pref.enabled;
    }
    await this.notificationPrefsRepo.bulkUpdate(userId, prefsMap);
    
    // Also update JSONB column for backward compatibility
    const user = await this.userRepository.findById(userId);
    if (user) {
      const updatedPreferences = { ...(user.notificationPreferences || {}) };
      for (const pref of preferences) {
        updatedPreferences[pref.type] = pref.enabled;
      }
      await this.userRepository.updateNotificationPreferences(userId, updatedPreferences);
    }

    return preferences;
  }

  /**
   * Get security settings for a user
   */
  async getSecuritySettings(userId: number): Promise<SecuritySetting[]> {
    // Try new separate table first, fallback to JSONB column
    try {
      const settings = await this.securitySettingsRepo.findAll(userId);
      if (Object.keys(settings).length > 0) {
        return Object.entries(settings).map(([type, value]) => {
          if (typeof value === 'object' && value !== null) {
            return {
              type,
              enabled: (value as any).enabled || false,
              metadata: value as Record<string, any>,
            };
          }
          return {
            type,
            enabled: value as boolean,
          };
        });
      }
    } catch (error) {
      // Fallback to JSONB column
    }

    // Fallback to JSONB column
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new Error('User not found');
    }

    const settings = user.securitySettings || {};
    return Object.entries(settings).map(([type, value]) => {
      if (typeof value === 'object' && value !== null) {
        return {
          type,
          enabled: (value as any).enabled || false,
          metadata: value as Record<string, any>,
        };
      }
      return {
        type,
        enabled: value as boolean,
      };
    });
  }

  /**
   * Update security setting
   */
  async updateSecuritySetting(
    userId: number,
    settingType: string,
    enabled: boolean,
    metadata?: Record<string, any>
  ): Promise<SecuritySetting> {
    // Update in separate table
    const value = metadata ? { enabled, ...metadata } : enabled;
    await this.securitySettingsRepo.upsert(userId, settingType, value);
    
    // Also update JSONB column for backward compatibility
    const user = await this.userRepository.findById(userId);
    if (user) {
      const settings = { ...(user.securitySettings || {}) };
      if (metadata) {
        settings[settingType] = { enabled, ...metadata };
      } else {
        settings[settingType] = enabled;
      }
      await this.userRepository.updateSecuritySettings(userId, settings);
    }

    return {
      type: settingType,
      enabled,
      metadata,
    };
  }

  /**
   * Bulk update security settings
   */
  async updateSecuritySettings(
    userId: number,
    settings: Array<{ type: string; enabled: boolean; metadata?: Record<string, any> }>
  ): Promise<SecuritySetting[]> {
    // Update in separate table
    const settingsMap: Record<string, any> = {};
    for (const setting of settings) {
      settingsMap[setting.type] = setting.metadata ? { enabled: setting.enabled, ...setting.metadata } : setting.enabled;
    }
    await this.securitySettingsRepo.bulkUpdate(userId, settingsMap);
    
    // Also update JSONB column for backward compatibility
    const user = await this.userRepository.findById(userId);
    if (user) {
      const updatedSettings = { ...(user.securitySettings || {}) };
      for (const setting of settings) {
        if (setting.metadata) {
          updatedSettings[setting.type] = { enabled: setting.enabled, ...setting.metadata };
        } else {
          updatedSettings[setting.type] = setting.enabled;
        }
      }
      await this.userRepository.updateSecuritySettings(userId, updatedSettings);
    }

    return settings.map(s => ({
      type: s.type,
      enabled: s.enabled,
      metadata: s.metadata,
    }));
  }

  /**
   * Get profile metadata
   */
  async getProfileMetadata(userId: number): Promise<ProfileMetadata | null> {
    // Try new separate table first, fallback to JSONB column
    try {
      const metadata = await this.profileMetadataRepo.findAll(userId);
      if (Object.keys(metadata).length > 0) {
        return metadata as ProfileMetadata;
      }
    } catch (error) {
      // Fallback to JSONB column
    }

    // Fallback to JSONB column
    const user = await this.userRepository.findById(userId);
    if (!user) {
      return null;
    }

    return user.profileMetadata || {};
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
  ): Promise<ProfileMetadata> {
    const existingMetadata = await this.getProfileMetadata(userId) || {};
    const updatedMetadata: ProfileMetadata = {
      ...existingMetadata,
    };

    if (data.gender !== undefined) {
      updatedMetadata.gender = data.gender;
      await this.profileMetadataRepo.upsert(userId, 'gender', data.gender);
    }
    if (data.birthdate !== undefined) {
      updatedMetadata.birthdate = data.birthdate;
      await this.profileMetadataRepo.upsert(userId, 'birthdate', data.birthdate);
    }
    if (data.profilePictureUrl !== undefined) {
      updatedMetadata.profilePictureUrl = data.profilePictureUrl;
      await this.profileMetadataRepo.upsert(userId, 'profilePictureUrl', data.profilePictureUrl);
    }
    if (data.metadata) {
      updatedMetadata.metadata = { ...(existingMetadata.metadata || {}), ...data.metadata };
      await this.profileMetadataRepo.upsert(userId, 'metadata', updatedMetadata.metadata);
    }

    // Also update JSONB column for backward compatibility
    await this.userRepository.updateProfileMetadata(userId, updatedMetadata);

    return updatedMetadata;
  }

  /**
   * Get app appearance settings
   */
  async getAppAppearance(userId: number): Promise<Record<string, any>> {
    const metadata = await this.getProfileMetadata(userId);
    if (!metadata || !metadata.appAppearance) {
      return {
        theme: 'system',
        language: 'en_US',
      };
    }
    
    return metadata.appAppearance;
  }

  /**
   * Update app appearance settings
   */
  async updateAppAppearance(
    userId: number,
    settings: { theme?: string; language?: string }
  ): Promise<Record<string, any>> {
    const existingMetadata = await this.getProfileMetadata(userId);
    const currentAppAppearance = existingMetadata?.appAppearance || {};
    
    const updatedAppAppearance = {
      ...currentAppAppearance,
      ...settings,
    };
    
    await this.updateProfileMetadata(userId, {
      metadata: {
        appAppearance: updatedAppAppearance,
      },
    });
    
    return updatedAppAppearance;
  }
}
