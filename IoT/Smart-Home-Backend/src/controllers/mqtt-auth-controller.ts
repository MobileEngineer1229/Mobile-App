import { Request, Response } from 'express';
import { env } from '../config/env';
import logger from '../utils/logger';

/**
 * MQTT Auth Controller
 *
 * Handles EMQX HTTP authentication webhook requests.
 * EMQX sends POST requests here to validate MQTT client credentials.
 */
export const authenticateMqttClient = async (req: Request, res: Response): Promise<void> => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      res.status(400).json({ result: 'deny', message: 'Missing credentials' });
      return;
    }

    // Validate against configured MQTT credentials
    const validCredentials = [
      {
        username: env.mqttUsername || 'smartify-backend',
        password: env.mqttPassword || '',
      },
      {
        username: 'smartify-android',
        password: env.mqttPassword || '',
      },
    ];

    const isValid = validCredentials.some(
      (cred) => cred.username === username && cred.password === password
    );

    if (isValid) {
      logger.infoWithEmoji('🔑', `MQTT auth success: ${username}`, 'MQTT_AUTH');
      res.status(200).json({ result: 'allow', is_superuser: false });
      return;
    }

    // Check if it's a device credential (device-{deviceNum})
    if (username.startsWith('device-')) {
      // For device auth, validate against devices table
      const { getPool } = await import('../config/database');
      const pool = getPool();
      const deviceNum = username.replace('device-', '');
      const result = await pool.query(
        `SELECT id, metadata FROM devices WHERE metadata->>'deviceNum' = $1`,
        [deviceNum]
      );

      if (result.rowCount && result.rowCount > 0) {
        const device = result.rows[0];
        const metadata = device.metadata || {};
        const storedPassword = metadata.mqttPassword;

        // Validate password if stored, otherwise allow (backward compatibility)
        if (!storedPassword || storedPassword === password) {
          logger.infoWithEmoji('🔑', `MQTT device auth success: ${username}`, 'MQTT_AUTH');
          res.status(200).json({ result: 'allow', is_superuser: false });
          return;
        } else {
          logger.warnWithEmoji('🚫', `MQTT device auth failed (wrong password): ${username}`, 'MQTT_AUTH');
          res.status(401).json({ result: 'deny', message: 'Invalid device password' });
          return;
        }
      }
    }

    logger.warnWithEmoji('🚫', `MQTT auth denied: ${username}`, 'MQTT_AUTH');
    res.status(401).json({ result: 'deny', message: 'Invalid credentials' });
  } catch (error) {
    logger.error('MQTT auth error:', error);
    res.status(500).json({ result: 'deny', message: 'Internal error' });
  }
};
