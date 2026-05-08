import { LinkedAccountRepository } from '../repositories/linked-account-repository';
import logger from '../utils/logger';

export class LinkedAccountService {
  constructor(private linkedAccountRepository: LinkedAccountRepository) {}

  /**
   * Get linked accounts for a user (including available providers)
   */
  async getLinkedAccounts(userId: number) {
    const linkedAccounts = await this.linkedAccountRepository.findByUserId(userId);
    const availableProviders = await this.linkedAccountRepository.getAvailableProviders();

    // Create a list with all providers, marking which are linked
    const providersMap = new Map<string, boolean>();
    linkedAccounts.forEach(account => {
      providersMap.set(account.provider, true);
    });

    const allAccounts = availableProviders.map(provider => {
      const existing = linkedAccounts.find(acc => acc.provider === provider);
      if (existing) {
        return {
          ...existing,
          isConnected: true,
        };
      }
      // Return a placeholder for unlinked providers
      return {
        id: 0,
        userId,
        provider,
        isConnected: false,
      } as any;
    });

    logger.infoWithEmoji('🔗', `Retrieved linked accounts for user ${userId}`, 'LINKED_ACCOUNTS');
    return allAccounts;
  }

  /**
   * Link an account
   */
  async linkAccount(
    userId: number,
    provider: string,
    data: {
      providerUserId?: string;
      accessToken?: string;
      refreshToken?: string;
      expiresAt?: string;
      metadata?: Record<string, any>;
    }
  ) {
    const account = await this.linkedAccountRepository.linkAccount(userId, provider, data);
    logger.infoWithEmoji('🔗', `User ${userId} linked account: ${provider}`, 'LINKED_ACCOUNTS');
    return account;
  }

  /**
   * Unlink an account
   */
  async unlinkAccount(userId: number, accountId: number) {
    await this.linkedAccountRepository.unlinkAccount(userId, accountId);
    logger.infoWithEmoji('🔓', `User ${userId} unlinked account ${accountId}`, 'LINKED_ACCOUNTS');
  }
}

