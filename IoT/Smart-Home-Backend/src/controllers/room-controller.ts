import { Request, Response, NextFunction } from 'express';
import { RoomService } from '../services/room-service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateRoomInput, UpdateRoomInput } from '../models/room';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

/**
 * Room controller
 */
export class RoomController {
  constructor(private roomService: RoomService) {}

  /**
   * Get all rooms, optionally filtered by home_id
   */
  getRooms = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = req.query.homeId ? parseInt(req.query.homeId as string, 10) : undefined;
      const rooms = await this.roomService.getRooms(req.user!.id, homeId);
      sendSuccess(res, rooms, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get room by ID
   */
  getRoomById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid room ID', 400);
        return;
      }

      const room = await this.roomService.getRoomById(id, req.user!.id);
      sendSuccess(res, room, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Create new room
   */
  createRoom = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const input: CreateRoomInput = req.body;
      const room = await this.roomService.createRoom(req.user!.id, input);
      sendSuccess(res, room, 201);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update room
   */
  updateRoom = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid room ID', 400);
        return;
      }

      const input: UpdateRoomInput = req.body;
      const room = await this.roomService.updateRoom(id, req.user!.id, input);
      sendSuccess(res, room, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Delete room
   */
  deleteRoom = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid room ID', 400);
        return;
      }

      await this.roomService.deleteRoom(id, req.user!.id);
      sendSuccess(res, { message: 'Room deleted successfully' }, 200);
    } catch (error) {
      next(error);
    }
  };
}

