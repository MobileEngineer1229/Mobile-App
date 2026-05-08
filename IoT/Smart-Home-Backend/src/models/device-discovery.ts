/**
 * Discovered device interface (for nearby device discovery)
 */
export interface DiscoveredDevice {
  name: string;
  type: string;
  macAddress: string;
  ipAddress?: string;
  signalStrength?: number;
  isPaired: boolean;
}

/**
 * Device discovery response
 */
export interface DeviceDiscoveryResponse {
  devices: DiscoveredDevice[];
  timestamp: Date;
}

