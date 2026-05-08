import { EventEmitter } from 'events';

export interface DeviceUpdateEvent {
  productId: string;
  deviceNum: string;
  type: 'property' | 'status' | 'info';
  data: Record<string, unknown>;
  timestamp: string;
}

/**
 * Singleton EventEmitter that bridges MQTT message handler to SSE connections.
 * MqttMessageHandler emits events here; SSE route listeners forward them to clients.
 */
export class DeviceEventBus extends EventEmitter {
  private static instance: DeviceEventBus;

  private constructor() {
    super();
    this.setMaxListeners(100); // Allow many SSE clients
  }

  static getInstance(): DeviceEventBus {
    if (!DeviceEventBus.instance) {
      DeviceEventBus.instance = new DeviceEventBus();
    }
    return DeviceEventBus.instance;
  }

  emitDeviceUpdate(
    productId: string,
    deviceNum: string,
    type: 'property' | 'status' | 'info',
    data: Record<string, unknown>
  ): void {
    const event: DeviceUpdateEvent = {
      productId,
      deviceNum,
      type,
      data,
      timestamp: new Date().toISOString(),
    };
    this.emit('device-update', event);
  }
}
