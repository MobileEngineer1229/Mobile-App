import { Router } from 'express';
import { DeviceDiscoveryController } from '../controllers/device-discovery-controller';
import { authenticate } from '../middleware/auth';

const router: Router = Router();

// Initialize controller
const deviceDiscoveryController = new DeviceDiscoveryController();

/**
 * @swagger
 * /devices/discover:
 *   get:
 *     summary: Discover nearby devices on the network
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     description: |
 *       Scans the local network for discoverable IoT devices.
 *       In production, this would perform actual network scanning.
 *       Currently returns mock data for development.
 *     responses:
 *       200:
 *         description: List of discovered devices
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: object
 *                   properties:
 *                     devices:
 *                       type: array
 *                       items:
 *                         type: object
 *                         properties:
 *                           name:
 *                             type: string
 *                             example: Smart Lamp
 *                           type:
 *                             type: string
 *                             example: Lighting
 *                           macAddress:
 *                             type: string
 *                             example: AA:BB:CC:DD:EE:01
 *                           ipAddress:
 *                             type: string
 *                             example: 192.168.1.101
 *                           signalStrength:
 *                             type: number
 *                             example: 85
 *                           isPaired:
 *                             type: boolean
 *                             example: false
 *                     timestamp:
 *                       type: string
 *                       format: date-time
 *       401:
 *         description: Unauthorized
 */
router.get('/discover', authenticate, deviceDiscoveryController.discoverDevices);

/**
 * @swagger
 * /devices/nearby:
 *   get:
 *     summary: Discover nearby devices on the network (alias for /discover)
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     description: |
 *       Alias endpoint for /devices/discover.
 *       Scans the local network for discoverable IoT devices.
 *     responses:
 *       200:
 *         description: List of discovered devices
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: object
 *                   properties:
 *                     devices:
 *                       type: array
 *                       items:
 *                         type: object
 *                     timestamp:
 *                       type: string
 *                       format: date-time
 *       401:
 *         description: Unauthorized
 */
router.get('/nearby', authenticate, deviceDiscoveryController.discoverDevices);

export default router;

