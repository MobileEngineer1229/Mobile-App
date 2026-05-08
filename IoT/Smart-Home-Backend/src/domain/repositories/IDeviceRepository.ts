import { Device, CreateDeviceInput, UpdateDeviceInput, DeviceListQuery } from '../../models/device';

/**
 * Device repository interface
 * Defines the contract for device data access operations
 */
export interface IDeviceRepository {
  findById(id: number, userId?: number): Promise<Device | null>;
  findByMacAddress(macAddress: string, userId?: number): Promise<Device | null>;
  findAll(userId: number, query: DeviceListQuery): Promise<{ devices: Device[]; total: number }>;
  findByHomeId(userId: number, homeId: number, query?: DeviceListQuery): Promise<{ devices: Device[]; total: number }>;
  create(userId: number, input: CreateDeviceInput): Promise<Device>;
  update(id: number, userId: number, input: UpdateDeviceInput): Promise<Device>;
  delete(id: number, userId: number): Promise<void>;
  updateLastSeen(id: number, userId: number): Promise<void>;
  findByCategory(userId: number, category: string, roomId?: number | null): Promise<{ devices: Device[]; total: number }>;
}
