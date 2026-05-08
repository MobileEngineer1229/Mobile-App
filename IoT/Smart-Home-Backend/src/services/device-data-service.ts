import { ReportRepository } from '../repositories/report-repository';
import { DeviceRepository } from '../repositories/device-repository';
import { EnergyConsumption } from '../models/report';
import logger from '../utils/logger';

// Default electricity rate in USD per kWh
const DEFAULT_ELECTRICITY_RATE = 0.15;

export interface EnergyConsumptionInput {
  consumptionKwh: number;
  date: string; // ISO date string (YYYY-MM-DD)
  costUsd?: number;
}

export interface BatchEnergyConsumptionInput {
  records: EnergyConsumptionInput[];
}

export class DeviceDataService {
  constructor(
    private reportRepository: ReportRepository,
    private deviceRepository: DeviceRepository
  ) {}

  /**
   * Submit energy consumption data for a device
   */
  async submitEnergyConsumption(
    deviceId: number,
    userId: number,
    input: EnergyConsumptionInput
  ): Promise<EnergyConsumption> {
    try {
      // Verify device belongs to user
      const device = await this.deviceRepository.findById(deviceId, userId);
      if (!device) {
        throw new Error('Device not found or does not belong to user');
      }

      // Parse date
      const date = new Date(input.date);
      if (isNaN(date.getTime())) {
        throw new Error('Invalid date format');
      }

      // Calculate cost if not provided
      const costUsd = input.costUsd ?? this.calculateCost(input.consumptionKwh);

      // Save to database
      const consumption = await this.reportRepository.upsertEnergyConsumption(
        deviceId,
        date,
        input.consumptionKwh,
        costUsd
      );

      logger.info('Energy consumption data saved', {
        deviceId,
        userId,
        consumptionKwh: input.consumptionKwh,
        date: input.date,
      });

      return consumption;
    } catch (error) {
      logger.error('Error submitting energy consumption', {
        error: error instanceof Error ? error.message : String(error),
        deviceId,
        userId,
      });
      throw error;
    }
  }

  /**
   * Submit multiple energy consumption records
   */
  async submitEnergyConsumptionBatch(
    deviceId: number,
    userId: number,
    input: BatchEnergyConsumptionInput
  ): Promise<EnergyConsumption[]> {
    try {
      // Verify device belongs to user
      const device = await this.deviceRepository.findById(deviceId, userId);
      if (!device) {
        throw new Error('Device not found or does not belong to user');
      }

      const results: EnergyConsumption[] = [];

      for (const record of input.records) {
        const date = new Date(record.date);
        if (isNaN(date.getTime())) {
          logger.warn('Skipping invalid date record', { date: record.date, deviceId });
          continue;
        }

        const costUsd = record.costUsd ?? this.calculateCost(record.consumptionKwh);

        const consumption = await this.reportRepository.upsertEnergyConsumption(
          deviceId,
          date,
          record.consumptionKwh,
          costUsd
        );

        results.push(consumption);
      }

      logger.info('Batch energy consumption data saved', {
        deviceId,
        userId,
        recordCount: results.length,
      });

      return results;
    } catch (error) {
      logger.error('Error submitting batch energy consumption', {
        error: error instanceof Error ? error.message : String(error),
        deviceId,
        userId,
      });
      throw error;
    }
  }

  /**
   * Submit energy data via webhook (device token authentication)
   */
  async submitEnergyViaWebhook(
    _deviceToken: string,
    _input: EnergyConsumptionInput
  ): Promise<EnergyConsumption> {
    try {
      // TODO: Implement device token validation
      // For now, this is a placeholder - you would:
      // 1. Validate device token
      // 2. Get device ID from token
      // 3. Call submitEnergyConsumption

      throw new Error('Device token authentication not yet implemented');
    } catch (error) {
      logger.error('Error in webhook energy submission', {
        error: error instanceof Error ? error.message : String(error),
      });
      throw error;
    }
  }

  /**
   * Calculate cost from consumption
   */
  private calculateCost(consumptionKwh: number, ratePerKwh: number = DEFAULT_ELECTRICITY_RATE): number {
    return Math.round(consumptionKwh * ratePerKwh * 100) / 100;
  }
}

