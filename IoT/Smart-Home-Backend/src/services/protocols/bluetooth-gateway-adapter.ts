import axios, { AxiosInstance } from 'axios';
import { Device } from '../../models/device';
import logger from '../../utils/logger';

/**
 * Bluetooth Gateway Adapter
 * Communicates with a Bluetooth gateway backend to control devices via Bluetooth
 * 
 * Architecture:
 * Phone → Backend API → Gateway Backend → Bluetooth Gateway → Device (via BLE)
 */
export class BluetoothGatewayAdapter {
  private gatewayApiClient: AxiosInstance;
  private gatewayBaseUrl: string;

  constructor(gatewayBaseUrl: string, gatewayApiKey?: string) {
    this.gatewayBaseUrl = gatewayBaseUrl;
    
    // Create HTTP client for gateway backend
    this.gatewayApiClient = axios.create({
      baseURL: gatewayBaseUrl,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
        ...(gatewayApiKey && { 'X-API-Key': gatewayApiKey }),
      },
    });
  }

  /**
   * Send command to device via Bluetooth gateway
   * @param device Device metadata should contain:
   *   - connection.gatewayId: Gateway identifier
   *   - connection.bluetoothAddress: Device Bluetooth MAC address
   *   - connection.serviceUUID: BLE service UUID (optional)
   *   - connection.characteristicUUID: BLE characteristic UUID (optional)
   */
  async sendCommand(
    device: Device,
    command: string,
    parameters: Record<string, unknown> = {}
  ): Promise<{ success: boolean; message: string; data?: unknown }> {
    try {
      const metadata = device.metadata as Record<string, unknown> | undefined;
      const connection = metadata?.connection as Record<string, unknown> | undefined;

      if (!connection) {
        throw new Error('Device connection information not found in metadata');
      }

      const gatewayId = connection.gatewayId as string | number;
      const bluetoothAddress = connection.bluetoothAddress as string;

      if (!gatewayId || !bluetoothAddress) {
        throw new Error('Gateway ID and Bluetooth address are required');
      }

      // Prepare command payload for gateway backend
      const commandPayload = {
        gatewayId,
        deviceAddress: bluetoothAddress,
        command,
        parameters,
        serviceUUID: connection.serviceUUID as string | undefined,
        characteristicUUID: connection.characteristicUUID as string | undefined,
      };

      logger.infoWithEmoji('📡', 'Sending command via Bluetooth gateway', 'BLUETOOTH_GATEWAY', {
        deviceId: device.id,
        deviceName: device.name,
        gatewayId,
        bluetoothAddress,
        command,
      });

      // Send command to gateway backend
      const response = await this.gatewayApiClient.post('/api/v1/gateway/bluetooth/command', commandPayload);

      if (response.data && response.data.success) {
        logger.infoWithEmoji('✅', 'Command sent successfully via Bluetooth gateway', 'BLUETOOTH_GATEWAY', {
          deviceId: device.id,
          command,
        });

        return {
          success: true,
          message: 'Command sent successfully',
          data: response.data.data,
        };
      } else {
        throw new Error(response.data?.message || 'Gateway returned unsuccessful response');
      }
    } catch (error) {
      logger.errorWithEmoji('❌', 'Failed to send command via Bluetooth gateway', 'BLUETOOTH_GATEWAY', {
        deviceId: device.id,
        error: error instanceof Error ? error.message : String(error),
      });

      throw error;
    }
  }

  /**
   * Get device status from gateway
   */
  async getDeviceStatus(device: Device): Promise<Record<string, unknown>> {
    try {
      const metadata = device.metadata as Record<string, unknown> | undefined;
      const connection = metadata?.connection as Record<string, unknown> | undefined;

      if (!connection) {
        throw new Error('Device connection information not found');
      }

      const gatewayId = connection.gatewayId as string | number;
      const bluetoothAddress = connection.bluetoothAddress as string;

      const response = await this.gatewayApiClient.get(
        `/api/v1/gateway/bluetooth/device/${bluetoothAddress}/status`,
        {
          params: { gatewayId },
        }
      );

      return response.data?.data || {};
    } catch (error) {
      logger.errorWithEmoji('❌', 'Failed to get device status from gateway', 'BLUETOOTH_GATEWAY', {
        deviceId: device.id,
        error: error instanceof Error ? error.message : String(error),
      });
      throw error;
    }
  }

  /**
   * Check if gateway is available
   */
  async checkGatewayHealth(gatewayId: string | number): Promise<boolean> {
    try {
      const response = await this.gatewayApiClient.get(`/api/v1/gateway/${gatewayId}/health`);
      return response.data?.status === 'ok';
    } catch (error) {
      logger.warnWithEmoji('⚠️', 'Gateway health check failed', 'BLUETOOTH_GATEWAY', {
        gatewayId,
        error: error instanceof Error ? error.message : String(error),
      });
      return false;
    }
  }
}
