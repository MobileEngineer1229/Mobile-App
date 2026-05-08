import { IRoomRepository } from '../domain/repositories/IRoomRepository';
import { Room, CreateRoomInput, UpdateRoomInput, RoomResponse } from '../models/room';
import { NotFoundError } from '../utils/errors';
import logger from '../utils/logger';

/**
 * Room service for business logic
 * Uses IRoomRepository interface (Dependency Inversion Principle)
 */
export class RoomService {
  constructor(private roomRepository: IRoomRepository) {}

  /**
   * Convert room to response format
   */
  private toRoomResponse(room: Room): RoomResponse {
    return {
      id: room.id,
      userId: room.userId,
      homeId: room.homeId,
      name: room.name,
      createdAt: room.createdAt,
      updatedAt: room.updatedAt,
    };
  }

  /**
   * Get room by ID
   */
  async getRoomById(id: number, userId: number): Promise<RoomResponse> {
    const room = await this.roomRepository.findById(id, userId);

    if (!room) {
      throw new NotFoundError('Room');
    }

    return this.toRoomResponse(room);
  }

  /**
   * Get all rooms for a user, optionally filtered by home
   */
  async getRooms(userId: number, homeId?: number): Promise<RoomResponse[]> {
    const rooms = await this.roomRepository.findAll(userId, homeId);
    return rooms.map((room) => this.toRoomResponse(room));
  }

  /**
   * Create new room
   */
  async createRoom(userId: number, input: CreateRoomInput): Promise<RoomResponse> {
    // Check if room with same name already exists for this user
    const existing = await this.roomRepository.findByName(input.name, userId);
    if (existing) {
      throw new Error('Room with this name already exists');
    }

    const room = await this.roomRepository.create(userId, input);
    logger.infoWithEmoji('🏠', `Room created: ${room.name}`, 'ROOM', { roomId: room.id, userId });
    return this.toRoomResponse(room);
  }

  /**
   * Update room
   */
  async updateRoom(id: number, userId: number, input: UpdateRoomInput): Promise<RoomResponse> {
    // If name is being updated, check for duplicates
    if (input.name) {
      const existing = await this.roomRepository.findByName(input.name, userId);
      if (existing && existing.id !== id) {
        throw new Error('Room with this name already exists');
      }
    }

    const room = await this.roomRepository.update(id, userId, input);
    logger.infoWithEmoji('🏠', `Room updated: ${room.name}`, 'ROOM', { roomId: room.id, userId });
    return this.toRoomResponse(room);
  }

  /**
   * Delete room
   */
  async deleteRoom(id: number, userId: number): Promise<void> {
    await this.roomRepository.delete(id, userId);
    logger.infoWithEmoji('🏠', `Room deleted`, 'ROOM', { roomId: id, userId });
  }
}

