import { Router } from 'express';
import { VoiceAssistantController } from '../controllers/voice-assistant-controller';
import { VoiceAssistantService } from '../services/voice-assistant-service';
import { VoiceAssistantRepository } from '../repositories/voice-assistant-repository';
import { getPool } from '../config/database';
import { authenticate } from '../middleware/auth';

const router: Router = Router();

// Initialize dependencies
const voiceAssistantRepository = new VoiceAssistantRepository(getPool());
const voiceAssistantService = new VoiceAssistantService(voiceAssistantRepository);
const voiceAssistantController = new VoiceAssistantController(voiceAssistantService);

/**
 * GET /voice-assistants
 * Get all voice assistants with linking status
 */
router.get('/', authenticate, voiceAssistantController.getVoiceAssistants);

/**
 * POST /voice-assistants/:id/link
 * Link a voice assistant
 */
router.post('/:id/link', authenticate, voiceAssistantController.linkAssistant);

/**
 * POST /voice-assistants/:id/unlink
 * Unlink a voice assistant
 */
router.post('/:id/unlink', authenticate, voiceAssistantController.unlinkAssistant);

export default router;

