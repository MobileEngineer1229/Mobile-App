export interface VoiceAssistant {
  id: number;
  name: string;
  isLinked: boolean;
  userId?: number;
  linkedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LinkVoiceAssistantRequest {
  userId: number;
  assistantId: number;
  accessToken?: string;
  refreshToken?: string;
  metadata?: Record<string, any>;
}

