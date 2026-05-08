import express from 'express';
import { authenticate } from '../middleware/auth';
import { validate } from '../middleware/validation';
import * as sceneController from '../controllers/sceneController';
import { createSceneValidator, updateSceneValidator } from '../validators/scene-validator';

const router = express.Router();

// All routes require authentication
router.use(authenticate);

/**
 * @swagger
 * /scenes:
 *   get:
 *     summary: Get all scenes for the authenticated user
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: type
 *         schema:
 *           type: string
 *           enum: [automation, tap_to_run]
 *         description: Filter scenes by type
 *     responses:
 *       200:
 *         description: List of scenes
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 scenes:
 *                   type: array
 *                   items:
 *                     $ref: '#/components/schemas/Scene'
 *       401:
 *         description: Unauthorized
 *       500:
 *         description: Internal server error
 */
router.get('/', sceneController.getScenes);

/**
 * @swagger
 * /scenes/{id}:
 *   get:
 *     summary: Get a specific scene by ID
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Scene ID
 *     responses:
 *       200:
 *         description: Scene details
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 scene:
 *                   $ref: '#/components/schemas/Scene'
 *       404:
 *         description: Scene not found
 *       401:
 *         description: Unauthorized
 */
router.get('/:id', sceneController.getSceneById);

/**
 * @swagger
 * /scenes:
 *   post:
 *     summary: Create a new scene
 *     tags: [Scenes]
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
 *               - type
 *             properties:
 *               name:
 *                 type: string
 *                 example: "Welcome Home Automation"
 *               type:
 *                 type: string
 *                 enum: [automation, tap_to_run]
 *                 example: "automation"
 *               conditionLogic:
 *                 type: string
 *                 enum: [any, all]
 *                 default: "any"
 *               icon:
 *                 type: string
 *                 example: "ic_sun"
 *               color:
 *                 type: string
 *                 example: "#405FF2"
 *               homeId:
 *                 type: integer
 *               conditions:
 *                 type: array
 *                 items:
 *                   $ref: '#/components/schemas/SceneCondition'
 *               tasks:
 *                 type: array
 *                 items:
 *                   $ref: '#/components/schemas/SceneTask'
 *     responses:
 *       201:
 *         description: Scene created successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 scene:
 *                   $ref: '#/components/schemas/Scene'
 *       400:
 *         description: Bad request
 *       401:
 *         description: Unauthorized
 *       500:
 *         description: Internal server error
 */
router.post('/', validate(createSceneValidator), sceneController.createScene);

/**
 * @swagger
 * /scenes/{id}:
 *   put:
 *     summary: Update a scene
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Scene ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *               conditionLogic:
 *                 type: string
 *                 enum: [any, all]
 *               icon:
 *                 type: string
 *               color:
 *                 type: string
 *               isEnabled:
 *                 type: boolean
 *               conditions:
 *                 type: array
 *                 items:
 *                   $ref: '#/components/schemas/SceneCondition'
 *               tasks:
 *                 type: array
 *                 items:
 *                   $ref: '#/components/schemas/SceneTask'
 *     responses:
 *       200:
 *         description: Scene updated successfully
 *       404:
 *         description: Scene not found
 *       401:
 *         description: Unauthorized
 */
router.put('/:id', validate(updateSceneValidator), sceneController.updateScene);

/**
 * @swagger
 * /scenes/{id}:
 *   delete:
 *     summary: Delete a scene
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Scene ID
 *     responses:
 *       200:
 *         description: Scene deleted successfully
 *       404:
 *         description: Scene not found
 *       401:
 *         description: Unauthorized
 */
router.delete('/:id', sceneController.deleteScene);

/**
 * @swagger
 * /scenes/{id}/toggle:
 *   patch:
 *     summary: Toggle scene enabled/disabled status
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Scene ID
 *     responses:
 *       200:
 *         description: Scene toggled successfully
 *       404:
 *         description: Scene not found
 *       401:
 *         description: Unauthorized
 */
router.patch('/:id/toggle', sceneController.toggleScene);

/**
 * @swagger
 * /scenes/{id}/execute:
 *   post:
 *     summary: Execute/Run a scene
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Scene ID
 *     responses:
 *       200:
 *         description: Scene executed successfully
 *       404:
 *         description: Scene not found
 *       401:
 *         description: Unauthorized
 */
router.post('/:id/execute', sceneController.executeScene);

/**
 * @swagger
 * /scenes/logs:
 *   get:
 *     summary: Get scene execution logs
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 50
 *         description: Maximum number of logs to return
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Number of logs to skip
 *       - in: query
 *         name: sceneId
 *         schema:
 *           type: integer
 *         description: Filter logs by scene ID
 *     responses:
 *       200:
 *         description: List of execution logs
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 logs:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       id:
 *                         type: integer
 *                       scene_id:
 *                         type: integer
 *                       scene_name:
 *                         type: string
 *                       status:
 *                         type: string
 *                         enum: [succeeded, failed]
 *                       error_message:
 *                         type: string
 *                         nullable: true
 *                       execution_timestamp:
 *                         type: string
 *                         format: date-time
 *       401:
 *         description: Unauthorized
 *       500:
 *         description: Internal server error
 */
router.get('/logs', sceneController.getSceneLogs);

/**
 * @swagger
 * /scenes/{id}/logs:
 *   get:
 *     summary: Get scene execution logs for a specific scene
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Scene ID
 *       - in: query
 *         name: limit
 *         schema:
 *           type: integer
 *           default: 50
 *         description: Maximum number of logs to return
 *       - in: query
 *         name: offset
 *         schema:
 *           type: integer
 *           default: 0
 *         description: Number of logs to skip
 *     responses:
 *       200:
 *         description: List of execution logs for the scene
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 logs:
 *                   type: array
 *                   items:
 *                     type: object
 *                     properties:
 *                       id:
 *                         type: integer
 *                       scene_id:
 *                         type: integer
 *                       scene_name:
 *                         type: string
 *                       status:
 *                         type: string
 *                         enum: [succeeded, failed]
 *                       error_message:
 *                         type: string
 *                         nullable: true
 *                       execution_timestamp:
 *                         type: string
 *                         format: date-time
 *       404:
 *         description: Scene not found
 *       401:
 *         description: Unauthorized
 *       500:
 *         description: Internal server error
 */
router.get('/:id/logs', sceneController.getSceneLogsById);

/**
 * @swagger
 * /scenes/reorder:
 *   post:
 *     summary: Reorder scenes
 *     tags: [Scenes]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - sceneIds
 *             properties:
 *               sceneIds:
 *                 type: array
 *                 items:
 *                   type: integer
 *                 description: Array of scene IDs in the desired order
 *               type:
 *                 type: string
 *                 enum: [automation, tap_to_run]
 *                 description: Optional filter by scene type
 *     responses:
 *       200:
 *         description: Scenes reordered successfully
 *       400:
 *         description: Bad request
 *       401:
 *         description: Unauthorized
 *       500:
 *         description: Internal server error
 */
router.post('/reorder', sceneController.reorderScenes);

export default router;
