import { Request, Response, NextFunction } from 'express';
import { HomeInvitationService } from '../services/home-invitation-service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateHomeInvitationInput } from '../models/home-invitation';

/**
 * Home invitation controller
 */
export class HomeInvitationController {
  constructor(private homeInvitationService: HomeInvitationService) {}

  /**
   * Create invitation for a home
   */
  createInvitation = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      if (isNaN(homeId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      const input: CreateHomeInvitationInput = req.body;
      const invitation = await this.homeInvitationService.createInvitation(
        homeId,
        input,
        req.user!.id
      );
      sendSuccess(res, invitation, 201);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get invitation by code
   */
  getInvitationByCode = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const code = req.params.code;
      if (!code) {
        sendError(res, 'VALIDATION_ERROR', 'Invitation code is required', 400);
        return;
      }

      const invitation = await this.homeInvitationService.getInvitationByCode(code);
      sendSuccess(res, invitation, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Accept invitation and join home
   */
  acceptInvitation = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const code = req.params.code;
      if (!code) {
        sendError(res, 'VALIDATION_ERROR', 'Invitation code is required', 400);
        return;
      }

      await this.homeInvitationService.acceptInvitation(code, req.user!.id);
      sendSuccess(res, { message: 'Successfully joined home' }, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get all invitations for a home
   */
  getHomeInvitations = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      if (isNaN(homeId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      const invitations = await this.homeInvitationService.getHomeInvitations(
        homeId,
        req.user!.id
      );
      sendSuccess(res, invitations, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Deactivate invitation
   */
  deactivateInvitation = async (
    req: Request,
    res: Response,
    next: NextFunction
  ): Promise<void> => {
    try {
      const code = req.params.code;
      if (!code) {
        sendError(res, 'VALIDATION_ERROR', 'Invitation code is required', 400);
        return;
      }

      await this.homeInvitationService.deactivateInvitation(code, req.user!.id);
      sendSuccess(res, { message: 'Invitation deactivated successfully' }, 200);
    } catch (error) {
      next(error);
    }
  };
}
