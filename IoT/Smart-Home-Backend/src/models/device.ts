/**
 * Device status enum
 */
export enum DeviceStatus {
  ONLINE = 'online',
  OFFLINE = 'offline',
  UNKNOWN = 'unknown',
}

/**
 * Device type enum
 */
export enum DeviceType {
  LAMP = 'lamp',
  CAMERA = 'camera',
  ELECTRONICS = 'electronics',
  CCTV = 'cctv',
  SPEAKER = 'speaker',
  THERMOSTAT = 'thermostat',
  LOCK = 'lock',
  TV = 'tv',
  APPLIANCE = 'appliance',
  SENSOR = 'sensor',
}

/**
 * Device model interface
 */
export interface Device {
  id: number;
  userId: number;
  name: string;
  type: DeviceType;
  status: DeviceStatus;
  macAddress: string;
  ipAddress?: string;
  roomId?: number | null;
  homeId: number; // Required - device must belong to a home
  lastSeen?: Date;
  metadata?: string | Record<string, unknown>; // JSON string or object (PostgreSQL JSONB returns as object)
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Device creation input
 */
export interface CreateDeviceInput {
  name: string;
  type: DeviceType;
  macAddress: string;
  ipAddress?: string;
  roomId?: number | null;
  homeId?: number; // Required - will be derived from room or user's primary home if not provided, but must never be null
  metadata?: Record<string, unknown>;
}

/**
 * Device update input
 */
export interface UpdateDeviceInput {
  name?: string;
  status?: DeviceStatus;
  ipAddress?: string;
  roomId?: number | null;
  homeId?: number | null;
  metadata?: Record<string, unknown>;
}

/**
 * Device response
 */
export interface DeviceResponse {
  id: number;
  userId: number;
  name: string;
  type: DeviceType;
  status: DeviceStatus;
  macAddress: string;
  ipAddress?: string;
  roomId?: number | null;
  homeId: number; // Required - device must belong to a home
  lastSeen?: Date;
  metadata?: Record<string, unknown>;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Device list query parameters
 */
export interface DeviceListQuery {
  page?: number;
  limit?: number;
  type?: DeviceType;
  status?: DeviceStatus;
  search?: string;
  roomId?: number | null; // null means devices without a room
  homeId?: number; // Filter devices by home (via rooms)
}

