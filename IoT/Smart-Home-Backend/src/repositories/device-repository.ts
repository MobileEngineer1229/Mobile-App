import { Pool } from 'pg';
import { Device, CreateDeviceInput, UpdateDeviceInput, DeviceListQuery, DeviceStatus } from '../models/device';
import { NotFoundError, ConflictError } from '../utils/errors';
import { IDeviceRepository } from '../domain/repositories/IDeviceRepository';

/**
 * Device repository for database operations
 * Implements IDeviceRepository interface
 */
export class DeviceRepository implements IDeviceRepository {
  constructor(private pool: Pool) {}

  /**
   * Find device by ID
   */
  async findById(id: number, userId?: number): Promise<Device | null> {
    let query = 'SELECT * FROM devices WHERE id = $1';
    const params: unknown[] = [id];

    if (userId !== undefined) {
      query += ' AND user_id = $2';
      params.push(userId);
    }

    const result = await this.pool.query(query, params);
    if (result.rows.length === 0) {
      return null;
    }

    const row = result.rows[0];
    return {
      id: row.id,
      userId: row.user_id,
      name: row.name,
      type: row.type,
      status: row.status,
      macAddress: row.mac_address,
      ipAddress: row.ip_address,
      roomId: row.room_id,
      homeId: row.home_id,
      lastSeen: row.last_seen,
      metadata: row.metadata,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  /**
   * Find device by MAC address
   */
  async findByMacAddress(macAddress: string, userId?: number): Promise<Device | null> {
    let query = 'SELECT * FROM devices WHERE mac_address = $1';
    const params: unknown[] = [macAddress];

    if (userId !== undefined) {
      query += ' AND user_id = $2';
      params.push(userId);
    }

    const result = await this.pool.query(query, params);
    if (result.rows.length === 0) {
      return null;
    }

    const row = result.rows[0];
    return {
      id: row.id,
      userId: row.user_id,
      name: row.name,
      type: row.type,
      status: row.status,
      macAddress: row.mac_address,
      ipAddress: row.ip_address,
      roomId: row.room_id,
      homeId: row.home_id,
      lastSeen: row.last_seen,
      metadata: row.metadata,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  /**
   * Find all devices for a user with pagination and filters
   */
  async findAll(userId: number, query: DeviceListQuery): Promise<{ devices: Device[]; total: number }> {
    const page = query.page || 1;
    const limit = query.limit || 10;
    const offset = (page - 1) * limit;

    let whereClause = 'WHERE devices.user_id = $1';
    const params: unknown[] = [userId];
    let paramIndex = 2;

    if (query.type) {
      whereClause += ` AND devices.type = $${paramIndex}`;
      params.push(query.type);
      paramIndex++;
    }

    if (query.status) {
      whereClause += ` AND devices.status = $${paramIndex}`;
      params.push(query.status);
      paramIndex++;
    }

    if (query.search) {
      whereClause += ` AND (devices.name LIKE $${paramIndex} OR devices.mac_address LIKE $${paramIndex + 1})`;
      const searchPattern = `%${query.search}%`;
      params.push(searchPattern, searchPattern);
      paramIndex += 2;
    }

    if (query.roomId !== undefined) {
      if (query.roomId === null) {
        // Filter for devices without a room
        whereClause += ` AND devices.room_id IS NULL`;
      } else {
        whereClause += ` AND devices.room_id = $${paramIndex}`;
        params.push(query.roomId);
        paramIndex++;
      }
    }

    // Filter by homeId (direct home_id column or via rooms table join)
    let joinClause = '';
    if (query.homeId !== undefined) {
      // Use LEFT JOIN to include devices without rooms (room_id IS NULL)
      // Show devices in rooms belonging to the home OR devices without rooms (belonging to the user)
      joinClause = 'LEFT JOIN rooms r ON devices.room_id = r.id';
      // Include devices without rooms - they belong to the user and should be shown for their primary home
      // OR devices in rooms belonging to the home
      whereClause += ` AND (r.home_id = $${paramIndex} OR devices.home_id = $${paramIndex} OR (devices.room_id IS NULL AND devices.user_id = $1))`;
      params.push(query.homeId);
      paramIndex++;
    }

    // Get total count
    // Use DISTINCT when there's a join to avoid counting duplicates
    const countQuery = joinClause 
      ? `SELECT COUNT(DISTINCT devices.id) as count FROM devices ${joinClause} ${whereClause}`
      : `SELECT COUNT(*) as count FROM devices ${whereClause}`;
    const countResult = await this.pool.query(countQuery, params);
    const total = parseInt(countResult.rows[0].count, 10) || 0;

    // Get devices
    const limitParamIndex = paramIndex;
    const offsetParamIndex = paramIndex + 1;
    const devicesResult = await this.pool.query(
      `SELECT devices.* FROM devices ${joinClause} ${whereClause} ORDER BY devices.created_at DESC LIMIT $${limitParamIndex} OFFSET $${offsetParamIndex}`,
      [...params, limit, offset]
    );

    // Map database rows to Device model (convert snake_case to camelCase)
    const devices = devicesResult.rows.map((row) => ({
      id: row.id,
      userId: row.user_id,
      name: row.name,
      type: row.type,
      status: row.status,
      macAddress: row.mac_address,
      ipAddress: row.ip_address,
      roomId: row.room_id,
      homeId: row.home_id,
      lastSeen: row.last_seen,
      metadata: row.metadata,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    }));

    return { devices, total };
  }

  /**
   * Find devices by home ID
   */
  async findByHomeId(userId: number, homeId: number, query?: DeviceListQuery): Promise<{ devices: Device[]; total: number }> {
    const page = query?.page || 1;
    const limit = query?.limit || 10;
    const offset = (page - 1) * limit;

    let whereClause = `WHERE devices.user_id = $1 AND (devices.home_id = $2 OR EXISTS (
      SELECT 1 FROM rooms r 
      WHERE r.id = devices.room_id 
      AND r.home_id = $2
      AND r.user_id = devices.user_id
    ))`;
    const params: unknown[] = [userId, homeId];
    let paramIndex = 3;

    if (query?.type) {
      whereClause += ` AND devices.type = $${paramIndex}`;
      params.push(query.type);
      paramIndex++;
    }

    if (query?.status) {
      whereClause += ` AND devices.status = $${paramIndex}`;
      params.push(query.status);
      paramIndex++;
    }

    if (query?.search) {
      whereClause += ` AND (devices.name LIKE $${paramIndex} OR devices.mac_address LIKE $${paramIndex + 1})`;
      const searchPattern = `%${query.search}%`;
      params.push(searchPattern, searchPattern);
      paramIndex += 2;
    }

    if (query?.roomId !== undefined) {
      if (query.roomId === null) {
        whereClause += ` AND devices.room_id IS NULL`;
      } else {
        whereClause += ` AND devices.room_id = $${paramIndex}`;
        params.push(query.roomId);
        paramIndex++;
      }
    }

    // Get total count
    const countResult = await this.pool.query(
      `SELECT COUNT(*) as count FROM devices ${whereClause}`,
      params
    );
    const total = parseInt(countResult.rows[0].count, 10) || 0;

    // Get devices
    const devicesResult = await this.pool.query(
      `SELECT devices.* FROM devices ${whereClause} ORDER BY devices.created_at DESC LIMIT $${paramIndex} OFFSET $${paramIndex + 1}`,
      [...params, limit, offset]
    );

    // Map database rows to Device model
    const devices = devicesResult.rows.map((row) => ({
      id: row.id,
      userId: row.user_id,
      name: row.name,
      type: row.type,
      status: row.status,
      macAddress: row.mac_address,
      ipAddress: row.ip_address,
      roomId: row.room_id,
      homeId: row.home_id,
      lastSeen: row.last_seen,
      metadata: row.metadata,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    }));

    return { devices, total };
  }

  /**
   * Create new device
   */
  async create(userId: number, input: CreateDeviceInput): Promise<Device> {
    // Check if MAC address already exists for this user
    const existingDevice = await this.findByMacAddress(input.macAddress, userId);
    if (existingDevice) {
      throw new ConflictError('Device with this MAC address already exists');
    }

    // Determine home_id (REQUIRED - never null):
    // 1. If homeId is explicitly provided, use it (preferred)
    // 2. If roomId is provided, get home_id from the room
    // 3. Otherwise, get user's primary home_id (fallback)
    let homeId: number | null = input.homeId || null;

    if (!homeId && input.roomId) {
      // Get home_id from room
      const roomResult = await this.pool.query(
        'SELECT home_id FROM rooms WHERE id = $1 AND user_id = $2',
        [input.roomId, userId]
      );
      if (roomResult.rows.length > 0 && roomResult.rows[0].home_id) {
        homeId = roomResult.rows[0].home_id;
      }
    }

    if (!homeId) {
      // Get user's primary home_id (fallback - should always exist)
      const homeResult = await this.pool.query(
        'SELECT id FROM homes WHERE user_id = $1 AND is_primary = true LIMIT 1',
        [userId]
      );
      if (homeResult.rows.length > 0) {
        homeId = homeResult.rows[0].id;
      }
    }

    // Validate that we have a homeId (REQUIRED - never null)
    if (!homeId) {
      throw new Error('Cannot create device: No home found. User must have at least one home.');
    }

    const metadataJson = input.metadata ? JSON.stringify(input.metadata) : null;

    const result = await this.pool.query(
      `INSERT INTO devices (user_id, name, type, status, mac_address, ip_address, room_id, home_id, metadata, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
       RETURNING id`,
      [
        userId,
        input.name,
        input.type,
        DeviceStatus.UNKNOWN,
        input.macAddress,
        input.ipAddress || null,
        input.roomId || null,
        homeId,
        metadataJson,
      ]
    );

    const insertId = result.rows[0].id;
    const device = await this.findById(insertId, userId);

    if (!device) {
      throw new Error('Failed to create device');
    }

    return device;
  }

  /**
   * Update device
   */
  async update(id: number, userId: number, input: UpdateDeviceInput): Promise<Device> {
    const updates: string[] = [];
    const values: unknown[] = [];
    let paramIndex = 1;

    if (input.name !== undefined) {
      updates.push(`name = $${paramIndex}`);
      values.push(input.name);
      paramIndex++;
    }
    if (input.status !== undefined) {
      updates.push(`status = $${paramIndex}`);
      values.push(input.status);
      paramIndex++;
    }
    if (input.ipAddress !== undefined) {
      updates.push(`ip_address = $${paramIndex}`);
      values.push(input.ipAddress);
      paramIndex++;
    }
    if (input.metadata !== undefined) {
      updates.push(`metadata = $${paramIndex}`);
      values.push(JSON.stringify(input.metadata));
      paramIndex++;
    }
    if (input.roomId !== undefined) {
      updates.push(`room_id = $${paramIndex}`);
      values.push(input.roomId);
      paramIndex++;
    }
    if (input.homeId !== undefined) {
      updates.push(`home_id = $${paramIndex}`);
      values.push(input.homeId);
      paramIndex++;
    }

    if (updates.length === 0) {
      const device = await this.findById(id, userId);
      if (!device) {
        throw new NotFoundError('Device');
      }
      return device;
    }

    // updated_at is automatically updated by trigger
    values.push(id, userId);

    await this.pool.query(
      `UPDATE devices SET ${updates.join(', ')} WHERE id = $${paramIndex} AND user_id = $${paramIndex + 1}`,
      values
    );

    const device = await this.findById(id, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    return device;
  }

  /**
   * Update device last seen timestamp
   */
  async updateLastSeen(id: number, userId: number): Promise<void> {
    await this.pool.query(
      'UPDATE devices SET last_seen = CURRENT_TIMESTAMP, status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2 AND user_id = $3',
      [DeviceStatus.ONLINE, id, userId]
    );
  }

  /**
   * Delete device
   */
  async delete(id: number, userId: number): Promise<void> {
    const result = await this.pool.query(
      'DELETE FROM devices WHERE id = $1 AND user_id = $2',
      [id, userId]
    );
    const affectedRows = result.rowCount || 0;

    if (affectedRows === 0) {
      throw new NotFoundError('Device');
    }
  }

  /**
   * Find devices by category
   */
  async findByCategory(
    userId: number,
    category: string,
    roomId?: number | null
  ): Promise<{ devices: Device[]; total: number }> {
    let whereClause = 'WHERE devices.user_id = $1';
    const params: unknown[] = [userId];
    let paramIndex = 2;

    // Build category filter based on metadata.category or device name/type
    let categoryFilter = '';
    const categoryLower = category.toLowerCase();

    if (categoryLower === 'lightning') {
      // Match devices with category='lightning' in metadata or name/type contains lamp/light
      categoryFilter = ` AND (
        metadata->>'category' = 'lightning' OR
        LOWER(name) LIKE '%lamp%' OR LOWER(name) LIKE '%light%' OR
        LOWER(name) LIKE '%bulb%' OR LOWER(name) LIKE '%lighting%'
      )`;
    } else if (categoryLower === 'cameras') {
      // Match devices with category='cameras' in metadata or name/type contains camera/cctv
      categoryFilter = ` AND (
        metadata->>'category' = 'cameras' OR
        LOWER(name) LIKE '%camera%' OR LOWER(name) LIKE '%cctv%' OR
        LOWER(name) LIKE '%webcam%' OR LOWER(name) LIKE '%security%'
      )`;
    } else if (categoryLower === 'electrical') {
      // Match devices with category='electrical' in metadata or other devices
      categoryFilter = ` AND (
        metadata->>'category' = 'electrical' OR
        (metadata->>'category' IS NULL AND 
         LOWER(name) NOT LIKE '%lamp%' AND LOWER(name) NOT LIKE '%light%' AND
         LOWER(name) NOT LIKE '%camera%' AND LOWER(name) NOT LIKE '%cctv%' AND
         LOWER(name) NOT LIKE '%speaker%' AND LOWER(name) NOT LIKE '%air%' AND
         LOWER(name) NOT LIKE '%conditioner%')
      )`;
    } else {
      // Unknown category - return empty result or all devices
      // For now, return empty result for unknown categories
      categoryFilter = ` AND 1=0`; // Return no results for unknown categories
    }

    whereClause += categoryFilter;

    // Add room filter if specified
    if (roomId !== undefined) {
      if (roomId === null) {
        whereClause += ` AND room_id IS NULL`;
      } else {
        whereClause += ` AND room_id = $${paramIndex}`;
        params.push(roomId);
        paramIndex++;
      }
    }

    // Get total count
    const countResult = await this.pool.query(
      `SELECT COUNT(*) as count FROM devices ${whereClause}`,
      params
    );
    const total = parseInt(countResult.rows[0].count, 10) || 0;

    // Get devices (no pagination limit for category view)
    const devicesResult = await this.pool.query(
      `SELECT * FROM devices ${whereClause} ORDER BY created_at DESC`,
      params
    );

    // Map database rows to Device model
    const devices = devicesResult.rows.map((row) => ({
      id: row.id,
      userId: row.user_id,
      name: row.name,
      type: row.type,
      status: row.status,
      macAddress: row.mac_address,
      ipAddress: row.ip_address,
      roomId: row.room_id,
      homeId: row.home_id,
      lastSeen: row.last_seen,
      metadata: row.metadata,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    }));

    return { devices, total };
  }
}

