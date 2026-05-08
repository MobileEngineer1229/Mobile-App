import axios, { AxiosInstance } from 'axios';
import { env } from '../../config/env';
import logger from '../../utils/logger';
import crypto from 'crypto';

/**
 * NanoMQ API Service
 *
 * Manages device credentials in NanoMQ broker via HTTP API.
 * NanoMQ uses /api/v4/ endpoints for auth user management.
 */

export interface EmqxDeviceCredentials {
  username: string;
  password: string;
  clientId: string;
}

export interface EmqxUserResponse {
  user_id: string;
  is_superuser: boolean;
}

export class EmqxApiService {
  private static instance: EmqxApiService;
  private client: AxiosInstance;

  private constructor() {
    const auth = Buffer.from(`${env.emqxApiKey}:${env.emqxApiSecret}`).toString('base64');

    this.client = axios.create({
      baseURL: env.emqxApiUrl,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Basic ${auth}`,
      },
      timeout: 10000,
    });

    logger.infoWithEmoji('🔧', `EMQX API Service initialized: ${env.emqxApiUrl}`, 'EMQX_API');
  }

  static getInstance(): EmqxApiService {
    if (!EmqxApiService.instance) {
      EmqxApiService.instance = new EmqxApiService();
    }
    return EmqxApiService.instance;
  }

  /**
   * Generate secure credentials for a device
   */
  generateDeviceCredentials(productId: string, deviceNum: string): EmqxDeviceCredentials {
    const username = `device-${deviceNum}`;
    const clientId = `${productId}-${deviceNum}`;
    // Generate a secure random password
    const password = crypto.randomBytes(16).toString('hex');

    return { username, password, clientId };
  }

  /**
   * Create authentication credentials for a device in EMQX
   * Uses the built-in database authentication mechanism
   */
  async createDeviceCredentials(
    credentials: EmqxDeviceCredentials
  ): Promise<{ success: boolean; error?: string }> {
    try {
      // NanoMQ v4 API endpoint for auth user management
      const response = await this.client.post(
        '/api/v4/auth_users',
        {
          username: credentials.username,
          password: credentials.password,
        }
      );

      if (response.status === 201 || response.status === 200) {
        logger.infoWithEmoji(
          '✅',
          `Created EMQX credentials for device: ${credentials.username}`,
          'EMQX_API'
        );
        return { success: true };
      }

      return { success: false, error: `Unexpected status: ${response.status}` };
    } catch (error: any) {
      // If user already exists (409 Conflict), consider it a success
      if (error.response?.status === 409) {
        logger.warnWithEmoji(
          '⚠️',
          `Device credentials already exist: ${credentials.username}`,
          'EMQX_API'
        );
        // Update the existing credentials with new password
        return this.updateDeviceCredentials(credentials);
      }

      logger.errorWithEmoji(
        '❌',
        `Failed to create EMQX credentials: ${error.message}`,
        'EMQX_API',
        { error: error.response?.data }
      );
      return { success: false, error: error.message };
    }
  }

  /**
   * Update existing device credentials in EMQX
   */
  async updateDeviceCredentials(
    credentials: EmqxDeviceCredentials
  ): Promise<{ success: boolean; error?: string }> {
    try {
      const response = await this.client.put(
        `/api/v4/auth_users/${credentials.username}`,
        {
          password: credentials.password,
        }
      );

      if (response.status === 200 || response.status === 204) {
        logger.infoWithEmoji(
          '✅',
          `Updated EMQX credentials for device: ${credentials.username}`,
          'EMQX_API'
        );
        return { success: true };
      }

      return { success: false, error: `Unexpected status: ${response.status}` };
    } catch (error: any) {
      logger.errorWithEmoji(
        '❌',
        `Failed to update EMQX credentials: ${error.message}`,
        'EMQX_API',
        { error: error.response?.data }
      );
      return { success: false, error: error.message };
    }
  }

  /**
   * Delete device credentials from EMQX
   */
  async deleteDeviceCredentials(username: string): Promise<{ success: boolean; error?: string }> {
    try {
      const response = await this.client.delete(
        `/api/v4/auth_users/${username}`
      );

      if (response.status === 204 || response.status === 200) {
        logger.infoWithEmoji('🗑️', `Deleted EMQX credentials for device: ${username}`, 'EMQX_API');
        return { success: true };
      }

      return { success: false, error: `Unexpected status: ${response.status}` };
    } catch (error: any) {
      // If user doesn't exist (404), consider it a success
      if (error.response?.status === 404) {
        logger.warnWithEmoji('⚠️', `Device credentials not found: ${username}`, 'EMQX_API');
        return { success: true };
      }

      logger.errorWithEmoji(
        '❌',
        `Failed to delete EMQX credentials: ${error.message}`,
        'EMQX_API',
        { error: error.response?.data }
      );
      return { success: false, error: error.message };
    }
  }

  /**
   * Check if device credentials exist in EMQX
   */
  async deviceCredentialsExist(username: string): Promise<boolean> {
    try {
      const response = await this.client.get(
        `/api/v4/auth_users/${username}`
      );
      return response.status === 200;
    } catch (error: any) {
      if (error.response?.status === 404) {
        return false;
      }
      logger.errorWithEmoji(
        '❌',
        `Failed to check EMQX credentials: ${error.message}`,
        'EMQX_API'
      );
      return false;
    }
  }

  /**
   * Get EMQX broker connection info
   */
  getBrokerInfo(): {
    host: string;
    port: number;
    protocol: string;
  } {
    const url = new URL(env.mqttBrokerUrl || 'mqtt://localhost:1883');
    return {
      host: url.hostname,
      port: parseInt(url.port) || 1883,
      protocol: url.protocol.replace(':', ''),
    };
  }

  /**
   * Test EMQX API connectivity
   */
  async testConnection(): Promise<boolean> {
    try {
      const response = await this.client.get('/api/v4/stats');
      logger.infoWithEmoji('✅', 'NanoMQ API connection successful', 'EMQX_API', {
        status: response.data,
      });
      return true;
    } catch (error: any) {
      logger.errorWithEmoji('❌', `NanoMQ API connection failed: ${error.message}`, 'EMQX_API');
      return false;
    }
  }

  /**
   * Get list of connected clients
   */
  async getConnectedClients(): Promise<any[]> {
    try {
      const response = await this.client.get('/api/v4/clients');
      return response.data?.data || [];
    } catch (error: any) {
      logger.errorWithEmoji(
        '❌',
        `Failed to get connected clients: ${error.message}`,
        'EMQX_API'
      );
      return [];
    }
  }
}
