import { Router } from 'express';
import { HomeMemberController } from '../controllers/home-member-controller';
import { HomeMemberService } from '../services/home-member-service';
import { HomeMemberRepository } from '../repositories/home-member-repository';
import { UserRepository } from '../repositories/user-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router = Router();

// Initialize dependencies
const homeMemberRepository = new HomeMemberRepository(getPool());
const userRepository = new UserRepository(getPool());
const homeMemberService = new HomeMemberService(homeMemberRepository, userRepository);
const homeMemberController = new HomeMemberController(homeMemberService);

/**
 * @swagger
 * /homes/{homeId}/members:
 *   get:
 *     summary: Get all members of a home
 *     tags: [Home Members]
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
 *         description: List of home members
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden
 */
router.get('/:homeId/members', authenticate, homeMemberController.getHomeMembers);

/**
 * @swagger
 * /homes/{homeId}/members/{memberId}:
 *   get:
 *     summary: Get member by ID
 *     tags: [Home Members]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: integer
 *       - in: path
 *         name: memberId
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Member details
 *       404:
 *         description: Member not found
 *       401:
 *         description: Unauthorized
 */
router.get('/:homeId/members/:memberId', authenticate, homeMemberController.getMemberById);

/**
 * @swagger
 * /homes/{homeId}/members:
 *   post:
 *     summary: Add member to home
 *     tags: [Home Members]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *             properties:
 *               email:
 *                 type: string
 *               userId:
 *                 type: integer
 *               role:
 *                 type: string
 *                 enum: [owner, admin, member]
 *     responses:
 *       201:
 *         description: Member added
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden
 */
router.post('/:homeId/members', authenticate, homeMemberController.addMember);

/**
 * @swagger
 * /homes/{homeId}/members/{memberId}:
 *   put:
 *     summary: Update member role
 *     tags: [Home Members]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: integer
 *       - in: path
 *         name: memberId
 *         required: true
 *         schema:
 *           type: integer
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               role:
 *                 type: string
 *                 enum: [owner, admin, member]
 *     responses:
 *       200:
 *         description: Member updated
 *       404:
 *         description: Member not found
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden
 */
router.put('/:homeId/members/:memberId', authenticate, homeMemberController.updateMember);

/**
 * @swagger
 * /homes/{homeId}/members/{memberId}:
 *   delete:
 *     summary: Remove member from home
 *     tags: [Home Members]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: homeId
 *         required: true
 *         schema:
 *           type: integer
 *       - in: path
 *         name: memberId
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Member removed
 *       404:
 *         description: Member not found
 *       401:
 *         description: Unauthorized
 *       403:
 *         description: Forbidden
 */
router.delete('/:homeId/members/:memberId', authenticate, homeMemberController.removeMember);

export default router;
