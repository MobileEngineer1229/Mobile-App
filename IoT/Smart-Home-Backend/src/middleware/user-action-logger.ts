import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { UserActionService } from '../services/user-action-service';
import { UserActionRepository } from '../repositories/user-action-repository';
import { getPool } from '../config/database';
import logger from '../utils/logger';
import { env } from '../config/env';

// Initialize service
const userActionRepository = new UserActionRepository(getPool());
const userActionService = new UserActionService(userActionRepository);

/**
 * Extract action type from endpoint and method
 */
function extractActionType(endpoint: string, method: string): { type: string; category: string } {
  // Remove query parameters and API prefix
  const cleanEndpoint = endpoint.replace(/\/api\/v\d+\//, '').split('?')[0];
  const parts = cleanEndpoint.split('/').filter(Boolean);

  // Determine category
  let category = 'general';
  if (cleanEndpoint.includes('login') || cleanEndpoint.includes('signup') || cleanEndpoint.includes('logout')) {
    category = 'authentication';
  } else if (cleanEndpoint.includes('device')) {
    category = 'device_management';
  } else if (cleanEndpoint.includes('room')) {
    category = 'room_management';
  } else if (cleanEndpoint.includes('home')) {
    category = 'home_management';
  } else if (cleanEndpoint.includes('settings') || cleanEndpoint.includes('profile')) {
    category = 'settings';
  } else if (cleanEndpoint.includes('report')) {
    category = 'reports';
  } else if (cleanEndpoint.includes('notification')) {
    category = 'notifications';
  } else if (cleanEndpoint.includes('chatbot')) {
    category = 'chatbot';
  } else if (cleanEndpoint.includes('control')) {
    category = 'device_control';
  }

  // Determine action type
  let type = `${method.toLowerCase()}_${parts[parts.length - 1] || 'unknown'}`;
  
  // Special cases
  if (cleanEndpoint.includes('login')) {
    type = 'login';
  } else if (cleanEndpoint.includes('signup')) {
    type = 'signup';
  } else if (cleanEndpoint.includes('logout')) {
    type = 'logout';
  } else if (cleanEndpoint.includes('control/power')) {
    type = 'device_power_control';
  } else if (cleanEndpoint.includes('control/lamp')) {
    type = 'lamp_control';
  } else if (cleanEndpoint.includes('control/camera')) {
    type = 'camera_control';
  } else if (cleanEndpoint.includes('control/speaker')) {
    type = 'speaker_control';
  } else if (cleanEndpoint.includes('control/ac')) {
    type = 'ac_control';
  } else if (cleanEndpoint.includes('command')) {
    type = 'device_command';
  }

  return { type, category };
}

/**
 * Sanitize request body to remove sensitive information
 */
function sanitizeRequestBody(body: any): any {
  if (!body || typeof body !== 'object') {
    return body;
  }

  const sanitized = { ...body };
  const sensitiveFields = ['password', 'currentPassword', 'newPassword', 'token', 'secret', 'apiKey'];

  for (const field of sensitiveFields) {
    if (sanitized[field]) {
      sanitized[field] = '[REDACTED]';
    }
  }

  return sanitized;
}

/**
 * Truncate response body if too large
 */
function truncateResponseBody(body: any, maxSize: number = 10000): any {
  if (!body) return body;
  
  const jsonString = JSON.stringify(body);
  if (jsonString.length <= maxSize) {
    return body;
  }

  return {
    _truncated: true,
    _originalSize: jsonString.length,
    _message: 'Response body truncated for logging',
  };
}

/**
 * Extract device info from user agent
 */
function extractDeviceInfo(userAgent: string | undefined): Record<string, any> | null {
  if (!userAgent) return null;

  const info: Record<string, any> = {};

  // Detect mobile
  if (/Mobile|Android|iPhone|iPad/i.test(userAgent)) {
    info.deviceType = 'mobile';
  } else {
    info.deviceType = 'desktop';
  }

  // Detect OS
  if (/Android/i.test(userAgent)) {
    info.os = 'Android';
  } else if (/iPhone|iPad|iPod/i.test(userAgent)) {
    info.os = 'iOS';
  } else if (/Windows/i.test(userAgent)) {
    info.os = 'Windows';
  } else if (/Mac/i.test(userAgent)) {
    info.os = 'macOS';
  } else if (/Linux/i.test(userAgent)) {
    info.os = 'Linux';
  }

  // Detect browser
  if (/Chrome/i.test(userAgent) && !/Edge/i.test(userAgent)) {
    info.browser = 'Chrome';
  } else if (/Firefox/i.test(userAgent)) {
    info.browser = 'Firefox';
  } else if (/Safari/i.test(userAgent) && !/Chrome/i.test(userAgent)) {
    info.browser = 'Safari';
  } else if (/Edge/i.test(userAgent)) {
    info.browser = 'Edge';
  }

  return Object.keys(info).length > 0 ? info : null;
}

/**
 * Extract user ID from JWT token without verification (for logging purposes)
 * This allows us to log actions even before authentication middleware runs
 */
function extractUserIdFromToken(req: Request): number | undefined {
  // First try req.user (set by authenticate middleware if it already ran)
  if (req.user?.id) {
    return req.user.id;
  }

  // Try to extract from JWT token in Authorization header
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    try {
      const token = authHeader.substring(7);
      // Decode without verification (we just need the userId for logging)
      // The authenticate middleware will verify it later
      const decoded = jwt.decode(token) as { userId?: number; id?: number } | null;
      if (decoded) {
        return decoded.userId || decoded.id;
      }
    } catch (error) {
      // Silently fail - token might be invalid, but that's handled by auth middleware
      logger.debug('Could not extract userId from token', {
        error: error instanceof Error ? error.message : String(error),
      });
    }
  }

  return undefined;
}

/**
 * User action logging middleware
 * Logs all user actions to the database
 * Captures all response types (json, send, end, etc.)
 */
export const userActionLogger = (
  req: Request,
  res: Response,
  next: NextFunction
): void => {
  const startTime = Date.now();
  
  // Skip logging for health check and swagger docs
  if (req.path === '/health' || req.path.startsWith('/api-docs')) {
    return next();
  }

  // Skip logging for non-authenticated requests (except login/signup/forgot-password)
  const isAuthEndpoint = req.path.includes('/login') || 
                         req.path.includes('/signup') || 
                         req.path.includes('/forgot-password') ||
                         req.path.includes('/verify-otp') ||
                         req.path.includes('/reset-password');
  
  // Extract userId from token or req.user
  const userId = extractUserIdFromToken(req);
  
  if (!userId && !isAuthEndpoint) {
    return next();
  }

  // Store response data
  let responseBody: any = null;
  let responseSent = false;

  // Helper function to log the action
  const logAction = (statusCode: number, body?: any) => {
    if (responseSent) return; // Prevent double logging
    responseSent = true;

    const duration = Date.now() - startTime;
    const { type, category } = extractActionType(req.path, req.method);

    // Determine final userId - extract from response for successful signup/login
    let finalUserId = userId;
    
    // For auth endpoints, try to extract userId from response body (for successful signup/login)
    // This is important because login/signup responses contain the user ID
    if (!finalUserId && isAuthEndpoint && body && body.data) {
      if (body.data.user && body.data.user.id) {
        finalUserId = body.data.user.id;
      } else if (body.data.id) {
        // Sometimes user data is directly in data
        finalUserId = body.data.id;
      } else if (body.data.userId) {
        // Check for userId field directly
        finalUserId = body.data.userId;
      }
    }
    
    // Also try to extract from token again in case it wasn't available earlier
    if (!finalUserId) {
      finalUserId = extractUserIdFromToken(req);
    }

    // Skip logging if no userId (to avoid foreign key constraint violation)
    // Only log authenticated actions or successful signup/login where userId is available
    if (!finalUserId) {
      // Log debug info for troubleshooting
      logger.debug('Skipping user action log - no userId available', {
        endpoint: req.path,
        method: req.method,
        isAuthEndpoint,
        hasBody: !!body,
        hasData: !!(body && body.data),
      });
      return;
    }

    // Prepare action log
    const actionInput = {
      userId: finalUserId,
      actionType: type,
      actionCategory: category,
      endpoint: req.path,
      method: req.method,
      requestBody: sanitizeRequestBody(req.body),
      responseStatus: statusCode,
      responseBody: truncateResponseBody(body || responseBody),
      ipAddress: req.ip || req.socket.remoteAddress || (Array.isArray(req.headers['x-forwarded-for']) ? req.headers['x-forwarded-for'][0] : req.headers['x-forwarded-for']) || null,
      userAgent: req.get('user-agent') || null,
      deviceInfo: extractDeviceInfo(req.get('user-agent')),
      sessionId: req.headers['x-session-id'] as string || null,
      durationMs: duration,
      errorMessage: statusCode >= 400 ? (body?.error?.message || body?.message || body?.error || 'Request failed') : null,
      metadata: {
        query: Object.keys(req.query).length > 0 ? req.query : undefined,
        params: Object.keys(req.params).length > 0 ? req.params : undefined,
        headers: {
          contentType: req.get('content-type'),
          accept: req.get('accept'),
        },
      },
    };

    // Log action asynchronously (don't block response)
    userActionService.logAction(actionInput).catch((error) => {
      // Log error details for debugging
      logger.error('Failed to log user action in middleware', {
        error: error instanceof Error ? error.message : String(error),
        errorStack: error instanceof Error ? error.stack : undefined,
        endpoint: req.path,
        method: req.method,
        userId: finalUserId,
        actionType: type,
        actionCategory: category,
        statusCode,
      });
    });
  };

  // Override res.json to capture response
  const originalJson = res.json.bind(res);
  res.json = function (body: any) {
    responseBody = body;
    // Get status code - use res.statusCode or default to 200
    const statusCode = res.statusCode || 200;
    logAction(statusCode, body);
    return originalJson.call(this, body);
  };

  // Override res.send to capture response
  const originalSend = res.send.bind(res);
  res.send = function (body: any) {
    responseBody = body;
    const statusCode = res.statusCode || 200;
    logAction(statusCode, body);
    return originalSend.call(this, body);
  };

  // Override res.end to capture response
  const originalEnd = res.end.bind(res);
  res.end = function (chunk?: any, encoding?: any) {
    if (chunk) {
      try {
        const chunkStr = chunk.toString();
        // Try to parse as JSON if it looks like JSON
        if (chunkStr.trim().startsWith('{') || chunkStr.trim().startsWith('[')) {
          responseBody = JSON.parse(chunkStr);
        } else {
          responseBody = chunkStr;
        }
      } catch {
        responseBody = chunk?.toString() || null;
      }
    }
    const statusCode = res.statusCode || 200;
    logAction(statusCode, responseBody);
    return originalEnd.call(this, chunk, encoding);
  };

  // Log on finish (fallback) - this ensures we capture the response even if other methods fail
  res.on('finish', () => {
    if (!responseSent) {
      const statusCode = res.statusCode || 200;
      logAction(statusCode, responseBody);
    }
  });

  next();
};
