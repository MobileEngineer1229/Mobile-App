import { IHomeInvitationRepository } from '../domain/repositories/IHomeInvitationRepository';
import { IHomeMemberRepository } from '../domain/repositories/IHomeMemberRepository';
import { IHomeRepository } from '../domain/repositories/IHomeRepository';
import {
  HomeInvitation,
  CreateHomeInvitationInput,
  HomeInvitationResponse,
} from '../models/home-invitation';
import { HomeMemberRole } from '../models/home-member';
import { NotFoundError, ForbiddenError, ConflictError } from '../utils/errors';
import logger from '../utils/logger';

/**
 * Home invitation service for business logic
 * Uses repository interfaces (Dependency Inversion Principle)
 */
export class HomeInvitationService {
  constructor(
    private homeInvitationRepository: IHomeInvitationRepository,
    private homeMemberRepository: IHomeMemberRepository,
    private homeRepository: IHomeRepository
  ) {}

  /**
   * Convert invitation to response format
   */
  private async toInvitationResponse(
    invitation: HomeInvitation
  ): Promise<HomeInvitationResponse> {
    const home = await this.homeRepository.findByIdOnly(invitation.homeId);
    return {
      id: invitation.id,
      homeId: invitation.homeId,
      code: invitation.code,
      createdBy: invitation.createdBy,
      expiresAt: invitation.expiresAt,
      maxUses: invitation.maxUses,
      currentUses: invitation.currentUses,
      isActive: invitation.isActive,
      createdAt: invitation.createdAt,
      home: home
        ? {
            id: home.id,
            name: home.name,
            address: home.address,
          }
        : undefined,
    };
  }

  /**
   * Create invitation for a home
   */
  async createInvitation(
    homeId: number,
    input: CreateHomeInvitationInput,
    userId: number
  ): Promise<HomeInvitationResponse> {
    // Check if user is owner or admin
    const userRole = await this.homeMemberRepository.getMemberRole(userId, homeId);
    if (!userRole || (userRole !== HomeMemberRole.OWNER && userRole !== HomeMemberRole.ADMIN)) {
      throw new ForbiddenError('Only owners and admins can create invitations');
    }

    // Verify home exists
    const home = await this.homeRepository.findById(homeId, userId);
    if (!home) {
      throw new NotFoundError('Home');
    }

    // Generate unique code
    const code = await this.homeInvitationRepository.generateUniqueCode();

    // Create invitation
    const invitation = await this.homeInvitationRepository.create(
      homeId,
      code,
      userId,
      input
    );

    logger.infoWithEmoji('📧', `Invitation created for home`, 'HOME_INVITATION', {
      homeId,
      code,
      createdBy: userId,
    });

    return this.toInvitationResponse(invitation);
  }

  /**
   * Get invitation by code
   */
  async getInvitationByCode(code: string): Promise<HomeInvitationResponse> {
    const invitation = await this.homeInvitationRepository.findActiveByCode(code);
    if (!invitation) {
      throw new NotFoundError('Invitation code is invalid or expired');
    }

    return this.toInvitationResponse(invitation);
  }

  /**
   * Accept invitation and join home
   */
  async acceptInvitation(code: string, userId: number): Promise<void> {
    // Get active invitation
    const invitation = await this.homeInvitationRepository.findActiveByCode(code);
    if (!invitation) {
      throw new NotFoundError('Invitation code is invalid or expired');
    }

    // Check if user is already a member
    const isMember = await this.homeMemberRepository.isMember(userId, invitation.homeId);
    if (isMember) {
      throw new ConflictError('You are already a member of this home');
    }

    // Add user as member (default role: member)
    await this.homeMemberRepository.create(
      invitation.homeId,
      userId,
      HomeMemberRole.MEMBER,
      invitation.createdBy
    );

    // Increment usage count
    await this.homeInvitationRepository.incrementUses(code);

    logger.infoWithEmoji('📧', `Invitation accepted`, 'HOME_INVITATION', {
      code,
      homeId: invitation.homeId,
      userId,
    });
  }

  /**
   * Get all invitations for a home
   */
  async getHomeInvitations(homeId: number, userId: number): Promise<HomeInvitationResponse[]> {
    // Check if user is owner or admin
    const userRole = await this.homeMemberRepository.getMemberRole(userId, homeId);
    if (!userRole || (userRole !== HomeMemberRole.OWNER && userRole !== HomeMemberRole.ADMIN)) {
      throw new ForbiddenError('Only owners and admins can view invitations');
    }

    const invitations = await this.homeInvitationRepository.findByHomeId(homeId);
    return Promise.all(invitations.map((inv) => this.toInvitationResponse(inv)));
  }

  /**
   * Deactivate invitation
   */
  async deactivateInvitation(code: string, userId: number): Promise<void> {
    const invitation = await this.homeInvitationRepository.findByCode(code);
    if (!invitation) {
      throw new NotFoundError('Invitation');
    }

    // Check if user is owner or admin
    const userRole = await this.homeMemberRepository.getMemberRole(userId, invitation.homeId);
    if (!userRole || (userRole !== HomeMemberRole.OWNER && userRole !== HomeMemberRole.ADMIN)) {
      throw new ForbiddenError('Only owners and admins can deactivate invitations');
    }

    await this.homeInvitationRepository.deactivate(code);
    logger.infoWithEmoji('📧', `Invitation deactivated`, 'HOME_INVITATION', {
      code,
      homeId: invitation.homeId,
      deactivatedBy: userId,
    });
  }
}
