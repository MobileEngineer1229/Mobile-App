import { Request, Response, NextFunction } from 'express';
import { AppError } from '../utils/errors';
import { sendError } from '../utils/response';
import logger from '../utils/logger';

/**
 * Error handling middleware
 */
export const errorHandler = (
  err: Error | AppError,
  req: Request,
  res: Response,
  _next: NextFunction
): void => {
  if (err instanceof AppError) {
    logger.errorWithEmoji('❌', `AppError: ${err.message}`, 'ERROR', {
      code: err.code,
      statusCode: err.statusCode,
      path: req.path,
      method: req.method,
      stack: err.stack,
    });
    sendError(res, err.code, err.message, err.statusCode);
    return;
  }

  // Handle unexpected errors
  logger.errorWithEmoji('❌', 'Unexpected error', 'ERROR', {
    message: err.message,
    path: req.path,
    method: req.method,
    stack: err.stack,
  });

  sendError(
    res,
    'INTERNAL_SERVER_ERROR',
    process.env.NODE_ENV === 'production'
      ? 'An unexpected error occurred'
      : err.message,
    500
  );
};

