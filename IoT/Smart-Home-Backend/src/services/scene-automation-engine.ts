import { Pool } from 'pg';
import { getPool } from '../config/database';
import { executeScene } from './sceneService';
import logger from '../utils/logger';

/**
 * Scene Automation Engine
 *
 * Evaluates automation scene conditions when MQTT data arrives.
 * Triggers scene execution when all/any conditions are met.
 * Includes debounce (60s cooldown per scene) to prevent rapid re-triggering.
 */
export class SceneAutomationEngine {
  private static instance: SceneAutomationEngine;
  private pool: Pool;
  private lastTriggered: Map<number, number> = new Map(); // sceneId → timestamp
  private readonly DEBOUNCE_MS = 60_000; // 60 seconds

  private constructor() {
    this.pool = getPool();
  }

  static getInstance(): SceneAutomationEngine {
    if (!SceneAutomationEngine.instance) {
      SceneAutomationEngine.instance = new SceneAutomationEngine();
    }
    return SceneAutomationEngine.instance;
  }

  /**
   * Called when a device reports property data via MQTT.
   * Finds automation scenes that might be affected and evaluates their conditions.
   */
  async evaluateForPropertyUpdate(
    productId: string,
    deviceNum: string,
    data: Record<string, unknown>
  ): Promise<void> {
    try {
      // Find the device to get its home_id and user_id
      const deviceResult = await this.pool.query(
        `SELECT id, user_id, home_id FROM devices
         WHERE metadata->>'productId' = $1
           AND metadata->>'deviceNum' = $2`,
        [productId, deviceNum]
      );

      if (deviceResult.rowCount === 0) return;

      const device = deviceResult.rows[0];
      await this.evaluateAutomations(device.user_id, device.home_id, 'property', data);
    } catch (error) {
      logger.error('Error evaluating automation for property update:', error);
    }
  }

  /**
   * Called when a device reports online/offline status via MQTT.
   */
  async evaluateForStatusChange(
    productId: string,
    deviceNum: string,
    data: Record<string, unknown>
  ): Promise<void> {
    try {
      const deviceResult = await this.pool.query(
        `SELECT id, user_id, home_id FROM devices
         WHERE metadata->>'productId' = $1
           AND metadata->>'deviceNum' = $2`,
        [productId, deviceNum]
      );

      if (deviceResult.rowCount === 0) return;

      const device = deviceResult.rows[0];
      const status = data.status === 1 || data.status === 'online' ? 'online' : 'offline';
      await this.evaluateAutomations(device.user_id, device.home_id, 'status', {
        deviceId: device.id,
        status,
      });
    } catch (error) {
      logger.error('Error evaluating automation for status change:', error);
    }
  }

  /**
   * Core evaluation: load enabled automations and check conditions.
   */
  private async evaluateAutomations(
    userId: number,
    homeId: number | null,
    triggerType: 'property' | 'status' | 'schedule',
    triggerData: Record<string, unknown>
  ): Promise<void> {
    // Query enabled automation scenes for this user/home
    let query = `
      SELECT id, name, condition_logic, home_id
      FROM smart_scenes
      WHERE user_id = $1
        AND type = 'automation'
        AND is_enabled = true
    `;
    const params: unknown[] = [userId];

    if (homeId) {
      query += ' AND (home_id = $2 OR home_id IS NULL)';
      params.push(homeId);
    }

    const scenesResult = await this.pool.query(query, params);

    for (const scene of scenesResult.rows) {
      // Debounce check
      const lastTrigger = this.lastTriggered.get(scene.id) || 0;
      if (Date.now() - lastTrigger < this.DEBOUNCE_MS) {
        continue;
      }

      // Load conditions
      const conditionsResult = await this.pool.query(
        `SELECT type, operator, value, unit, location, device_id, device_status, arm_mode, metadata
         FROM scene_conditions WHERE scene_id = $1`,
        [scene.id]
      );

      const conditions = conditionsResult.rows;
      if (conditions.length === 0) continue;

      const results = await Promise.all(
        conditions.map((c: Record<string, unknown>) => this.evaluateCondition(c, triggerData))
      );

      const conditionsMet =
        scene.condition_logic === 'all' ? results.every((r: boolean) => r) : results.some((r: boolean) => r);

      if (conditionsMet) {
        logger.infoWithEmoji(
          '🤖',
          `Automation triggered: "${scene.name}" (id=${scene.id})`,
          'AUTOMATION'
        );
        this.lastTriggered.set(scene.id, Date.now());

        try {
          await executeScene(scene.id, userId);
        } catch (error) {
          logger.error(`Error executing automation scene ${scene.id}:`, error);
        }
      }
    }
  }

  /**
   * Evaluate a single condition against current data.
   */
  private async evaluateCondition(
    condition: Record<string, unknown>,
    triggerData: Record<string, unknown>
  ): Promise<boolean> {
    const type = condition.type as string;

    switch (type) {
      case 'temperature':
        return this.evaluateNumericCondition(
          condition,
          triggerData.temperature as number | undefined
        );

      case 'humidity':
        return this.evaluateHumidityCondition(condition, triggerData.humidity as number | undefined);

      case 'wind_speed':
        return this.evaluateNumericCondition(
          condition,
          triggerData.windSpeed as number | undefined
        );

      case 'device_status':
        return this.evaluateDeviceStatusCondition(condition, triggerData);

      case 'weather':
        return this.evaluateWeatherCondition(condition);

      case 'sunrise_sunset':
        // Evaluated by scheduler, not by property updates
        return false;

      case 'schedule_time':
        // Evaluated by scheduler, not by property updates
        return false;

      case 'arm_mode':
        return this.evaluateArmModeCondition(condition);

      case 'location':
      case 'location_arrive_at':
      case 'location_leave':
        return this.evaluateLocationCondition(condition);

      default:
        logger.warnWithEmoji(
          '⚠️',
          `Unknown condition type: ${type}`,
          'AUTOMATION'
        );
        return false;
    }
  }

  private evaluateNumericCondition(
    condition: Record<string, unknown>,
    currentValue: number | undefined
  ): boolean {
    if (currentValue === undefined || currentValue === null) return false;

    const threshold = condition.value as number;
    const operator = condition.operator as string;

    switch (operator) {
      case '<':
        return currentValue < threshold;
      case '<=':
        return currentValue <= threshold;
      case '>':
        return currentValue > threshold;
      case '>=':
        return currentValue >= threshold;
      case '=':
      case '==':
        return currentValue === threshold;
      default:
        return false;
    }
  }

  private evaluateHumidityCondition(
    condition: Record<string, unknown>,
    currentHumidity: number | undefined
  ): boolean {
    if (currentHumidity === undefined || currentHumidity === null) return false;

    const operator = condition.operator as string;

    // Humidity can be evaluated as numeric or category
    if (['<', '<=', '>', '>=', '=', '=='].includes(operator)) {
      return this.evaluateNumericCondition(condition, currentHumidity);
    }

    // Category-based: dry (<40), comfortable (40-70), moist (>70)
    switch (operator) {
      case 'dry':
        return currentHumidity < 40;
      case 'comfortable':
        return currentHumidity >= 40 && currentHumidity <= 70;
      case 'moist':
        return currentHumidity > 70;
      default:
        return this.evaluateNumericCondition(condition, currentHumidity);
    }
  }

  private evaluateDeviceStatusCondition(
    condition: Record<string, unknown>,
    triggerData: Record<string, unknown>
  ): boolean {
    const expectedStatus = condition.device_status as string;
    const conditionDeviceId = condition.device_id as number | undefined;
    const triggerDeviceId = triggerData.deviceId as number | undefined;
    const triggerStatus = triggerData.status as string | undefined;

    // Only match if it's the specific device referenced in the condition
    if (conditionDeviceId && triggerDeviceId && conditionDeviceId !== triggerDeviceId) {
      return false;
    }

    if (!triggerStatus || !expectedStatus) return false;
    return triggerStatus === expectedStatus;
  }

  private async evaluateWeatherCondition(
    condition: Record<string, unknown>
  ): Promise<boolean> {
    // Weather conditions require external API integration.
    // For now, check if weather data was recently fetched and stored.
    try {
      const metadata = condition.metadata as Record<string, unknown> | undefined;
      const location = (condition.location as string) || 'default';

      // Check cached weather from a simple key-value store in DB
      const result = await this.pool.query(
        `SELECT metadata->>'lastWeather' as weather
         FROM users LIMIT 1`
      );

      if (result.rowCount === 0) return false;

      const currentWeather = result.rows[0].weather;
      const expectedWeather = condition.operator as string;

      if (!currentWeather || !expectedWeather) return false;
      return currentWeather.toLowerCase() === expectedWeather.toLowerCase();
    } catch {
      return false;
    }
  }

  private async evaluateArmModeCondition(
    condition: Record<string, unknown>
  ): Promise<boolean> {
    try {
      const expectedMode = condition.arm_mode as string;
      if (!expectedMode) return false;

      // Check current arm mode from home settings
      const homeId = condition.metadata
        ? (condition.metadata as Record<string, unknown>).homeId
        : null;

      let query = `SELECT metadata->>'armMode' as arm_mode FROM homes`;
      const params: unknown[] = [];

      if (homeId) {
        query += ' WHERE id = $1';
        params.push(homeId);
      }
      query += ' LIMIT 1';

      const result = await this.pool.query(query, params);
      if (result.rowCount === 0) return false;

      return result.rows[0].arm_mode === expectedMode;
    } catch {
      return false;
    }
  }

  private async evaluateLocationCondition(
    _condition: Record<string, unknown>
  ): Promise<boolean> {
    // Location conditions are evaluated when the user posts their location
    // via POST /api/v1/users/location — that endpoint triggers evaluation.
    // Returning false here since this is called from MQTT property updates.
    return false;
  }

  /**
   * Evaluate schedule_time conditions — called by SceneScheduler.
   */
  async evaluateScheduleConditions(): Promise<void> {
    try {
      const now = new Date();
      const currentTime = `${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`;
      const dayOfWeek = now.getDay(); // 0=Sunday, 6=Saturday
      const isWeekday = dayOfWeek >= 1 && dayOfWeek <= 5;
      const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
      const dayNames = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
      const currentDayName = dayNames[dayOfWeek];

      // Find all enabled automations with schedule_time conditions
      const scenesResult = await this.pool.query(
        `SELECT DISTINCT s.id, s.user_id, s.name, s.condition_logic, s.home_id
         FROM smart_scenes s
         JOIN scene_conditions c ON c.scene_id = s.id
         WHERE s.type = 'automation'
           AND s.is_enabled = true
           AND c.type = 'schedule_time'`
      );

      for (const scene of scenesResult.rows) {
        // Debounce
        const lastTrigger = this.lastTriggered.get(scene.id) || 0;
        if (Date.now() - lastTrigger < this.DEBOUNCE_MS) continue;

        const conditionsResult = await this.pool.query(
          `SELECT type, operator, value, unit, location, device_id, device_status, arm_mode, metadata
           FROM scene_conditions WHERE scene_id = $1`,
          [scene.id]
        );

        const conditions = conditionsResult.rows;
        const results = conditions.map((c: Record<string, unknown>) => {
          if (c.type === 'schedule_time') {
            return this.evaluateScheduleTimeCondition(c, currentTime, isWeekday, isWeekend, currentDayName);
          }
          // Non-schedule conditions should use their latest known data
          // For simplicity, skip non-schedule conditions in schedule evaluation
          return true;
        });

        const conditionsMet =
          scene.condition_logic === 'all'
            ? results.every((r: boolean) => r)
            : results.some((r: boolean) => r);

        if (conditionsMet) {
          logger.infoWithEmoji(
            '⏰',
            `Scheduled automation triggered: "${scene.name}" (id=${scene.id})`,
            'SCHEDULER'
          );
          this.lastTriggered.set(scene.id, Date.now());

          try {
            await executeScene(scene.id, scene.user_id);
          } catch (error) {
            logger.error(`Error executing scheduled scene ${scene.id}:`, error);
          }
        }
      }
    } catch (error) {
      logger.error('Error evaluating schedule conditions:', error);
    }
  }

  private evaluateScheduleTimeCondition(
    condition: Record<string, unknown>,
    currentTime: string,
    isWeekday: boolean,
    isWeekend: boolean,
    currentDayName: string
  ): boolean {
    const scheduleTime = String(condition.value || '');
    const operator = (condition.operator as string) || 'every_day';

    // Check if time matches (HHMM format)
    if (scheduleTime !== currentTime) return false;

    // Check day filter
    switch (operator) {
      case 'every_day':
        return true;
      case 'weekdays':
        return isWeekday;
      case 'weekends':
        return isWeekend;
      default:
        // Check specific day name
        return operator.toLowerCase() === currentDayName;
    }
  }
}
