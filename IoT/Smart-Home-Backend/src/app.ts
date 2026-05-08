/// <reference path="./types/express.d.ts" />
import express, { Application } from 'express';
import http from 'http';
import https from 'https';
import fs from 'fs';
import cors from 'cors';
import helmet from 'helmet';
import compression from 'compression';
import { env } from './config/env';
import { testConnection, getPool } from './config/database';
import { runMigrations } from './utils/migrations';
import { errorHandler } from './middleware/error-handler';
import { requestLogger } from './middleware/request-logger';
import { userActionLogger } from './middleware/user-action-logger';
import logger from './utils/logger';
import swaggerUi from 'swagger-ui-express';
import { swaggerSpec } from './swagger/swagger';
import userRoutes from './routes/user-routes';
import deviceRoutes from './routes/device-routes';
import deviceControlRoutes from './routes/device-control-routes';
import roomRoutes from './routes/room-routes';
import deviceDiscoveryRoutes from './routes/device-discovery-routes';
import deviceTypeRoutes from './routes/device-type-routes';
import reportRoutes from './routes/report-routes';
import deviceDataRoutes from './routes/device-data-routes';
import notificationRoutes from './routes/notification-routes';
import chatbotRoutes from './routes/chatbot-routes';
import voiceAssistantRoutes from './routes/voice-assistant-routes';
import homeRoutes from './routes/home-routes';
import homeMemberRoutes from './routes/home-member-routes';
import homeInvitationRoutes from './routes/home-invitation-routes';
import sceneRoutes from './routes/sceneRoutes';
import userActionRoutes from './routes/user-action-routes';
import appVersionRoutes from './routes/app-version-routes';
import { MqttClientService } from './services/mqtt/mqtt-client';
import { MqttMessageHandler } from './services/mqtt/mqtt-message-handler';
import { DeviceStatusWebSocketServer } from './services/websocket/websocket-server';
import { SceneScheduler } from './services/scene-scheduler';
import mqttAuthRoutes from './routes/mqtt-auth-routes';
import sseRoutes from './routes/sse-routes';

const app: Application = express();

// CORS configuration - MUST come BEFORE Helmet for Swagger to work
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: false,
}));

// Security middleware
// Configure Helmet for HTTP (disable HTTPS-only headers)
app.use(
  helmet({
    contentSecurityPolicy: false, // Disable CSP for development
    crossOriginOpenerPolicy: false, // Allow COOP for HTTP
    crossOriginEmbedderPolicy: false, // Allow COEP for HTTP
    originAgentCluster: false, // Disable origin-keyed agent cluster for HTTP
  })
);

// Body parsing middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Compression middleware
app.use(compression());

// Request logging middleware (before routes)
app.use(requestLogger);

// User action logging middleware (after request logger, before routes)
// This will log all user actions to the database
app.use(userActionLogger);

// Swagger UI setup with custom options for network access
const swaggerUiOptions = {
  customCss: '.swagger-ui .topbar { display: none }',
  customSiteTitle: 'Smart Home Backend API',
  customfavIcon: '/favicon.ico',
  swaggerOptions: {
    persistAuthorization: true,
    displayRequestDuration: true,
    filter: true,
    tryItOutEnabled: true,
  },
};

// Setup Swagger UI - serve static files and setup UI
// swaggerUi.serve serves the static files (CSS, JS, etc.)
// swaggerUi.setup serves the HTML page
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec, swaggerUiOptions));

// Health check endpoint
app.get('/health', (_req, res) => {
  const mqttStatus = MqttClientService.getInstance().isConnected() ? 'connected' : 'disconnected';
  logger.server.health('ok', {
    timestamp: new Date().toISOString(),
    environment: env.nodeEnv,
  });
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    environment: env.nodeEnv,
    mqtt: mqttStatus,
  });
});

// API routes
const apiPrefix = `/api/${env.apiVersion}`;
app.use(`${apiPrefix}/users`, userRoutes);
// Register device-specific routes BEFORE device routes with :id parameter
// Order matters: more specific routes must come before parameterized routes
app.use(`${apiPrefix}/devices`, sseRoutes); // SSE /stream must come before /:id routes
app.use(`${apiPrefix}/devices`, deviceDiscoveryRoutes);
app.use(`${apiPrefix}/devices`, deviceTypeRoutes);
app.use(`${apiPrefix}/devices`, deviceDataRoutes); // Device data routes (before deviceRoutes to avoid conflicts)
app.use(`${apiPrefix}/devices`, deviceControlRoutes); // Device control routes (before deviceRoutes to avoid conflicts)
app.use(`${apiPrefix}/devices`, deviceRoutes); // This has /:id route, so it should be last
app.use(`${apiPrefix}/rooms`, roomRoutes);
app.use(`${apiPrefix}/reports`, reportRoutes);
app.use(`${apiPrefix}/notifications`, notificationRoutes);
app.use(`${apiPrefix}/chatbot`, chatbotRoutes);
app.use(`${apiPrefix}/voice-assistants`, voiceAssistantRoutes);
// Home routes must be ordered carefully - specific routes before parameterized routes
app.use(`${apiPrefix}/homes`, homeInvitationRoutes); // /homes/invitations/:code must come before /homes/:id
app.use(`${apiPrefix}/homes`, homeMemberRoutes); // /homes/:homeId/members must come before /homes/:id
app.use(`${apiPrefix}/homes`, homeRoutes); // /homes/:id comes last
app.use(`${apiPrefix}/scenes`, sceneRoutes);
app.use(`${apiPrefix}/mqtt`, mqttAuthRoutes);
app.use(`${apiPrefix}/user-actions`, userActionRoutes);
app.use(`${apiPrefix}/app`, appVersionRoutes);

// 404 handler
app.use((_req, res) => {
  res.status(404).json({
    success: false,
    error: {
      code: 'NOT_FOUND',
      message: 'Route not found',
    },
    meta: {
      timestamp: new Date().toISOString(),
    },
  });
});

// Error handling middleware (must be last)
app.use(errorHandler);

/**
 * Start the server
 */
const startServer = async (): Promise<void> => {
  try {
    // Test database connection
    await testConnection();

    // Run migrations automatically (if enabled)
    if (env.autoMigrate) {
      const pool = getPool();
      await runMigrations(pool);
      
      // Verify user_actions table exists after migrations
      const { UserActionRepository } = await import('./repositories/user-action-repository');
      const userActionRepo = new UserActionRepository(pool);
      const tableExists = await userActionRepo.verifyTableExists();
      if (tableExists) {
        logger.infoWithEmoji('✅', 'User actions table verified', 'USER_ACTION');
      } else {
        logger.errorWithEmoji('❌', 'User actions table not found. Please run migration: migration_add_user_actions.sql', 'USER_ACTION');
      }
    } else {
      logger.infoWithEmoji('⏭️', 'Automatic migrations disabled', 'MIGRATION');
    }

    // Start server - bind to all interfaces (0.0.0.0) to allow Android emulator access via 10.0.2.2
    let server: http.Server | https.Server;

    if (env.enableHttps && env.sslCertPath && env.sslKeyPath) {
      // HTTPS server
      try {
        const httpsOptions = {
          cert: fs.readFileSync(env.sslCertPath),
          key: fs.readFileSync(env.sslKeyPath),
        };

        // Use the same port for HTTPS if HTTPS_PORT not specified
        const httpsPort = env.httpsPort || env.port;
        server = https.createServer(httpsOptions, app).listen(httpsPort, '0.0.0.0', () => {
          logger.server.start(httpsPort, env.nodeEnv);
          logger.infoWithEmoji('🔒', `HTTPS Server running on port ${httpsPort}`, 'SERVER');
          logger.infoWithEmoji('🌐', `API Base URL: https://localhost:${httpsPort}/api/v1/`, 'SERVER');
          logger.infoWithEmoji('💻', `Network HTTPS: https://172.86.88.76:${httpsPort}/api/v1/`, 'SERVER');
          logger.infoWithEmoji('📚', `Swagger UI: https://localhost:${httpsPort}/api-docs`, 'SERVER');
          logger.infoWithEmoji('📚', `Swagger UI (Network): https://172.86.88.76:${httpsPort}/api-docs`, 'SERVER');
        });
      } catch (error) {
        logger.errorWithEmoji('❌', 'Failed to start HTTPS server', 'SERVER', {
          error: error instanceof Error ? error.message : String(error),
        });
        logger.infoWithEmoji('ℹ️', 'Falling back to HTTP server', 'SERVER');
        // Fallback to HTTP
        server = http.createServer(app).listen(env.port, '0.0.0.0', () => {
          logger.server.start(env.port, env.nodeEnv);
          logger.infoWithEmoji('🌐', `API Base URL: http://localhost:${env.port}/api/v1/`, 'SERVER');
          logger.infoWithEmoji('💻', `Network HTTP: http://172.86.88.76:${env.port}/api/v1/`, 'SERVER');
          logger.infoWithEmoji('📚', `Swagger UI: http://localhost:${env.port}/api-docs`, 'SERVER');
        });
      }
    } else {
      // HTTP server (default)
      server = http.createServer(app).listen(env.port, '0.0.0.0', () => {
        logger.server.start(env.port, env.nodeEnv);
        logger.infoWithEmoji('🌐', `API Base URL: http://localhost:${env.port}/api/v1/`, 'SERVER');
        logger.infoWithEmoji('📱', `Android Emulator: http://10.0.2.2:${env.port}/api/v1/`, 'SERVER');
        logger.infoWithEmoji('💻', `Network HTTP: http://172.86.88.76:${env.port}/api/v1/`, 'SERVER');
        logger.infoWithEmoji('📚', `Swagger UI (Local): http://localhost:${env.port}/api-docs`, 'SERVER');
        logger.infoWithEmoji('📚', `Swagger UI (Network): http://172.86.88.76:${env.port}/api-docs`, 'SERVER');
        logger.infoWithEmoji('🔌', `WebSocket endpoint: ws://172.86.88.76:${env.port}/api/v1/devices/stream`, 'WEBSOCKET');
      });
    }

    // Initialize WebSocket server
    const wsServer = new DeviceStatusWebSocketServer(server);

    // Connect MQTT client and initialize message handler with WebSocket support
    try {
      const mqttClient = MqttClientService.getInstance();
      await mqttClient.connect();

      const pool = getPool();
      const mqttHandler = new MqttMessageHandler(pool, wsServer);
      await mqttHandler.initialize();

      // Start the scene automation scheduler (checks time-based conditions every 60s)
      const scheduler = SceneScheduler.getInstance();
      scheduler.start();

      // Graceful shutdown
      const shutdown = () => {
        scheduler.stop();
        mqttClient.disconnect();
      };
      process.on('SIGINT', shutdown);
      process.on('SIGTERM', shutdown);
    } catch (error) {
      logger.warnWithEmoji('⚠️', 'MQTT connection failed — continuing without MQTT', 'MQTT', {
        error: error instanceof Error ? error.message : String(error),
      });
    }
  } catch (error) {
    logger.errorWithEmoji('❌', 'Failed to start server', 'SERVER', {
      error: error instanceof Error ? error.message : String(error),
    });
    process.exit(1);
  }
};

// Start server if this file is run directly
if (require.main === module) {
  startServer();
}

export default app;

