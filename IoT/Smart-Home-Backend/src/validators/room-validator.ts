import { body } from 'express-validator';

/**
 * Validation rules for room creation
 */
export const createRoomValidator = [
  body('name')
    .trim()
    .notEmpty()
    .withMessage('Room name is required')
    .isLength({ min: 1, max: 100 })
    .withMessage('Room name must be between 1 and 100 characters'),
];

/**
 * Validation rules for room update
 */
export const updateRoomValidator = [
  body('name')
    .optional()
    .trim()
    .notEmpty()
    .withMessage('Room name cannot be empty')
    .isLength({ min: 1, max: 100 })
    .withMessage('Room name must be between 1 and 100 characters'),
];

