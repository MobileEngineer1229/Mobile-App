import { Request, Response, NextFunction } from 'express';
import { AdditionalSettingsService } from '../services/additional-settings-service';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

export class AdditionalSettingsController {
  constructor(private additionalSettingsService: AdditionalSettingsService) {}

  /**
   * Get additional settings
   */
  getAdditionalSettings = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const settings = await this.additionalSettingsService.getAdditionalSettings(req.user!.id);
      sendSuccess(res, settings, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update additional settings
   */
  updateAdditionalSettings = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { settings } = req.body;
      if (!Array.isArray(settings)) {
        sendError(res, 'BAD_REQUEST', 'Settings must be an array', 400);
        return;
      }

      const updated = await this.additionalSettingsService.bulkUpdateSettings(req.user!.id, settings);
      sendSuccess(res, updated, 200);
    } catch (error) {
      next(error);
    }
  };
}

