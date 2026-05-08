import { HomeInvitation, CreateHomeInvitationInput } from '../../models/home-invitation';

/**
 * Home invitation repository interface
 * Defines the contract for home invitation data access operations
 */
export interface IHomeInvitationRepository {
  findByCode(code: string): Promise<HomeInvitation | null>;
  findActiveByCode(code: string): Promise<HomeInvitation | null>;
  findByHomeId(homeId: number): Promise<HomeInvitation[]>;
  create(homeId: number, code: string, createdBy: number, input: CreateHomeInvitationInput): Promise<HomeInvitation>;
  incrementUses(code: string): Promise<void>;
  deactivate(code: string): Promise<void>;
  delete(code: string): Promise<void>;
  generateUniqueCode(): Promise<string>;
}
