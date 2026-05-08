import { Router } from 'express';
import { AppVersionController } from '../controllers/app-version-controller';
import { AppVersionService } from '../services/app-version-service';
import { AppVersionRepository } from '../repositories/app-version-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router: Router = Router();

// Initialize dependencies
const appVersionRepository = new AppVersionRepository(getPool());
const appVersionService = new AppVersionService(appVersionRepository);
const appVersionController = new AppVersionController(appVersionService);

/**
 * @swagger
 * /app/version/check:
 *   post:
 *     summary: Check app version and get update status
 *     tags: [App Version]
 *     description: |
 *       Checks the client app version against the server's version requirements.
 *       Returns whether an update is available or required.
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - platform
 *               - versionName
 *               - versionCode
 *             properties:
 *               platform:
 *                 type: string
 *                 enum: [android, ios]
 *                 example: android
 *               versionName:
 *                 type: string
 *                 example: "1.0.0"
 *               versionCode:
 *                 type: integer
 *                 example: 1
 *     responses:
 *       200:
 *         description: Version check result
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: object
 *                   properties:
 *                     currentVersion:
 *                       type: object
 *                       properties:
 *                         versionName:
 *                           type: string
 *                         versionCode:
 *                           type: integer
 *                     minimumRequiredVersion:
 *                       type: string
 *                     updateAvailable:
 *                       type: boolean
 *                     updateRequired:
 *                       type: boolean
 *                     updateUrl:
 *                       type: string
 *                     releaseNotes:
 *                       type: string
 *                     message:
 *                       type: string
 *       400:
 *         description: Bad request
 */
router.post('/version/check', appVersionController.checkVersion);

/**
 * @swagger
 * /app/version/{platform}:
 *   get:
 *     summary: Get active version for platform
 *     tags: [App Version]
 *     parameters:
 *       - in: path
 *         name: platform
 *         required: true
 *         schema:
 *           type: string
 *           enum: [android, ios]
 *     responses:
 *       200:
 *         description: Active version information
 */
router.get('/version/:platform', appVersionController.getActiveVersion);

/**
 * @swagger
 * /app/versions/{platform}:
 *   get:
 *     summary: Get all versions for platform (requires auth)
 *     tags: [App Version]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: platform
 *         required: true
 *         schema:
 *           type: string
 *           enum: [android, ios]
 *     responses:
 *       200:
 *         description: List of versions
 */
router.get('/versions/:platform', authenticate, appVersionController.getAllVersions);

/**
 * @swagger
 * /app/versions/{id}:
 *   get:
 *     summary: Get version by ID (requires auth)
 *     tags: [App Version]
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
 *         description: Version information
 */
router.get('/versions/:id', authenticate, appVersionController.getVersionById);

/**
 * @swagger
 * /app/versions:
 *   post:
 *     summary: Create new version (requires auth)
 *     tags: [App Version]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - platform
 *               - versionName
 *               - versionCode
 *               - minimumRequiredVersion
 *             properties:
 *               platform:
 *                 type: string
 *                 enum: [android, ios]
 *               versionName:
 *                 type: string
 *               versionCode:
 *                 type: integer
 *               minimumRequiredVersion:
 *                 type: string
 *               updateUrl:
 *                 type: string
 *               forceUpdate:
 *                 type: boolean
 *               releaseNotes:
 *                 type: string
 *               isActive:
 *                 type: boolean
 *     responses:
 *       201:
 *         description: Version created
 */
router.post('/versions', authenticate, appVersionController.createVersion);

/**
 * @swagger
 * /app/versions/{id}:
 *   put:
 *     summary: Update version (requires auth)
 *     tags: [App Version]
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
 *               versionName:
 *                 type: string
 *               versionCode:
 *                 type: integer
 *               minimumRequiredVersion:
 *                 type: string
 *               updateUrl:
 *                 type: string
 *               forceUpdate:
 *                 type: boolean
 *               releaseNotes:
 *                 type: string
 *               isActive:
 *                 type: boolean
 *     responses:
 *       200:
 *         description: Version updated
 */
router.put('/versions/:id', authenticate, appVersionController.updateVersion);

/**
 * @swagger
 * /app/versions/{id}:
 *   delete:
 *     summary: Delete version (requires auth)
 *     tags: [App Version]
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
 *         description: Version deleted
 */
router.delete('/versions/:id', authenticate, appVersionController.deleteVersion);

export default router;
