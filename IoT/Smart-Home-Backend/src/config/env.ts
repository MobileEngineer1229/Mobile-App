import dotenv from 'dotenv';
import { resolve } from 'path';

// Resolve .env relative to project root (works regardless of cwd)
dotenv.config({ path: resolve(__dirname, '..', '..', '.env') });

interface EnvConfig {
  nodeEnv: string;
  port: number;
  httpsPort?: number;
  enableHttps: boolean;
  sslCertPath?: string;
  sslKeyPath?: string;
  jwtSecret: string;
  jwtExpiresIn: string;
  apiVersion: string;
  autoMigrate: boolean;
  bluetoothGatewayUrl?: string;
  bluetoothGatewayApiKey?: string;
  mqttBrokerUrl?: string;
  mqttUsername?: string;
  mqttPassword?: string;
  mqttClientId?: string;
  emqxApiUrl?: string;
  emqxApiKey?: string;
  emqxApiSecret?: string;
}

/**
 * Validate required environment variables
 */
const validateEnv = (): void => {
  const required = ['JWT_SECRET', 'DB_HOST', 'DB_NAME'];
  const missing = required.filter((key) => !process.env[key]);

  if (missing.length > 0) {
    throw new Error(`Missing required environment variables: ${missing.join(', ')}`);
  }
};

validateEnv();

export const env: EnvConfig = {
  nodeEnv: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT || '3000', 10),
  httpsPort: process.env.HTTPS_PORT ? parseInt(process.env.HTTPS_PORT, 10) : undefined,
  enableHttps: process.env.ENABLE_HTTPS === 'true',
  sslCertPath: process.env.SSL_CERT_PATH,
  sslKeyPath: process.env.SSL_KEY_PATH,
  jwtSecret: process.env.JWT_SECRET || '',
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '7d',
  apiVersion: process.env.API_VERSION || 'v1',
  autoMigrate: process.env.AUTO_MIGRATE !== 'false', // Default to true, set to 'false' to disable
  bluetoothGatewayUrl: process.env.BLUETOOTH_GATEWAY_URL,
  bluetoothGatewayApiKey: process.env.BLUETOOTH_GATEWAY_API_KEY,
  mqttBrokerUrl: process.env.MQTT_BROKER_URL || 'mqtt://localhost:1883',
  mqttUsername: process.env.MQTT_USERNAME || undefined,
  mqttPassword: process.env.MQTT_PASSWORD || undefined,
  mqttClientId: process.env.MQTT_CLIENT_ID || `smarthome-backend-${process.pid}`,
  emqxApiUrl: process.env.EMQX_API_URL || 'http://localhost:18083',
  emqxApiKey: process.env.EMQX_API_KEY || 'admin',
  emqxApiSecret: process.env.EMQX_API_SECRET || 'public',
};

