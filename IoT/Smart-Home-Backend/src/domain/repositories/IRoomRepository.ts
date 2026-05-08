import { Room, CreateRoomInput, UpdateRoomInput } from '../../models/room';

/**
 * Room repository interface
 * Defines the contract for room data access operations
 */
export interface IRoomRepository {
  findById(id: number, userId: number): Promise<Room | null>;
  findAll(userId: number, homeId?: number): Promise<Room[]>;
  findByName(name: string, userId: number, homeId?: number): Promise<Room | null>;
  create(userId: number, input: CreateRoomInput): Promise<Room>;
  update(id: number, userId: number, input: UpdateRoomInput): Promise<Room>;
  delete(id: number, userId: number): Promise<void>;
}
