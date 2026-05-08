import { Request, Response, NextFunction } from 'express';
import { DeviceService } from '../services/device-service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateDeviceInput, UpdateDeviceInput, DeviceListQuery } from '../models/device';

/**
 * Device controller
 */
export class DeviceController {
  constructor(private deviceService: DeviceService) {}

  /**
   * Get all devices
   */
  getDevices = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const query: DeviceListQuery = {
        page: req.query.page ? parseInt(req.query.page as string, 10) : undefined,
        limit: req.query.limit ? parseInt(req.query.limit as string, 10) : undefined,
        type: req.query.type as DeviceListQuery['type'],
        status: req.query.status as DeviceListQuery['status'],
        search: req.query.search as string,
        roomId: req.query.roomId !== undefined
          ? req.query.roomId === 'null' || req.query.roomId === null
            ? null
            : parseInt(req.query.roomId as string, 10)
          : undefined,
        homeId: req.query.homeId ? parseInt(req.query.homeId as string, 10) : undefined,
      };

      const result = await this.deviceService.getDevices(req.user!.id, query);
      sendSuccess(res, result.devices, 200, {
        pagination: result.pagination,
        timestamp: new Date().toISOString(),
      });
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get device by ID
   */
  getDeviceById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const device = await this.deviceService.getDeviceById(id, req.user!.id);
      sendSuccess(res, device, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Create new device
   */
  createDevice = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const input: CreateDeviceInput = req.body;
      const device = await this.deviceService.createDevice(req.user!.id, input);
      sendSuccess(res, device, 201);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update device
   */
  updateDevice = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      const input: UpdateDeviceInput = req.body;
      const device = await this.deviceService.updateDevice(id, req.user!.id, input);
      sendSuccess(res, device, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Delete device
   */
  deleteDevice = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid device ID', 400);
        return;
      }

      await this.deviceService.deleteDevice(id, req.user!.id);
      sendSuccess(res, { message: 'Device deleted successfully' }, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get devices by category
   */
  getDevicesByCategory = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const category = req.params.category as string;
      const roomId = req.query.roomId !== undefined
        ? req.query.roomId === 'null' || req.query.roomId === null
          ? null
          : parseInt(req.query.roomId as string, 10)
        : undefined;

      const result = await this.deviceService.getDevicesByCategory(req.user!.id, category, roomId);
      sendSuccess(res, result.devices, 200, {
        pagination: result.pagination,
        timestamp: new Date().toISOString(),
      });
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get devices by home ID
   */
  getDevicesByHomeId = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      if (isNaN(homeId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      const query: DeviceListQuery = {
        page: req.query.page ? parseInt(req.query.page as string, 10) : undefined,
        limit: req.query.limit ? parseInt(req.query.limit as string, 10) : undefined,
        type: req.query.type as DeviceListQuery['type'],
        status: req.query.status as DeviceListQuery['status'],
        search: req.query.search as string,
        roomId: req.query.roomId !== undefined
          ? req.query.roomId === 'null' || req.query.roomId === null
            ? null
            : parseInt(req.query.roomId as string, 10)
          : undefined,
      };

      const result = await this.deviceService.getDevicesByHomeId(req.user!.id, homeId, query);
      sendSuccess(res, result.devices, 200, {
        pagination: result.pagination,
        timestamp: new Date().toISOString(),
      });
    } catch (error) {
      next(error);
    }
  };
}

