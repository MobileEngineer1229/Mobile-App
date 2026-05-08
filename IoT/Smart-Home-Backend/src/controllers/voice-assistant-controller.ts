import { Request, Response, NextFunction } from 'express';
import { VoiceAssistantService } from '../services/voice-assistant-service';
import { sendSuccess, sendError } from '../utils/response';
import { LinkVoiceAssistantRequest } from '../models/voice-assistant';
import logger from '../utils/logger'; // eslint-disable-line @typescript-eslint/no-unused-vars

export class VoiceAssistantController {
  constructor(private voiceAssistantService: VoiceAssistantService) {}

  /**
   * Get all voice assistants with linking status
   */
  getVoiceAssistants = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const assistants = await this.voiceAssistantService.getVoiceAssistants(req.user!.id);
      sendSuccess(res, assistants, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Link a voice assistant
   */
  linkAssistant = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const assistantId = parseInt(req.params.id, 10);
      if (isNaN(assistantId)) {
        sendError(res, 'BAD_REQUEST', 'Invalid assistant ID', 400);
        return;
      }

      const request: LinkVoiceAssistantRequest = {
        userId: req.user!.id,
        assistantId,
        accessToken: req.body.accessToken,
        refreshToken: req.body.refreshToken,
        metadata: req.body.metadata,
      };

      const assistant = await this.voiceAssistantService.linkAssistant(request);
      sendSuccess(res, assistant, 200);
    } catch (error) {
      next(error);
    }
  };

  /**
   * Unlink a voice assistant
   */
  unlinkAssistant = async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const assistantId = parseInt(req.params.id, 10);
      if (isNaN(assistantId)) {
        sendError(res, 'BAD_REQUEST', 'Invalid assistant ID', 400);
        return;
      }

      await this.voiceAssistantService.unlinkAssistant(req.user!.id, assistantId);
      sendSuccess(res, { message: 'Voice assistant unlinked successfully' }, 200);
    } catch (error) {
      next(error);
    }
  };
}

