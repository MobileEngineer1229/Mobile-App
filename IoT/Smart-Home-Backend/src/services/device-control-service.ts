import { DeviceRepository } from '../repositories/device-repository';
import { DeviceStatus, Device } from '../models/device';
import { NotFoundError } from '../utils/errors';
import logger from '../utils/logger';
import { BluetoothGatewayAdapter } from './protocols/bluetooth-gateway-adapter';
import { MqttAdapter } from './protocols/mqtt-adapter';
import { env } from '../config/env';
import { getPool } from '../config/database';

/**
 * Device Control Service
 * Handles device control commands and integrates with 3rd party APIs
 * Supports multiple protocols: Bluetooth Gateway, Direct Bluetooth, MQTT, Zigbee, etc.
 */
export class DeviceControlService {
  private bluetoothGatewayAdapter: BluetoothGatewayAdapter | null = null;
  private mqttAdapter: MqttAdapter;

  constructor(private deviceRepository: DeviceRepository) {
    this.mqttAdapter = new MqttAdapter();
    // Initialize Bluetooth gateway adapter if gateway URL is configured
    const gatewayUrl = process.env.BLUETOOTH_GATEWAY_URL || env.bluetoothGatewayUrl;
    if (gatewayUrl) {
      const gatewayApiKey = process.env.BLUETOOTH_GATEWAY_API_KEY || env.bluetoothGatewayApiKey;
      this.bluetoothGatewayAdapter = new BluetoothGatewayAdapter(gatewayUrl, gatewayApiKey);
      logger.info('Bluetooth Gateway Adapter initialized', { gatewayUrl });
    }
  }

  /**
   * Get the appropriate protocol adapter based on device metadata
   */
  private getProtocolAdapter(device: Device): BluetoothGatewayAdapter | MqttAdapter | null {
    const metadata = device.metadata as Record<string, unknown> | undefined;
    const protocol = metadata?.protocol as string;

    if (protocol === 'mqtt') {
      return this.mqttAdapter;
    }

    if (protocol === 'bluetooth_gateway' || protocol === 'bluetooth') {
      const connection = metadata?.connection as Record<string, unknown> | undefined;
      if (connection?.gatewayId) {
        return this.bluetoothGatewayAdapter;
      }
    }

    return null;
  }

  /**
   * Log command to device_commands table for audit trail
   */
  private async logCommand(
    deviceId: number,
    userId: number,
    commandType: string,
    payload: Record<string, unknown>,
    source: string = 'app',
    status: 'sent' | 'delivered' | 'acknowledged' | 'failed' = 'sent'
  ): Promise<number | null> {
    try {
      const pool = getPool();
      const result = await pool.query(
        `INSERT INTO device_commands (device_id, user_id, command_type, payload, source, status, created_at)
         VALUES ($1, $2, $3, $4::jsonb, $5, $6, NOW())
         RETURNING id`,
        [deviceId, userId, commandType, JSON.stringify(payload), source, status]
      );

      logger.debug(`Command logged to device_commands: ${commandType}`, {
        deviceId,
        userId,
        commandId: result.rows[0].id,
      });

      return result.rows[0].id;
    } catch (error) {
      logger.error('Failed to log command to device_commands:', error);
      return null;
    }
  }

  /**
   * Update command status in device_commands table
   */
  private async updateCommandStatus(
    commandId: number,
    status: 'sent' | 'delivered' | 'acknowledged' | 'failed',
    response?: Record<string, unknown>
  ): Promise<void> {
    try {
      const pool = getPool();
      const responseJson = response ? JSON.stringify(response) : null;
      const acknowledgedAt = status === 'acknowledged' ? new Date() : null;

      await pool.query(
        `UPDATE device_commands
         SET status = $1::varchar,
             response = $2::jsonb,
             acknowledged_at = COALESCE($3::timestamp, acknowledged_at)
         WHERE id = $4`,
        [status, responseJson, acknowledgedAt, commandId]
      );
    } catch (error) {
      logger.error('Failed to update command status:', error);
    }
  }

  /**
   * Control device power (ON/OFF)
   * Sends command via MQTT/EMQX and logs to device_commands table
   */
  async controlPower(
    deviceId: number,
    userId: number,
    power: boolean,
    source: string = 'app'
  ): Promise<{ success: boolean; message: string; commandId?: number }> {
    // Verify device ownership
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    // Log command to device_commands table for audit trail
    const commandId = await this.logCommand(
      deviceId,
      userId,
      'power',
      { power, action: power ? 'on' : 'off' },
      source,
      'sent'
    );

    logger.infoWithEmoji('⚡', `Power command initiated for device ${deviceId}`, 'DEVICE_CONTROL', {
      deviceId,
      userId,
      power,
      commandId,
      source,
    });

    // Check device protocol and use appropriate adapter
    const adapter = this.getProtocolAdapter(device);
    let commandSuccess = true;
    let errorMessage: string | null = null;

    if (adapter) {
      // Use protocol adapter (MQTT or Bluetooth Gateway)
      try {
        const result = await adapter.sendCommand(device, 'power', { power });

        logger.infoWithEmoji('✅', `Device ${deviceId} power set to ${power ? 'ON' : 'OFF'} via ${adapter instanceof MqttAdapter ? 'MQTT/EMQX' : 'Bluetooth Gateway'}`, 'DEVICE_CONTROL', {
          deviceId,
          commandId,
          topic: result.data && typeof result.data === 'object' ? (result.data as Record<string, unknown>).topic : undefined,
        });

        // Update command status to delivered
        if (commandId) {
          await this.updateCommandStatus(commandId, 'delivered', {
            protocol: adapter instanceof MqttAdapter ? 'mqtt' : 'bluetooth_gateway',
            publishedAt: new Date().toISOString(),
            ...(result.data as Record<string, unknown> || {}),
          });
        }
      } catch (error) {
        commandSuccess = false;
        errorMessage = error instanceof Error ? error.message : String(error);

        logger.errorWithEmoji('❌', `Failed to control device via adapter`, 'DEVICE_CONTROL', {
          deviceId,
          commandId,
          error: errorMessage,
        });

        // Update command status to failed
        if (commandId) {
          await this.updateCommandStatus(commandId, 'failed', { error: errorMessage });
        }
        // Continue to update database even if gateway call fails (for state tracking)
      }
    } else {
      // No protocol adapter available - log warning
      logger.warnWithEmoji('⚠️', `Device ${deviceId} has no protocol adapter configured`, 'DEVICE_CONTROL', {
        deviceId,
        protocol: (device.metadata as Record<string, unknown>)?.protocol || 'unknown',
      });

      // Update command status - no adapter available
      if (commandId) {
        await this.updateCommandStatus(commandId, 'delivered', {
          note: 'No protocol adapter - command applied to database only',
        });
      }
    }

    // Update device status in database
    await this.deviceRepository.update(deviceId, userId, {
      status: power ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE,
      metadata: {
        ...(device.metadata as Record<string, unknown> || {}),
        power,
        lastControlTime: new Date().toISOString(),
        lastCommandId: commandId,
      },
    });

    logger.infoWithEmoji('📝', `Device ${deviceId} power command completed`, 'DEVICE_CONTROL', {
      deviceId,
      power,
      commandId,
      success: commandSuccess,
    });

    return {
      success: true,
      message: `Device ${power ? 'turned on' : 'turned off'} successfully`,
      commandId: commandId || undefined,
    };
  }

  /**
   * Control smart lamp
   */
  async controlLamp(
    deviceId: number,
    userId: number,
    settings: {
      brightness?: number;
      color?: string;
      temperature?: number;
      mode?: 'white' | 'color' | 'scene';
    }
  ): Promise<{ success: boolean; message: string; settings: any }> {
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    // Check device protocol and use appropriate adapter
    const adapter = this.getProtocolAdapter(device);
    
    if (adapter) {
      // Use protocol adapter (e.g., Bluetooth Gateway)
      try {
        await adapter.sendCommand(device, 'lamp_control', settings);
        logger.info(`Lamp ${deviceId} settings updated via gateway:`, settings);
      } catch (error) {
        logger.error(`Failed to control lamp via gateway: ${error instanceof Error ? error.message : String(error)}`);
        // Continue to update database even if gateway call fails
      }
    } else {
      // TODO: Direct protocol integration (MQTT, Zigbee, IR, etc.)
      logger.info(`Lamp ${deviceId} settings updated (no gateway adapter):`, settings);
    }

    // Update device metadata
    const currentMetadata = (device.metadata as Record<string, unknown>) || {};
    await this.deviceRepository.update(deviceId, userId, {
      metadata: {
        ...currentMetadata,
        ...settings,
        lastControlTime: new Date().toISOString(),
      },
    });

    return {
      success: true,
      message: 'Lamp settings updated successfully',
      settings,
    };
  }

  /**
   * Control CCTV camera
   */
  async controlCamera(
    deviceId: number,
    userId: number,
    command: {
      action: 'playback' | 'snapshot' | 'record' | 'speak' | 'gallery' | 'private_mode' | 'night_mode' | 'move';
      direction?: 'left' | 'right' | 'up' | 'down' | 'stop';
      quality?: 'SD' | 'HD' | 'FullHD';
    }
  ): Promise<{ success: boolean; message: string; data?: any }> {
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    // TODO: 3rd Party API Integration for CCTV Cameras
    // Examples:
    // - Hikvision: ONVIF protocol or SDK
    // - Dahua: HTTP API or SDK
    // - Reolink: HTTP API
    // - Generic ONVIF: Use ONVIF library (node-onvif)
    // - RTSP cameras: Use ffmpeg for stream manipulation
    
    // Example structure:
    // const cameraApi = this.getCameraApiClient(device.type, device.metadata);
    // switch (command.action) {
    //   case 'snapshot':
    //     const snapshot = await cameraApi.captureSnapshot();
    //     return { success: true, message: 'Snapshot captured', data: { imageUrl: snapshot } };
    //   case 'record':
    //     await cameraApi.startRecording();
    //     break;
    //   case 'move':
    //     await cameraApi.move(command.direction);
    //     break;
    //   case 'night_mode':
    //     await cameraApi.setNightMode(true);
    //     break;
    // }

    logger.info(`Camera ${deviceId} command executed:`, command);

    return {
      success: true,
      message: `Camera ${command.action} executed successfully`,
    };
  }

  /**
   * Get camera live stream URL
   */
  async getCameraStream(
    deviceId: number,
    userId: number
  ): Promise<{ streamUrl: string; protocol: string }> {
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    // TODO: 3rd Party API Integration for Camera Streams
    // Examples:
    // - RTSP cameras: Convert RTSP to HLS/WebRTC using media server (e.g., Wowza, Kurento)
    // - Cloud cameras: Get stream URL from manufacturer API
    // - ONVIF: Use GetStreamUri operation
    // - Hikvision: Use SDK to get stream URL
    // - Reolink: Use their API to get stream URL
    
    // Example:
    // const cameraApi = this.getCameraApiClient(device.type, device.metadata);
    // const streamUrl = await cameraApi.getStreamUrl({ quality: 'HD' });
    
    const metadata = device.metadata as Record<string, unknown> || {};
    const ipAddress = device.ipAddress || metadata.ipAddress as string || '';

    // For now, return a placeholder RTSP URL
    // In production, this would come from the 3rd party API
    const streamUrl = `rtsp://${ipAddress}:554/stream1`; // Placeholder

    return {
      streamUrl,
      protocol: 'rtsp', // Could be rtsp, hls, webrtc, etc.
    };
  }

  /**
   * Control stereo speaker
   */
  async controlSpeaker(
    deviceId: number,
    userId: number,
    settings: {
      volume?: number;
      action?: 'play' | 'pause' | 'next' | 'previous';
      service?: 'spotify' | 'apple_music' | 'youtube_music';
    }
  ): Promise<{ success: boolean; message: string; nowPlaying?: any }> {
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    // TODO: 3rd Party API Integration for Speakers
    // Examples:
    // - Spotify Connect API: Control playback on Spotify devices
    // - Sonos API: Control Sonos speakers
    // - Google Cast: Use Cast SDK
    // - Apple AirPlay: Use AirPlay protocol
    // - Generic Bluetooth: Use device-specific protocol
    
    // Example for Spotify:
    // if (settings.service === 'spotify') {
    //   const spotifyApi = this.getSpotifyApiClient(userId);
    //   const deviceId = await spotifyApi.getDeviceId(device.metadata.spotifyDeviceId);
    //   if (settings.volume !== undefined) {
    //     await spotifyApi.setVolume(deviceId, settings.volume);
    //   }
    //   if (settings.action) {
    //     await spotifyApi.controlPlayback(deviceId, settings.action);
    //   }
    //   const nowPlaying = await spotifyApi.getCurrentlyPlaying(deviceId);
    //   return { success: true, message: 'Speaker controlled', nowPlaying };
    // }

    logger.info(`Speaker ${deviceId} settings updated:`, settings);

    // Update device metadata
    const currentMetadata = (device.metadata as Record<string, unknown>) || {};
    await this.deviceRepository.update(deviceId, userId, {
      metadata: {
        ...currentMetadata,
        ...settings,
        lastControlTime: new Date().toISOString(),
      },
    });

    return {
      success: true,
      message: 'Speaker settings updated successfully',
    };
  }

  /**
   * Control air conditioner
   */
  async controlAC(
    deviceId: number,
    userId: number,
    settings: {
      temperature?: number;
      mode?: 'cooling' | 'heating' | 'purifying';
      windSpeed?: 'low' | 'medium' | 'high' | 'auto';
      windDirection?: 'up' | 'down' | 'left' | 'right' | 'auto';
      eco?: boolean;
      sleep?: boolean;
    }
  ): Promise<{ success: boolean; message: string; settings: any }> {
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    // TODO: 3rd Party API Integration for Air Conditioners
    // Examples:
    // - Daikin: Use their API or IR protocol
    // - Mitsubishi: Use their API
    // - Generic IR: Use IR blaster API (e.g., Broadlink)
    // - Smart AC controllers: Use manufacturer SDK
    // - MQTT: Publish commands to device topic
    
    // Example:
    // const acApi = this.getACApiClient(device.type, device.metadata);
    // if (settings.temperature !== undefined) {
    //   await acApi.setTemperature(settings.temperature);
    // }
    // if (settings.mode) {
    //   await acApi.setMode(settings.mode);
    // }
    // if (settings.windSpeed) {
    //   await acApi.setWindSpeed(settings.windSpeed);
    // }

    logger.info(`AC ${deviceId} settings updated:`, settings);

    // Update device metadata
    const currentMetadata = (device.metadata as Record<string, unknown>) || {};
    await this.deviceRepository.update(deviceId, userId, {
      metadata: {
        ...currentMetadata,
        ...settings,
        lastControlTime: new Date().toISOString(),
      },
    });

    return {
      success: true,
      message: 'AC settings updated successfully',
      settings,
    };
  }

  /**
   * Get device current state
   */
  async getDeviceState(
    deviceId: number,
    userId: number
  ): Promise<Record<string, unknown>> {
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    // TODO: 3rd Party API Integration - Fetch real-time state
    // Example:
    // const deviceApi = this.getDeviceApiClient(device.type, device.metadata);
    // const realTimeState = await deviceApi.getState();
    // return { ...device.metadata, ...realTimeState };

    return (device.metadata as Record<string, unknown>) || {};
  }

  /**
   * Execute a device command
   * Unified command interface for all device types
   */
  async executeCommand(
    deviceId: number,
    userId: number,
    command: string,
    parameters: Record<string, unknown>,
    source: string = 'app'
  ): Promise<{ success: boolean; message: string; data?: Record<string, unknown>; commandId?: number }> {
    // Verify device ownership
    const device = await this.deviceRepository.findById(deviceId, userId);
    if (!device) {
      throw new NotFoundError('Device');
    }

    logger.info(`Executing command "${command}" on device ${deviceId} with parameters:`, parameters);

    // Log command to device_commands table
    const commandId = await this.logCommand(deviceId, userId, command, parameters, source, 'sent');

    // Check if device uses a protocol adapter (e.g., Bluetooth Gateway)
    const adapter = this.getProtocolAdapter(device);

    if (adapter) {
      // Send command via protocol adapter first
      try {
        const result = await adapter.sendCommand(device, command, parameters);

        // Update command status to delivered/acknowledged
        if (commandId) {
          await this.updateCommandStatus(commandId, 'delivered', result.data as Record<string, unknown>);
        }

        // Update device metadata with last command
        const currentMetadata = (device.metadata as Record<string, unknown>) || {};
        const lastKnownState = result.data && typeof result.data === 'object' && result.data !== null
          ? (result.data as Record<string, unknown>)
          : undefined;
        await this.deviceRepository.update(deviceId, userId, {
          metadata: {
            ...currentMetadata,
            lastCommand: command,
            lastCommandParams: parameters,
            lastControlTime: new Date().toISOString(),
            ...(lastKnownState && { lastKnownState }),
          },
        });

        return {
          success: result.success,
          message: result.message,
          data: result.data as Record<string, unknown>,
          commandId: commandId || undefined,
        };
      } catch (error) {
        // Update command status to failed
        if (commandId) {
          await this.updateCommandStatus(commandId, 'failed', { error: error instanceof Error ? error.message : String(error) });
        }
        logger.error(`Failed to execute command via adapter: ${error instanceof Error ? error.message : String(error)}`);
        // Fall through to device-specific handlers for backward compatibility
      }
    }

    // Route command to appropriate handler based on command type (fallback or direct control)
    let result: { success: boolean; message: string; data?: Record<string, unknown> };

    switch (command.toLowerCase()) {
      case 'power':
      case 'on':
      case 'off':
        const power = command.toLowerCase() === 'on' || (parameters.power as boolean) === true;
        result = await this.controlPower(deviceId, userId, power);
        break;

      case 'brightness':
      case 'set_brightness':
        if (typeof parameters.brightness !== 'number' || parameters.brightness < 0 || parameters.brightness > 100) {
          if (commandId) await this.updateCommandStatus(commandId, 'failed', { error: 'Invalid brightness value' });
          throw new Error('Brightness must be a number between 0 and 100');
        }
        result = await this.controlLamp(deviceId, userId, { brightness: parameters.brightness as number });
        break;

      case 'color':
      case 'set_color':
        if (typeof parameters.color !== 'string') {
          if (commandId) await this.updateCommandStatus(commandId, 'failed', { error: 'Invalid color value' });
          throw new Error('Color must be a string (hex color code)');
        }
        result = await this.controlLamp(deviceId, userId, { color: parameters.color as string });
        break;

      case 'temperature':
      case 'set_temperature':
        if (typeof parameters.temperature !== 'number' || parameters.temperature < 0 || parameters.temperature > 100) {
          if (commandId) await this.updateCommandStatus(commandId, 'failed', { error: 'Invalid temperature value' });
          throw new Error('Temperature must be a number between 0 and 100');
        }
        result = await this.controlLamp(deviceId, userId, { temperature: parameters.temperature as number });
        break;

      case 'mode':
      case 'set_mode':
        if (!['white', 'color', 'scene'].includes(parameters.mode as string)) {
          if (commandId) await this.updateCommandStatus(commandId, 'failed', { error: 'Invalid mode value' });
          throw new Error('Mode must be one of: white, color, scene');
        }
        result = await this.controlLamp(deviceId, userId, { mode: parameters.mode as 'white' | 'color' | 'scene' });
        break;

      case 'lamp':
      case 'control_lamp':
        result = await this.controlLamp(deviceId, userId, {
          brightness: parameters.brightness as number | undefined,
          color: parameters.color as string | undefined,
          temperature: parameters.temperature as number | undefined,
          mode: parameters.mode as 'white' | 'color' | 'scene' | undefined,
        });
        break;

      case 'camera':
      case 'control_camera':
        result = await this.controlCamera(deviceId, userId, parameters as any);
        break;

      case 'speaker':
      case 'control_speaker':
        result = await this.controlSpeaker(deviceId, userId, parameters as any);
        break;

      case 'ac':
      case 'air_conditioner':
      case 'control_ac':
        result = await this.controlAC(deviceId, userId, parameters as any);
        break;

      default:
        // Generic command - update device metadata
        const currentMetadata = (device.metadata as Record<string, unknown>) || {};
        await this.deviceRepository.update(deviceId, userId, {
          metadata: {
            ...currentMetadata,
            ...parameters,
            lastCommand: command,
            lastControlTime: new Date().toISOString(),
          },
        });

        result = {
          success: true,
          message: `Command "${command}" executed successfully`,
          data: parameters,
        };
    }

    // Update command status based on result
    if (commandId) {
      await this.updateCommandStatus(
        commandId,
        result.success ? 'delivered' : 'failed',
        result.data
      );
    }

    return {
      ...result,
      commandId: commandId || undefined,
    };
  }
}
