import { Request, Response, NextFunction } from 'express';
import { DeviceControlService } from '../services/device-control-service';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

/**
 * Device Control Controller
 * Handles device control commands and integrates with 3rd party APIs
 */
export class DeviceControlController {
  constructor(private deviceControlService: DeviceControlService) {}

  /**
   * Control device power (ON/OFF)
   * Sends command via MQTT/EMQX and logs to device_commands table
   * @swagger
   * /api/v1/devices/{id}/control/power:
   *   post:
   *     summary: Turn device on or off
   *     description: Sends power command to device via MQTT/EMQX broker and logs the command for audit trail
   *     tags: [Device Control]
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
   *             type: object
   *             required:
   *               - power
   *             properties:
   *               power:
   *                 type: boolean
   *                 description: true for ON, false for OFF
   *               source:
   *                 type: string
   *                 enum: [app, scene, voice, schedule]
   *                 default: app
   *                 description: Source of the command for logging
   *     responses:
   *       200:
   *         description: Device power control successful
   *         content:
   *           application/json:
   *             schema:
   *               type: object
   *               properties:
   *                 success:
   *                   type: boolean
   *                 message:
   *                   type: string
   *                 commandId:
   *                   type: integer
   *                   description: ID of the logged command in device_commands table
   *       401:
   *         description: Unauthorized
   *       404:
   *         description: Device not found
   */
  controlPower = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const { power, source } = req.body;
      if (typeof power !== 'boolean') {
        sendError(res, 'VALIDATION_ERROR', 'Power must be a boolean', 400);
        return;
      }

      const result = await this.deviceControlService.controlPower(
        deviceId,
        req.user!.id,
        power,
        source || 'app'
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Control smart lamp (brightness, color, temperature)
   * @swagger
   * /api/v1/devices/{id}/control/lamp:
   *   post:
   *     summary: Control smart lamp settings
   *     tags: [Device Control]
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
   *             type: object
   *             properties:
   *               brightness:
   *                 type: integer
   *                 minimum: 0
   *                 maximum: 100
   *                 description: Brightness percentage (0-100)
   *               color:
   *                 type: string
   *                 description: Hex color code (e.g., "#FF0000")
   *               temperature:
   *                 type: integer
   *                 minimum: 0
   *                 maximum: 100
   *                 description: Color temperature (0=cold, 100=warm)
   *               mode:
   *                 type: string
   *                 enum: [white, color, scene]
   *                 description: Lamp mode
   *     responses:
   *       200:
   *         description: Lamp control successful
   */
  controlLamp = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const result = await this.deviceControlService.controlLamp(
        deviceId,
        req.user!.id,
        req.body
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Control CCTV camera
   * @swagger
   * /api/v1/devices/{id}/control/camera:
   *   post:
   *     summary: Control CCTV camera
   *     tags: [Device Control]
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
   *             type: object
   *             required:
   *               - action
   *             properties:
   *               action:
   *                 type: string
   *                 enum: [playback, snapshot, record, speak, gallery, private_mode, night_mode, move]
   *                 description: Camera action
   *               direction:
   *                 type: string
   *                 enum: [left, right, up, down, stop]
   *                 description: Movement direction (for move action)
   *               quality:
   *                 type: string
   *                 enum: [SD, HD, FullHD]
   *                 description: Video quality
   *     responses:
   *       200:
   *         description: Camera control successful
   */
  controlCamera = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const result = await this.deviceControlService.controlCamera(
        deviceId,
        req.user!.id,
        req.body
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get camera live stream URL
   * @swagger
   * /api/v1/devices/{id}/control/camera/stream:
   *   get:
   *     summary: Get camera live stream URL
   *     tags: [Device Control]
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
   *         description: Stream URL
   */
  getCameraStream = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const result = await this.deviceControlService.getCameraStream(
        deviceId,
        req.user!.id
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Control stereo speaker
   * @swagger
   * /api/v1/devices/{id}/control/speaker:
   *   post:
   *     summary: Control stereo speaker
   *     tags: [Device Control]
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
   *             type: object
   *             properties:
   *               volume:
   *                 type: integer
   *                 minimum: 0
   *                 maximum: 100
   *                 description: Volume percentage (0-100)
   *               action:
   *                 type: string
   *                 enum: [play, pause, next, previous]
   *                 description: Playback action
   *               service:
   *                 type: string
   *                 enum: [spotify, apple_music, youtube_music]
   *                 description: Music service
   *     responses:
   *       200:
   *         description: Speaker control successful
   */
  controlSpeaker = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const result = await this.deviceControlService.controlSpeaker(
        deviceId,
        req.user!.id,
        req.body
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Control air conditioner
   * @swagger
   * /api/v1/devices/{id}/control/ac:
   *   post:
   *     summary: Control air conditioner
   *     tags: [Device Control]
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
   *             type: object
   *             properties:
   *               temperature:
   *                 type: integer
   *                 minimum: 16
   *                 maximum: 30
   *                 description: Target temperature in Celsius
   *               mode:
   *                 type: string
   *                 enum: [cooling, heating, purifying]
   *                 description: AC mode
   *               windSpeed:
   *                 type: string
   *                 enum: [low, medium, high, auto]
   *                 description: Wind speed
   *               windDirection:
   *                 type: string
   *                 enum: [up, down, left, right, auto]
   *                 description: Wind direction
   *               eco:
   *                 type: boolean
   *                 description: Eco mode enabled
   *               sleep:
   *                 type: boolean
   *                 description: Sleep mode enabled
   *     responses:
   *       200:
   *         description: AC control successful
   */
  controlAC = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const result = await this.deviceControlService.controlAC(
        deviceId,
        req.user!.id,
        req.body
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get device current state
   * @swagger
   * /api/v1/devices/{id}/control/state:
   *   get:
   *     summary: Get device current state
   *     tags: [Device Control]
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
   *         description: Device state
   */
  getDeviceState = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const result = await this.deviceControlService.getDeviceState(
        deviceId,
        req.user!.id
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Execute device command (unified command interface)
   * @swagger
   * /api/v1/devices/{id}/command:
   *   post:
   *     summary: Execute a device command
   *     tags: [Device Control]
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
   *             type: object
   *             required:
   *               - command
   *             properties:
   *               command:
   *                 type: string
   *                 description: Command name (e.g., "power", "brightness", "color", "mode", "lamp", "camera", "speaker", "ac")
   *                 enum: [power, on, off, brightness, set_brightness, color, set_color, temperature, set_temperature, mode, set_mode, lamp, control_lamp, camera, control_camera, speaker, control_speaker, ac, air_conditioner, control_ac]
   *               parameters:
   *                 type: object
   *                 description: Command parameters (varies by command type)
   *                 example:
   *                   power: true
   *                   brightness: 85
   *                   color: "#FF0000"
   *                   temperature: 50
   *                   mode: "white"
   *     responses:
   *       200:
   *         description: Command executed successfully
   *       400:
   *         description: Invalid command or parameters
   *       401:
   *         description: Unauthorized
   *       404:
   *         description: Device not found
   */
  executeCommand = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const deviceId = parseInt(req.params.id, 10);
      if (isNaN(deviceId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const { command, parameters } = req.body;

      if (!command || typeof command !== 'string') {
        sendError(res, 'VALIDATION_ERROR', 'Command is required and must be a string', 400);
        return;
      }

      if (parameters && typeof parameters !== 'object') {
        sendError(res, 'VALIDATION_ERROR', 'Parameters must be an object', 400);
        return;
      }

      const result = await this.deviceControlService.executeCommand(
        deviceId,
        req.user!.id,
        command,
        parameters || {}
      );

      sendSuccess(res, result, 200);
    } catch (error) {
      next(error);
    }
  };
}
