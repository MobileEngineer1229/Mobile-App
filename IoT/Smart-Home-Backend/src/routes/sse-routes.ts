import { Router, Request, Response } from 'express';
import { authenticate } from '../middleware/auth';
import { DeviceEventBus, DeviceUpdateEvent } from '../services/sse/device-event-bus';
import logger from '../utils/logger';

const router: Router = Router();

/**
 * GET /devices/stream
 * Server-Sent Events endpoint for real-time device updates.
 * Requires JWT authentication.
 *
 * Events sent:
 * - event: device-update
 *   data: { productId, deviceNum, type, data, timestamp }
 */
router.get('/stream', authenticate, (req: Request, res: Response) => {
  res.writeHead(200, {
    'Content-Type': 'text/event-stream',
    'Cache-Control': 'no-cache',
    Connection: 'keep-alive',
    'X-Accel-Buffering': 'no', // Disable nginx buffering
  });

  // Send initial connection event
  res.write(`event: connected\ndata: ${JSON.stringify({ message: 'SSE connected' })}\n\n`);

  const eventBus = DeviceEventBus.getInstance();

  const onDeviceUpdate = (event: DeviceUpdateEvent) => {
    res.write(`event: device-update\ndata: ${JSON.stringify(event)}\n\n`);
  };

  eventBus.on('device-update', onDeviceUpdate);

  // Keep-alive ping every 30 seconds
  const keepAlive = setInterval(() => {
    res.write(`: ping\n\n`);
  }, 30000);

  // Cleanup on disconnect
  req.on('close', () => {
    eventBus.removeListener('device-update', onDeviceUpdate);
    clearInterval(keepAlive);
    logger.infoWithEmoji('🔌', 'SSE client disconnected', 'SSE');
  });

  logger.infoWithEmoji('📡', 'SSE client connected', 'SSE', {
    userId: (req as unknown as Record<string, unknown>).userId,
  });
});

export default router;
