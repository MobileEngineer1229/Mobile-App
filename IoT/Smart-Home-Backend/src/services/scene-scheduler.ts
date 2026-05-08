import { SceneAutomationEngine } from './scene-automation-engine';
import logger from '../utils/logger';

/**
 * Scene Scheduler
 *
 * Runs every 60 seconds to evaluate time-based automation conditions.
 * Uses setInterval — no external cron dependency needed.
 */
export class SceneScheduler {
  private static instance: SceneScheduler;
  private intervalId: ReturnType<typeof setInterval> | null = null;
  private readonly INTERVAL_MS = 60_000; // 60 seconds

  private constructor() {}

  static getInstance(): SceneScheduler {
    if (!SceneScheduler.instance) {
      SceneScheduler.instance = new SceneScheduler();
    }
    return SceneScheduler.instance;
  }

  start(): void {
    if (this.intervalId) {
      logger.warnWithEmoji('⚠️', 'Scene scheduler already running', 'SCHEDULER');
      return;
    }

    logger.infoWithEmoji('⏰', 'Scene scheduler started (60s interval)', 'SCHEDULER');

    // Run immediately on start, then every 60s
    this.tick();
    this.intervalId = setInterval(() => this.tick(), this.INTERVAL_MS);
  }

  stop(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
      logger.infoWithEmoji('⏹️', 'Scene scheduler stopped', 'SCHEDULER');
    }
  }

  private async tick(): Promise<void> {
    try {
      const engine = SceneAutomationEngine.getInstance();
      await engine.evaluateScheduleConditions();
    } catch (error) {
      logger.error('Scheduler tick error:', error);
    }
  }
}
