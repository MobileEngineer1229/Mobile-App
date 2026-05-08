import { Router } from 'express';
import { DeviceTypeController } from '../controllers/device-type-controller';
import { DeviceTypeRepository } from '../repositories/device-type-repository';
import { getPool } from '../config/database';

const router: Router = Router();

// Initialize repository and controller
const deviceTypeRepository = new DeviceTypeRepository(getPool());
const deviceTypeController = new DeviceTypeController(deviceTypeRepository);

/**
 * @swagger
 * /devices/types:
 *   get:
 *     summary: Get device type templates for manual device addition
 *     tags: [Devices]
 *     description: |
 *       Returns a list of device type templates organized by category.
 *       Used for the "Add Manual" feature in the mobile app.
 *     parameters:
 *       - in: query
 *         name: category
 *         schema:
 *           type: string
 *           enum: [Popular, Lightning, Camera, Electronics]
 *         description: Filter templates by category
 *     responses:
 *       200:
 *         description: List of device type templates
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
 *                     templates:
 *                       type: array
 *                       items:
 *                         type: object
 *                         properties:
 *                           id:
 *                             type: string
 *                             example: smart_lamp
 *                           name:
 *                             type: string
 *                             example: Smart Lamp
 *                           category:
 *                             type: string
 *                             example: Popular
 *                           description:
 *                             type: string
 *                             example: Wi-Fi enabled smart lamp
 *                           defaultType:
 *                             type: string
 *                             enum: [lamp, camera, electronics]
 *                             example: lamp
 *                     categories:
 *                       type: array
 *                       items:
 *                         type: string
 *                       example: [Popular, Lightning, Camera, Electronics]
 */
router.get('/types', deviceTypeController.getDeviceTypes);

export default router;

