import { ChatbotRepository } from '../repositories/chatbot-repository';
import {
  ChatbotMessage,
  SendMessageRequest,
  SendMessageResponse,
  ChatHistoryQuery,
  ChatHistoryResponse,
} from '../models/chatbot';
import logger from '../utils/logger';

export class ChatbotService {
  constructor(private chatbotRepository: ChatbotRepository) {}

  /**
   * Process user message and generate response
   */
  async sendMessage(userId: number, request: SendMessageRequest): Promise<SendMessageResponse> {
    // Save user message
    const userMessage = await this.chatbotRepository.createMessage(userId, 'user', request.message);

    // Get recent conversation context
    const recentMessages = await this.chatbotRepository.getRecentMessages(userId, 5);

    // Generate bot response
    const botResponse = this.generateResponse(request.message, recentMessages);

    // Save bot response
    const assistantMessage = await this.chatbotRepository.createMessage(
      userId,
      'assistant',
      botResponse.message,
      botResponse.metadata
    );

    logger.info('Chatbot message processed', {
      userId,
      userMessageId: userMessage.id,
      assistantMessageId: assistantMessage.id,
    });

    return {
      userMessage,
      assistantMessage,
      conversationId: request.conversationId,
    };
  }

  /**
   * Get chat history
   */
  async getHistory(userId: number, query: ChatHistoryQuery): Promise<ChatHistoryResponse> {
    const page = query.page || 1;
    const limit = query.limit || 50;

    const { messages, total } = await this.chatbotRepository.getHistory(userId, query);
    const totalPages = Math.ceil(total / limit);

    return {
      messages,
      total,
      page,
      limit,
      totalPages,
    };
  }

  /**
   * Clear chat history
   */
  async clearHistory(userId: number): Promise<{ count: number }> {
    const count = await this.chatbotRepository.deleteHistory(userId);
    logger.info('Chat history cleared', { userId, count });
    return { count };
  }

  /**
   * Generate bot response based on user message
   */
  private generateResponse(
    userMessage: string,
    _context: ChatbotMessage[]
  ): { message: string; metadata?: Record<string, unknown> } {
    const message = userMessage.toLowerCase().trim();

    // Greeting responses
    if (this.matchesPattern(message, ['hi', 'hello', 'hey', 'greetings'])) {
      return {
        message: `Hello there! 👋 How can I assist you today?`,
      };
    }

    // Help requests
    if (this.matchesPattern(message, ['help', 'what can you do', 'what do you do'])) {
      return {
        message: `Awesome! ▶️ With Smartify, you can control devices, set up automation, manage energy, and more! What are you interested in exploring first?`,
        metadata: {
          suggestions: ['Automation', 'Device Control', 'Energy Management', 'Reports'],
        },
      };
    }

    // Automation questions
    if (this.matchesPattern(message, ['automation', 'automate', 'schedule', 'routine'])) {
      return {
        message: `Great question! Automation lets you create smart routines. For example, you can set lights to turn on at sunset, or have your thermostat adjust when you leave home. Would you like to create an automation?`,
        metadata: {
          suggestions: ['Create Automation', 'View Existing Automations', 'Learn More'],
        },
      };
    }

    // Device control
    if (this.matchesPattern(message, ['device', 'control', 'turn on', 'turn off', 'switch'])) {
      return {
        message: `You can control all your smart devices from the main screen! Tap on any device to turn it on/off, adjust settings, or view details. Need help with a specific device?`,
      };
    }

    // Energy management
    if (this.matchesPattern(message, ['energy', 'consumption', 'usage', 'bill', 'cost'])) {
      return {
        message: `Check out the Reports section to see your energy consumption, costs, and detailed analytics. You can view data by device, room, or time period. Want to see your monthly summary?`,
        metadata: {
          suggestions: ['View Reports', 'Energy Tips', 'Set Budget'],
        },
      };
    }

    // Account/security
    if (this.matchesPattern(message, ['account', 'profile', 'settings', 'password', 'security'])) {
      return {
        message: `You can manage your account settings, update your profile, change your password, and configure security options from the Settings menu. Is there something specific you'd like to change?`,
      };
    }

    // Room management
    if (this.matchesPattern(message, ['room', 'rooms', 'add room', 'organize'])) {
      return {
        message: `Rooms help you organize your devices! You can create rooms, assign devices to rooms, and filter devices by room. This makes managing your smart home much easier. Want to create a new room?`,
      };
    }

    // Reports/analytics
    if (this.matchesPattern(message, ['report', 'analytics', 'statistics', 'data', 'summary'])) {
      return {
        message: `The Reports section shows detailed analytics about your energy consumption, device usage, and costs. You can view monthly summaries, statistics, and device-level details. Check it out!`,
      };
    }

    // Default response
    return {
      message: `I'm here to help you with your Smartify smart home! You can ask me about:
• Setting up automation
• Controlling devices
• Managing energy consumption
• Organizing rooms
• Viewing reports
• Account settings

What would you like to know more about?`,
      metadata: {
        suggestions: ['Automation', 'Devices', 'Energy', 'Rooms', 'Reports'],
      },
    };
  }

  /**
   * Check if message matches any pattern
   */
  private matchesPattern(message: string, patterns: string[]): boolean {
    return patterns.some((pattern) => message.includes(pattern));
  }
}

