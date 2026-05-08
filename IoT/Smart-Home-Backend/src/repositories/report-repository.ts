import { Pool, QueryResult } from 'pg';
import {
  EnergyConsumption,
  MonthlySummary,
  StatisticsDataPoint,
  DeviceConsumptionSummary,
  DeviceConsumptionDetail,
  ReportsQuery,
} from '../models/report';
import logger from '../utils/logger';

export class ReportRepository {
  constructor(private pool: Pool) {}

  /**
   * Get monthly usage summary (this month and previous month)
   */
  async getMonthlySummary(userId: number): Promise<MonthlySummary[]> {
    const query = `
      WITH monthly_totals AS (
        SELECT 
          TO_CHAR(ec.date, 'YYYY-MM') AS month,
          SUM(ec.consumption_kwh) AS consumption_kwh,
          SUM(ec.cost_usd) AS cost_usd
        FROM energy_consumption ec
        INNER JOIN devices d ON ec.device_id = d.id
        WHERE d.user_id = $1
          AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
        GROUP BY TO_CHAR(ec.date, 'YYYY-MM')
      )
      SELECT 
        month,
        COALESCE(consumption_kwh, 0)::DECIMAL(10, 2) AS consumption_kwh,
        COALESCE(cost_usd, 0)::DECIMAL(10, 2) AS cost_usd
      FROM monthly_totals
      ORDER BY month DESC
      LIMIT 2;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [userId]);
      return result.rows.map((row) => ({
        month: row.month,
        consumptionKwh: parseFloat(row.consumption_kwh),
        costUsd: parseFloat(row.cost_usd),
      }));
    } catch (error) {
      logger.error('Error fetching monthly summary', {
        error: error instanceof Error ? error.message : String(error),
        userId,
      });
      throw error;
    }
  }

  /**
   * Get statistics data points for a date range
   */
  async getStatistics(
    userId: number,
    dateRange: string,
    startDate?: string,
    endDate?: string
  ): Promise<StatisticsDataPoint[]> {
    let dateFilter = '';
    const params: (number | string)[] = [userId];

    // Build date filter based on range
    switch (dateRange) {
      case 'today':
        dateFilter = "AND ec.date = CURRENT_DATE";
        break;
      case 'this_week':
        dateFilter = "AND ec.date >= DATE_TRUNC('week', CURRENT_DATE)";
        break;
      case 'last_month':
        dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') AND ec.date < DATE_TRUNC('month', CURRENT_DATE)";
        break;
      case 'last_3_months':
        dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '3 months')";
        break;
      case 'last_6_months':
        dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '6 months')";
        break;
      case 'this_year':
        dateFilter = "AND ec.date >= DATE_TRUNC('year', CURRENT_DATE)";
        break;
      case 'last_year':
        dateFilter = "AND ec.date >= DATE_TRUNC('year', CURRENT_DATE - INTERVAL '1 year') AND ec.date < DATE_TRUNC('year', CURRENT_DATE)";
        break;
      case 'custom':
        if (startDate && endDate) {
          params.push(startDate, endDate);
          dateFilter = "AND ec.date >= $2 AND ec.date <= $3";
        }
        break;
      case 'all_time':
      default:
        dateFilter = '';
        break;
    }

    // Determine grouping based on date range
    let groupBy = "TO_CHAR(ec.date, 'YYYY-MM')"; // Default: monthly
    if (dateRange === 'today' || dateRange === 'this_week') {
      groupBy = "ec.date::TEXT"; // Daily for short ranges
    }

    const query = `
      SELECT 
        ${groupBy} AS period,
        SUM(ec.consumption_kwh)::DECIMAL(10, 2) AS consumption_kwh,
        SUM(ec.cost_usd)::DECIMAL(10, 2) AS cost_usd
      FROM energy_consumption ec
      INNER JOIN devices d ON ec.device_id = d.id
      WHERE d.user_id = $1
        ${dateFilter}
      GROUP BY ${groupBy}
      ORDER BY period ASC;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, params);
      return result.rows.map((row) => ({
        period: row.period,
        consumptionKwh: parseFloat(row.consumption_kwh),
        costUsd: parseFloat(row.cost_usd),
      }));
    } catch (error) {
      logger.error('Error fetching statistics', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        dateRange,
      });
      throw error;
    }
  }

  /**
   * Get device consumption summary (grouped by device or type)
   */
  async getDeviceConsumption(
    userId: number,
    query: ReportsQuery
  ): Promise<DeviceConsumptionSummary[]> {
    let dateFilter = '';
    const params: (number | string)[] = [userId];

    // Build date filter
    if (query.dateRange) {
      switch (query.dateRange) {
        case 'today':
          dateFilter = "AND ec.date = CURRENT_DATE";
          break;
        case 'this_week':
          dateFilter = "AND ec.date >= DATE_TRUNC('week', CURRENT_DATE)";
          break;
        case 'last_month':
          dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') AND ec.date < DATE_TRUNC('month', CURRENT_DATE)";
          break;
        case 'last_3_months':
          dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '3 months')";
          break;
        case 'last_6_months':
          dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '6 months')";
          break;
        case 'this_year':
          dateFilter = "AND ec.date >= DATE_TRUNC('year', CURRENT_DATE)";
          break;
        case 'last_year':
          dateFilter = "AND ec.date >= DATE_TRUNC('year', CURRENT_DATE - INTERVAL '1 year') AND ec.date < DATE_TRUNC('year', CURRENT_DATE)";
          break;
        case 'custom':
          if (query.startDate && query.endDate) {
            params.push(query.startDate, query.endDate);
            dateFilter = "AND ec.date >= $" + params.length + " AND ec.date <= $" + (params.length + 1);
          }
          break;
        case 'all_time':
        default:
          dateFilter = '';
          break;
      }
    }

    // Additional filters
    let deviceFilter = '';
    if (query.deviceId) {
      params.push(query.deviceId);
      deviceFilter = `AND d.id = $${params.length}`;
    }

    let typeFilter = '';
    if (query.deviceType) {
      params.push(query.deviceType);
      typeFilter = `AND d.type::TEXT = $${params.length}`;
    }

    let roomFilter = '';
    if (query.roomId !== undefined) {
      if (query.roomId === null) {
        roomFilter = 'AND d.room_id IS NULL';
      } else {
        params.push(query.roomId);
        roomFilter = `AND d.room_id = $${params.length}`;
      }
    }

    // Group by device type or individual device
    const groupBy = query.groupBy === 'type' 
      ? 'd.type, d.type::TEXT'
      : 'd.id, d.name, d.type, d.room_id, r.name';

    const selectFields = query.groupBy === 'type'
      ? `
        d.type::TEXT AS device_type,
        COUNT(DISTINCT d.id) AS device_count,
        NULL::INTEGER AS device_id,
        NULL::TEXT AS device_name,
        NULL::INTEGER AS room_id,
        NULL::TEXT AS room_name
      `
      : `
        d.id AS device_id,
        d.name AS device_name,
        d.type::TEXT AS device_type,
        d.room_id AS room_id,
        r.name AS room_name,
        NULL::INTEGER AS device_count
      `;

    const sql = `
      SELECT 
        ${selectFields},
        SUM(ec.consumption_kwh)::DECIMAL(10, 2) AS total_consumption_kwh,
        SUM(ec.cost_usd)::DECIMAL(10, 2) AS total_cost_usd
      FROM energy_consumption ec
      INNER JOIN devices d ON ec.device_id = d.id
      LEFT JOIN rooms r ON d.room_id = r.id
      WHERE d.user_id = $1
        ${dateFilter}
        ${deviceFilter}
        ${typeFilter}
        ${roomFilter}
      GROUP BY ${groupBy}
      ORDER BY total_consumption_kwh DESC;
    `;

    try {
      const result: QueryResult = await this.pool.query(sql, params);
      return result.rows.map((row) => ({
        deviceId: row.device_id || 0,
        deviceName: row.device_name || row.device_type || 'Unknown',
        deviceType: row.device_type,
        roomId: row.room_id,
        roomName: row.room_name,
        totalConsumptionKwh: parseFloat(row.total_consumption_kwh),
        totalCostUsd: parseFloat(row.total_cost_usd),
        deviceCount: row.device_count || undefined,
      }));
    } catch (error) {
      logger.error('Error fetching device consumption', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        query,
      });
      throw error;
    }
  }

  /**
   * Get detailed consumption for a specific device type
   */
  async getDeviceTypeDetails(
    userId: number,
    deviceType: string,
    dateRange?: string,
    startDate?: string,
    endDate?: string
  ): Promise<DeviceConsumptionDetail[]> {
    let dateFilter = '';
    const params: (number | string)[] = [userId, deviceType];

    if (dateRange) {
      switch (dateRange) {
        case 'today':
          dateFilter = "AND ec.date = CURRENT_DATE";
          break;
        case 'this_week':
          dateFilter = "AND ec.date >= DATE_TRUNC('week', CURRENT_DATE)";
          break;
        case 'last_month':
          dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') AND ec.date < DATE_TRUNC('month', CURRENT_DATE)";
          break;
        case 'last_3_months':
          dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '3 months')";
          break;
        case 'last_6_months':
          dateFilter = "AND ec.date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '6 months')";
          break;
        case 'this_year':
          dateFilter = "AND ec.date >= DATE_TRUNC('year', CURRENT_DATE)";
          break;
        case 'last_year':
          dateFilter = "AND ec.date >= DATE_TRUNC('year', CURRENT_DATE - INTERVAL '1 year') AND ec.date < DATE_TRUNC('year', CURRENT_DATE)";
          break;
        case 'custom':
          if (startDate && endDate) {
            params.push(startDate, endDate);
            dateFilter = "AND ec.date >= $3 AND ec.date <= $4";
          }
          break;
        case 'all_time':
        default:
          dateFilter = '';
          break;
      }
    }

    const query = `
      SELECT 
        d.id AS device_id,
        d.name AS device_name,
        d.type::TEXT AS device_type,
        d.room_id AS room_id,
        r.name AS room_name,
        ec.date,
        ec.consumption_kwh,
        ec.cost_usd
      FROM energy_consumption ec
      INNER JOIN devices d ON ec.device_id = d.id
      LEFT JOIN rooms r ON d.room_id = r.id
      WHERE d.user_id = $1
        AND d.type::TEXT = $2
        ${dateFilter}
      ORDER BY ec.date DESC, d.name ASC;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, params);
      return result.rows.map((row) => ({
        deviceId: row.device_id,
        deviceName: row.device_name,
        deviceType: row.device_type,
        roomId: row.room_id,
        roomName: row.room_name,
        consumptionKwh: parseFloat(row.consumption_kwh),
        costUsd: parseFloat(row.cost_usd),
        date: new Date(row.date),
      }));
    } catch (error) {
      logger.error('Error fetching device type details', {
        error: error instanceof Error ? error.message : String(error),
        userId,
        deviceType,
      });
      throw error;
    }
  }

  /**
   * Insert or update energy consumption record
   */
  async upsertEnergyConsumption(
    deviceId: number,
    date: Date,
    consumptionKwh: number,
    costUsd: number
  ): Promise<EnergyConsumption> {
    const query = `
      INSERT INTO energy_consumption (device_id, date, consumption_kwh, cost_usd)
      VALUES ($1, $2, $3, $4)
      ON CONFLICT (device_id, date)
      DO UPDATE SET
        consumption_kwh = EXCLUDED.consumption_kwh,
        cost_usd = EXCLUDED.cost_usd,
        updated_at = CURRENT_TIMESTAMP
      RETURNING *;
    `;

    try {
      const result: QueryResult = await this.pool.query(query, [
        deviceId,
        date.toISOString().split('T')[0],
        consumptionKwh,
        costUsd,
      ]);

      const row = result.rows[0];
      return {
        id: row.id,
        deviceId: row.device_id,
        date: new Date(row.date),
        consumptionKwh: parseFloat(row.consumption_kwh),
        costUsd: parseFloat(row.cost_usd),
        createdAt: new Date(row.created_at),
        updatedAt: new Date(row.updated_at),
      };
    } catch (error) {
      logger.error('Error upserting energy consumption', {
        error: error instanceof Error ? error.message : String(error),
        deviceId,
        date,
      });
      throw error;
    }
  }
}

