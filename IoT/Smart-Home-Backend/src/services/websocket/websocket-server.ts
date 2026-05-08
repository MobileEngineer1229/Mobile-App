import { WebSocketServer, WebSocket } from 'ws';
import { IncomingMessage, Server } from 'http';
import jwt from 'jsonwebtoken';
import { env } from '../../config/env';
import logger from '../../utils/logger';

/**
 * WebSocket server for real-time device status updates.
 * Clients authenticate via JWT and receive device status changes from MQTT.
 */
export class DeviceStatusWebSocketServer {
  private wss: WebSocketServer;
  private clients: Map<number, Set<WebSocket>> = new Map(); // userId -> Set of WebSocket connections

  constructor(server: Server) {
    this.wss = new WebSocketServer({
      server,
      path: '/api/v1/devices/stream'
    });

    this.wss.on('connection', (ws: WebSocket, request: IncomingMessage) => {
      this.handleConnection(ws, request);
    });

    logger.infoWithEmoji('🔌', 'WebSocket server initialized at /api/v1/devices/stream', 'WEBSOCKET');
  }

  /**
   * Handle new WebSocket connection
   */
  private handleConnection(ws: WebSocket, request: IncomingMessage) {
    // Extract JWT token from query string or headers
    const url = new URL(request.url || '', `http://${request.headers.host}`);
    const token = url.searchParams.get('token') ||
                  request.headers.authorization?.replace('Bearer ', '');

    if (!token) {
      logger.warn('WebSocket connection rejected: No token provided');
      ws.close(4001, 'Authentication required');
      return;
    }

    // Verify JWT token
    try {
      const decoded = jwt.verify(token, env.jwtSecret) as { id: number; email: string };
      const userId = decoded.id;

      // Store client connection
      if (!this.clients.has(userId)) {
        this.clients.set(userId, new Set());
      }
      this.clients.get(userId)!.add(ws);

      logger.infoWithEmoji('✅', `WebSocket client connected: userId=${userId}`, 'WEBSOCKET');

      // Send initial connection success message
      ws.send(JSON.stringify({
        type: 'connected',
        message: 'WebSocket connection established',
        timestamp: new Date().toISOString()
      }));

      // Handle client messages (heartbeat, subscriptions, etc.)
      ws.on('message', (message: Buffer) => {
        this.handleClientMessage(ws, userId, message);
      });

      // Handle client disconnect
      ws.on('close', () => {
        const userClients = this.clients.get(userId);
        if (userClients) {
          userClients.delete(ws);
          if (userClients.size === 0) {
            this.clients.delete(userId);
          }
        }
        logger.infoWithEmoji('❌', `WebSocket client disconnected: userId=${userId}`, 'WEBSOCKET');
      });

      // Handle errors
      ws.on('error', (error) => {
        logger.errorWithEmoji('⚠️', 'WebSocket error', 'WEBSOCKET', { userId, error: error.message });
      });

    } catch (error) {
      logger.warn('WebSocket connection rejected: Invalid token');
      ws.close(4001, 'Invalid authentication token');
    }
  }

  /**
   * Handle incoming messages from clients
   */
  private handleClientMessage(ws: WebSocket, userId: number, message: Buffer) {
    try {
      const data = JSON.parse(message.toString());

      // Handle heartbeat/ping
      if (data.type === 'ping') {
        ws.send(JSON.stringify({ type: 'pong', timestamp: new Date().toISOString() }));
      }

    } catch (error) {
      logger.error('Error parsing WebSocket message', error);
    }
  }

  /**
   * Broadcast device status update to user's connected clients
   */
  public broadcastDeviceStatus(userId: number, deviceData: {
    productId: string;
    deviceNum: string;
    status: string;
    timestamp: string;
  }) {
    const userClients = this.clients.get(userId);
    if (!userClients || userClients.size === 0) {
      return; // No connected clients for this user
    }

    const message = JSON.stringify({
      type: 'device_status',
      data: deviceData
    });

    let sentCount = 0;
    userClients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
        sentCount++;
      }
    });

    if (sentCount > 0) {
      logger.debug(`Broadcast device status to ${sentCount} client(s) for userId=${userId}`);
    }
  }

  /**
   * Broadcast device property update to user's connected clients
   */
  public broadcastDeviceProperty(userId: number, propertyData: {
    productId: string;
    deviceNum: string;
    properties: any;
    timestamp: string;
  }) {
    const userClients = this.clients.get(userId);
    if (!userClients || userClients.size === 0) {
      return;
    }

    const message = JSON.stringify({
      type: 'device_property',
      data: propertyData
    });

    let sentCount = 0;
    userClients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
        sentCount++;
      }
    });

    if (sentCount > 0) {
      logger.debug(`Broadcast device property to ${sentCount} client(s) for userId=${userId}`);
    }
  }

  /**
   * Broadcast to all connected clients (admin notifications, etc.)
   */
  public broadcastToAll(message: any) {
    const payload = JSON.stringify(message);
    let sentCount = 0;

    this.clients.forEach((userClients) => {
      userClients.forEach((client) => {
        if (client.readyState === WebSocket.OPEN) {
          client.send(payload);
          sentCount++;
        }
      });
    });

    logger.debug(`Broadcast message to ${sentCount} total client(s)`);
  }

  /**
   * Get connected client count
   */
  public getConnectedClientCount(): number {
    let total = 0;
    this.clients.forEach((userClients) => {
      total += userClients.size;
    });
    return total;
  }

  /**
   * Get connected users count
   */
  public getConnectedUserCount(): number {
    return this.clients.size;
  }
}
