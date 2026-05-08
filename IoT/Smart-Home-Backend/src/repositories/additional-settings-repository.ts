import { UserRepository } from './user-repository';
import { UserAdditionalSettingsRepository } from './user-additional-settings-repository';
import { getPool } from '../config/database';

export interface AdditionalSetting {
  key: string;
  value?: string;
  metadata?: Record<string, any>;
}

export class AdditionalSettingsRepository {
  private additionalSettingsRepo: UserAdditionalSettingsRepository;

  constructor(
    private userRepository: UserRepository
  ) {
    const pool = getPool();
    this.additionalSettingsRepo = new UserAdditionalSettingsRepository(pool);
  }

  /**
   * Get all additional settings for a user
   */
  async findByUserId(userId: number): Promise<AdditionalSetting[]> {
    // Try new separate table first, fallback to JSONB column
    try {
      const settings = await this.additionalSettingsRepo.findAll(userId);
      if (Object.keys(settings).length > 0) {
        return Object.entries(settings).map(([key, value]) => {
          if (typeof value === 'object' && value !== null) {
            return {
              key,
              value: (value as any).value,
              metadata: value as Record<string, any>,
            };
          }
          return {
            key,
            value: value as string,
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

    const settings = user.additionalSettings || {};
    return Object.entries(settings).map(([key, value]) => {
      if (typeof value === 'object' && value !== null) {
        return {
          key,
          value: (value as any).value,
          metadata: value as Record<string, any>,
        };
      }
      return {
        key,
        value: value as string,
      };
    });
  }

  /**
   * Get a specific setting
   */
  async findByKey(userId: number, settingKey: string): Promise<AdditionalSetting | null> {
    // Try new separate table first, fallback to JSONB column
    try {
      const value = await this.additionalSettingsRepo.findOne(userId, settingKey);
      if (value !== null) {
        if (typeof value === 'object' && value !== null) {
          return {
            key: settingKey,
            value: (value as any).value,
            metadata: value as Record<string, any>,
          };
        }
        return {
          key: settingKey,
          value: value as string,
        };
      }
    } catch (error) {
      // Fallback to JSONB column
    }

    // Fallback to JSONB column
    const user = await this.userRepository.findById(userId);
    if (!user) {
      return null;
    }

    const settings = user.additionalSettings || {};
    const value = settings[settingKey];

    if (value === undefined) {
      return null;
    }

    if (typeof value === 'object' && value !== null) {
      return {
        key: settingKey,
        value: (value as any).value,
        metadata: value as Record<string, any>,
      };
    }

    return {
      key: settingKey,
      value: value as string,
    };
  }

  /**
   * Update or create a setting
   */
  async upsertSetting(
    userId: number,
    settingKey: string,
    settingValue: string,
    metadata?: Record<string, any>
  ): Promise<AdditionalSetting> {
    // Update in separate table
    const value = metadata ? { value: settingValue, ...metadata } : settingValue;
    await this.additionalSettingsRepo.upsert(userId, settingKey, value);
    
    // Also update JSONB column for backward compatibility
    const user = await this.userRepository.findById(userId);
    if (user) {
      const settings = { ...(user.additionalSettings || {}) };
      if (metadata) {
        settings[settingKey] = {
          value: settingValue,
          ...metadata,
        };
      } else {
        settings[settingKey] = settingValue;
      }
      await this.userRepository.updateAdditionalSettings(userId, settings);
    }

    return {
      key: settingKey,
      value: settingValue,
      metadata,
    };
  }

  /**
   * Bulk update settings
   */
  async bulkUpdateSettings(
    userId: number,
    settings: Array<{ key: string; value: string; metadata?: Record<string, any> }>
  ): Promise<AdditionalSetting[]> {
    // Update in separate table
    const settingsMap: Record<string, any> = {};
    for (const setting of settings) {
      if (setting.metadata) {
        settingsMap[setting.key] = {
          value: setting.value,
          ...setting.metadata,
        };
      } else {
        settingsMap[setting.key] = setting.value;
      }
    }
    await this.additionalSettingsRepo.bulkUpdate(userId, settingsMap);
    
    // Also update JSONB column for backward compatibility
    const user = await this.userRepository.findById(userId);
    if (user) {
      const updatedSettings = { ...(user.additionalSettings || {}) };
      for (const setting of settings) {
        if (setting.metadata) {
          updatedSettings[setting.key] = {
            value: setting.value,
            ...setting.metadata,
          };
        } else {
          updatedSettings[setting.key] = setting.value;
        }
      }
      await this.userRepository.updateAdditionalSettings(userId, updatedSettings);
    }

    return settings.map(s => ({
      key: s.key,
      value: s.value,
      metadata: s.metadata,
    }));
  }
}
