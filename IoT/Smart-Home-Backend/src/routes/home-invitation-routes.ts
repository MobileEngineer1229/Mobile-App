import { Router } from 'express';
import { HomeInvitationController } from '../controllers/home-invitation-controller';
import { HomeInvitationService } from '../services/home-invitation-service';
import { HomeInvitationRepository } from '../repositories/home-invitation-repository';
import { HomeMemberRepository } from '../repositories/home-member-repository';
import { HomeRepository } from '../repositories/home-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router = Router();

// Initialize dependencies
const homeInvitationRepository = new HomeInvitationRepository(getPool());
const homeMemberRepository = new HomeMemberRepository(getPool());
const homeRepository = new HomeRepository(getPool());
const homeInvitationService = new HomeInvitationService(
  homeInvitationRepository,
  homeMemberRepository,
  homeRepository
);
const homeInvitationController = new HomeInvitationController(homeInvitationService);

/**
 * @swagger
 * /homes/{homeId}/invitations:
 *   post:
 *     summary: Create invitation for a home
 *     tags: [Home Invitations]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       required: false
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               expiresAt:
 *                 type: string
 *                 format: date-time
 *               maxUses:
 *                 type: integer
 *                 default: 1
 *     responses:
 *       201:
 *         description: Invitation created
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden
 */
router.post('/:homeId/invitations', authenticate, homeInvitationController.createInvitation);

/**
 * @swagger
 * /homes/invitations/{code}:
 *   get:
 *     summary: Get invitation by code
 *     tags: [Home Invitations]
 *     parameters:
 *       - in: path
 *         name: code
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Invitation details
 *       404:
 *         description: Invitation not found
 */
router.get('/invitations/:code', homeInvitationController.getInvitationByCode);

/**
 * @swagger
 * /homes/invitations/{code}/accept:
 *   post:
 *     summary: Accept invitation and join home
 *     tags: [Home Invitations]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: code
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Successfully joined home
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Invitation not found
 */
router.post('/invitations/:code/accept', authenticate, homeInvitationController.acceptInvitation);

/**
 * @swagger
 * /homes/{homeId}/invitations:
 *   get:
 *     summary: Get all invitations for a home
 *     tags: [Home Invitations]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: List of invitations
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden
 */
router.get('/:homeId/invitations', authenticate, homeInvitationController.getHomeInvitations);

/**
 * @swagger
 * /homes/invitations/{code}/deactivate:
 *   post:
 *     summary: Deactivate invitation
 *     tags: [Home Invitations]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: code
 *         required: true
 *         schema:
 *           type: string
 *     responses:
 *       200:
 *         description: Invitation deactivated
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden
 *       404:
 *         description: Invitation not found
 */
router.post(
  '/invitations/:code/deactivate',
  authenticate,
  homeInvitationController.deactivateInvitation
);

export default router;
