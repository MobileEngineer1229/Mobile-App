import { Router } from 'express';
import { DeviceController } from '../controllers/device-controller';
import { DeviceService } from '../services/device-service';
import { DeviceRepository } from '../repositories/device-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';
import { validate } from '../middleware/validation';
import {
  createDeviceValidator,
  updateDeviceValidator,
} from '../validators/device-validator';

const router: Router = Router();

// Initialize dependencies
const deviceRepository = new DeviceRepository(getPool());
const deviceService = new DeviceService(deviceRepository);
const deviceController = new DeviceController(deviceService);

/**
 * @swagger
 * /devices:
 *   get:
 *     summary: Get all devices for the authenticated user
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *         description: Page number
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 10
 *         description: Items per page
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [sensor, actuator, controller]
 *         description: Filter by device type
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [online, offline, unknown]
 *         description: Filter by device status
 *       - in: query
 *         name: search
 *         schema:
 *           type: string
 *         description: Search by name or MAC address
 *       - in: query
 *         name: roomId
 *         schema:
 *           type: integer
 *           nullable: true
 *         description: Filter by room ID (null for devices without a room)
 *       - in: query
 *         name: homeId
 *         schema:
 *           type: integer
 *         description: Filter devices by home ID (via rooms belonging to the home)
 *     responses:
 *       200:
 *         description: List of devices
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/DeviceListResponse'
 *       401:
 *         description: Unauthorized
 */
router.get('/', authenticate, deviceController.getDevices);

/**
 * @swagger
 * /devices/{id}:
 *   get:
 *     summary: Get device by ID
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Device ID
 *     responses:
 *       200:
 *         description: Device details
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/DeviceResponse'
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Device not found
 */
router.get('/:id', authenticate, deviceController.getDeviceById);

/**
 * @swagger
 * /devices:
 *   post:
 *     summary: Create a new device
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateDeviceInput'
 *     responses:
 *       201:
 *         description: Device created successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/DeviceResponse'
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 *       409:
 *         description: Device with MAC address already exists
 */
router.post('/', authenticate, validate(createDeviceValidator), deviceController.createDevice);

/**
 * @swagger
 * /devices/{id}:
 *   put:
 *     summary: Update device
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Device ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/UpdateDeviceInput'
 *     responses:
 *       200:
 *         description: Device updated successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/DeviceResponse'
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Device not found
 */
router.put('/:id', authenticate, validate(updateDeviceValidator), deviceController.updateDevice);

/**
 * @swagger
 * /devices/{id}:
 *   delete:
 *     summary: Delete device
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Device ID
 *     responses:
 *       200:
 *         description: Device deleted successfully
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Device not found
 */
router.delete('/:id', authenticate, deviceController.deleteDevice);

/**
 * @swagger
 * /devices/category/{category}:
 *   get:
 *     summary: Get devices by category
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: category
 *         required: true
 *         schema:
 *           type: string
 *           enum: [lightning, cameras, electrical]
 *         description: Device category
 *       - in: query
 *         name: roomId
 *         schema:
 *           type: integer
 *           nullable: true
 *         description: Filter by room ID
 *     responses:
 *       200:
 *         description: List of devices in the category
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/DeviceListResponse'
 *       401:
 *         description: Unauthorized
 */
router.get('/category/:category', authenticate, deviceController.getDevicesByCategory);

/**
 * @swagger
 * /devices/home/{homeId}:
 *   get:
 *     summary: Get devices by home ID
 *     tags: [Devices]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: integer
 *         description: Home ID
 *       - in: query
 *         name: page
 *         schema:
 *           type: integer
 *           default: 1
 *         description: Page number
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 10
 *         description: Items per page
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [lamp, camera, electronics]
 *         description: Filter by device type
 *       - in: query
 *         name: status
 *         schema:
 *           type: string
 *           enum: [online, offline, unknown]
 *         description: Filter by device status
 *       - in: query
 *         name: search
 *         schema:
 *           type: string
 *         description: Search by name or MAC address
 *       - in: query
 *         name: roomId
 *         schema:
 *           type: integer
 *           nullable: true
 *         description: Filter by room ID (null for devices without a room)
 *     responses:
 *       200:
 *         description: List of devices in the home
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/DeviceListResponse'
 *       400:
 *         description: Invalid home ID
 *       401:
 *         description: Unauthorized
 */
router.get('/home/:homeId', authenticate, deviceController.getDevicesByHomeId);

export default router;

