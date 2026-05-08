import { Router } from 'express';
import { RoomController } from '../controllers/room-controller';
import { RoomService } from '../services/room-service';
import { RoomRepository } from '../repositories/room-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';
import { validate } from '../middleware/validation';
import {
  createRoomValidator,
  updateRoomValidator,
} from '../validators/room-validator';

const router: Router = Router();

// Initialize dependencies
const roomRepository = new RoomRepository(getPool());
const roomService = new RoomService(roomRepository);
const roomController = new RoomController(roomService);

/**
 * @swagger
 * /rooms:
 *   get:
 *     summary: Get all rooms for the authenticated user
 *     tags: [Rooms]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: query
 *         name: homeId
 *         schema:
 *           type: integer
 *         description: Filter rooms by home ID
 *     responses:
 *       200:
 *         description: List of rooms
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
 *                     $ref: '#/components/schemas/RoomResponse'
 *       401:
 *         description: Unauthorized
 */
router.get('/', authenticate, roomController.getRooms);

/**
 * @swagger
 * /rooms/{id}:
 *   get:
 *     summary: Get room by ID
 *     tags: [Rooms]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Room ID
 *     responses:
 *       200:
 *         description: Room details
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/RoomResponse'
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Room not found
 */
router.get('/:id', authenticate, roomController.getRoomById);

/**
 * @swagger
 * /rooms:
 *   post:
 *     summary: Create a new room
 *     tags: [Rooms]
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
 *                 example: Living Room
 *     responses:
 *       201:
 *         description: Room created successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/RoomResponse'
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 *       409:
 *         description: Room with this name already exists
 */
router.post('/', authenticate, validate(createRoomValidator), roomController.createRoom);

/**
 * @swagger
 * /rooms/{id}:
 *   put:
 *     summary: Update room
 *     tags: [Rooms]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Room ID
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             properties:
 *               name:
 *                 type: string
 *                 example: Living Room
 *     responses:
 *       200:
 *         description: Room updated successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/RoomResponse'
 *       400:
 *         description: Validation error
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Room not found
 *       409:
 *         description: Room with this name already exists
 */
router.put('/:id', authenticate, validate(updateRoomValidator), roomController.updateRoom);

/**
 * @swagger
 * /rooms/{id}:
 *   delete:
 *     summary: Delete room
 *     tags: [Rooms]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: integer
 *         description: Room ID
 *     responses:
 *       200:
 *         description: Room deleted successfully
 *       401:
 *         description: Unauthorized
 *       404:
 *         description: Room not found
 */
router.delete('/:id', authenticate, roomController.deleteRoom);

export default router;

