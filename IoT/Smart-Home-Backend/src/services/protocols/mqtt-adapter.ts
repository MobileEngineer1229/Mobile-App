import { Device } from '../../models/device';
import { MqttClientService } from '../mqtt/mqtt-client';
import logger from '../../utils/logger';

/**
 * MQTT Protocol Adapter
 * Sends commands to IoT devices via EMQX broker using FastBee topic conventions.
 *
 * Architecture:
 * Phone → Backend API → EMQX Broker → Device (via MQTT)
 *
 * FastBee topic structure:
 * - Server → Device: /fastbee/{productId}/{deviceNum}/property/set
 * - Device → Server: /fastbee/{productId}/{deviceNum}/property/post
 * - Device status:    /fastbee/{productId}/{deviceNum}/status
 *
 * Device metadata must contain: { protocol: "mqtt", productId: string, deviceNum: string }
 */
export class MqttAdapter {
  private mqttClient: MqttClientService;

  constructor() {
    this.mqttClient = MqttClientService.getInstance();
  }

  /**
   * Send command to device via MQTT using FastBee topic convention
   */
  async sendCommand(
    device: Device,
    command: string,
    parameters: Record<string, unknown> = {}
  ): Promise<{ success: boolean; message: string; data?: unknown }> {
    const metadata = device.metadata as Record<string, unknown> | undefined;
    const productId = metadata?.productId as string;
    const deviceNum = metadata?.deviceNum as string;

    if (!productId || !deviceNum) {
      throw new Error('Device must have productId and deviceNum in metadata for MQTT protocol');
    }

    const topic = `/fastbee/${productId}/${deviceNum}/property/set`;
    const payload = {
      command,
      ...parameters,
      timestamp: Date.now(),
    };

    logger.infoWithEmoji('📡', 'Sending MQTT command via FastBee protocol', 'MQTT_ADAPTER', {
      deviceId: device.id,
      deviceName: device.name,
      topic,
      command,
    });

    try {
      await this.mqttClient.publish(topic, payload, 1);

      logger.infoWithEmoji('✅', 'MQTT command published successfully', 'MQTT_ADAPTER', {
        deviceId: device.id,
        command,
        topic,
      });

      return {
        success: true,
        message: `Command published to ${topic}`,
        data: { topic, payload },
      };
    } catch (error) {
      logger.errorWithEmoji('❌', 'Failed to publish MQTT command', 'MQTT_ADAPTER', {
        deviceId: device.id,
        error: error instanceof Error ? error.message : String(error),
      });
      throw error;
    }
  }

  /**
   * Get device status from metadata (updated by MqttMessageHandler)
   */
  async getDeviceStatus(device: Device): Promise<Record<string, unknown>> {
    const metadata = device.metadata as Record<string, unknown> | undefined;
    return {
      protocol: 'mqtt',
      productId: metadata?.productId,
      deviceNum: metadata?.deviceNum,
      lastProperties: metadata?.lastProperties,
      mqttOnline: metadata?.mqttOnline,
      lastMqttUpdate: metadata?.lastMqttUpdate,
    };
  }
}
