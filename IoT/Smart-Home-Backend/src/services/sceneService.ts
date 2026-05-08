import { getPool } from '../config/database';
import logger from '../utils/logger';
import { DeviceControlService } from './device-control-service';
import { DeviceRepository } from '../repositories/device-repository';

export interface SceneCondition {
    id?: number;
    type: string;
    operator?: string;
    value?: number;
    unit?: string;
    location?: string;
    deviceId?: number;
    deviceStatus?: string;
    armMode?: string;
    metadata?: any;
}

export interface SceneTask {
    id?: number;
    type: string;
    deviceId?: number;
    deviceName?: string;
    roomName?: string;
    function?: string;
    sceneIdTarget?: number;
    sceneName?: string;
    armMode?: string;
    notificationMessage?: string;
    delaySeconds?: number;
    orderIndex?: number;
    metadata?: any;
}

export interface CreateSceneData {
    userId: number;
    homeId?: number;
    name: string;
    type: string;
    conditionLogic: string;
    conditions: SceneCondition[];
    tasks: SceneTask[];
    icon?: string;
    color?: string;
    isEnabled: boolean;
}

export interface UpdateSceneData {
    name?: string;
    conditionLogic?: string;
    conditions?: SceneCondition[];
    tasks?: SceneTask[];
    icon?: string;
    color?: string;
    isEnabled?: boolean;
}

export const getScenesByUserId = async (userId: number, type?: string): Promise<any[]> => {
    try {
        let query = `
            SELECT s.*,
                   COALESCE(
                       (SELECT json_agg(
                           json_build_object(
                               'id', c.id,
                               'type', c.type,
                               'operator', c.operator,
                               'value', c.value,
                               'unit', c.unit,
                               'location', c.location,
                               'deviceId', c.device_id,
                               'deviceStatus', c.device_status,
                               'armMode', c.arm_mode,
                               'metadata', c.metadata
                           )
                       )
                       FROM scene_conditions c
                       WHERE c.scene_id = s.id),
                       '[]'::json
                   ) as conditions,
                   COALESCE(
                       (SELECT json_agg(
                           json_build_object(
                               'id', t.id,
                               'type', t.type,
                               'deviceId', t.device_id,
                               'deviceName', t.device_name,
                               'roomName', t.room_name,
                               'function', t.function,
                               'sceneId', t.scene_id_target,
                               'sceneName', t.scene_name,
                               'armMode', t.arm_mode,
                               'notificationMessage', t.notification_message,
                               'delaySeconds', t.delay_seconds,
                               'orderIndex', t.order_index,
                               'metadata', t.metadata
                           )
                           ORDER BY t.order_index
                       )
                       FROM scene_tasks t
                       WHERE t.scene_id = s.id),
                       '[]'::json
                   ) as tasks
            FROM smart_scenes s
            WHERE s.user_id = $1
        `;
        
        const params: any[] = [userId];
        
        if (type) {
            query += ' AND s.type = $2';
            params.push(type);
        }
        
        query += ' ORDER BY COALESCE(s.order_index, 0) ASC, s.created_at DESC';
        
        const result = await getPool().query(query, params);
        return result.rows;
    } catch (error) {
        logger.error('Error fetching scenes:', error);
        throw error;
    }
};

export const getSceneById = async (sceneId: number, userId: number): Promise<any | null> => {
    try {
        const query = `
            SELECT s.*,
                   COALESCE(
                       (SELECT json_agg(
                           json_build_object(
                               'id', c.id,
                               'type', c.type,
                               'operator', c.operator,
                               'value', c.value,
                               'unit', c.unit,
                               'location', c.location,
                               'deviceId', c.device_id,
                               'deviceStatus', c.device_status,
                               'armMode', c.arm_mode,
                               'metadata', c.metadata
                           )
                       )
                       FROM scene_conditions c
                       WHERE c.scene_id = s.id),
                       '[]'::json
                   ) as conditions,
                   COALESCE(
                       (SELECT json_agg(
                           json_build_object(
                               'id', t.id,
                               'type', t.type,
                               'deviceId', t.device_id,
                               'deviceName', t.device_name,
                               'roomName', t.room_name,
                               'function', t.function,
                               'sceneId', t.scene_id_target,
                               'sceneName', t.scene_name,
                               'armMode', t.arm_mode,
                               'notificationMessage', t.notification_message,
                               'delaySeconds', t.delay_seconds,
                               'orderIndex', t.order_index,
                               'metadata', t.metadata
                           )
                           ORDER BY t.order_index
                       )
                       FROM scene_tasks t
                       WHERE t.scene_id = s.id),
                       '[]'::json
                   ) as tasks
            FROM smart_scenes s
            WHERE s.id = $1 AND s.user_id = $2
        `;
        
        const result = await getPool().query(query, [sceneId, userId]);
        return result.rows[0] || null;
    } catch (error) {
        logger.error('Error fetching scene:', error);
        throw error;
    }
};

export const createScene = async (data: CreateSceneData): Promise<any> => {
    const client = await getPool().connect();
    try {
        await client.query('BEGIN');
        
        // Insert scene
        const sceneResult = await client.query(
            `INSERT INTO smart_scenes (user_id, home_id, name, type, condition_logic, icon, color, is_enabled)
             VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
             RETURNING *`,
            [data.userId, data.homeId || null, data.name, data.type, data.conditionLogic, data.icon || null, data.color || null, data.isEnabled]
        );
        
        const scene = sceneResult.rows[0];
        const sceneId = scene.id;
        
        // Insert conditions
        for (let i = 0; i < data.conditions.length; i++) {
            const condition = data.conditions[i];
            
            // For schedule_time conditions, store hour/minute in metadata
            let metadata = condition.metadata;
            if (condition.type === 'schedule_time' && condition.value != null) {
                // value is stored as HHMM (e.g., 2145 for 21:45)
                const timeValue = condition.value;
                const hour = Math.floor(timeValue / 100);
                const minute = timeValue % 100;
                metadata = {
                    hour: hour,
                    minute: minute,
                    repeat: condition.operator || 'every_day'
                };
            }
            
            // For location conditions, store coordinates in metadata if available
            if ((condition.type === 'location_arrive_at' || condition.type === 'location_leave') && condition.location) {
                if (!metadata) metadata = {};
                metadata.address = condition.location;
                // If metadata contains lat/lng, preserve them
            }
            
            await client.query(
                `INSERT INTO scene_conditions (scene_id, type, operator, value, unit, location, device_id, device_status, arm_mode, metadata)
                 VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
                [
                    sceneId,
                    condition.type,
                    condition.operator || null,
                    condition.value || null,
                    condition.unit || null,
                    condition.location || null,
                    condition.deviceId || null,
                    condition.deviceStatus || null,
                    condition.armMode || null,
                    metadata ? JSON.stringify(metadata) : null
                ]
            );
        }
        
        // Insert tasks
        for (let i = 0; i < data.tasks.length; i++) {
            const task = data.tasks[i];
            await client.query(
                `INSERT INTO scene_tasks (scene_id, type, device_id, device_name, room_name, function, scene_id_target, scene_name, arm_mode, notification_message, delay_seconds, order_index, metadata)
                 VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)`,
                [
                    sceneId,
                    task.type,
                    task.deviceId || null,
                    task.deviceName || null,
                    task.roomName || null,
                    task.function || null,
                    task.sceneIdTarget || null,
                    task.sceneName || null,
                    task.armMode || null,
                    task.notificationMessage || null,
                    task.delaySeconds || null,
                    task.orderIndex !== undefined ? task.orderIndex : i,
                    task.metadata ? JSON.stringify(task.metadata) : null
                ]
            );
        }
        
        await client.query('COMMIT');
        
        // Fetch and return the complete scene
        return await getSceneById(sceneId, data.userId);
    } catch (error) {
        await client.query('ROLLBACK');
        logger.error('Error creating scene:', error);
        throw error;
    } finally {
        client.release();
    }
};

export const updateScene = async (sceneId: number, userId: number, data: UpdateSceneData): Promise<any | null> => {
    const client = await getPool().connect();
    try {
        await client.query('BEGIN');
        
        // Check if scene exists and belongs to user
        const sceneCheck = await client.query(
            'SELECT id FROM smart_scenes WHERE id = $1 AND user_id = $2',
            [sceneId, userId]
        );
        
        if (sceneCheck.rows.length === 0) {
            await client.query('ROLLBACK');
            return null;
        }
        
        // Update scene fields
        const updates: string[] = [];
        const values: any[] = [];
        let paramIndex = 1;
        
        if (data.name !== undefined) {
            updates.push(`name = $${paramIndex++}`);
            values.push(data.name);
        }
        if (data.conditionLogic !== undefined) {
            updates.push(`condition_logic = $${paramIndex++}`);
            values.push(data.conditionLogic);
        }
        if (data.isEnabled !== undefined) {
            updates.push(`is_enabled = $${paramIndex++}`);
            values.push(data.isEnabled);
        }
        if (data.icon !== undefined) {
            updates.push(`icon = $${paramIndex++}`);
            values.push(data.icon);
        }
        if (data.color !== undefined) {
            updates.push(`color = $${paramIndex++}`);
            values.push(data.color);
        }
        
        if (updates.length > 0) {
            values.push(sceneId, userId);
            await client.query(
                `UPDATE smart_scenes SET ${updates.join(', ')} WHERE id = $${paramIndex++} AND user_id = $${paramIndex++}`,
                values
            );
        }
        
        // Update conditions if provided
        if (data.conditions !== undefined) {
            // Delete existing conditions
            await client.query('DELETE FROM scene_conditions WHERE scene_id = $1', [sceneId]);
            
            // Insert new conditions
            for (const condition of data.conditions) {
                // For schedule_time conditions, store hour/minute in metadata
                let metadata = condition.metadata;
                if (condition.type === 'schedule_time' && condition.value != null) {
                    const timeValue = condition.value;
                    const hour = Math.floor(timeValue / 100);
                    const minute = timeValue % 100;
                    metadata = {
                        hour: hour,
                        minute: minute,
                        repeat: condition.operator || 'every_day'
                    };
                }
                
                // For location conditions, store coordinates in metadata if available
                if ((condition.type === 'location_arrive_at' || condition.type === 'location_leave') && condition.location) {
                    if (!metadata) metadata = {};
                    metadata.address = condition.location;
                }
                
                await client.query(
                    `INSERT INTO scene_conditions (scene_id, type, operator, value, unit, location, device_id, device_status, arm_mode, metadata)
                     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
                    [
                        sceneId,
                        condition.type,
                        condition.operator || null,
                        condition.value || null,
                        condition.unit || null,
                        condition.location || null,
                        condition.deviceId || null,
                        condition.deviceStatus || null,
                        condition.armMode || null,
                        metadata ? JSON.stringify(metadata) : null
                    ]
                );
            }
        }
        
        // Update tasks if provided
        if (data.tasks !== undefined) {
            // Delete existing tasks
            await client.query('DELETE FROM scene_tasks WHERE scene_id = $1', [sceneId]);
            
            // Insert new tasks
            for (let i = 0; i < data.tasks.length; i++) {
                const task = data.tasks[i];
                await client.query(
                    `INSERT INTO scene_tasks (scene_id, type, device_id, device_name, room_name, function, scene_id_target, scene_name, arm_mode, notification_message, delay_seconds, order_index, metadata)
                     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)`,
                    [
                        sceneId,
                        task.type,
                        task.deviceId || null,
                        task.deviceName || null,
                        task.roomName || null,
                        task.function || null,
                        task.sceneIdTarget || null,
                        task.sceneName || null,
                        task.armMode || null,
                        task.notificationMessage || null,
                        task.delaySeconds || null,
                        task.orderIndex !== undefined ? task.orderIndex : i,
                        task.metadata ? JSON.stringify(task.metadata) : null
                    ]
                );
            }
        }
        
        await client.query('COMMIT');
        
        // Fetch and return the updated scene
        return await getSceneById(sceneId, userId);
    } catch (error) {
        await client.query('ROLLBACK');
        logger.error('Error updating scene:', error);
        throw error;
    } finally {
        client.release();
    }
};

export const deleteScene = async (sceneId: number, userId: number): Promise<boolean> => {
    try {
        const result = await getPool().query(
            'DELETE FROM smart_scenes WHERE id = $1 AND user_id = $2 RETURNING id',
            [sceneId, userId]
        );
        return result.rows.length > 0;
    } catch (error) {
        logger.error('Error deleting scene:', error);
        throw error;
    }
};

export const toggleScene = async (sceneId: number, userId: number): Promise<any | null> => {
    try {
        const result = await getPool().query(
            `UPDATE smart_scenes 
             SET is_enabled = NOT is_enabled 
             WHERE id = $1 AND user_id = $2 
             RETURNING *`,
            [sceneId, userId]
        );
        
        if (result.rows.length === 0) {
            return null;
        }
        
        return await getSceneById(sceneId, userId);
    } catch (error) {
        logger.error('Error toggling scene:', error);
        throw error;
    }
};

export const executeScene = async (sceneId: number, userId: number): Promise<any | null> => {
    try {
        // Get scene and verify ownership
        const scene = await getSceneById(sceneId, userId);
        if (!scene || !scene.is_enabled) {
            return null;
        }
        
        // Execute tasks in order
        const tasks = scene.tasks || [];
        const executionResults = [];
        
        for (const task of tasks) {
            try {
                // Handle delay tasks
                if (task.type === 'delay' && task.delaySeconds) {
                    await new Promise(resolve => setTimeout(resolve, task.delaySeconds * 1000));
                    executionResults.push({ taskId: task.id, type: task.type, status: 'completed' });
                    continue;
                }
                
                // Handle device control tasks
                if (task.type === 'control_device' && task.deviceId) {
                    const pool = getPool();
                    const deviceRepo = new DeviceRepository(pool);
                    const deviceControlService = new DeviceControlService(deviceRepo);

                    const command = task.function || 'power';
                    const params = task.metadata || {};
                    const result = await deviceControlService.executeCommand(
                        task.deviceId,
                        userId,
                        command,
                        params
                    );

                    logger.info(`Executed task: Control device ${task.deviceId} with function ${task.function}`, result);
                    executionResults.push({
                        taskId: task.id,
                        type: task.type,
                        deviceId: task.deviceId,
                        function: task.function,
                        status: result.success ? 'completed' : 'failed'
                    });
                }
                
                // Handle select_scene tasks (execute another scene)
                if (task.type === 'select_scene' && task.sceneIdTarget) {
                    logger.info(`Executing task: Select scene ${task.sceneIdTarget} (${task.sceneName})`);
                    // Recursively execute the target scene
                    const targetSceneResult = await executeScene(task.sceneIdTarget, userId);
                    executionResults.push({ 
                        taskId: task.id, 
                        type: task.type, 
                        sceneIdTarget: task.sceneIdTarget,
                        sceneName: task.sceneName,
                        targetSceneResult: targetSceneResult,
                        status: targetSceneResult ? 'completed' : 'failed' 
                    });
                }
                
                // Handle change_arm_mode tasks
                if (task.type === 'change_arm_mode') {
                    logger.info(`Executing task: Change arm mode to ${task.armMode}`);
                    // TODO: Implement actual arm mode change
                    executionResults.push({ 
                        taskId: task.id, 
                        type: task.type, 
                        armMode: task.armMode,
                        status: 'completed' 
                    });
                }
                
                // Handle send_notification tasks
                if (task.type === 'send_notification') {
                    logger.info(`Executing task: Send notification - ${task.notificationMessage}`);
                    // TODO: Implement actual notification sending
                    executionResults.push({ 
                        taskId: task.id, 
                        type: task.type, 
                        notificationMessage: task.notificationMessage,
                        status: 'completed' 
                    });
                }
                
            } catch (error) {
                logger.error(`Error executing task ${task.id}:`, error);
                executionResults.push({ taskId: task.id, type: task.type, status: 'failed', error: error instanceof Error ? error.message : String(error) });
            }
        }
        
        const hasFailures = executionResults.some((r: any) => r.status === 'failed');
        const executionStatus = hasFailures ? 'failed' : 'succeeded';
        const errorMessage = hasFailures ? 
            executionResults.find((r: any) => r.status === 'failed')?.error : undefined;
        
        // Log execution
        await logSceneExecution(
            scene.id,
            scene.name,
            userId,
            scene.home_id,
            executionStatus,
            errorMessage
        );
        
        return {
            sceneId: scene.id,
            sceneName: scene.name,
            executedAt: new Date().toISOString(),
            results: executionResults,
            status: executionStatus
        };
    } catch (error) {
        logger.error('Error executing scene:', error);
        // Try to log the failure
        try {
            const scene = await getSceneById(sceneId, userId);
            if (scene) {
                await logSceneExecution(
                    sceneId,
                    scene.name,
                    userId,
                    scene.home_id,
                    'failed',
                    error instanceof Error ? error.message : String(error)
                );
            }
        } catch (logError) {
            logger.error('Error logging scene execution failure:', logError);
        }
        throw error;
    }
};

// Log scene execution
export const logSceneExecution = async (
    sceneId: number,
    sceneName: string,
    userId: number,
    homeId: number | null,
    status: 'succeeded' | 'failed',
    errorMessage?: string
): Promise<void> => {
    try {
        await getPool().query(
            `INSERT INTO scene_execution_logs (scene_id, scene_name, user_id, home_id, status, error_message)
             VALUES ($1, $2, $3, $4, $5, $6)`,
            [sceneId, sceneName, userId, homeId, status, errorMessage || null]
        );
    } catch (error) {
        logger.error('Error logging scene execution:', error);
        // Don't throw - logging failure shouldn't break execution
    }
};

// Get scene execution logs
export const getSceneLogs = async (
    userId: number,
    limit: number = 50,
    offset: number = 0,
    sceneId?: number
): Promise<any[]> => {
    try {
        let query = `
            SELECT id, scene_id, scene_name, status, error_message, execution_timestamp, metadata
            FROM scene_execution_logs
            WHERE user_id = $1
        `;
        const params: any[] = [userId];
        
        if (sceneId) {
            query += ' AND scene_id = $2';
            params.push(sceneId);
        }
        
        query += ' ORDER BY execution_timestamp DESC LIMIT $' + (params.length + 1) + ' OFFSET $' + (params.length + 2);
        params.push(limit, offset);
        
        const result = await getPool().query(query, params);
        return result.rows;
    } catch (error) {
        logger.error('Error fetching scene logs:', error);
        throw error;
    }
};

// Reorder scenes
export const reorderScenes = async (
    userId: number,
    sceneIds: number[],
    type?: string
): Promise<boolean> => {
    const client = await getPool().connect();
    try {
        await client.query('BEGIN');
        
        // Update order_index for each scene
        for (let i = 0; i < sceneIds.length; i++) {
            const sceneId = sceneIds[i];
            let query = `
                UPDATE smart_scenes 
                SET order_index = $1 
                WHERE id = $2 AND user_id = $3
            `;
            const params: any[] = [i, sceneId, userId];
            
            if (type) {
                query += ' AND type = $4';
                params.push(type);
            }
            
            await client.query(query, params);
        }
        
        await client.query('COMMIT');
        return true;
    } catch (error) {
        await client.query('ROLLBACK');
        logger.error('Error reordering scenes:', error);
        throw error;
    } finally {
        client.release();
    }
};
