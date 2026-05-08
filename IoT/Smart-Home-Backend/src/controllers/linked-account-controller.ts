import { Request, Response, NextFunction } from 'express';
import { LinkedAccountService } from '../services/linked-account-service';
import { sendSuccess, sendError } from '../utils/response';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

export class LinkedAccountController {
  constructor(private linkedAccountService: LinkedAccountService) {}

  /**
   * Get linked accounts
   */
  getLinkedAccounts = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const accounts = await this.linkedAccountService.getLinkedAccounts(req.user!.id);
      sendSuccess(res, accounts, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Link an account
   */
  linkAccount = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const provider = req.params.provider;
      if (!provider) {
        sendError(res, 'BAD_REQUEST', 'Provider is required', 400);
        return;
      }

      const account = await this.linkedAccountService.linkAccount(req.user!.id, provider, {
        providerUserId: req.body.providerUserId,
        accessToken: req.body.accessToken,
        refreshToken: req.body.refreshToken,
        expiresAt: req.body.expiresAt,
        metadata: req.body.metadata,
      });
      sendSuccess(res, account, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Unlink an account
   */
  unlinkAccount = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const accountId = parseInt(req.params.id, 10);
      if (isNaN(accountId)) {
        sendError(res, 'BAD_REQUEST', 'Invalid account ID', 400);
        return;
      }

      await this.linkedAccountService.unlinkAccount(req.user!.id, accountId);
      sendSuccess(res, { message: 'Account unlinked successfully' }, 200);
    } catch (error) {
      next(error);
    }
  };
}

