import { Request, Response, NextFunction } from 'express';
import { sendSuccess } from '../utils/response';
import { DeviceTypeTemplatesResponse } from '../models/device-type';
import { DeviceTypeRepository } from '../repositories/device-type-repository';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

/**
 * Device type controller
 * Provides device type templates for manual device addition
 */
export class DeviceTypeController {
  constructor(private deviceTypeRepository: DeviceTypeRepository) {}

  /**
   * Get device type templates
   */
  getDeviceTypes = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const category = req.query.category as string | undefined;

      // Fetch device types from database
      const templates = await this.deviceTypeRepository.findAll(category);
      const categories = await this.deviceTypeRepository.getCategories();

      const response: DeviceTypeTemplatesResponse = {
        templates,
        categories,
      };

      sendSuccess(res, response, 200);
    } catch (error) {
      next(error);
    }
  };
}

