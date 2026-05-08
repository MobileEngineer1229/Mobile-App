import { Request, Response } from 'express';
import { DeviceDataService, EnergyConsumptionInput, BatchEnergyConsumptionInput } from '../services/device-data-service';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger';

export class DeviceDataController {
  constructor(private deviceDataService: DeviceDataService) {}

  /**
   * Submit energy consumption data
   * POST /api/v1/devices/:deviceId/energy
   */
  submitEnergyConsumption = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const deviceId = parseInt(req.params.deviceId, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const input: EnergyConsumptionInput = {
        consumptionKwh: req.body.consumptionKwh,
        date: req.body.date,
        costUsd: req.body.costUsd,
      };

      // Validation
      if (!input.consumptionKwh || input.consumptionKwh < 0) {
        sendError(res, 'VALIDATION_ERROR', 'consumptionKwh is required and must be >= 0', 400);
        return;
      }

      if (!input.date) {
        sendError(res, 'VALIDATION_ERROR', 'date is required', 400);
        return;
      }

      const consumption = await this.deviceDataService.submitEnergyConsumption(
        deviceId,
        userId,
        input
      );

      sendSuccess(res, consumption, 200);
    } catch (error) {
      logger.error('Error in submitEnergyConsumption controller', {
        error: error instanceof Error ? error.message : String(error),
      });

      if (error instanceof Error && error.message.includes('not found')) {
        sendError(res, 'NOT_FOUND', error.message, 404);
      } else {
        sendError(res, 'INTERNAL_ERROR', 'Failed to save energy consumption data', 500);
      }
    }
  };

  /**
   * Submit batch energy consumption data
   * POST /api/v1/devices/:deviceId/energy/batch
   */
  submitEnergyConsumptionBatch = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const deviceId = parseInt(req.params.deviceId, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const input: BatchEnergyConsumptionInput = {
        records: req.body.records || [],
      };

      if (!input.records || input.records.length === 0) {
        sendError(res, 'VALIDATION_ERROR', 'records array is required and cannot be empty', 400);
        return;
      }

      const results = await this.deviceDataService.submitEnergyConsumptionBatch(
        deviceId,
        userId,
        input
      );

      sendSuccess(res, { saved: results.length, records: results }, 200);
    } catch (error) {
      logger.error('Error in submitEnergyConsumptionBatch controller', {
        error: error instanceof Error ? error.message : String(error),
      });

      if (error instanceof Error && error.message.includes('not found')) {
        sendError(res, 'NOT_FOUND', error.message, 404);
      } else {
        sendError(res, 'INTERNAL_ERROR', 'Failed to save batch energy consumption data', 500);
      }
    }
  };

  /**
   * Submit energy data via webhook (device token auth)
   * POST /api/v1/devices/webhook/energy
   */
  submitEnergyViaWebhook = async (req: Request, res: Response): Promise<void> => {
    try {
      const deviceToken = req.body.deviceToken;
      if (!deviceToken) {
        sendError(res, 'UNAUTHORIZED', 'Device token is required', 401);
        return;
      }

      const input: EnergyConsumptionInput = {
        consumptionKwh: req.body.consumptionKwh,
        date: req.body.date,
        costUsd: req.body.costUsd,
      };

      // Validation
      if (!input.consumptionKwh || input.consumptionKwh < 0) {
        sendError(res, 'VALIDATION_ERROR', 'consumptionKwh is required and must be >= 0', 400);
        return;
      }

      if (!input.date) {
        sendError(res, 'VALIDATION_ERROR', 'date is required', 400);
        return;
      }

      const consumption = await this.deviceDataService.submitEnergyViaWebhook(deviceToken, input);
      sendSuccess(res, consumption, 200);
    } catch (error) {
      logger.error('Error in submitEnergyViaWebhook controller', {
        error: error instanceof Error ? error.message : String(error),
      });

      if (error instanceof Error && error.message.includes('not yet implemented')) {
        sendError(res, 'NOT_IMPLEMENTED', 'Device token authentication not yet implemented', 501);
      } else {
        sendError(res, 'INTERNAL_ERROR', 'Failed to process webhook', 500);
      }
    }
  };
}

