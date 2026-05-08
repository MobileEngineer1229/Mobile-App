import { Request, Response, NextFunction } from 'express';
import { UserSettingsService } from '../services/user-settings-service';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

export class UserSettingsController {
  constructor(private userSettingsService: UserSettingsService) {}

  /**
   * Get notification preferences
   */
  getNotificationPreferences = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const preferences = await this.userSettingsService.getNotificationPreferences(req.user!.id);
      sendSuccess(res, preferences, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update notification preference
   */
  updateNotificationPreference = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { type, enabled } = req.body;
      if (type === undefined || enabled === undefined) {
        sendError(res, 'BAD_REQUEST', 'Type and enabled are required', 400);
        return;
      }

      const preference = await this.userSettingsService.updateNotificationPreference(
        req.user!.id,
        type,
        enabled
      );
      sendSuccess(res, preference, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Bulk update notification preferences
   */
  updateNotificationPreferences = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { preferences } = req.body;
      if (!Array.isArray(preferences)) {
        sendError(res, 'BAD_REQUEST', 'Preferences must be an array', 400);
        return;
      }

      const updated = await this.userSettingsService.updateNotificationPreferences(
        req.user!.id,
        preferences
      );
      sendSuccess(res, updated, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get security settings
   */
  getSecuritySettings = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const settings = await this.userSettingsService.getSecuritySettings(req.user!.id);
      sendSuccess(res, settings, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update security setting
   */
  updateSecuritySetting = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { type, enabled, metadata } = req.body;
      if (type === undefined || enabled === undefined) {
        sendError(res, 'BAD_REQUEST', 'Type and enabled are required', 400);
        return;
      }

      const setting = await this.userSettingsService.updateSecuritySetting(
        req.user!.id,
        type,
        enabled,
        metadata
      );
      sendSuccess(res, setting, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Bulk update security settings
   */
  updateSecuritySettings = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { settings } = req.body;
      if (!Array.isArray(settings)) {
        sendError(res, 'BAD_REQUEST', 'Settings must be an array', 400);
        return;
      }

      const updated = await this.userSettingsService.updateSecuritySettings(
        req.user!.id,
        settings
      );
      sendSuccess(res, updated, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get app appearance settings
   */
  getAppAppearance = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const appearance = await this.userSettingsService.getAppAppearance(req.user!.id);
      sendSuccess(res, appearance, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update app appearance settings
   */
  updateAppAppearance = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { theme, language } = req.body;
      const settings: { theme?: string; language?: string } = {};
      if (theme !== undefined) {
        settings.theme = theme;
      }
      if (language !== undefined) {
        settings.language = language;
      }

      const updated = await this.userSettingsService.updateAppAppearance(req.user!.id, settings);
      sendSuccess(res, updated, 200);
    } catch (error) {
      next(error);
    }
  };
}

