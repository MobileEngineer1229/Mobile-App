/**
 * App Version model interface
 */
export interface AppVersion {
  id: number;
  platform: 'android' | 'ios';
  versionName: string;
  versionCode: number;
  minimumRequiredVersion: string;
  updateUrl?: string;
  forceUpdate: boolean;
  releaseNotes?: string;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * App Version creation input
 */
export interface CreateAppVersionInput {
  platform: 'android' | 'ios';
  versionName: string;
  versionCode: number;
  minimumRequiredVersion: string;
  updateUrl?: string;
  forceUpdate?: boolean;
  releaseNotes?: string;
  isActive?: boolean;
}

/**
 * App Version update input
 */
export interface UpdateAppVersionInput {
  versionName?: string;
  versionCode?: number;
  minimumRequiredVersion?: string;
  updateUrl?: string;
  forceUpdate?: boolean;
  releaseNotes?: string;
  isActive?: boolean;
}

/**
 * Version check request
 */
export interface VersionCheckRequest {
  platform: 'android' | 'ios';
  versionName: string;
  versionCode: number;
}

/**
 * Version check response
 */
export interface VersionCheckResponse {
  currentVersion: {
    versionName: string;
    versionCode: number;
  };
  minimumRequiredVersion: string;
  updateAvailable: boolean;
  updateRequired: boolean;
  updateUrl?: string;
  releaseNotes?: string;
  message?: string;
}
