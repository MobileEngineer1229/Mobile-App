import mqtt, { MqttClient, IClientOptions } from 'mqtt';
import { EventEmitter } from 'events';
import { env } from '../../config/env';
import logger from '../../utils/logger';

/**
 * Singleton MQTT Client Service
 * Manages connection to EMQX broker with auto-reconnect and event emission.
 */
export class MqttClientService extends EventEmitter {
  private static instance: MqttClientService;
  private client: MqttClient | null = null;
  private connected = false;

  private constructor() {
    super();
  }

  static getInstance(): MqttClientService {
    if (!MqttClientService.instance) {
      MqttClientService.instance = new MqttClientService();
    }
    return MqttClientService.instance;
  }

  async connect(): Promise<void> {
    if (this.client && this.connected) {
      return;
    }

    const brokerUrl = env.mqttBrokerUrl || 'mqtt://localhost:1883';

    const options: IClientOptions = {
      clientId: env.mqttClientId || `smarthome-backend-${process.pid}`,
      clean: false,
      reconnectPeriod: 5000,
      connectTimeout: 10000,
      username: env.mqttUsername || undefined,
      password: env.mqttPassword || undefined,
    };

    return new Promise((resolve, reject) => {
      this.client = mqtt.connect(brokerUrl, options);

      this.client.on('connect', () => {
        this.connected = true;
        logger.infoWithEmoji('📡', `MQTT connected to ${brokerUrl}`, 'MQTT');
        this.emit('connected');
        resolve();
      });

      this.client.on('reconnect', () => {
        logger.warnWithEmoji('🔄', 'MQTT reconnecting...', 'MQTT');
      });

      this.client.on('disconnect', () => {
        this.connected = false;
        logger.warnWithEmoji('⚠️', 'MQTT disconnected', 'MQTT');
        this.emit('disconnected');
      });

      this.client.on('error', (error) => {
        logger.errorWithEmoji('❌', 'MQTT error', 'MQTT', {
          error: error.message,
        });
        if (!this.connected) {
          reject(error);
        }
      });

      this.client.on('message', (topic: string, payload: Buffer) => {
        this.emit('message', topic, payload);
      });

      this.client.on('close', () => {
        this.connected = false;
      });
    });
  }

  async publish(topic: string, payload: string | object, qos: 0 | 1 | 2 = 1): Promise<void> {
    if (!this.client || !this.connected) {
      throw new Error('MQTT client not connected');
    }

    const message = typeof payload === 'object' ? JSON.stringify(payload) : payload;

    return new Promise((resolve, reject) => {
      this.client!.publish(topic, message, { qos }, (error) => {
        if (error) {
          logger.errorWithEmoji('❌', `MQTT publish failed: ${topic}`, 'MQTT', {
            error: error.message,
          });
          reject(error);
        } else {
          logger.infoWithEmoji('📤', `MQTT published to ${topic}`, 'MQTT');
          resolve();
        }
      });
    });
  }

  async subscribe(topic: string, qos: 0 | 1 | 2 = 1): Promise<void> {
    if (!this.client || !this.connected) {
      throw new Error('MQTT client not connected');
    }

    return new Promise((resolve, reject) => {
      this.client!.subscribe(topic, { qos }, (error) => {
        if (error) {
          logger.errorWithEmoji('❌', `MQTT subscribe failed: ${topic}`, 'MQTT', {
            error: error.message,
          });
          reject(error);
        } else {
          logger.infoWithEmoji('📥', `MQTT subscribed to ${topic}`, 'MQTT');
          resolve();
        }
      });
    });
  }

  async unsubscribe(topic: string): Promise<void> {
    if (!this.client) return;

    return new Promise((resolve, reject) => {
      this.client!.unsubscribe(topic, (error) => {
        if (error) reject(error);
        else resolve();
      });
    });
  }

  isConnected(): boolean {
    return this.connected;
  }

  disconnect(): void {
    if (this.client) {
      this.client.end(true);
      this.client = null;
      this.connected = false;
      logger.infoWithEmoji('🔌', 'MQTT client disconnected', 'MQTT');
    }
  }
}
