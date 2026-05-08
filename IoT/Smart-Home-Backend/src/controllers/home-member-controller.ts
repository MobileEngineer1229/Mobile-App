import { Request, Response, NextFunction } from 'express';
import { HomeMemberService } from '../services/home-member-service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateHomeMemberInput, UpdateHomeMemberInput } from '../models/home-member';

/**
 * Home member controller
 */
export class HomeMemberController {
  constructor(private homeMemberService: HomeMemberService) {}

  /**
   * Get all members of a home
   */
  getHomeMembers = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      if (isNaN(homeId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      const members = await this.homeMemberService.getHomeMembers(homeId, req.user!.id);
      sendSuccess(res, members, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Get member by ID
   */
  getMemberById = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      const memberId = parseInt(req.params.memberId, 10);
      if (isNaN(homeId) || isNaN(memberId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID or member ID', 400);
        return;
      }

      const member = await this.homeMemberService.getMemberById(memberId, homeId, req.user!.id);
      sendSuccess(res, member, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Add member to home
   */
  addMember = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      if (isNaN(homeId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID', 400);
        return;
      }

      const input: CreateHomeMemberInput = req.body;
      const member = await this.homeMemberService.addMember(homeId, input, req.user!.id);
      sendSuccess(res, member, 201);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Update member role
   */
  updateMember = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      const memberId = parseInt(req.params.memberId, 10);
      if (isNaN(homeId) || isNaN(memberId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID or member ID', 400);
        return;
      }

      const input: UpdateHomeMemberInput = req.body;
      const member = await this.homeMemberService.updateMember(
        memberId,
        homeId,
        input,
        req.user!.id
      );
      sendSuccess(res, member, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Remove member from home
   */
  removeMember = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const homeId = parseInt(req.params.homeId, 10);
      const memberId = parseInt(req.params.memberId, 10);
      if (isNaN(homeId) || isNaN(memberId)) {
        sendError(res, 'VALIDATION_ERROR', 'Invalid home ID or member ID', 400);
        return;
      }

      await this.homeMemberService.removeMember(memberId, homeId, req.user!.id);
      sendSuccess(res, { message: 'Member removed successfully' }, 200);
    } catch (error) {
      next(error);
    }
  };
}
