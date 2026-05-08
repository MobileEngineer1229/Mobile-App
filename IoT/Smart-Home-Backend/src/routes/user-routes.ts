import { Router } from 'express';
import { UserController } from '../controllers/user-controller';
import { UserService } from '../services/user-service';
import { UserRepository } from '../repositories/user-repository';
import { PasswordResetController } from '../controllers/password-reset-controller';
import { PasswordResetService } from '../services/password-reset-service';
import { PasswordResetRepository } from '../repositories/password-reset-repository';
import { UserSettingsRepository } from '../repositories/user-settings-repository';
import { UserSettingsService } from '../services/user-settings-service';
import { UserSettingsController } from '../controllers/user-settings-controller';
import { AdditionalSettingsRepository } from '../repositories/additional-settings-repository';
import { AdditionalSettingsService } from '../services/additional-settings-service';
import { AdditionalSettingsController } from '../controllers/additional-settings-controller';
import linkedAccountRoutes from './linked-account-routes';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';
import { validate } from '../middleware/validation';
import { sendSuccess, sendError } from '../utils/response';
import {
  signupValidator,
  loginValidator,
  updateProfileValidator,
} from '../validators/user-validator';

const router: Router = Router();

// Initialize dependencies
const userRepository = new UserRepository(getPool());
const userService = new UserService(userRepository);
const userController = new UserController(userService);

// Password reset dependencies
const passwordResetRepository = new PasswordResetRepository(getPool());
const passwordResetService = new PasswordResetService(passwordResetRepository, userRepository);
const passwordResetController = new PasswordResetController(passwordResetService);

// User settings dependencies
const userSettingsRepository = new UserSettingsRepository(userRepository);
const userSettingsService = new UserSettingsService(userSettingsRepository);
const userSettingsController = new UserSettingsController(userSettingsService);

// Additional settings dependencies
const additionalSettingsRepository = new AdditionalSettingsRepository(userRepository);
const additionalSettingsService = new AdditionalSettingsService(additionalSettingsRepository);
const additionalSettingsController = new AdditionalSettingsController(additionalSettingsService);

/**
 * @swagger
 * /users/signup:
 *   post:
 *     summary: Register a new user
 *     tags: [Users]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *               - password
 *               - firstName
 *               - lastName
 *             properties:
 *               email:
 *                 type: string
 *                 format: email
 *                 example: user@example.com
 *               password:
 *                 type: string
 *                 format: password
 *                 minLength: 8
 *                 example: Password123
 *               firstName:
 *                 type: string
 *                 example: John
 *               lastName:
 *                 type: string
 *                 example: Doe
 *               phone:
 *                 type: string
 *                 example: +1234567890
 *     responses:
 *       201:
 *         description: User created successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/UserResponse'
 *       400:
 *         description: Validation error
 *       409:
 *         description: Email already exists
 */
router.post('/signup', validate(signupValidator), userController.signup);

/**
 * @swagger
 * /users/login:
 *   post:
 *     summary: Login user
 *     tags: [Users]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *               - password
 *             properties:
 *               email:
 *                 type: string
 *                 format: email
 *                 example: user@example.com
 *               password:
 *                 type: string
 *                 format: password
 *                 example: Password123
 *     responses:
 *       200:
 *         description: Login successful
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/LoginResponse'
 *       401:
 *         description: Invalid credentials
 */
router.post('/login', validate(loginValidator), userController.login);

/**
 * @swagger
 * /users/profile:
 *   get:
 *     summary: Get current user profile
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: User profile
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/UserResponse'
 *       401:
 *         description: Unauthorized
 */
router.get('/profile', authenticate, userController.getProfile);

/**
 * @swagger
 * /users/profile:
 *   put:
 *     summary: Update user profile
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               firstName:
 *                 type: string
 *                 example: John
 *               lastName:
 *                 type: string
 *                 example: Doe
 *               phone:
 *                 type: string
 *                 example: +1234567890
 *     responses:
 *       200:
 *         description: Profile updated successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/UserResponse'
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 */
router.put('/profile', authenticate, validate(updateProfileValidator), userController.updateProfile);

/**
 * @swagger
 * /users/forgot-password:
 *   post:
 *     summary: Request password reset - send OTP to email
 *     tags: [Users]
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
 *                 format: email
 *                 example: user@example.com
 *     responses:
 *       200:
 *         description: OTP sent successfully (or generic message for security)
 *       400:
 *         description: Validation error
 */
router.post('/forgot-password', passwordResetController.forgotPassword);

/**
 * @swagger
 * /users/verify-otp:
 *   post:
 *     summary: Verify OTP code
 *     tags: [Users]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *               - otpCode
 *             properties:
 *               email:
 *                 type: string
 *                 format: email
 *               otpCode:
 *                 type: string
 *                 pattern: '^[0-9]{6}$'
 *                 example: "123456"
 *     responses:
 *       200:
 *         description: OTP verified successfully
 *       400:
 *         description: Invalid or expired OTP
 */
router.post('/verify-otp', passwordResetController.verifyOTP);

/**
 * @swagger
 * /users/reset-password:
 *   post:
 *     summary: Reset password with OTP
 *     tags: [Users]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *               - otpCode
 *               - newPassword
 *             properties:
 *               email:
 *                 type: string
 *                 format: email
 *               otpCode:
 *                 type: string
 *                 pattern: '^[0-9]{6}$'
 *               newPassword:
 *                 type: string
 *                 format: password
 *                 minLength: 6
 *     responses:
 *       200:
 *         description: Password reset successfully
 *       400:
 *         description: Invalid OTP or validation error
 */
router.post('/reset-password', passwordResetController.resetPassword);

/**
 * @swagger
 * /users/settings:
 *   get:
 *     summary: Get user settings
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: User settings
 *       401:
 *         description: Unauthorized
 */
router.get('/settings', authenticate, userController.getSettings);

/**
 * @swagger
 * /users/settings:
 *   put:
 *     summary: Update user settings
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               notifications:
 *                 type: object
 *               theme:
 *                 type: string
 *               language:
 *                 type: string
 *     responses:
 *       200:
 *         description: Settings updated successfully
 *       401:
 *         description: Unauthorized
 */
router.put('/settings', authenticate, userController.updateSettings);

/**
 * @swagger
 * /users/logout:
 *   post:
 *     summary: Logout user
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Logout successful
 *       401:
 *         description: Unauthorized
 */
router.post('/logout', authenticate, userController.logout);

/**
 * GET /users/notifications/preferences
 * Get notification preferences
 */
router.get('/notifications/preferences', authenticate, userSettingsController.getNotificationPreferences);

/**
 * PUT /users/notifications/preferences
 * Bulk update notification preferences
 */
router.put('/notifications/preferences', authenticate, userSettingsController.updateNotificationPreferences);

/**
 * PUT /users/notifications/preferences/:type
 * Update single notification preference
 */
router.put('/notifications/preferences/:type', authenticate, userSettingsController.updateNotificationPreference);

/**
 * GET /users/security/settings
 * Get security settings
 */
router.get('/security/settings', authenticate, userSettingsController.getSecuritySettings);

/**
 * @swagger
 * /users/security/settings:
 *   put:
 *     summary: Bulk update security settings
 *     tags: [Users]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - settings
 *             properties:
 *               settings:
 *                 type: array
 *                 items:
 *                   type: object
 *                   required:
 *                     - type
 *                     - enabled
 *                   properties:
 *                     type:
 *                       type: string
 *                       enum: [biometric_id, face_id, sms_authenticator, google_authenticator]
 *                       example: biometric_id
 *                     enabled:
 *                       type: boolean
 *                       example: true
 *                     metadata:
 *                       type: object
 *                       description: Additional metadata for the setting
 *     responses:
 *       200:
 *         description: Security settings updated successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 data:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       type:
 *                         type: string
 *                       enabled:
 *                         type: boolean
 *                       metadata:
 *                         type: object
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 */
router.put('/security/settings', authenticate, userSettingsController.updateSecuritySettings);

/**
 * PUT /users/security/settings/:type
 * Update security setting
 */
router.put('/security/settings/:type', authenticate, userSettingsController.updateSecuritySetting);

/**
 * POST /users/change-password
 * Change password
 */
router.post('/change-password', authenticate, userController.changePassword);

/**
 * POST /users/deactivate
 * Deactivate account
 */
router.post('/deactivate', authenticate, userController.deactivateAccount);

/**
 * POST /users/delete
 * Delete account
 */
router.post('/delete', authenticate, userController.deleteAccount);

/**
 * GET /users/additional-settings
 * Get additional settings
 */
router.get('/additional-settings', authenticate, additionalSettingsController.getAdditionalSettings);

/**
 * PUT /users/additional-settings
 * Update additional settings
 */
router.put('/additional-settings', authenticate, additionalSettingsController.updateAdditionalSettings);

/**
 * GET /users/data-analytics
 * Get data analytics settings
 */
router.get('/data-analytics', authenticate, (_req, res) => {
  // Return data analytics information
  sendSuccess(res, {
    dataUsage: {
      enabled: true,
      lastUpdated: new Date().toISOString(),
    },
    adPreferences: {
      personalized: false,
      lastUpdated: new Date().toISOString(),
    },
  }, 200);
});

/**
 * POST /users/data-analytics/download
 * Download user data
 */
router.post('/data-analytics/download', authenticate, (req, res) => {
  // Return download link or initiate download
  sendSuccess(res, {
    downloadUrl: `/users/data-analytics/download/${req.user?.id}`,
    expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(), // 24 hours
  }, 200);
});

// Linked accounts routes
router.use('/linked-accounts', linkedAccountRoutes);

/**
 * GET /users/app-appearance
 * Get app appearance settings
 */
router.get('/app-appearance', authenticate, userSettingsController.getAppAppearance);

/**
 * PUT /users/app-appearance
 * Update app appearance settings
 */
router.put('/app-appearance', authenticate, userSettingsController.updateAppAppearance);

/**
 * POST /users/location
 * Update user's current GPS location (for location-based scene automation)
 */
router.post('/location', authenticate, async (req, res) => {
  try {
    const userId = (req as any).user?.userId || (req as any).user?.id;
    if (!userId) {
      sendError(res, 'UNAUTHORIZED', 'Unauthorized', 401);
      return;
    }

    const { lat, lng } = req.body;
    if (lat === undefined || lng === undefined) {
      sendError(res, 'VALIDATION_ERROR', 'lat and lng are required', 400);
      return;
    }

    if (typeof lat !== 'number' || typeof lng !== 'number') {
      sendError(res, 'VALIDATION_ERROR', 'lat and lng must be numbers', 400);
      return;
    }

    // Store location in user metadata
    const pool = getPool();
    await pool.query(
      `UPDATE users
       SET metadata = COALESCE(metadata, '{}'::jsonb) || $1::jsonb,
           updated_at = NOW()
       WHERE id = $2`,
      [JSON.stringify({ lastLocation: { lat, lng, updatedAt: new Date().toISOString() } }), userId]
    );

    sendSuccess(res, { lat, lng, updatedAt: new Date().toISOString() }, 200);
  } catch (error) {
    sendError(res, 'INTERNAL_ERROR', 'Failed to update location', 500);
  }
});

export default router;

