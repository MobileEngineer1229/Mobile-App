import { AppVersionRepository } from '../repositories/app-version-repository';
import {
  AppVersion,
  CreateAppVersionInput,
  UpdateAppVersionInput,
  VersionCheckRequest,
  VersionCheckResponse,
} from '../models/app-version';

/**
 * App Version service for business logic
 */
export class AppVersionService {
  constructor(private appVersionRepository: AppVersionRepository) {}

  /**
   * Check app version and return update status
   */
  async checkVersion(request: VersionCheckRequest): Promise<VersionCheckResponse> {
    const { currentVersion, updateAvailable, updateRequired, message } =
      await this.appVersionRepository.checkVersion(
        request.platform,
        request.versionName,
        request.versionCode
      );

    if (!currentVersion) {
      return {
        currentVersion: {
          versionName: request.versionName,
          versionCode: request.versionCode,
        },
        minimumRequiredVersion: request.versionName,
        updateAvailable: false,
        updateRequired: false,
        message: 'No version information available',
      };
    }

    return {
      currentVersion: {
        versionName: currentVersion.versionName,
        versionCode: currentVersion.versionCode,
      },
      minimumRequiredVersion: currentVersion.minimumRequiredVersion,
      updateAvailable,
      updateRequired,
      updateUrl: currentVersion.updateUrl,
      releaseNotes: currentVersion.releaseNotes,
      message,
    };
  }

  /**
   * Get active version for platform
   */
  async getActiveVersion(platform: 'android' | 'ios'): Promise<AppVersion | null> {
    return this.appVersionRepository.getActiveVersion(platform);
  }

  /**
   * Get all versions for platform
   */
  async getAllVersions(platform: 'android' | 'ios'): Promise<AppVersion[]> {
    return this.appVersionRepository.getAllVersions(platform);
  }

  /**
   * Get version by ID
   */
  async getVersionById(id: number): Promise<AppVersion> {
    const version = await this.appVersionRepository.findById(id);
    if (!version) {
      throw new Error('App version not found');
    }
    return version;
  }

  /**
   * Create new version
   */
  async createVersion(input: CreateAppVersionInput): Promise<AppVersion> {
    return this.appVersionRepository.create(input);
  }

  /**
   * Update version
   */
  async updateVersion(id: number, input: UpdateAppVersionInput): Promise<AppVersion> {
    return this.appVersionRepository.update(id, input);
  }

  /**
   * Delete version
   */
  async deleteVersion(id: number): Promise<void> {
    return this.appVersionRepository.delete(id);
  }
}
