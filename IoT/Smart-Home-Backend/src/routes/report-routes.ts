import { Router } from 'express';
import { authenticate } from '../middleware/auth';
import { ReportController } from '../controllers/report-controller';
import { ReportService } from '../services/report-service';
import { ReportRepository } from '../repositories/report-repository';
import { getPool } from '../config/database';

const router = Router();

// Initialize dependencies
const reportRepository = new ReportRepository(getPool());
const reportService = new ReportService(reportRepository);
const reportController = new ReportController(reportService);

/**
 * @swagger
 * /reports/monthly-summary:
 *   get:
 *     summary: Get monthly usage summary
 *     description: Returns this month's and previous month's energy consumption and cost
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Monthly summary retrieved successfully
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
 *                     thisMonth:
 *                       type: object
 *                       properties:
 *                         month:
 *                           type: string
 *                         consumptionKwh:
 *                           type: number
 *                         costUsd:
 *                           type: number
 *                     previousMonth:
 *                       type: object
 *                       properties:
 *                         month:
 *                           type: string
 *                         consumptionKwh:
 *                           type: number
 *                         costUsd:
 *                           type: number
 *       401:
 *         description: Unauthorized
 */
router.get('/monthly-summary', authenticate, reportController.getMonthlySummary);

/**
 * @swagger
 * /reports/statistics:
 *   get:
 *     summary: Get statistics for a date range
 *     description: Returns consumption statistics grouped by period (daily, monthly, etc.)
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: dateRange
 *         schema:
 *           type: string
 *           enum: [today, this_week, last_month, last_3_months, last_6_months, this_year, last_year, all_time, custom]
 *         description: Date range for statistics
 *       - in: query
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date
 *         description: Start date for custom range (ISO format)
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date
 *         description: End date for custom range (ISO format)
 *     responses:
 *       200:
 *         description: Statistics retrieved successfully
 */
router.get('/statistics', authenticate, reportController.getStatistics);

/**
 * @swagger
 * /reports/devices:
 *   get:
 *     summary: Get device consumption summary
 *     description: Returns energy consumption grouped by device or device type
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: dateRange
 *         schema:
 *           type: string
 *           enum: [today, this_week, last_month, last_3_months, last_6_months, this_year, last_year, all_time, custom]
 *       - in: query
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date
 *       - in: query
 *         name: deviceId
 *         schema:
 *           type: integer
 *       - in: query
 *         name: deviceType
 *         schema:
 *           type: string
 *       - in: query
 *         name: roomId
 *         schema:
 *           type: integer
 *       - in: query
 *         name: groupBy
 *         schema:
 *           type: string
 *           enum: [device, type, room]
 *     responses:
 *       200:
 *         description: Device consumption retrieved successfully
 */
router.get('/devices', authenticate, reportController.getDeviceConsumption);

/**
 * @swagger
 * /reports/devices/{type}/details:
 *   get:
 *     summary: Get detailed consumption for a device type
 *     description: Returns detailed consumption records for all devices of a specific type
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: type
 *         required: true
 *         schema:
 *           type: string
 *         description: Device type (e.g., "Smart Lamp", "sensor")
 *       - in: query
 *         name: dateRange
 *         schema:
 *           type: string
 *           enum: [today, this_week, last_month, last_3_months, last_6_months, this_year, last_year, all_time, custom]
 *       - in: query
 *         name: startDate
 *         schema:
 *           type: string
 *           format: date
 *       - in: query
 *         name: endDate
 *         schema:
 *           type: string
 *           format: date
 *     responses:
 *       200:
 *         description: Device type details retrieved successfully
 */
router.get('/devices/:type/details', authenticate, reportController.getDeviceTypeDetails);

export default router;

