import { body } from 'express-validator';

/**
 * Validation rules for home creation
 */
export const createHomeValidation = [
  body('name')
    .trim()
    .notEmpty()
    .withMessage('Home name is required')
    .isLength({ max: 100 })
    .withMessage('Home name must be at most 100 characters'),

  body('address')
    .optional()
    .trim()
    .isLength({ max: 255 })
    .withMessage('Address must be at most 255 characters'),

  body('latitude')
    .optional()
    .isFloat({ min: -90, max: 90 })
    .withMessage('Latitude must be between -90 and 90'),

  body('longitude')
    .optional()
    .isFloat({ min: -180, max: 180 })
    .withMessage('Longitude must be between -180 and 180'),

  body('country')
    .optional()
    .trim()
    .isLength({ max: 100 })
    .withMessage('Country must be at most 100 characters'),

  body('isPrimary')
    .optional()
    .isBoolean()
    .withMessage('isPrimary must be a boolean'),
];

/**
 * Validation rules for home update
 */
export const updateHomeValidation = [
  body('name')
    .optional()
    .trim()
    .notEmpty()
    .withMessage('Home name cannot be empty')
    .isLength({ max: 100 })
    .withMessage('Home name must be at most 100 characters'),

  body('address')
    .optional()
    .trim()
    .isLength({ max: 255 })
    .withMessage('Address must be at most 255 characters'),

  body('latitude')
    .optional()
    .isFloat({ min: -90, max: 90 })
    .withMessage('Latitude must be between -90 and 90'),

  body('longitude')
    .optional()
    .isFloat({ min: -180, max: 180 })
    .withMessage('Longitude must be between -180 and 180'),

  body('country')
    .optional()
    .trim()
    .isLength({ max: 100 })
    .withMessage('Country must be at most 100 characters'),

  body('isPrimary')
    .optional()
    .isBoolean()
    .withMessage('isPrimary must be a boolean'),
];
