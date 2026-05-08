import { Router } from 'express';
import { LinkedAccountController } from '../controllers/linked-account-controller';
import { LinkedAccountService } from '../services/linked-account-service';
import { LinkedAccountRepository } from '../repositories/linked-account-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router: Router = Router();

// Initialize dependencies
const linkedAccountRepository = new LinkedAccountRepository(getPool());
const linkedAccountService = new LinkedAccountService(linkedAccountRepository);
const linkedAccountController = new LinkedAccountController(linkedAccountService);

/**
 * GET /users/linked-accounts
 * Get all linked accounts
 */
router.get('/', authenticate, linkedAccountController.getLinkedAccounts);

/**
 * POST /users/linked-accounts/:provider/link
 * Link an account
 */
router.post('/:provider/link', authenticate, linkedAccountController.linkAccount);

/**
 * POST /users/linked-accounts/:id/unlink
 * Unlink an account
 */
router.post('/:id/unlink', authenticate, linkedAccountController.unlinkAccount);

export default router;

