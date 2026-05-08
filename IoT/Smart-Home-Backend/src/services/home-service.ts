import { IHomeRepository } from '../domain/repositories/IHomeRepository';
import { IHomeMemberRepository } from '../domain/repositories/IHomeMemberRepository';
import { Home, CreateHomeInput, UpdateHomeInput, HomeResponse } from '../models/home';
import { HomeMemberRole } from '../models/home-member';
import { NotFoundError } from '../utils/errors';
import logger from '../utils/logger';

/**
 * Home service for business logic
 * Uses repository interfaces (Dependency Inversion Principle)
 */
export class HomeService {
  constructor(
    private homeRepository: IHomeRepository,
    private homeMemberRepository?: IHomeMemberRepository
  ) {}

  /**
   * Convert home to response format
   */
  private toHomeResponse(home: Home): HomeResponse {
    return {
      id: home.id,
      userId: home.userId,
      name: home.name,
      address: home.address,
      latitude: home.latitude,
      longitude: home.longitude,
      country: home.country,
      isPrimary: home.isPrimary,
      createdAt: home.createdAt,
      updatedAt: home.updatedAt,
    };
  }

  /**
   * Get home by ID (checks both owner and members)
   */
  async getHomeById(id: number, userId: number): Promise<HomeResponse> {
    const home = await this.homeRepository.findById(id, userId);

    if (!home) {
      throw new NotFoundError('Home');
    }

    return this.toHomeResponse(home);
  }

  /**
   * Get all homes for a user
   */
  async getHomes(userId: number): Promise<HomeResponse[]> {
    const homes = await this.homeRepository.findAll(userId);
    return homes.map((home) => this.toHomeResponse(home));
  }

  /**
   * Get primary home for a user
   */
  async getPrimaryHome(userId: number): Promise<HomeResponse | null> {
    const home = await this.homeRepository.findPrimary(userId);
    return home ? this.toHomeResponse(home) : null;
  }

  /**
   * Create new home
   */
  async createHome(userId: number, input: CreateHomeInput): Promise<HomeResponse> {
    // Check if home with same name already exists for this user
    const existing = await this.homeRepository.findByName(input.name, userId);
    if (existing) {
      throw new Error('Home with this name already exists');
    }

    // If no primary home exists, make this one primary
    const primaryHome = await this.homeRepository.findPrimary(userId);
    if (!primaryHome && input.isPrimary !== false) {
      input.isPrimary = true;
    }

    const home = await this.homeRepository.create(userId, input);
    
    // Automatically add creator as owner member
    if (this.homeMemberRepository) {
      try {
        await this.homeMemberRepository.create(home.id, userId, HomeMemberRole.OWNER, userId);
      } catch (error) {
        // If member creation fails, log but don't fail home creation
        logger.errorWithEmoji('❌', `Failed to add creator as owner member`, 'HOME', {
          homeId: home.id,
          userId,
          error: error instanceof Error ? error.message : String(error),
        });
      }
    }
    
    logger.infoWithEmoji('🏡', `Home created: ${home.name}`, 'HOME', { homeId: home.id, userId });
    return this.toHomeResponse(home);
  }

  /**
   * Update home
   */
  async updateHome(id: number, userId: number, input: UpdateHomeInput): Promise<HomeResponse> {
    // If name is being updated, check for duplicates
    if (input.name) {
      const existing = await this.homeRepository.findByName(input.name, userId);
      if (existing && existing.id !== id) {
        throw new Error('Home with this name already exists');
      }
    }

    const home = await this.homeRepository.update(id, userId, input);
    logger.infoWithEmoji('🏡', `Home updated: ${home.name}`, 'HOME', { homeId: home.id, userId });
    return this.toHomeResponse(home);
  }

  /**
   * Delete home
   */
  async deleteHome(id: number, userId: number): Promise<void> {
    await this.homeRepository.delete(id, userId);
    logger.infoWithEmoji('🏡', `Home deleted`, 'HOME', { homeId: id, userId });
  }
}
