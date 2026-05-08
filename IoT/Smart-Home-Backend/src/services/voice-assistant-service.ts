import { VoiceAssistantRepository } from '../repositories/voice-assistant-repository';
import { VoiceAssistant, LinkVoiceAssistantRequest } from '../models/voice-assistant';
import logger from '../utils/logger';

export class VoiceAssistantService {
  constructor(private voiceAssistantRepository: VoiceAssistantRepository) {}

  /**
   * Get all voice assistants with linking status for a user
   */
  async getVoiceAssistants(userId: number): Promise<VoiceAssistant[]> {
    const assistants = await this.voiceAssistantRepository.findByUserId(userId);
    logger.infoWithEmoji('📱', `Retrieved ${assistants.length} voice assistants for user ${userId}`, 'VOICE_ASSISTANT');
    return assistants;
  }

  /**
   * Link a voice assistant to a user
   */
  async linkAssistant(request: LinkVoiceAssistantRequest): Promise<VoiceAssistant> {
    const assistant = await this.voiceAssistantRepository.linkAssistant(request);
    logger.infoWithEmoji('🔗', `User ${request.userId} linked assistant: ${assistant.name}`, 'VOICE_ASSISTANT', {
      assistantId: assistant.id,
      assistantName: assistant.name,
    });
    return assistant;
  }

  /**
   * Unlink a voice assistant from a user
   */
  async unlinkAssistant(userId: number, assistantId: number): Promise<void> {
    await this.voiceAssistantRepository.unlinkAssistant(userId, assistantId);
    logger.infoWithEmoji('🔓', `User ${userId} unlinked assistant ${assistantId}`, 'VOICE_ASSISTANT');
  }
}

