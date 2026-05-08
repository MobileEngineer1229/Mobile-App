import { Router } from 'express';
import { DeviceControlController } from '../controllers/device-control-controller';
import { DeviceControlService } from '../services/device-control-service';
import { DeviceRepository } from '../repositories/device-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router: Router = Router();

// Initialize dependencies
const deviceRepository = new DeviceRepository(getPool());
const deviceControlService = new DeviceControlService(deviceRepository);
const deviceControlController = new DeviceControlController(deviceControlService);

/**
 * @swagger
 * components:
 *   schemas:
 *     DeviceControlResponse:
 *       type: object
 *       properties:
 *         success:
 *           type: boolean
 *         message:
 *           type: string
 */

// All routes require authentication
router.use(authenticate);

/**
 * Control device power
 */
router.post('/:id/control/power', deviceControlController.controlPower);

/**
 * Control smart lamp
 */
router.post('/:id/control/lamp', deviceControlController.controlLamp);

/**
 * Control CCTV camera
 */
router.post('/:id/control/camera', deviceControlController.controlCamera);

/**
 * Get camera live stream URL
 */
router.get('/:id/control/camera/stream', deviceControlController.getCameraStream);

/**
 * Control stereo speaker
 */
router.post('/:id/control/speaker', deviceControlController.controlSpeaker);

/**
 * Control air conditioner
 */
router.post('/:id/control/ac', deviceControlController.controlAC);

/**
 * Get device current state
 */
router.get('/:id/control/state', deviceControlController.getDeviceState);

/**
 * Execute device command (unified command interface)
 */
router.post('/:id/command', deviceControlController.executeCommand);

export default router;
