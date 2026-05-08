import { body, ValidationChain } from 'express-validator';

/**
 * Create scene validation rules
 */
export const createSceneValidator: ValidationChain[] = [
  body('name')
    .trim()
    .notEmpty()
    .withMessage('Scene name is required')
    .isLength({ min: 1, max: 255 })
    .withMessage('Scene name must be between 1 and 255 characters'),
  body('type')
    .isIn(['automation', 'tap_to_run'])
    .withMessage('Scene type must be either "automation" or "tap_to_run"'),
  body('conditionLogic')
    .optional()
    .isIn(['any', 'all'])
    .withMessage('Condition logic must be either "any" or "all"'),
  body('icon')
    .optional()
    .isString()
    .withMessage('Icon must be a string'),
  body('color')
    .optional()
    .matches(/^#[0-9A-Fa-f]{6}$/)
    .withMessage('Color must be a valid hex color code (e.g., #405FF2)'),
  body('homeId')
    .optional()
    .isInt({ min: 1 })
    .withMessage('Home ID must be a positive integer'),
  body('isEnabled')
    .optional()
    .isBoolean()
    .withMessage('isEnabled must be a boolean'),
  body('conditions')
    .optional()
    .isArray()
    .withMessage('Conditions must be an array'),
  body('conditions.*.type')
    .if(body('conditions').isArray())
    .notEmpty()
    .withMessage('Condition type is required')
    .isIn([
      'temperature',
      'humidity',
      'weather',
      'sunrise_sunset',
      'wind_speed',
      'location',
      'location_arrive_at',
      'location_leave',
      'schedule_time',
      'time',
      'device_status',
      'arm_mode',
      'alarm',
      'tap_to_run'
    ])
    .withMessage('Invalid condition type'),
  body('conditions.*.operator')
    .optional()
    .isString()
    .withMessage('Condition operator must be a string'),
  body('conditions.*.value')
    .optional()
    .isNumeric()
    .withMessage('Condition value must be a number'),
  body('conditions.*.location')
    .optional()
    .isString()
    .withMessage('Condition location must be a string'),
  body('conditions.*.metadata')
    .optional()
    .isObject()
    .withMessage('Condition metadata must be an object'),
  body('tasks')
    .optional()
    .isArray()
    .withMessage('Tasks must be an array'),
  body('tasks.*.type')
    .if(body('tasks').isArray())
    .notEmpty()
    .withMessage('Task type is required')
    .isIn([
      'control_device',
      'select_scene',
      'change_arm_mode',
      'send_notification',
      'delay'
    ])
    .withMessage('Invalid task type'),
  body('tasks.*.deviceId')
    .optional()
    .isInt({ min: 1 })
    .withMessage('Device ID must be a positive integer'),
  body('tasks.*.sceneIdTarget')
    .optional()
    .isInt({ min: 1 })
    .withMessage('Scene ID target must be a positive integer'),
  body('tasks.*.delaySeconds')
    .optional()
    .isInt({ min: 0 })
    .withMessage('Delay seconds must be a non-negative integer'),
  body('tasks.*.orderIndex')
    .optional()
    .isInt({ min: 0 })
    .withMessage('Order index must be a non-negative integer'),
];

/**
 * Update scene validation rules
 */
export const updateSceneValidator: ValidationChain[] = [
  body('name')
    .optional()
    .trim()
    .isLength({ min: 1, max: 255 })
    .withMessage('Scene name must be between 1 and 255 characters'),
  body('conditionLogic')
    .optional()
    .isIn(['any', 'all'])
    .withMessage('Condition logic must be either "any" or "all"'),
  body('icon')
    .optional()
    .isString()
    .withMessage('Icon must be a string'),
  body('color')
    .optional()
    .matches(/^#[0-9A-Fa-f]{6}$/)
    .withMessage('Color must be a valid hex color code (e.g., #405FF2)'),
  body('isEnabled')
    .optional()
    .isBoolean()
    .withMessage('isEnabled must be a boolean'),
  body('conditions')
    .optional()
    .isArray()
    .withMessage('Conditions must be an array'),
  body('tasks')
    .optional()
    .isArray()
    .withMessage('Tasks must be an array'),
];
