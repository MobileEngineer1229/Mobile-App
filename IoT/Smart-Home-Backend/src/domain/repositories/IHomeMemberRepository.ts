import { HomeMember, HomeMemberRole, UpdateHomeMemberInput } from '../../models/home-member';

/**
 * Home member repository interface
 * Defines the contract for home member data access operations
 */
export interface IHomeMemberRepository {
  findByHomeId(homeId: number): Promise<HomeMember[]>;
  findById(memberId: number, homeId: number): Promise<HomeMember | null>;
  findByUserIdAndHomeId(userId: number, homeId: number): Promise<HomeMember | null>;
  create(homeId: number, userId: number, role: HomeMemberRole, addedBy: number): Promise<HomeMember>;
  update(memberId: number, homeId: number, input: UpdateHomeMemberInput): Promise<HomeMember>;
  delete(memberId: number, homeId: number): Promise<void>;
  isMember(userId: number, homeId: number): Promise<boolean>;
  getMemberRole(userId: number, homeId: number): Promise<HomeMemberRole | null>;
}
