import { Router } from 'express';
import { HomeController } from '../controllers/home-controller';
import { HomeRepository } from '../repositories/home-repository';
import { HomeMemberRepository } from '../repositories/home-member-repository';
import { HomeService } from '../services/home-service';
import { HomeInvitationController } from '../controllers/home-invitation-controller';
import { HomeInvitationService } from '../services/home-invitation-service';
import { HomeInvitationRepository } from '../repositories/home-invitation-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';
import { validate } from '../middleware/validation';
import { createHomeValidation, updateHomeValidation } from '../validators/home-validator';

const router = Router();
const homeRepository = new HomeRepository(getPool());
const homeMemberRepository = new HomeMemberRepository(getPool());
const homeService = new HomeService(homeRepository, homeMemberRepository);
const homeController = new HomeController(homeService);

// Initialize invitation service for /join endpoint
const homeInvitationRepository = new HomeInvitationRepository(getPool());
const homeInvitationService = new HomeInvitationService(
  homeInvitationRepository,
  homeMemberRepository,
  homeRepository
);
const homeInvitationController = new HomeInvitationController(homeInvitationService);

/**
 * @swagger
 * /homes:
 *   get:
 *     summary: Get all homes for the authenticated user
 *     tags: [Homes]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: List of homes
 *       401:
 *         description: Unauthorized
 */
router.get('/', authenticate, homeController.getHomes);

/**
 * @swagger
 * /homes/primary:
 *   get:
 *     summary: Get primary home for the authenticated user
 *     tags: [Homes]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Primary home
 *       404:
 *         description: No primary home found
 *       401:
 *         description: Unauthorized
 */
router.get('/primary', authenticate, homeController.getPrimaryHome);

/**
 * @swagger
 * /homes:
 *   post:
 *     summary: Create a new home
 *     tags: [Homes]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - name
 *             properties:
 *               name:
 *                 type: string
 *               address:
 *                 type: string
 *               latitude:
 *                 type: number
 *               longitude:
 *                 type: number
 *               country:
 *                 type: string
 *               isPrimary:
 *                 type: boolean
 *     responses:
 *       201:
 *         description: Home created
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 */
router.post('/', authenticate, validate(createHomeValidation), homeController.createHome);

/**
 * @swagger
 * /homes/{id}:
 *   get:
 *     summary: Get home by ID
 *     tags: [Homes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Home details
 *       404:
 *         description: Home not found
 *       401:
 *         description: Unauthorized
 */
router.get('/:id', authenticate, homeController.getHomeById);

/**
 * @swagger
 * /homes/{id}:
 *   put:
 *     summary: Update home
 *     tags: [Homes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
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
 *               name:
 *                 type: string
 *               address:
 *                 type: string
 *               latitude:
 *                 type: number
 *               longitude:
 *                 type: number
 *               country:
 *                 type: string
 *               isPrimary:
 *                 type: boolean
 *     responses:
 *       200:
 *         description: Home updated
 *       404:
 *         description: Home not found
 *       401:
 *         description: Unauthorized
 */
router.put('/:id', authenticate, validate(updateHomeValidation), homeController.updateHome);

/**
 * @swagger
 * /homes/{id}:
 *   delete:
 *     summary: Delete home
 *     tags: [Homes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *     responses:
 *       200:
 *         description: Home deleted
 *       404:
 *         description: Home not found
 *       401:
 *         description: Unauthorized
 */
router.delete('/:id', authenticate, homeController.deleteHome);

/**
 * @swagger
 * /homes/join:
 *   post:
 *     summary: Join home by invitation code (alternative endpoint)
 *     tags: [Homes]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - code
 *             properties:
 *               code:
 *                 type: string
 *                 description: Invitation code
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
router.post('/join', authenticate, async (req, res, next) => {
  try {
    const { code } = req.body;
    if (!code) {
      res.status(400).json({ error: 'Invitation code is required' });
      return;
    }

    // Create a mock request object with the code in params to reuse acceptInvitation
    const mockReq = {
      ...req,
      params: { code },
    } as any;

    await homeInvitationController.acceptInvitation(mockReq, res, next);
  } catch (error) {
    next(error);
  }
});

export default router;
