/**
 * Device type template (for manual device addition)
 */
export interface DeviceTypeTemplate {
  id: string;
  name: string;
  category: string;
  description?: string;
  icon?: string;
  defaultType: 'lamp' | 'camera' | 'electronics' | 'cctv' | 'speaker' | 'thermostat' | 'lock' | 'tv' | 'appliance' | 'sensor';
  metadata?: Record<string, unknown>;
}

/**
 * Device type templates response
 */
export interface DeviceTypeTemplatesResponse {
  templates: DeviceTypeTemplate[];
  categories: string[];
}

