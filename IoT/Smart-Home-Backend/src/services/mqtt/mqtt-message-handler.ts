import { Pool } from 'pg';
import { MqttClientService } from './mqtt-client';
import { SceneAutomationEngine } from '../scene-automation-engine';
import { DeviceEventBus } from '../sse/device-event-bus';
import { DeviceStatusWebSocketServer } from '../websocket/websocket-server';
import logger from '../../utils/logger';

/** Map productId prefix to device type */
const PRODUCT_TYPE_MAP: Record<string, string> = {
  '1001': 'lamp',
  '1002': 'electronics',
  '2001': 'sensor',
  '2002': 'sensor',
  '3001': 'camera',
  '4001': 'speaker',
  '5001': 'thermostat',
  '6001': 'lock',
};

/**
 * MQTT Message Handler
 * Subscribes to FastBee wildcard topics and processes incoming device messages.
 * Updates device metadata in the database when devices report properties or status changes.
 *
 * Subscribed topics:
 * - /fastbee/+/+/property/post  → Device reports property values
 * - /fastbee/+/+/status         → Device online/offline status
 * - /fastbee/+/+/info/post      → Device info (firmware, model, etc.)
 */
export class MqttMessageHandler {
  private mqttClient: MqttClientService;
  private pool: Pool;
  private wsServer?: DeviceStatusWebSocketServer;

  constructor(pool: Pool, wsServer?: DeviceStatusWebSocketServer) {
    this.mqttClient = MqttClientService.getInstance();
    this.pool = pool;
    this.wsServer = wsServer;
  }

  async initialize(): Promise<void> {
    await this.mqttClient.subscribe('/fastbee/+/+/property/post');
    await this.mqttClient.subscribe('/fastbee/+/+/status');
    await this.mqttClient.subscribe('/fastbee/+/+/info/post');

    this.mqttClient.on('message', (topic: string, payload: Buffer) => {
      this.handleMessage(topic, payload).catch((error) => {
        logger.errorWithEmoji('❌', 'MQTT message handler error', 'MQTT_HANDLER', {
          topic,
          error: error instanceof Error ? error.message : String(error),
        });
      });
    });

    logger.infoWithEmoji('📡', 'MQTT message handler initialized', 'MQTT_HANDLER');
  }

  private async handleMessage(topic: string, payload: Buffer): Promise<void> {
    const parts = topic.split('/');
    // Expected: /fastbee/{productId}/{deviceNum}/...
    // Split gives: ['', 'fastbee', productId, deviceNum, ...]
    if (parts.length < 5 || parts[1] !== 'fastbee') {
      return;
    }

    const productId = parts[2];
    const deviceNum = parts[3];
    const messageType = parts.slice(4).join('/');

    let data: Record<string, unknown>;
    try {
      data = JSON.parse(payload.toString());
    } catch {
      logger.warnWithEmoji('⚠️', 'Invalid JSON in MQTT message', 'MQTT_HANDLER', {
        topic,
        payload: payload.toString().substring(0, 200),
      });
      return;
    }

    logger.infoWithEmoji('📥', `MQTT message received: ${messageType}`, 'MQTT_HANDLER', {
      productId,
      deviceNum,
      messageType,
    });

    switch (messageType) {
      case 'property/post':
        await this.handlePropertyReport(productId, deviceNum, data);
        break;
      case 'status':
        await this.handleStatusChange(productId, deviceNum, data);
        break;
      case 'info/post':
        await this.handleDeviceInfo(productId, deviceNum, data);
        break;
      default:
        logger.warnWithEmoji('⚠️', `Unknown MQTT message type: ${messageType}`, 'MQTT_HANDLER');
    }
  }

  /**
   * Handle device property report — merge reported properties into device metadata
   */
  private async handlePropertyReport(
    productId: string,
    deviceNum: string,
    data: Record<string, unknown>
  ): Promise<void> {
    const updated = await this.updateDeviceMetadata(productId, deviceNum, {
      lastProperties: data,
      lastMqttUpdate: new Date().toISOString(),
    });

    if (!updated) {
      await this.autoRegisterDevice(productId, deviceNum, data);
    }

    // Persist each property to device_states table
    await this.upsertDeviceStates(productId, deviceNum, data, 'mqtt');

    // Emit SSE event
    DeviceEventBus.getInstance().emitDeviceUpdate(productId, deviceNum, 'property', data);

    // Broadcast to WebSocket clients
    await this.broadcastPropertyToWebSocket(productId, deviceNum, data);

    // Evaluate automation conditions based on new property data
    try {
      const engine = SceneAutomationEngine.getInstance();
      await engine.evaluateForPropertyUpdate(productId, deviceNum, data);
    } catch (error) {
      logger.error('Error triggering automation evaluation for property update:', error);
    }
  }

  /**
   * Handle device online/offline status change
   */
  private async handleStatusChange(
    productId: string,
    deviceNum: string,
    data: Record<string, unknown>
  ): Promise<void> {
    const isOnline = data.status === 1 || data.status === 'online';
    const newStatus = isOnline ? 'online' : 'offline';

    // Update both device status column and metadata
    const result = await this.pool.query(
      `UPDATE devices
       SET status = $1::device_status_enum,
           last_seen = CASE WHEN $1 = 'online' THEN NOW() ELSE last_seen END,
           metadata = jsonb_set(
             COALESCE(metadata, '{}'::jsonb),
             '{mqttOnline}',
             to_jsonb($1::text)
           ),
           updated_at = NOW()
       WHERE metadata->>'productId' = $2
         AND metadata->>'deviceNum' = $3
       RETURNING id`,
      [newStatus, productId, deviceNum]
    );

    if (result.rowCount === 0) {
      await this.autoRegisterDevice(productId, deviceNum, data);
    } else {
      logger.infoWithEmoji('📶', `Device ${newStatus}: ${productId}/${deviceNum}`, 'MQTT_HANDLER');

      // Evaluate automation conditions based on status change
      try {
        const engine = SceneAutomationEngine.getInstance();
        await engine.evaluateForStatusChange(productId, deviceNum, data);
      } catch (error) {
        logger.error('Error triggering automation evaluation for status change:', error);
      }
    }

    // Emit SSE event
    DeviceEventBus.getInstance().emitDeviceUpdate(productId, deviceNum, 'status', data);

    // Broadcast to WebSocket clients
    await this.broadcastStatusToWebSocket(productId, deviceNum, newStatus);
  }

  /**
   * Handle device info report (firmware, model, etc.)
   */
  private async handleDeviceInfo(
    productId: string,
    deviceNum: string,
    data: Record<string, unknown>
  ): Promise<void> {
    await this.updateDeviceMetadata(productId, deviceNum, {
      deviceInfo: data,
      lastInfoUpdate: new Date().toISOString(),
    });

    DeviceEventBus.getInstance().emitDeviceUpdate(productId, deviceNum, 'info', data);
  }

  /**
   * Merge key-value pairs into device metadata JSONB.
   * Returns true if a device was found and updated.
   */
  private async updateDeviceMetadata(
    productId: string,
    deviceNum: string,
    updates: Record<string, unknown>
  ): Promise<boolean> {
    const result = await this.pool.query(
      `UPDATE devices
       SET metadata = COALESCE(metadata, '{}'::jsonb) || $1::jsonb,
           updated_at = NOW()
       WHERE metadata->>'productId' = $2
         AND metadata->>'deviceNum' = $3
       RETURNING id`,
      [JSON.stringify(updates), productId, deviceNum]
    );

    if (result.rowCount === 0) {
      logger.warnWithEmoji('⚠️', 'No device found for MQTT metadata update', 'MQTT_HANDLER', {
        productId,
        deviceNum,
      });
      return false;
    }
    return true;
  }

  /**
   * Auto-register an unknown device that published on a FastBee topic.
   * Assigns to the first home found in the system.
   */
  private async autoRegisterDevice(
    productId: string,
    deviceNum: string,
    data: Record<string, unknown>
  ): Promise<void> {
    // Find a default home to assign the device to
    const homeResult = await this.pool.query(`SELECT id FROM homes ORDER BY id ASC LIMIT 1`);
    if (homeResult.rowCount === 0) {
      logger.warnWithEmoji(
        '⚠️',
        'Cannot auto-register device — no homes exist in the system',
        'MQTT_HANDLER',
        { productId, deviceNum }
      );
      return;
    }

    const homeId = homeResult.rows[0].id;
    const deviceType = PRODUCT_TYPE_MAP[productId] || 'electronics';
    const deviceName = `${deviceType}-${deviceNum}`;
    const isOnline = data.status === 1 || data.status === 'online';

    const metadata = {
      protocol: 'mqtt',
      productId,
      deviceNum,
      autoRegistered: true,
      registeredAt: new Date().toISOString(),
    };

    // Find the home owner to assign as initial device owner
    const ownerResult = await this.pool.query(
      `SELECT user_id FROM homes WHERE id = $1`,
      [homeId]
    );
    const userId = ownerResult.rows[0]?.user_id || null;

    await this.pool.query(
      `INSERT INTO devices (user_id, name, type, status, home_id, metadata, created_at, updated_at)
       VALUES ($1, $2, $3::device_type_enum, $4::device_status_enum, $5, $6::jsonb, NOW(), NOW())
       ON CONFLICT ((metadata->>'productId'), (metadata->>'deviceNum'))
         WHERE metadata->>'protocol' = 'mqtt'
       DO UPDATE SET status = EXCLUDED.status, updated_at = NOW()`,
      [userId, deviceName, deviceType, isOnline ? 'online' : 'unknown', homeId, JSON.stringify(metadata)]
    );

    logger.infoWithEmoji(
      '🆕',
      `Auto-registered device: ${deviceName} (${productId}/${deviceNum})`,
      'MQTT_HANDLER',
      { productId, deviceNum, deviceType, homeId }
    );
  }

  /**
   * Upsert property values into device_states table.
   * Each key in `data` becomes a row in device_states.
   */
  private async upsertDeviceStates(
    productId: string,
    deviceNum: string,
    data: Record<string, unknown>,
    source: string
  ): Promise<void> {
    // Find device id
    const deviceResult = await this.pool.query(
      `SELECT id FROM devices WHERE metadata->>'productId' = $1 AND metadata->>'deviceNum' = $2 LIMIT 1`,
      [productId, deviceNum]
    );
    if (deviceResult.rowCount === 0) return;

    const deviceId = deviceResult.rows[0].id;

    for (const [key, value] of Object.entries(data)) {
      try {
        await this.pool.query(
          `INSERT INTO device_states (device_id, property_key, property_value, source, updated_at)
           VALUES ($1, $2, $3::jsonb, $4, NOW())
           ON CONFLICT (device_id, property_key)
           DO UPDATE SET property_value = $3::jsonb, source = $4, updated_at = NOW()`,
          [deviceId, key, JSON.stringify(value), source]
        );
      } catch (error) {
        logger.error(`Error upserting device state ${key}:`, error);
      }
    }
  }

  /**
   * Broadcast device property update to WebSocket clients
   */
  private async broadcastPropertyToWebSocket(
    productId: string,
    deviceNum: string,
    properties: Record<string, unknown>
  ): Promise<void> {
    if (!this.wsServer) return;

    // Find device and user_id
    const result = await this.pool.query(
      `SELECT id, user_id FROM devices WHERE metadata->>'productId' = $1 AND metadata->>'deviceNum' = $2 LIMIT 1`,
      [productId, deviceNum]
    );

    if (result.rowCount === 0) return;

    const userId = result.rows[0].user_id;
    if (!userId) return;

    this.wsServer.broadcastDeviceProperty(userId, {
      productId,
      deviceNum,
      properties,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Broadcast device status update to WebSocket clients
   */
  private async broadcastStatusToWebSocket(
    productId: string,
    deviceNum: string,
    status: string
  ): Promise<void> {
    if (!this.wsServer) return;

    // Find device and user_id
    const result = await this.pool.query(
      `SELECT id, user_id FROM devices WHERE metadata->>'productId' = $1 AND metadata->>'deviceNum' = $2 LIMIT 1`,
      [productId, deviceNum]
    );

    if (result.rowCount === 0) return;

    const userId = result.rows[0].user_id;
    if (!userId) return;

    this.wsServer.broadcastDeviceStatus(userId, {
      productId,
      deviceNum,
      status,
      timestamp: new Date().toISOString(),
    });
  }
}
