import { User, CreateUserInput, UpdateUserInput } from '../../models/user';

/**
 * User repository interface
 * Defines the contract for user data access operations
 */
export interface IUserRepository {
  findById(id: number): Promise<User | null>;
  findByEmail(email: string): Promise<User | null>;
  create(input: CreateUserInput): Promise<User>;
  update(id: number, input: UpdateUserInput): Promise<User>;
  updatePassword(id: number, hashedPassword: string): Promise<void>;
  delete(id: number): Promise<void>;
}
