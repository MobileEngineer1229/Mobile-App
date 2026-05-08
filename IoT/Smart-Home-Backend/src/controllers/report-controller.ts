import { Request, Response } from 'express';
import { ReportService } from '../services/report-service';
import { sendSuccess, sendError } from '../utils/response';
import { ReportsQuery, DateRange } from '../models/report';
import logger from '../utils/logger';

export class ReportController {
  constructor(private reportService: ReportService) {}

  /**
   * Get monthly usage summary
   * GET /api/v1/reports/monthly-summary
   */
  getMonthlySummary = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const summary = await this.reportService.getMonthlySummary(userId);
      sendSuccess(res, summary, 200);
    } catch (error) {
      logger.error('Error in getMonthlySummary controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to fetch monthly summary', 500);
    }
  };

  /**
   * Get statistics for a date range
   * GET /api/v1/reports/statistics
   */
  getStatistics = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const dateRange = (req.query.dateRange as DateRange) || 'last_6_months';
      const startDate = req.query.startDate as string | undefined;
      const endDate = req.query.endDate as string | undefined;

      const statistics = await this.reportService.getStatistics(
        userId,
        dateRange,
        startDate,
        endDate
      );
      sendSuccess(res, statistics, 200);
    } catch (error) {
      logger.error('Error in getStatistics controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to fetch statistics', 500);
    }
  };

  /**
   * Get device consumption summary
   * GET /api/v1/reports/devices
   */
  getDeviceConsumption = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const query: ReportsQuery = {
        dateRange: (req.query.dateRange as DateRange) || 'last_6_months',
        startDate: req.query.startDate as string | undefined,
        endDate: req.query.endDate as string | undefined,
        deviceId: req.query.deviceId ? parseInt(req.query.deviceId as string, 10) : undefined,
        deviceType: req.query.deviceType as string | undefined,
        roomId: req.query.roomId
          ? req.query.roomId === 'null'
            ? null
            : parseInt(req.query.roomId as string, 10)
          : undefined,
        groupBy: (req.query.groupBy as 'device' | 'type' | 'room') || 'device',
      };

      const consumption = await this.reportService.getDeviceConsumption(userId, query);
      sendSuccess(res, consumption, 200);
    } catch (error) {
      logger.error('Error in getDeviceConsumption controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to fetch device consumption', 500);
    }
  };

  /**
   * Get detailed consumption for a device type
   * GET /api/v1/reports/devices/:type/details
   */
  getDeviceTypeDetails = async (req: Request, res: Response): Promise<void> => {
    try {
      const userId = req.user?.id;
      if (!userId) {
        sendError(res, 'UNAUTHORIZED', 'User not authenticated', 401);
        return;
      }

      const deviceType = req.params.type;
      const dateRange = (req.query.dateRange as DateRange) || 'last_6_months';
      const startDate = req.query.startDate as string | undefined;
      const endDate = req.query.endDate as string | undefined;

      const details = await this.reportService.getDeviceTypeDetails(
        userId,
        deviceType,
        dateRange as DateRange,
        startDate,
        endDate
      );
      sendSuccess(res, details, 200);
    } catch (error) {
      logger.error('Error in getDeviceTypeDetails controller', {
        error: error instanceof Error ? error.message : String(error),
      });
      sendError(res, 'INTERNAL_ERROR', 'Failed to fetch device type details', 500);
    }
  };
}

