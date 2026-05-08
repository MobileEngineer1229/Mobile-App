import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { IUserRepository } from '../domain/repositories/IUserRepository';
import { User, CreateUserInput, UpdateUserInput, LoginInput, LoginResponse, UserResponse } from '../models/user';
import { UnauthorizedError, NotFoundError } from '../utils/errors';
import { env } from '../config/env';
import logger from '../utils/logger';

/**
 * User service for business logic
 * Uses IUserRepository interface (Dependency Inversion Principle)
 */
export class UserService {
  constructor(private userRepository: IUserRepository) {}

  /**
   * Hash password
   */
  private async hashPassword(password: string): Promise<string> {
    const saltRounds = 10;
    return bcrypt.hash(password, saltRounds);
  }

  /**
   * Compare password with hash
   */
  private async comparePassword(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }

  /**
   * Generate JWT token
   */
  private generateToken(userId: number, email: string): string {
    if (!env.jwtSecret) {
      throw new Error('JWT_SECRET is not configured');
    }
    return jwt.sign({ userId, email }, env.jwtSecret, {
      expiresIn: env.jwtExpiresIn,
    } as jwt.SignOptions);
  }

  /**
   * Convert user to response format (remove password)
   */
  private toUserResponse(user: User): UserResponse {
    const { password, ...userResponse } = user;
    return {
      ...userResponse,
      notificationPreferences: user.notificationPreferences,
      securitySettings: user.securitySettings,
      profileMetadata: user.profileMetadata,
      additionalSettings: user.additionalSettings,
    };
  }

  /**
   * Sign up new user
   */
  async signup(input: CreateUserInput): Promise<LoginResponse> {
    const hashedPassword = await this.hashPassword(input.password);
    const user = await this.userRepository.create({
      ...input,
      password: hashedPassword,
    });
    logger.user.create(user.id, user.email, {
      firstName: user.firstName,
      lastName: user.lastName,
    });

    const token = this.generateToken(user.id, user.email);

    return {
      user: this.toUserResponse(user),
      token,
    };
  }

  /**
   * Login user
   */
  async login(input: LoginInput): Promise<LoginResponse> {
    const user = await this.userRepository.findByEmail(input.email);

    if (!user) {
      logger.auth.login(input.email, false, { reason: 'User not found' });
      throw new UnauthorizedError('Invalid email or password');
    }

    const isPasswordValid = await this.comparePassword(input.password, user.password);

    if (!isPasswordValid) {
      logger.auth.login(input.email, false, { reason: 'Invalid password', userId: user.id });
      throw new UnauthorizedError('Invalid email or password');
    }

    const token = this.generateToken(user.id, user.email);
    logger.auth.login(input.email, true, { userId: user.id });

    return {
      user: this.toUserResponse(user),
      token,
    };
  }

  /**
   * Get user by ID
   */
  async getUserById(id: number): Promise<UserResponse> {
    const user = await this.userRepository.findById(id);
    
    if (!user) {
      throw new NotFoundError('User');
    }

    return this.toUserResponse(user);
  }

  /**
   * Update user profile
   */
  async updateProfile(userId: number, input: UpdateUserInput): Promise<UserResponse> {
    const user = await this.userRepository.update(userId, input);
    logger.user.update(userId, user.email, {
      updatedFields: Object.keys(input),
    });
    return this.toUserResponse(user);
  }

  /**
   * Get user settings
   */
  async getSettings(_userId: number): Promise<Record<string, any>> {
    // For now, return default settings
    // In production, you might want to store settings in a separate table
    return {
      notifications: {
        push: true,
        email: true,
        sms: false,
      },
      theme: 'dark',
      language: 'en',
      homeManagement: {
        autoSync: true,
      },
    };
  }

  /**
   * Update user settings
   */
  async updateSettings(userId: number, settings: Record<string, any>): Promise<Record<string, any>> {
    // For now, just return the updated settings
    // In production, you might want to store settings in a separate table
    logger.user.update(userId, '', {
      action: 'settings_updated',
      settings: Object.keys(settings),
    });
    return settings;
  }

  /**
   * Change password
   */
  async changePassword(userId: number, currentPassword: string, newPassword: string): Promise<void> {
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new NotFoundError('User');
    }

    const isPasswordValid = await this.comparePassword(currentPassword, user.password);
    if (!isPasswordValid) {
      throw new UnauthorizedError('Current password is incorrect');
    }

    const hashedPassword = await this.hashPassword(newPassword);
    await this.userRepository.updatePassword(userId, hashedPassword);
    logger.user.update(userId, user.email, {
      action: 'password_changed',
    });
  }

  /**
   * Deactivate account
   */
  async deactivateAccount(userId: number): Promise<void> {
    // In production, you might want to add an 'active' or 'deactivated_at' field
    // For now, we'll just log it
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new NotFoundError('User');
    }
    logger.user.update(userId, user.email, {
      action: 'account_deactivated',
    });
  }

  /**
   * Delete account
   */
  async deleteAccount(userId: number): Promise<void> {
    // In production, you might want to soft delete or cascade delete
    // For now, we'll just log it
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new NotFoundError('User');
    }
    logger.user.update(userId, user.email, {
      action: 'account_deleted',
    });
  }
}

