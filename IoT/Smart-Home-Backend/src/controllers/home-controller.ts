import { Request, Response, NextFunction } from 'express';
import { HomeService } from '../services/home-service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateHomeInput, UpdateHomeInput } from '../models/home';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

/**
 * Home controller
 */
export class HomeController {
  constructor(private homeService: HomeService) {}

  /**
   * Get all homes
   */
  getHomes = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homes = await this.homeService.getHomes(req.user!.id);
      sendSuccess(res, homes, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get primary home
   */
  getPrimaryHome = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const home = await this.homeService.getPrimaryHome(req.user!.id);
      if (!home) {
        sendError(res, 'NOT_FOUND', 'No primary home found', 404);
        return;
      }
      sendSuccess(res, home, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get home by ID
   */
  getHomeById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      const home = await this.homeService.getHomeById(id, req.user!.id);
      sendSuccess(res, home, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Create new home
   */
  createHome = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const input: CreateHomeInput = req.body;
      const home = await this.homeService.createHome(req.user!.id, input);
      sendSuccess(res, home, 201);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update home
   */
  updateHome = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      const input: UpdateHomeInput = req.body;
      const home = await this.homeService.updateHome(id, req.user!.id, input);
      sendSuccess(res, home, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Delete home
   */
  deleteHome = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const id = parseInt(req.params.id, 10);
      if (isNaN(id)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      await this.homeService.deleteHome(id, req.user!.id);
      sendSuccess(res, { message: 'Home deleted successfully' }, 200);
    } catch (error) {
      next(error);
    }
  };
}
