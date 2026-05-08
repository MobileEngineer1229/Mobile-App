import { Router } from 'express';
import { authenticate } from '../middleware/auth';
import { DeviceDataController } from '../controllers/device-data-controller';
import { DeviceDataService } from '../services/device-data-service';
import { ReportRepository } from '../repositories/report-repository';
import { DeviceRepository } from '../repositories/device-repository';
import { getPool } from '../config/database';

const router = Router();

// Initialize dependencies
const reportRepository = new ReportRepository(getPool());
const deviceRepository = new DeviceRepository(getPool());
const deviceDataService = new DeviceDataService(reportRepository, deviceRepository);
const deviceDataController = new DeviceDataController(deviceDataService);

/**
 * @swagger
 * /devices/{deviceId}/energy:
 *   post:
 *     summary: Submit energy consumption data from IoT device
 *     description: Allows IoT devices to send their energy consumption data to the backend
 *     tags: [Device Data]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: deviceId
 *         required: true
 *         schema:
 *           type: integer
 *         description: Device ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - consumptionKwh
 *               - date
 *             properties:
 *               consumptionKwh:
 *                 type: number
 *                 description: Energy consumption in kWh
 *                 example: 2.5
 *               date:
 *                 type: string
 *                 format: date
 *                 description: Date of consumption (YYYY-MM-DD)
 *                 example: "2024-12-03"
 *               costUsd:
 *                 type: number
 *                 description: Optional cost in USD (will be calculated if not provided)
 *                 example: 0.375
 *     responses:
 *       200:
 *         description: Energy consumption data saved successfully
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Device not found
 */
router.post('/:deviceId/energy', authenticate, deviceDataController.submitEnergyConsumption);

/**
 * @swagger
 * /devices/{deviceId}/energy/batch:
 *   post:
 *     summary: Submit multiple energy consumption records
 *     description: Allows devices to submit multiple days of consumption data at once
 *     tags: [Device Data]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: deviceId
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               records:
 *                 type: array
 *                 items:
 *                   type: object
 *                   required:
 *                     - consumptionKwh
 *                     - date
 *                   properties:
 *                     consumptionKwh:
 *                       type: number
 *                     date:
 *                       type: string
 *                       format: date
 *                     costUsd:
 *                       type: number
 *     responses:
 *       200:
 *         description: Records saved successfully
 */
router.post('/:deviceId/energy/batch', authenticate, deviceDataController.submitEnergyConsumptionBatch);

/**
 * @swagger
 * /devices/webhook/energy:
 *   post:
 *     summary: Webhook endpoint for device energy data (device authentication)
 *     description: Allows devices to submit energy data using device token (alternative to user auth)
 *     tags: [Device Data]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - deviceToken
 *               - consumptionKwh
 *               - date
 *             properties:
 *               deviceToken:
 *                 type: string
 *                 description: Device authentication token
 *               consumptionKwh:
 *                 type: number
 *               date:
 *                 type: string
 *                 format: date
 *               costUsd:
 *                 type: number
 *     responses:
 *       200:
 *         description: Data saved successfully
 *       401:
 *         description: Invalid device token
 */
router.post('/webhook/energy', deviceDataController.submitEnergyViaWebhook);

export default router;

