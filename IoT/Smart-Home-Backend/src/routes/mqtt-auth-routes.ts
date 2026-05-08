import { Router, Request, Response } from 'express';
import { Pool } from 'pg';
import { authenticateMqttClient } from '../controllers/mqtt-auth-controller';
import { authenticate } from '../middleware/auth';
import { MqttClientService } from '../services/mqtt/mqtt-client';
import { EmqxApiService } from '../services/emqx/emqx-api-service';
import logger from '../utils/logger';
import { getPool } from '../config/database';
import { env } from '../config/env';

const router: Router = Router();

/**
 * EMQX HTTP Authentication Webhook
 * POST /mqtt/auth
 *
 * EMQX sends client credentials here for validation.
 * No JWT auth required — EMQX itself is the caller.
 */
router.post('/auth', authenticateMqttClient);

/**
 * Verify an MQTT device is reachable before registration.
 * POST /mqtt/verify-device
 * Body: { productId: string, deviceNum: string }
 *
 * Publishes a ping to the device's property/set topic and listens for
 * a response on property/post within a timeout.
 * Returns { reachable: true/false }.
 *
 * Requires JWT auth (called from mobile app).
 */
router.post('/verify-device', authenticate, async (req: Request, res: Response) => {
  const { productId, deviceNum } = req.body;

  if (!productId || !deviceNum) {
    res.status(400).json({ success: false, error: 'productId and deviceNum are required' });
    return;
  }

  const mqttClient = MqttClientService.getInstance();
  if (!mqttClient.isConnected()) {
    res.status(503).json({ success: false, error: 'MQTT broker not connected' });
    return;
  }

  const responseTopic = `/fastbee/${productId}/${deviceNum}/property/post`;
  const commandTopic = `/fastbee/${productId}/${deviceNum}/property/set`;
  const TIMEOUT_MS = 5000;
  let responded = false;

  try {
    // Subscribe to the device's response topic temporarily
    await mqttClient.subscribe(responseTopic, 1);

    // Set up listener for response
    const responsePromise = new Promise<boolean>((resolve) => {
      const onMessage = (topic: string, _payload: Buffer) => {
        if (topic === responseTopic) {
          responded = true;
          mqttClient.removeListener('message', onMessage);
          resolve(true);
        }
      };

      mqttClient.on('message', onMessage);

      // Timeout
      setTimeout(() => {
        mqttClient.removeListener('message', onMessage);
        resolve(false);
      }, TIMEOUT_MS);
    });

    // Send a ping command to the device
    await mqttClient.publish(
      commandTopic,
      { command: 'ping', timestamp: Date.now() },
      1
    );

    logger.infoWithEmoji('🔍', `Verifying device ${productId}/${deviceNum}`, 'MQTT_VERIFY');

    const reachable = await responsePromise;

    // Unsubscribe from response topic (the wildcard subscription still covers it)
    // No need to unsubscribe since the handler already has the wildcard

    logger.infoWithEmoji(
      reachable ? '✅' : '⚠️',
      `Device ${productId}/${deviceNum} ${reachable ? 'reachable' : 'not reachable'}`,
      'MQTT_VERIFY'
    );

    res.json({
      success: true,
      data: {
        productId,
        deviceNum,
        reachable,
      },
    });
  } catch (error) {
    logger.error('MQTT verify-device error:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to verify device',
    });
  }
});

/**
 * Send a command to an MQTT device and log it.
 * POST /mqtt/command
 * Body: { deviceId: number, commandType: string, payload: object, source?: string }
 */
router.post('/command', authenticate, async (req: Request, res: Response) => {
  const { deviceId, commandType, payload, source } = req.body;
  const userId = (req as any).user.id;

  if (!deviceId || !commandType || !payload) {
    res.status(400).json({ success: false, error: 'deviceId, commandType, and payload are required' });
    return;
  }

  const pool: Pool = getPool();

  try {
    // Look up device to get MQTT topic info
    const deviceResult = await pool.query(
      `SELECT id, metadata FROM devices WHERE id = $1`,
      [deviceId]
    );

    if (deviceResult.rowCount === 0) {
      res.status(404).json({ success: false, error: 'Device not found' });
      return;
    }

    const device = deviceResult.rows[0];
    const metadata = device.metadata || {};
    const productId = metadata.productId;
    const deviceNum = metadata.deviceNum;

    if (!productId || !deviceNum) {
      res.status(400).json({ success: false, error: 'Device is not an MQTT device (missing productId/deviceNum)' });
      return;
    }

    // Publish command via MQTT
    const mqttClient = MqttClientService.getInstance();
    if (!mqttClient.isConnected()) {
      res.status(503).json({ success: false, error: 'MQTT broker not connected' });
      return;
    }

    const commandTopic = `/fastbee/${productId}/${deviceNum}/property/set`;
    await mqttClient.publish(commandTopic, payload, 1);

    // Log the command
    const commandResult = await pool.query(
      `INSERT INTO device_commands (device_id, user_id, command_type, payload, source, status, created_at)
       VALUES ($1, $2, $3, $4::jsonb, $5, 'sent', NOW())
       RETURNING id`,
      [deviceId, userId, commandType, JSON.stringify(payload), source || 'app']
    );

    logger.infoWithEmoji('📤', `Command sent to ${productId}/${deviceNum}: ${commandType}`, 'MQTT_CMD', { deviceId, payload });

    res.json({
      success: true,
      data: {
        commandId: commandResult.rows[0].id,
        topic: commandTopic,
      },
    });
  } catch (error) {
    logger.error('MQTT command error:', error);
    res.status(500).json({ success: false, error: 'Failed to send command' });
  }
});

/**
 * Get current states of a device.
 * GET /mqtt/device-states/:deviceId
 */
router.get('/device-states/:deviceId', authenticate, async (req: Request, res: Response) => {
  const { deviceId } = req.params;
  const pool: Pool = getPool();

  try {
    const result = await pool.query(
      `SELECT property_key, property_value, source, updated_at
       FROM device_states
       WHERE device_id = $1
       ORDER BY property_key`,
      [deviceId]
    );

    // Convert rows into a key-value map for convenience
    const states: Record<string, unknown> = {};
    for (const row of result.rows) {
      states[row.property_key] = {
        value: row.property_value,
        source: row.source,
        updatedAt: row.updated_at,
      };
    }

    res.json({ success: true, data: states });
  } catch (error) {
    logger.error('Error fetching device states:', error);
    res.status(500).json({ success: false, error: 'Failed to fetch device states' });
  }
});

/**
 * Get command history for a device.
 * GET /mqtt/command-history/:deviceId
 */
router.get('/command-history/:deviceId', authenticate, async (req: Request, res: Response) => {
  const { deviceId } = req.params;
  const limit = Math.min(parseInt(req.query.limit as string) || 50, 200);
  const pool: Pool = getPool();

  try {
    const result = await pool.query(
      `SELECT id, command_type, payload, status, source, response, created_at, acknowledged_at
       FROM device_commands
       WHERE device_id = $1
       ORDER BY created_at DESC
       LIMIT $2`,
      [deviceId, limit]
    );

    res.json({ success: true, data: result.rows });
  } catch (error) {
    logger.error('Error fetching command history:', error);
    res.status(500).json({ success: false, error: 'Failed to fetch command history' });
  }
});

/**
 * Register a new MQTT device.
 * POST /mqtt/register-device
 * Body: { productId: string, deviceNum: string, name?: string, type?: string, homeId?: number }
 *
 * This endpoint:
 * 1. Generates MQTT credentials for the device
 * 2. Creates the credentials in EMQX broker
 * 3. Saves the device to the database with MQTT metadata
 * 4. Returns MQTT connection info for the mobile app
 *
 * Requires JWT auth.
 */
router.post('/register-device', authenticate, async (req: Request, res: Response) => {
  const { productId, deviceNum, name, type, homeId } = req.body;
  const userId = (req as any).user.id;

  if (!productId || !deviceNum) {
    res.status(400).json({
      success: false,
      error: { code: 'VALIDATION_ERROR', message: 'productId and deviceNum are required' },
    });
    return;
  }

  const pool: Pool = getPool();

  try {
    // Check if device already exists with this productId/deviceNum
    const existingDevice = await pool.query(
      `SELECT id, name, metadata FROM devices
       WHERE metadata->>'productId' = $1 AND metadata->>'deviceNum' = $2`,
      [productId, deviceNum]
    );

    if (existingDevice.rowCount && existingDevice.rowCount > 0) {
      const device = existingDevice.rows[0];
      const metadata = device.metadata || {};

      // Ensure user_id and home_id are set on the existing device
      // (fixes devices previously registered with NULL user_id)
      const homeResult = await pool.query(
        `SELECT id FROM homes WHERE user_id = $1 AND is_primary = true LIMIT 1`,
        [userId]
      );
      const existingHomeId = homeResult.rows[0]?.id || homeId || null;
      await pool.query(
        `UPDATE devices SET user_id = $1, home_id = COALESCE($2::integer, home_id), updated_at = NOW() WHERE id = $3`,
        [userId, existingHomeId, device.id]
      );

      // Return existing device info with MQTT credentials from database
      res.json({
        success: true,
        message: 'Device already registered',
        data: {
          deviceId: device.id,
          deviceName: device.name,
          mqtt: {
            brokerUrl: env.mqttBrokerUrl,
            host: new URL(env.mqttBrokerUrl || 'mqtt://localhost:1883').hostname,
            port: parseInt(new URL(env.mqttBrokerUrl || 'mqtt://localhost:1883').port) || 1883,
            username: metadata.mqttUsername || `device-${deviceNum}`,        // ✅ Use stored or fallback
            password: metadata.mqttPassword || null,                         // ✅ Use stored password
            clientId: metadata.mqttClientId || `${productId}-${deviceNum}`,  // ✅ Use stored or fallback
            topics: {
              subscribe: `/fastbee/${productId}/${deviceNum}/property/set`,
              publish: `/fastbee/${productId}/${deviceNum}/property/post`,
              status: `/fastbee/${productId}/${deviceNum}/status`,
            },
          },
        },
      });
      return;
    }

    // Determine device type from productId if not provided
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

    const deviceType = type || PRODUCT_TYPE_MAP[productId] || 'electronics';
    const deviceName = name || `${deviceType.charAt(0).toUpperCase() + deviceType.slice(1)} (${deviceNum})`;

    // Get home ID - use provided, or get user's primary home
    let finalHomeId = homeId;
    if (!finalHomeId) {
      const homeResult = await pool.query(
        `SELECT id FROM homes WHERE user_id = $1 AND is_primary = true LIMIT 1`,
        [userId]
      );
      if (homeResult.rowCount && homeResult.rowCount > 0) {
        finalHomeId = homeResult.rows[0].id;
      } else {
        // Auto-create a default home for the user (new signups have no home yet)
        const createHomeResult = await pool.query(
          `INSERT INTO homes (user_id, name, is_primary) VALUES ($1, 'My Home', true) RETURNING id`,
          [userId]
        );
        finalHomeId = createHomeResult.rows[0].id;
        logger.infoWithEmoji('🏠', `Auto-created default home for user ${userId}`, 'MQTT_REGISTER');
      }
    }

    // Generate MQTT credentials
    const emqxService = EmqxApiService.getInstance();
    const credentials = emqxService.generateDeviceCredentials(productId, deviceNum);

    // Create credentials in EMQX
    const emqxResult = await emqxService.createDeviceCredentials(credentials);
    if (!emqxResult.success) {
      logger.warnWithEmoji(
        '⚠️',
        `Failed to create EMQX credentials (continuing): ${emqxResult.error}`,
        'MQTT_REGISTER'
      );
      // Continue anyway - device can still work with backend auth webhook
    }

    // Generate a random MAC address for MQTT devices (they don't have physical MAC)
    const randomMac = 'MQ:' + Array.from({ length: 5 }, () =>
      Math.floor(Math.random() * 256).toString(16).padStart(2, '0').toUpperCase()
    ).join(':');

    // Create device in database with full MQTT credentials
    const metadata = {
      protocol: 'mqtt',
      productId,
      deviceNum,
      mqttClientId: credentials.clientId,      // ✅ Save Client ID
      mqttUsername: credentials.username,      // ✅ Save Username
      mqttPassword: credentials.password,      // ✅ Save Password
      registeredAt: new Date().toISOString(),
      connectionSource: 'mobile_registration',
    };

    const insertResult = await pool.query(
      `INSERT INTO devices (user_id, name, type, status, mac_address, home_id, metadata, created_at, updated_at)
       VALUES ($1, $2, $3::device_type_enum, 'unknown'::device_status_enum, $4, $5, $6::jsonb, NOW(), NOW())
       RETURNING id, name, type, status, mac_address, home_id, metadata, created_at`,
      [userId, deviceName, deviceType, randomMac, finalHomeId, JSON.stringify(metadata)]
    );

    const newDevice = insertResult.rows[0];

    logger.infoWithEmoji(
      '✅',
      `Registered MQTT device: ${deviceName} (${productId}/${deviceNum})`,
      'MQTT_REGISTER',
      { deviceId: newDevice.id, userId }
    );

    // Return success with MQTT connection info
    res.status(201).json({
      success: true,
      message: 'Device registered successfully',
      data: {
        deviceId: newDevice.id,
        deviceName: newDevice.name,
        deviceType: newDevice.type,
        homeId: newDevice.home_id,
        mqtt: {
          brokerUrl: env.mqttBrokerUrl,
          host: new URL(env.mqttBrokerUrl || 'mqtt://localhost:1883').hostname,
          port: parseInt(new URL(env.mqttBrokerUrl || 'mqtt://localhost:1883').port) || 1883,
          username: credentials.username,
          password: credentials.password,
          clientId: credentials.clientId,
          topics: {
            subscribe: `/fastbee/${productId}/${deviceNum}/property/set`,
            publish: `/fastbee/${productId}/${deviceNum}/property/post`,
            status: `/fastbee/${productId}/${deviceNum}/status`,
          },
        },
      },
    });
  } catch (error: any) {
    logger.error('MQTT device registration error:', error);
    res.status(500).json({
      success: false,
      error: { code: 'REGISTRATION_ERROR', message: error.message || 'Failed to register device' },
    });
  }
});

/**
 * Send MQTT credentials to IoT device via FastBee provisioning topic.
 * POST /mqtt/provision-device
 *
 * Flow:
 * 1. Mobile calls /register-device → gets MQTT credentials
 * 2. Mobile calls /provision-device → backend sends credentials to device via MQTT
 * 3. IoT device receives credentials on provisioning topic
 * 4. IoT device reconnects with proper credentials
 *
 * FastBee Provisioning Topic: /fastbee/{productId}/{deviceNum}/provision
 * The IoT device should subscribe to this topic during initial setup.
 *
 * Requires JWT auth.
 */
router.post('/provision-device', authenticate, async (req: Request, res: Response) => {
  const { productId, deviceNum, mqtt } = req.body;
  const userId = (req as any).user.id;

  if (!productId || !deviceNum) {
    res.status(400).json({
      success: false,
      error: { code: 'VALIDATION_ERROR', message: 'productId and deviceNum are required' },
    });
    return;
  }

  if (!mqtt || !mqtt.username || !mqtt.password || !mqtt.clientId) {
    res.status(400).json({
      success: false,
      error: { code: 'VALIDATION_ERROR', message: 'mqtt credentials (username, password, clientId) are required' },
    });
    return;
  }

  try {
    const mqttClient = MqttClientService.getInstance();

    if (!mqttClient.isConnected()) {
      res.status(503).json({
        success: false,
        error: { code: 'BROKER_DISCONNECTED', message: 'MQTT broker not connected' },
      });
      return;
    }

    // FastBee provisioning topic - device should subscribe to this during setup
    const provisionTopic = `/fastbee/${productId}/${deviceNum}/provision`;

    // Provisioning payload with all credentials device needs to connect
    const provisionPayload = {
      action: 'provision',
      broker: {
        host: new URL(env.mqttBrokerUrl || 'mqtt://localhost:1883').hostname,
        port: parseInt(new URL(env.mqttBrokerUrl || 'mqtt://localhost:1883').port) || 1883,
        protocol: 'mqtt',
      },
      credentials: {
        username: mqtt.username,
        password: mqtt.password,
        clientId: mqtt.clientId,
      },
      topics: {
        subscribe: `/fastbee/${productId}/${deviceNum}/property/set`,
        publish: `/fastbee/${productId}/${deviceNum}/property/post`,
        status: `/fastbee/${productId}/${deviceNum}/status`,
      },
      timestamp: Date.now(),
    };

    // Publish provisioning data to device
    await mqttClient.publish(provisionTopic, provisionPayload, 1);

    logger.infoWithEmoji(
      '📡',
      `Sent provisioning data to device ${productId}/${deviceNum}`,
      'MQTT_PROVISION',
      { userId, topic: provisionTopic }
    );

    // Log this provisioning command
    const pool = getPool();
    await pool.query(
      `INSERT INTO device_commands (device_id, user_id, command_type, payload, source, status, created_at)
       SELECT d.id, $1, 'provision', $2::jsonb, 'app', 'sent', NOW()
       FROM devices d
       WHERE d.metadata->>'productId' = $3 AND d.metadata->>'deviceNum' = $4
       LIMIT 1`,
      [userId, JSON.stringify({ action: 'provision', topic: provisionTopic }), productId, deviceNum]
    );

    res.json({
      success: true,
      message: 'Provisioning data sent to device',
      data: {
        topic: provisionTopic,
        timestamp: provisionPayload.timestamp,
      },
    });
  } catch (error: any) {
    logger.error('MQTT device provisioning error:', error);
    res.status(500).json({
      success: false,
      error: { code: 'PROVISION_ERROR', message: error.message || 'Failed to provision device' },
    });
  }
});

/**
 * Get MQTT connection info for the mobile app.
 * GET /mqtt/connection-info
 *
 * Returns broker URL, credentials, and topic patterns for the mobile app
 * to connect and control devices.
 *
 * Requires JWT auth.
 */
router.get('/connection-info', authenticate, async (req: Request, res: Response) => {
  const userId = (req as any).user.id;

  try {
    const brokerUrl = env.mqttBrokerUrl || 'mqtt://localhost:1883';
    const url = new URL(brokerUrl);

    res.json({
      success: true,
      data: {
        broker: {
          host: url.hostname,
          port: parseInt(url.port) || 1883,
          protocol: url.protocol.replace(':', ''),
          url: brokerUrl,
        },
        credentials: {
          username: 'smartify-android',
          password: env.mqttPassword,
          clientId: `smartify-android-${userId}-${Date.now()}`,
        },
        topics: {
          // Topic patterns for subscribing to all devices
          deviceProperty: '/fastbee/+/+/property/post',
          deviceStatus: '/fastbee/+/+/status',
          deviceInfo: '/fastbee/+/+/info/post',
          // Topic pattern for sending commands
          commandPattern: '/fastbee/{productId}/{deviceNum}/property/set',
        },
      },
    });
  } catch (error: any) {
    logger.error('Error getting MQTT connection info:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get connection info',
    });
  }
});

/**
 * Test EMQX API connection.
 * GET /mqtt/emqx-status
 *
 * Returns EMQX broker status and connected clients.
 *
 * Requires JWT auth.
 */
router.get('/emqx-status', authenticate, async (_req: Request, res: Response) => {
  try {
    const emqxService = EmqxApiService.getInstance();
    const isConnected = await emqxService.testConnection();
    const clients = isConnected ? await emqxService.getConnectedClients() : [];

    res.json({
      success: true,
      data: {
        emqxApiConnected: isConnected,
        mqttBrokerConnected: MqttClientService.getInstance().isConnected(),
        connectedClients: clients.length,
        clients: clients.slice(0, 20), // Return first 20 clients
      },
    });
  } catch (error: any) {
    logger.error('Error getting EMQX status:', error);
    res.status(500).json({
      success: false,
      error: 'Failed to get EMQX status',
    });
  }
});

export default router;
