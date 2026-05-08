/**
 * Chatbot message role
 */
export type ChatbotRole = 'user' | 'assistant';

/**
 * Chatbot message model
 */
export interface ChatbotMessage {
  id: number;
  userId: number;
  role: ChatbotRole;
  message: string;
  metadata?: Record<string, unknown> | null;
  createdAt: Date;
}

/**
 * Send message request
 */
export interface SendMessageRequest {
  message: string;
  conversationId?: string; // Optional: for maintaining context
}

/**
 * Send message response
 */
export interface SendMessageResponse {
  userMessage: ChatbotMessage;
  assistantMessage: ChatbotMessage;
  conversationId?: string;
}

/**
 * Chat history query
 */
export interface ChatHistoryQuery {
  page?: number;
  limit?: number;
  startDate?: string;
  endDate?: string;
}

/**
 * Chat history response
 */
export interface ChatHistoryResponse {
  messages: ChatbotMessage[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

