import { IHomeMemberRepository } from '../domain/repositories/IHomeMemberRepository';
import { IUserRepository } from '../domain/repositories/IUserRepository';
import {
  HomeMember,
  HomeMemberRole,
  CreateHomeMemberInput,
  UpdateHomeMemberInput,
  HomeMemberResponse,
} from '../models/home-member';
import { NotFoundError, ConflictError, ForbiddenError } from '../utils/errors';
import logger from '../utils/logger';

/**
 * Home member service for business logic
 * Uses repository interfaces (Dependency Inversion Principle)
 */
export class HomeMemberService {
  constructor(
    private homeMemberRepository: IHomeMemberRepository,
    private userRepository: IUserRepository
  ) {}

  /**
   * Convert home member to response format
   */
  private async toHomeMemberResponse(member: HomeMember): Promise<HomeMemberResponse> {
    const user = await this.userRepository.findById(member.userId);
    return {
      id: member.id,
      homeId: member.homeId,
      userId: member.userId,
      role: member.role,
      addedBy: member.addedBy,
      createdAt: member.createdAt,
      user: user
        ? {
            id: user.id,
            email: user.email,
            firstName: user.firstName,
            lastName: user.lastName,
          }
        : undefined,
    };
  }

  /**
   * Get all members of a home
   */
  async getHomeMembers(homeId: number, userId: number): Promise<HomeMemberResponse[]> {
    // Check if user is a member of the home
    const isMember = await this.homeMemberRepository.isMember(userId, homeId);
    if (!isMember) {
      throw new ForbiddenError('You are not a member of this home');
    }

    const members = await this.homeMemberRepository.findByHomeId(homeId);
    return Promise.all(members.map((member) => this.toHomeMemberResponse(member)));
  }

  /**
   * Get member by ID
   */
  async getMemberById(
    memberId: number,
    homeId: number,
    userId: number
  ): Promise<HomeMemberResponse> {
    // Check if user is a member of the home
    const isMember = await this.homeMemberRepository.isMember(userId, homeId);
    if (!isMember) {
      throw new ForbiddenError('You are not a member of this home');
    }

    const member = await this.homeMemberRepository.findById(memberId, homeId);
    if (!member) {
      throw new NotFoundError('Home member');
    }

    return this.toHomeMemberResponse(member);
  }

  /**
   * Add member to home
   */
  async addMember(
    homeId: number,
    input: CreateHomeMemberInput,
    addedBy: number
  ): Promise<HomeMemberResponse> {
    // Check if adder is owner or admin
    const adderRole = await this.homeMemberRepository.getMemberRole(addedBy, homeId);
    if (!adderRole || (adderRole !== HomeMemberRole.OWNER && adderRole !== HomeMemberRole.ADMIN)) {
      throw new ForbiddenError('Only owners and admins can add members');
    }

    // Find user by email or use provided userId
    let targetUserId: number;
    if (input.email) {
      const user = await this.userRepository.findByEmail(input.email);
      if (!user) {
        throw new NotFoundError('User with this email');
      }
      targetUserId = user.id;
    } else if (input.userId) {
      targetUserId = input.userId;
    } else {
      throw new Error('Either email or userId must be provided');
    }

    // Check if user is already a member
    const existing = await this.homeMemberRepository.findByUserIdAndHomeId(targetUserId, homeId);
    if (existing) {
      throw new ConflictError('User is already a member of this home');
    }

    // Default role is member
    const role = input.role || HomeMemberRole.MEMBER;

    // Create member
    const member = await this.homeMemberRepository.create(homeId, targetUserId, role, addedBy);
    logger.infoWithEmoji('👥', `Member added to home`, 'HOME_MEMBER', {
      homeId,
      userId: targetUserId,
      role,
      addedBy,
    });

    return this.toHomeMemberResponse(member);
  }

  /**
   * Update member role
   */
  async updateMember(
    memberId: number,
    homeId: number,
    input: UpdateHomeMemberInput,
    userId: number
  ): Promise<HomeMemberResponse> {
    // Check if user is owner or admin
    const userRole = await this.homeMemberRepository.getMemberRole(userId, homeId);
    if (!userRole || (userRole !== HomeMemberRole.OWNER && userRole !== HomeMemberRole.ADMIN)) {
      throw new ForbiddenError('Only owners and admins can update member roles');
    }

    // Get member to update
    const member = await this.homeMemberRepository.findById(memberId, homeId);
    if (!member) {
      throw new NotFoundError('Home member');
    }

    // Only owner can change owner role
    if (member.role === HomeMemberRole.OWNER && userRole !== HomeMemberRole.OWNER) {
      throw new ForbiddenError('Only owner can change owner role');
    }

    // Update member
    const updated = await this.homeMemberRepository.update(memberId, homeId, input);
    logger.infoWithEmoji('👥', `Member role updated`, 'HOME_MEMBER', {
      memberId,
      homeId,
      newRole: updated.role,
      updatedBy: userId,
    });

    return this.toHomeMemberResponse(updated);
  }

  /**
   * Remove member from home
   */
  async removeMember(memberId: number, homeId: number, userId: number): Promise<void> {
    // Check if user is owner or admin
    const userRole = await this.homeMemberRepository.getMemberRole(userId, homeId);
    if (!userRole || (userRole !== HomeMemberRole.OWNER && userRole !== HomeMemberRole.ADMIN)) {
      throw new ForbiddenError('Only owners and admins can remove members');
    }

    // Get member to remove
    const member = await this.homeMemberRepository.findById(memberId, homeId);
    if (!member) {
      throw new NotFoundError('Home member');
    }

    // Cannot remove owner
    if (member.role === HomeMemberRole.OWNER) {
      throw new ForbiddenError('Cannot remove owner from home');
    }

    // Cannot remove yourself if you're the only admin
    if (member.userId === userId && userRole === HomeMemberRole.ADMIN) {
      const members = await this.homeMemberRepository.findByHomeId(homeId);
      const adminCount = members.filter((m) => m.role === HomeMemberRole.ADMIN).length;
      if (adminCount === 1) {
        throw new ForbiddenError('Cannot remove the only admin from home');
      }
    }

    await this.homeMemberRepository.delete(memberId, homeId);
    logger.infoWithEmoji('👥', `Member removed from home`, 'HOME_MEMBER', {
      memberId,
      homeId,
      removedBy: userId,
    });
  }
}
