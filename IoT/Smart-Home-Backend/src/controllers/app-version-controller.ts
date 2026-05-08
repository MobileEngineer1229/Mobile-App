import { Request, Response } from 'express';
import { AppVersionService } from '../services/app-version-service';
import { VersionCheckRequest } from '../models/app-version';
import { sendSuccess, sendError } from '../utils/response';
import { BadRequestError, AppError } from '../utils/errors';

/**
 * App Version controller
 */
export class AppVersionController {
  constructor(private appVersionService: AppVersionService) {}

  /**
   * Check app version
   * POST /api/v1/app/version/check
   */
  checkVersion = async (req: Request, res: Response): Promise<void> => {
    try {
      const { platform, versionName, versionCode } = req.body;

      if (!platform || !versionName || versionCode === undefined) {
        throw new BadRequestError('Platform, versionName, and versionCode are required');
      }

      if (platform !== 'android' && platform !== 'ios') {
        throw new BadRequestError('Platform must be "android" or "ios"');
      }

      const request: VersionCheckRequest = {
        platform,
        versionName: String(versionName),
        versionCode: Number(versionCode),
      };

      const result = await this.appVersionService.checkVersion(request);
      sendSuccess(res, result, 200);
    } catch (error) {
      if (error instanceof AppError) {
        sendError(res, error.code, error.message, error.statusCode);
      } else {
        sendError(res, 'INTERNAL_ERROR', error instanceof Error ? error.message : 'An error occurred', 500);
      }
    }
  };

  /**
   * Get active version for platform
   * GET /api/v1/app/version/:platform
   */
  getActiveVersion = async (req: Request, res: Response): Promise<void> => {
    try {
      const { platform } = req.params;

      if (platform !== 'android' && platform !== 'ios') {
        throw new BadRequestError('Platform must be "android" or "ios"');
      }

      const version = await this.appVersionService.getActiveVersion(platform);
      if (!version) {
        sendSuccess(res, { version: null, message: 'No active version found' }, 200);
        return;
      }

      sendSuccess(res, { version }, 200);
    } catch (error) {
      if (error instanceof AppError) {
        sendError(res, error.code, error.message, error.statusCode);
      } else {
        sendError(res, 'INTERNAL_ERROR', error instanceof Error ? error.message : 'An error occurred', 500);
      }
    }
  };

  /**
   * Get all versions for platform
   * GET /api/v1/app/versions/:platform
   */
  getAllVersions = async (req: Request, res: Response): Promise<void> => {
    try {
      const { platform } = req.params;

      if (platform !== 'android' && platform !== 'ios') {
        throw new BadRequestError('Platform must be "android" or "ios"');
      }

      const versions = await this.appVersionService.getAllVersions(platform);
      sendSuccess(res, { versions }, 200);
    } catch (error) {
      if (error instanceof AppError) {
        sendError(res, error.code, error.message, error.statusCode);
      } else {
        sendError(res, 'INTERNAL_ERROR', error instanceof Error ? error.message : 'An error occurred', 500);
      }
    }
  };

  /**
   * Get version by ID
   * GET /api/v1/app/versions/:id
   */
  getVersionById = async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      const version = await this.appVersionService.getVersionById(Number(id));
      sendSuccess(res, { version }, 200);
    } catch (error) {
      if (error instanceof AppError) {
        sendError(res, error.code, error.message, error.statusCode);
      } else {
        sendError(res, 'INTERNAL_ERROR', error instanceof Error ? error.message : 'An error occurred', 500);
      }
    }
  };

  /**
   * Create new version
   * POST /api/v1/app/versions
   */
  createVersion = async (req: Request, res: Response): Promise<void> => {
    try {
      const version = await this.appVersionService.createVersion(req.body);
      sendSuccess(res, { version }, 201);
    } catch (error) {
      if (error instanceof AppError) {
        sendError(res, error.code, error.message, error.statusCode);
      } else {
        sendError(res, 'INTERNAL_ERROR', error instanceof Error ? error.message : 'An error occurred', 500);
      }
    }
  };

  /**
   * Update version
   * PUT /api/v1/app/versions/:id
   */
  updateVersion = async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      const version = await this.appVersionService.updateVersion(Number(id), req.body);
      sendSuccess(res, { version }, 200);
    } catch (error) {
      if (error instanceof AppError) {
        sendError(res, error.code, error.message, error.statusCode);
      } else {
        sendError(res, 'INTERNAL_ERROR', error instanceof Error ? error.message : 'An error occurred', 500);
      }
    }
  };

  /**
   * Delete version
   * DELETE /api/v1/app/versions/:id
   */
  deleteVersion = async (req: Request, res: Response): Promise<void> => {
    try {
      const { id } = req.params;
      await this.appVersionService.deleteVersion(Number(id));
      sendSuccess(res, { message: 'Version deleted successfully' }, 200);
    } catch (error) {
      if (error instanceof AppError) {
        sendError(res, error.code, error.message, error.statusCode);
      } else {
        sendError(res, 'INTERNAL_ERROR', error instanceof Error ? error.message : 'An error occurred', 500);
      }
    }
  };
}
