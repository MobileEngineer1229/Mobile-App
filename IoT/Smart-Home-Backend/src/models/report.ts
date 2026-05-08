/**
 * Energy consumption record
 */
export interface EnergyConsumption {
  id: number;
  deviceId: number;
  date: Date;
  consumptionKwh: number;
  costUsd: number;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Monthly usage summary
 */
export interface MonthlySummary {
  month: string; // Format: "YYYY-MM"
  consumptionKwh: number;
  costUsd: number;
}

/**
 * Date range for reports
 */
export type DateRange =
  | 'today'
  | 'this_week'
  | 'last_month'
  | 'last_3_months'
  | 'last_6_months'
  | 'this_year'
  | 'last_year'
  | 'all_time'
  | 'custom';

/**
 * Statistics data point
 */
export interface StatisticsDataPoint {
  period: string; // e.g., "2024-01", "2024-12-01"
  consumptionKwh: number;
  costUsd: number;
}

/**
 * Device consumption summary
 */
export interface DeviceConsumptionSummary {
  deviceId: number;
  deviceName: string;
  deviceType: string;
  roomId?: number | null;
  roomName?: string | null;
  totalConsumptionKwh: number;
  totalCostUsd: number;
  deviceCount?: number; // For grouped by type
}

/**
 * Detailed device consumption
 */
export interface DeviceConsumptionDetail {
  deviceId: number;
  deviceName: string;
  deviceType: string;
  roomId?: number | null;
  roomName?: string | null;
  consumptionKwh: number;
  costUsd: number;
  date: Date;
}

/**
 * Reports query parameters
 */
export interface ReportsQuery {
  dateRange?: DateRange;
  startDate?: string; // ISO date string for custom range
  endDate?: string; // ISO date string for custom range
  deviceId?: number;
  deviceType?: string;
  roomId?: number | null;
  groupBy?: 'device' | 'type' | 'room';
}

/**
 * Monthly summary response
 */
export interface MonthlySummaryResponse {
  thisMonth: MonthlySummary;
  previousMonth: MonthlySummary;
}

/**
 * Statistics response
 */
export interface StatisticsResponse {
  dateRange: DateRange;
  startDate?: string;
  endDate?: string;
  data: StatisticsDataPoint[];
}

/**
 * Device consumption response
 */
export interface DeviceConsumptionResponse {
  devices: DeviceConsumptionSummary[];
  totalConsumptionKwh: number;
  totalCostUsd: number;
}

/**
 * Detailed device consumption response
 */
export interface DeviceConsumptionDetailResponse {
  deviceId: number;
  deviceName: string;
  deviceType: string;
  roomId?: number | null;
  roomName?: string | null;
  records: DeviceConsumptionDetail[];
  totalConsumptionKwh: number;
  totalCostUsd: number;
}

