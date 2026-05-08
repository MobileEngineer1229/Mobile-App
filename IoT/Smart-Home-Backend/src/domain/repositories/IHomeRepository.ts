import { Home, CreateHomeInput, UpdateHomeInput } from '../../models/home';

/**
 * Home repository interface
 * Defines the contract for home data access operations
 */
export interface IHomeRepository {
  findById(id: number, userId: number): Promise<Home | null>;
  findByIdOnly(id: number): Promise<Home | null>;
  findAll(userId: number): Promise<Home[]>;
  findPrimary(userId: number): Promise<Home | null>;
  findByName(name: string, userId: number): Promise<Home | null>;
  create(userId: number, input: CreateHomeInput): Promise<Home>;
  update(id: number, userId: number, input: UpdateHomeInput): Promise<Home>;
  delete(id: number, userId: number): Promise<void>;
}
