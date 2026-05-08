import { ReportRepository } from '../repositories/report-repository';
import {
  MonthlySummaryResponse,
  StatisticsResponse,
  DeviceConsumptionResponse,
  DeviceConsumptionDetailResponse,
  ReportsQuery,
  DateRange,
} from '../models/report';
import logger from '../utils/logger';

// Default electricity rate in USD per kWh (can be made configurable)
const DEFAULT_ELECTRICITY_RATE = 0.15;

export class ReportService {
  constructor(private reportRepository: ReportRepository) {}

  /**
   * Get monthly usage summary
   */
  async getMonthlySummary(userId: number): Promise<MonthlySummaryResponse> {
    try {
      const summaries = await this.reportRepository.getMonthlySummary(userId);
      
      // Get current month and previous month
      const now = new Date();
      const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
      const previousMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const previousMonth = `${previousMonthDate.getFullYear()}-${String(previousMonthDate.getMonth() + 1).padStart(2, '0')}`;

      const thisMonthData = summaries.find((s) => s.month === currentMonth) || {
        month: currentMonth,
        consumptionKwh: 0,
        costUsd: 0,
      };

      const previousMonthData = summaries.find((s) => s.month === previousMonth) || {
        month: previousMonth,
        consumptionKwh: 0,
        costUsd: 0,
      };

      return {
        thisMonth: thisMonthData,
        previousMonth: previousMonthData,
      };
    } catch (error) {
      logger.error('Error getting monthly summary', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Get statistics for a date range
   */
  async getStatistics(
    userId: number,
    dateRange: DateRange,
    startDate?: string,
    endDate?: string
  ): Promise<StatisticsResponse> {
    try {
      const data = await this.reportRepository.getStatistics(
        userId,
        dateRange,
        startDate,
        endDate
      );

      return {
        dateRange,
        startDate,
        endDate,
        data,
      };
    } catch (error) {
      logger.error('Error getting statistics', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        dateRange,
      });
      throw error;
    }
  }

  /**
   * Get device consumption summary
   */
  async getDeviceConsumption(
    userId: number,
    query: ReportsQuery
  ): Promise<DeviceConsumptionResponse> {
    try {
      const devices = await this.reportRepository.getDeviceConsumption(userId, query);

      const totalConsumptionKwh = devices.reduce(
        (sum, device) => sum + device.totalConsumptionKwh,
        0
      );
      const totalCostUsd = devices.reduce((sum, device) => sum + device.totalCostUsd, 0);

      return {
        devices,
        totalConsumptionKwh,
        totalCostUsd,
      };
    } catch (error) {
      logger.error('Error getting device consumption', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        query,
      });
      throw error;
    }
  }

  /**
   * Get detailed consumption for a device type
   */
  async getDeviceTypeDetails(
    userId: number,
    deviceType: string,
    dateRange?: DateRange,
    startDate?: string,
    endDate?: string
  ): Promise<DeviceConsumptionDetailResponse> {
    try {
      const records = await this.reportRepository.getDeviceTypeDetails(
        userId,
        deviceType,
        dateRange,
        startDate,
        endDate
      );

      if (records.length === 0) {
        return {
          deviceId: 0,
          deviceName: deviceType,
          deviceType,
          roomId: null,
          roomName: null,
          records: [],
          totalConsumptionKwh: 0,
          totalCostUsd: 0,
        };
      }

      // Group by device and calculate totals
      const deviceMap = new Map<number, typeof records>();
      records.forEach((record) => {
        if (!deviceMap.has(record.deviceId)) {
          deviceMap.set(record.deviceId, []);
        }
        deviceMap.get(record.deviceId)!.push(record);
      });

      // Get first device info (all should have same type)
      const firstRecord = records[0];
      const totalConsumptionKwh = records.reduce(
        (sum, record) => sum + record.consumptionKwh,
        0
      );
      const totalCostUsd = records.reduce((sum, record) => sum + record.costUsd, 0);

      return {
        deviceId: firstRecord.deviceId,
        deviceName: firstRecord.deviceName,
        deviceType: firstRecord.deviceType,
        roomId: firstRecord.roomId,
        roomName: firstRecord.roomName,
        records,
        totalConsumptionKwh,
        totalCostUsd,
      };
    } catch (error) {
      logger.error('Error getting device type details', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        deviceType,
      });
      throw error;
    }
  }

  /**
   * Calculate cost from consumption (helper method)
   */
  calculateCost(consumptionKwh: number, ratePerKwh: number = DEFAULT_ELECTRICITY_RATE): number {
    return Math.round(consumptionKwh * ratePerKwh * 100) / 100;
  }
}

