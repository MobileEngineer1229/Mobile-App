import winston from 'winston';
import path from 'path';
import fs from 'fs';

/**
 * Get server start time (formatted as HH-MM-SS for readability)
 * This will be used as prefix for log filenames
 */
const getServerStartTime = (): string => {
  const now = new Date();
  const hours = now.getHours().toString().padStart(2, '0');
  const minutes = now.getMinutes().toString().padStart(2, '0');
  const seconds = now.getSeconds().toString().padStart(2, '0');
  return `${hours}-${minutes}-${seconds}`;
};

/**
 * Get current date folder (formatted as YYYY-MM-DD)
 */
const getDateFolder = (): string => {
  const now = new Date();
  const year = now.getFullYear();
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  const day = now.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
};

/**
 * Ensure logs directory structure exists
 */
const ensureLogDirectory = (dateFolder: string): string => {
  const logsBaseDir = path.join(process.cwd(), 'logs');
  const dateDir = path.join(logsBaseDir, dateFolder);
  
  if (!fs.existsSync(logsBaseDir)) {
    fs.mkdirSync(logsBaseDir, { recursive: true });
  }
  
  if (!fs.existsSync(dateDir)) {
    fs.mkdirSync(dateDir, { recursive: true });
  }
  
  return dateDir;
};

// Initialize server start time and date folder
const serverStartTime = getServerStartTime();
const dateFolder = getDateFolder();
const logDirectory = ensureLogDirectory(dateFolder);

/**
 * Custom log levels with priorities
 * Higher number = higher priority
 */
const customLevels = {
  levels: {
    error: 0,
    warn: 1,
    info: 2,
    success: 3,
    http: 4,
    verbose: 5,
    debug: 6,
    silly: 7,
  },
  colors: {
    error: 'red',
    warn: 'yellow',
    info: 'blue',
    success: 'green',
    http: 'magenta',
    verbose: 'cyan',
    debug: 'white',
    silly: 'grey',
  },
};

/**
 * Enhanced emoji constants for logging - Strong emoji system
 */
const EMOJIS = {
  // Status Emojis
  ERROR: '❌',
  WARN: '⚠️',
  INFO: 'ℹ️',
  SUCCESS: '✅',
  DEBUG: '🔍',
  VERBOSE: '📝',
  SILLY: '🎭',
  
  // Network & API Emojis
  HTTP: '🌐',
  API: '🌐',
  REQUEST: '📥',
  RESPONSE: '📤',
  NETWORK: '📡',
  CONNECTED: '🔗',
  DISCONNECTED: '🔌',
  
  // Database Emojis
  DATABASE: '🗄️',
  QUERY: '🔎',
  TRANSACTION: '💾',
  MIGRATION: '🔄',
  
  // Security & Auth Emojis
  AUTH: '🔐',
  SECURITY: '🛡️',
  LOCK: '🔒',
  UNLOCK: '🔓',
  TOKEN: '🎫',
  LOGIN: '🚪',
  LOGOUT: '🚶',
  
  // Device & IoT Emojis
  DEVICE: '📱',
  SMART_DEVICE: '🏠',
  LIGHT: '💡',
  THERMOSTAT: '🌡️',
  CAMERA: '📷',
  SENSOR: '📊',
  ACTUATOR: '⚙️',
  
  // User & Profile Emojis
  USER: '👤',
  PROFILE: '👥',
  AVATAR: '🦸',
  
  // Server & System Emojis
  SERVER: '🚀',
  STARTUP: '🎬',
  SHUTDOWN: '🛑',
  RESTART: '🔄',
  HEALTH: '💚',
  HEARTBEAT: '💓',
  
  // Notification & Communication Emojis
  NOTIFICATION: '🔔',
  MESSAGE: '💬',
  CHAT: '💭',
  EMAIL: '📧',
  ALERT: '🚨',
  BELL: '🔔',
  
  // Data & Analytics Emojis
  DATA: '📊',
  ANALYTICS: '📈',
  CHART: '📉',
  REPORT: '📋',
  STATS: '📊',
  ENERGY: '⚡',
  
  // Room & Location Emojis
  ROOM: '🏠',
  LOCATION: '📍',
  MAP: '🗺️',
  
  // Action Emojis
  CREATE: '➕',
  UPDATE: '✏️',
  DELETE: '🗑️',
  READ: '👁️',
  SEARCH: '🔍',
  FILTER: '🔽',
  SORT: '🔀',
  REFRESH: '🔄',
  SYNC: '🔄',
  
  // Validation & Check Emojis
  VALIDATION: '✔️',
  CHECK: '✅',
  CROSS: '❌',
  WARNING: '⚠️',
  
  // Time & Schedule Emojis
  TIME: '⏰',
  SCHEDULE: '📅',
  CALENDAR: '📆',
  CLOCK: '🕐',
  
  // Automation & Rules Emojis
  AUTOMATION: '🤖',
  RULE: '📜',
  TRIGGER: '⚡',
  ACTION: '🎯',
  
  // Settings & Configuration Emojis
  SETTINGS: '⚙️',
  CONFIG: '🔧',
  TOOLS: '🛠️',
  PREFERENCES: '🎛️',
  
  // File & Storage Emojis
  FILE: '📄',
  FOLDER: '📁',
  SAVE: '💾',
  UPLOAD: '📤',
  DOWNLOAD: '📥',
  
  // Miscellaneous Emojis
  FIRE: '🔥',
  STAR: '⭐',
  HEART: '❤️',
  THUMBS_UP: '👍',
  THUMBS_DOWN: '👎',
  ROCKET: '🚀',
  PARTY: '🎉',
  TROPHY: '🏆',
  GIFT: '🎁',
  MAGIC: '✨',
  SPARKLES: '✨',
  LIGHTNING: '⚡',
  RAINBOW: '🌈',
} as const;

/**
 * Custom format for file output with datetime stamp first
 */
const fileFormat = winston.format.printf((info) => {
  const { level, message, timestamp, emoji, context, service, ...meta } = info;
  
  // Build metadata string, excluding known fields
  const cleanMeta: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(meta)) {
    if (key !== 'emoji' && key !== 'context' && key !== 'timestamp' && key !== 'service') {
      cleanMeta[key] = value;
    }
  }
  
  const metaStr = Object.keys(cleanMeta).length ? ` ${JSON.stringify(cleanMeta)}` : '';
  const contextStr = context ? `[${context}]` : '';
  
  // Datetime stamp first, then rest of the log entry
  return `${timestamp} [${level.toUpperCase()}] ${contextStr} ${message}${metaStr}`.trim();
});

/**
 * Custom format for console output with emojis
 */
const emojiFormat = winston.format.printf(({ level, message, timestamp, emoji, context, ...meta }) => {
  const emojiStr = emoji || '';
  const contextStr = context ? `[${context}]` : '';
  const metaStr = Object.keys(meta).length ? JSON.stringify(meta, null, 2) : '';
  
  return `${emojiStr} ${timestamp} ${level} ${contextStr} ${message} ${metaStr}`;
});

/**
 * Create Winston logger instance with custom levels
 */
const logger = winston.createLogger({
  levels: customLevels.levels,
  level: process.env.NODE_ENV === 'production' ? 'info' : 'debug',
  format: winston.format.combine(
    winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
    winston.format.errors({ stack: true }),
    winston.format.splat(),
    winston.format.json()
  ),
  defaultMeta: { service: 'smart-home-backend' },
  transports: [
    new winston.transports.File({ 
      filename: path.join(logDirectory, `${serverStartTime}-error.log`), 
      level: 'error',
      format: winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss.SSS' }),
        fileFormat
      )
    }),
    new winston.transports.File({ 
      filename: path.join(logDirectory, `${serverStartTime}-warn.log`),
      level: 'warn',
      format: winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss.SSS' }),
        fileFormat
      )
    }),
    new winston.transports.File({ 
      filename: path.join(logDirectory, `${serverStartTime}-success.log`),
      level: 'success',
      format: winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss.SSS' }),
        fileFormat
      )
    }),
    new winston.transports.File({ 
      filename: path.join(logDirectory, `${serverStartTime}-combined.log`),
      format: winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss.SSS' }),
        fileFormat
      )
    }),
  ],
});

// Add colors to winston
winston.addColors(customLevels.colors);

/**
 * Console transport with emojis for development
 */
if (process.env.NODE_ENV !== 'production') {
  logger.add(
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.colorize(),
        winston.format.timestamp({ format: 'HH:mm:ss' }),
        emojiFormat
      ),
    })
  );
} else {
  // Production console without emojis but still formatted
  logger.add(
    new winston.transports.Console({
      format: winston.format.combine(
        winston.format.timestamp(),
        winston.format.json()
      ),
    })
  );
}

/**
 * Enhanced logger interface with emoji support and multiple log levels
 */
interface EnhancedLogger extends winston.Logger {
  // Direct level methods with emojis
  errorWithEmoji: (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => void;
  warnWithEmoji: (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => void;
  infoWithEmoji: (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => void;
  successWithEmoji: (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => void;
  debugWithEmoji: (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => void;
  verboseWithEmoji: (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => void;
  httpWithEmoji: (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => void;
  
  // Convenience methods for each level
  error: winston.LeveledLogMethod;
  warn: winston.LeveledLogMethod;
  info: winston.LeveledLogMethod;
  success: winston.LeveledLogMethod;
  debug: winston.LeveledLogMethod;
  verbose: winston.LeveledLogMethod;
  http: winston.LeveledLogMethod;
  silly: winston.LeveledLogMethod;
  
  // Database logs
  db: {
    connect: (message: string, meta?: Record<string, unknown>) => void;
    query: (message: string, meta?: Record<string, unknown>) => void;
    error: (message: string, meta?: Record<string, unknown>) => void;
    transaction: (message: string, meta?: Record<string, unknown>) => void;
    migration: (message: string, meta?: Record<string, unknown>) => void;
    success: (message: string, meta?: Record<string, unknown>) => void;
  };
  
  // API logs
  api: {
    request: (method: string, path: string, meta?: Record<string, unknown>) => void;
    response: (method: string, path: string, statusCode: number, meta?: Record<string, unknown>) => void;
    error: (method: string, path: string, error: string, meta?: Record<string, unknown>) => void;
    success: (method: string, path: string, message: string, meta?: Record<string, unknown>) => void;
  };
  
  // Auth logs
  auth: {
    login: (email: string, success: boolean, meta?: Record<string, unknown>) => void;
    signup: (email: string, success: boolean, meta?: Record<string, unknown>) => void;
    token: (action: string, meta?: Record<string, unknown>) => void;
    unauthorized: (message: string, meta?: Record<string, unknown>) => void;
    logout: (email: string, meta?: Record<string, unknown>) => void;
    passwordReset: (email: string, success: boolean, meta?: Record<string, unknown>) => void;
    success: (message: string, meta?: Record<string, unknown>) => void;
  };
  
  // Device logs
  device: {
    create: (deviceId: number, deviceName: string, userId: number, meta?: Record<string, unknown>) => void;
    update: (deviceId: number, deviceName: string, userId: number, meta?: Record<string, unknown>) => void;
    delete: (deviceId: number, userId: number, meta?: Record<string, unknown>) => void;
    status: (deviceId: number, status: string, meta?: Record<string, unknown>) => void;
    connection: (deviceId: number, connected: boolean, meta?: Record<string, unknown>) => void;
    success: (deviceId: number, message: string, meta?: Record<string, unknown>) => void;
  };
  
  // User logs
  user: {
    create: (userId: number, email: string, meta?: Record<string, unknown>) => void;
    update: (userId: number, email: string, meta?: Record<string, unknown>) => void;
    profile: (userId: number, action: string, meta?: Record<string, unknown>) => void;
    success: (userId: number, message: string, meta?: Record<string, unknown>) => void;
  };
  
  // Server logs
  server: {
    start: (port: number, env: string) => void;
    shutdown: (reason: string) => void;
    health: (status: string, meta?: Record<string, unknown>) => void;
    success: (message: string, meta?: Record<string, unknown>) => void;
  };
}

/**
 * Helper function to call custom log levels safely
 */
const logWithLevel = (level: string, message: string, meta?: Record<string, unknown>): void => {
  (logger as unknown as Record<string, (message: string, meta?: Record<string, unknown>) => void>)[level](message, meta);
};

/**
 * Add emoji-based logging methods
 */
const enhancedLogger = logger as EnhancedLogger;

// Generic emoji logging methods for all levels
enhancedLogger.errorWithEmoji = (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => {
  logger.error(message, { emoji, context, ...meta });
};

enhancedLogger.warnWithEmoji = (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => {
  logger.warn(message, { emoji, context, ...meta });
};

enhancedLogger.infoWithEmoji = (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => {
  logger.info(message, { emoji, context, ...meta });
};

enhancedLogger.successWithEmoji = (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => {
  logWithLevel('success', message, { emoji, context, ...meta });
};

enhancedLogger.debugWithEmoji = (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => {
  logger.debug(message, { emoji, context, ...meta });
};

enhancedLogger.verboseWithEmoji = (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => {
  logger.verbose(message, { emoji, context, ...meta });
};

enhancedLogger.httpWithEmoji = (emoji: string, message: string, context?: string, meta?: Record<string, unknown>) => {
  logWithLevel('http', message, { emoji, context, ...meta });
};

// Database logging methods
enhancedLogger.db = {
  connect: (message: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', message, { emoji: EMOJIS.CONNECTED, context: 'DATABASE', ...meta });
  },
  query: (message: string, meta?: Record<string, unknown>) => {
    logger.debug(message, { emoji: EMOJIS.QUERY, context: 'DATABASE', ...meta });
  },
  error: (message: string, meta?: Record<string, unknown>) => {
    logger.error(message, { emoji: EMOJIS.ERROR, context: 'DATABASE', ...meta });
  },
  transaction: (message: string, meta?: Record<string, unknown>) => {
    logger.info(message, { emoji: EMOJIS.TRANSACTION, context: 'DATABASE', ...meta });
  },
  migration: (message: string, meta?: Record<string, unknown>) => {
    logger.info(message, { emoji: EMOJIS.MIGRATION, context: 'DATABASE', ...meta });
  },
  success: (message: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', message, { emoji: EMOJIS.SUCCESS, context: 'DATABASE', ...meta });
  },
};

// API logging methods
enhancedLogger.api = {
  request: (method: string, path: string, meta?: Record<string, unknown>) => {
    const methodEmoji = method === 'GET' ? '📥' : method === 'POST' ? '➕' : method === 'PUT' ? '✏️' : method === 'DELETE' ? '🗑️' : EMOJIS.REQUEST;
    logger.info(`${method} ${path}`, { 
      emoji: methodEmoji, 
      context: 'API', 
      method, 
      path, 
      ...meta 
    });
  },
  response: (method: string, path: string, statusCode: number, meta?: Record<string, unknown>) => {
    let emoji: string = EMOJIS.SUCCESS;
    if (statusCode >= 500) emoji = EMOJIS.ERROR;
    else if (statusCode >= 400) emoji = EMOJIS.WARN;
    else if (statusCode >= 300) emoji = EMOJIS.REFRESH;
    else if (statusCode === 201) emoji = EMOJIS.CREATE;
    else if (statusCode === 204) emoji = EMOJIS.DELETE;
    
    const level = statusCode >= 400 ? 'error' : statusCode >= 300 ? 'warn' : 'success';
    logWithLevel(level, `${method} ${path} - ${statusCode}`, { 
      emoji, 
      context: 'API', 
      method, 
      path, 
      statusCode, 
      ...meta 
    });
  },
  error: (method: string, path: string, error: string, meta?: Record<string, unknown>) => {
    logger.error(`${method} ${path} - ${error}`, { 
      emoji: EMOJIS.ERROR, 
      context: 'API', 
      method, 
      path, 
      error, 
      ...meta 
    });
  },
  success: (method: string, path: string, message: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', `${method} ${path} - ${message}`, { 
      emoji: EMOJIS.SUCCESS, 
      context: 'API', 
      method, 
      path, 
      ...meta 
    });
  },
};

// Auth logging methods
enhancedLogger.auth = {
  login: (email: string, success: boolean, meta?: Record<string, unknown>) => {
    const emoji = success ? EMOJIS.LOGIN : EMOJIS.ERROR;
    const message = success ? `Login successful: ${email}` : `Login failed: ${email}`;
    const level = success ? 'success' : 'warn';
    logWithLevel(level, message, { emoji, context: 'AUTH', email, success, ...meta });
  },
  signup: (email: string, success: boolean, meta?: Record<string, unknown>) => {
    const emoji = success ? EMOJIS.USER : EMOJIS.ERROR;
    const message = success ? `Signup successful: ${email}` : `Signup failed: ${email}`;
    const level = success ? 'success' : 'warn';
    logWithLevel(level, message, { emoji, context: 'AUTH', email, success, ...meta });
  },
  token: (action: string, meta?: Record<string, unknown>) => {
    logger.info(`Token ${action}`, { emoji: EMOJIS.TOKEN, context: 'AUTH', action, ...meta });
  },
  unauthorized: (message: string, meta?: Record<string, unknown>) => {
    logger.warn(message, { emoji: EMOJIS.LOCK, context: 'AUTH', ...meta });
  },
  logout: (email: string, meta?: Record<string, unknown>) => {
    logger.info(`Logout: ${email}`, { emoji: EMOJIS.LOGOUT, context: 'AUTH', email, ...meta });
  },
  passwordReset: (email: string, success: boolean, meta?: Record<string, unknown>) => {
    const emoji = success ? EMOJIS.UNLOCK : EMOJIS.ERROR;
    const level = success ? 'success' : 'warn';
    logWithLevel(level, `Password reset ${success ? 'successful' : 'failed'}: ${email}`, { emoji, context: 'AUTH', email, success, ...meta });
  },
  success: (message: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', message, { emoji: EMOJIS.SUCCESS, context: 'AUTH', ...meta });
  },
};

// Device logging methods
enhancedLogger.device = {
  create: (deviceId: number, deviceName: string, userId: number, meta?: Record<string, unknown>) => {
    logWithLevel('success', `Device created: ${deviceName} (ID: ${deviceId})`, { 
      emoji: EMOJIS.DEVICE, 
      context: 'DEVICE', 
      deviceId, 
      deviceName, 
      userId, 
      ...meta 
    });
  },
  update: (deviceId: number, deviceName: string, userId: number, meta?: Record<string, unknown>) => {
    logWithLevel('success', `Device updated: ${deviceName} (ID: ${deviceId})`, { 
      emoji: EMOJIS.DEVICE, 
      context: 'DEVICE', 
      deviceId, 
      deviceName, 
      userId, 
      ...meta 
    });
  },
  delete: (deviceId: number, userId: number, meta?: Record<string, unknown>) => {
    logger.info(`Device deleted (ID: ${deviceId})`, { 
      emoji: EMOJIS.DEVICE, 
      context: 'DEVICE', 
      deviceId, 
      userId, 
      ...meta 
    });
  },
  status: (deviceId: number, status: string, meta?: Record<string, unknown>) => {
    logger.info(`Device status changed (ID: ${deviceId}): ${status}`, { 
      emoji: EMOJIS.DEVICE, 
      context: 'DEVICE', 
      deviceId, 
      status, 
      ...meta 
    });
  },
  connection: (deviceId: number, connected: boolean, meta?: Record<string, unknown>) => {
    const emoji = connected ? EMOJIS.CONNECTED : EMOJIS.DISCONNECTED;
    const level = connected ? 'success' : 'warn';
    logWithLevel(level, `Device ${connected ? 'connected' : 'disconnected'} (ID: ${deviceId})`, { 
      emoji, 
      context: 'DEVICE', 
      deviceId, 
      connected, 
      ...meta 
    });
  },
  success: (deviceId: number, message: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', `Device ${message} (ID: ${deviceId})`, { 
      emoji: EMOJIS.SUCCESS, 
      context: 'DEVICE', 
      deviceId, 
      ...meta 
    });
  },
};

// User logging methods
enhancedLogger.user = {
  create: (userId: number, email: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', `User created: ${email} (ID: ${userId})`, { 
      emoji: EMOJIS.USER, 
      context: 'USER', 
      userId, 
      email, 
      ...meta 
    });
  },
  update: (userId: number, email: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', `User updated: ${email} (ID: ${userId})`, { 
      emoji: EMOJIS.USER, 
      context: 'USER', 
      userId, 
      email, 
      ...meta 
    });
  },
  profile: (userId: number, action: string, meta?: Record<string, unknown>) => {
    logger.info(`User profile ${action} (ID: ${userId})`, { 
      emoji: EMOJIS.USER, 
      context: 'USER', 
      userId, 
      action, 
      ...meta 
    });
  },
  success: (userId: number, message: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', `User ${message} (ID: ${userId})`, { 
      emoji: EMOJIS.SUCCESS, 
      context: 'USER', 
      userId, 
      ...meta 
    });
  },
};

// Server logging methods
enhancedLogger.server = {
  start: (port: number, env: string) => {
    logWithLevel('success', `Server started on port ${port}`, { 
      emoji: EMOJIS.STARTUP, 
      context: 'SERVER', 
      port, 
      env 
    });
  },
  shutdown: (reason: string) => {
    logger.warn(`Server shutting down: ${reason}`, { 
      emoji: EMOJIS.SHUTDOWN, 
      context: 'SERVER', 
      reason 
    });
  },
  health: (status: string, meta?: Record<string, unknown>) => {
    const level = status === 'ok' ? 'success' : 'warn';
    logWithLevel(level, `Health check: ${status}`, { 
      emoji: EMOJIS.HEALTH, 
      context: 'SERVER', 
      status, 
      ...meta 
    });
  },
  success: (message: string, meta?: Record<string, unknown>) => {
    logWithLevel('success', message, { 
      emoji: EMOJIS.SUCCESS, 
      context: 'SERVER', 
      ...meta 
    });
  },
};

// Export logger with emojis
export default enhancedLogger;
export { EMOJIS };
