import { body, ValidationChain } from 'express-validator';
import { DeviceType, DeviceStatus } from '../models/device';

/**
 * Create device validation rules
 */
export const createDeviceValidator: ValidationChain[] = [
  body('name')
    .trim()
    .notEmpty()
    .withMessage('Device name is required')
    .isLength({ min: 2, max: 100 })
    .withMessage('Device name must be between 2 and 100 characters'),
  body('type')
    .isIn(Object.values(DeviceType))
    .withMessage(`Device type must be one of: ${Object.values(DeviceType).join(', ')}`),
  body('macAddress')
    .trim()
    .notEmpty()
    .withMessage('MAC address is required')
    .matches(/^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/)
    .withMessage('MAC address must be in format XX:XX:XX:XX:XX:XX or XX-XX-XX-XX-XX-XX'),
  body('ipAddress')
    .optional()
    .isIP()
    .withMessage('IP address must be a valid IP address'),
  body('metadata')
    .optional()
    .isObject()
    .withMessage('Metadata must be an object'),
];

/**
 * Update device validation rules
 */
export const updateDeviceValidator: ValidationChain[] = [
  body('name')
    .optional()
    .trim()
    .isLength({ min: 2, max: 100 })
    .withMessage('Device name must be between 2 and 100 characters'),
  body('status')
    .optional()
    .isIn(Object.values(DeviceStatus))
    .withMessage(`Device status must be one of: ${Object.values(DeviceStatus).join(', ')}`),
  body('ipAddress')
    .optional()
    .isIP()
    .withMessage('IP address must be a valid IP address'),
  body('metadata')
    .optional()
    .isObject()
    .withMessage('Metadata must be an object'),
];

