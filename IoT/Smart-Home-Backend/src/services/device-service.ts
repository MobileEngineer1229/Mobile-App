import { IDeviceRepository } from '../domain/repositories/IDeviceRepository';
import {
  Device,
  CreateDeviceInput,
  UpdateDeviceInput,
  DeviceListQuery,
  DeviceResponse,
  DeviceStatus,
} from '../models/device';
import { NotFoundError } from '../utils/errors';
import logger from '../utils/logger';

/**
 * Device service for business logic
 * Uses IDeviceRepository interface (Dependency Inversion Principle)
 */
export class DeviceService {
  constructor(private deviceRepository: IDeviceRepository) {}

  /**
   * Convert device to response format (parse metadata JSON)
   */
  private toDeviceResponse(device: Device): DeviceResponse {
    let metadata: Record<string, unknown> | undefined;
    if (device.metadata) {
      try {
        // PostgreSQL JSONB returns as object, MySQL returns as string
        if (typeof device.metadata === 'string') {
          metadata = JSON.parse(device.metadata);
        } else {
          metadata = device.metadata as Record<string, unknown>;
        }
      } catch {
        metadata = undefined;
      }
    }

    return {
      ...device,
      metadata,
    };
  }

  /**
   * Get device by ID
   */
  async getDeviceById(id: number, userId: number): Promise<DeviceResponse> {
    const device = await this.deviceRepository.findById(id, userId);

    if (!device) {
      throw new NotFoundError('Device');
    }

    return this.toDeviceResponse(device);
  }

  /**
   * Get all devices for a user
   */
  async getDevices(userId: number, query: DeviceListQuery): Promise<{
    devices: DeviceResponse[];
    pagination: {
      page: number;
      limit: number;
      total: number;
      totalPages: number;
    };
  }> {
    const page = query.page || 1;
    const limit = query.limit || 10;

    const { devices, total } = await this.deviceRepository.findAll(userId, query);

    const deviceResponses = devices.map((device) => this.toDeviceResponse(device));

    return {
      devices: deviceResponses,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  /**
   * Get devices by home ID
   */
  async getDevicesByHomeId(userId: number, homeId: number, query?: DeviceListQuery): Promise<{
    devices: DeviceResponse[];
    pagination: {
      page: number;
      limit: number;
      total: number;
      totalPages: number;
    };
  }> {
    const page = query?.page || 1;
    const limit = query?.limit || 10;

    const { devices, total } = await this.deviceRepository.findByHomeId(userId, homeId, query);

    const deviceResponses = devices.map((device: Device) => this.toDeviceResponse(device));

    return {
      devices: deviceResponses,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  /**
   * Create new device
   */
  async createDevice(userId: number, input: CreateDeviceInput): Promise<DeviceResponse> {
    const device = await this.deviceRepository.create(userId, input);
    logger.device.create(device.id, device.name, userId, {
      type: device.type,
      macAddress: device.macAddress,
    });
    return this.toDeviceResponse(device);
  }

  /**
   * Update device
   */
  async updateDevice(id: number, userId: number, input: UpdateDeviceInput): Promise<DeviceResponse> {
    const device = await this.deviceRepository.update(id, userId, input);
    logger.device.update(id, device.name, userId, {
      updatedFields: Object.keys(input),
    });
    return this.toDeviceResponse(device);
  }

  /**
   * Delete device
   */
  async deleteDevice(id: number, userId: number): Promise<void> {
    await this.deviceRepository.delete(id, userId);
    logger.device.delete(id, userId);
  }

  /**
   * Update device status (e.g., when device connects)
   */
  async updateDeviceStatus(id: number, userId: number, status: DeviceStatus): Promise<DeviceResponse> {
    const device = await this.deviceRepository.update(id, userId, { status });
    logger.device.status(id, status, { userId, deviceName: device.name });
    return this.toDeviceResponse(device);
  }

  /**
   * Update device last seen timestamp
   */
  async updateLastSeen(id: number, userId: number): Promise<void> {
    await this.deviceRepository.updateLastSeen(id, userId);
  }

  /**
   * Get devices by category
   */
  async getDevicesByCategory(
    userId: number,
    category: string,
    roomId?: number | null
  ): Promise<{
    devices: DeviceResponse[];
    pagination: {
      page: number;
      limit: number;
      total: number;
      totalPages: number;
    };
  }> {
    const { devices, total } = await this.deviceRepository.findByCategory(userId, category, roomId);

    const deviceResponses = devices.map((device) => this.toDeviceResponse(device));

    return {
      devices: deviceResponses,
      pagination: {
        page: 1,
        limit: 100,
        total,
        totalPages: Math.ceil(total / 100),
      },
    };
  }
}

