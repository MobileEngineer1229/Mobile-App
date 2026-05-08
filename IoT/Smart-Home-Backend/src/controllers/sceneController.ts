import { Request, Response } from 'express';
import * as sceneService from '../services/sceneService';
import logger from '../utils/logger';

export const getScenes = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const type = req.query.type as string | undefined; // 'automation' or 'tap_to_run'
        const scenes = await sceneService.getScenesByUserId(userId, type);
        res.json({ scenes });
    } catch (error) {
        logger.error('Error fetching scenes:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const getSceneById = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        const sceneId = parseInt(req.params.id);

        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const scene = await sceneService.getSceneById(sceneId, userId);
        if (!scene) {
            res.status(404).json({ error: 'Scene not found' });
            return;
        }

        res.json({ scene });
    } catch (error) {
        logger.error('Error fetching scene:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const createScene = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const { name, type, conditionLogic, conditions, tasks, homeId, icon, color } = req.body;

        if (!name || !type) {
            res.status(400).json({ error: 'Name and type are required' });
            return;
        }

        const scene = await sceneService.createScene({
            userId,
            homeId,
            name,
            type,
            conditionLogic: conditionLogic || 'any',
            conditions: conditions || [],
            tasks: tasks || [],
            icon: icon || null,
            color: color || null,
            isEnabled: true
        });

        res.status(201).json({ scene });
    } catch (error) {
        logger.error('Error creating scene:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const updateScene = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        const sceneId = parseInt(req.params.id);

        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const { name, conditionLogic, conditions, tasks, isEnabled, icon, color } = req.body;

        const scene = await sceneService.updateScene(sceneId, userId, {
            name,
            conditionLogic,
            conditions,
            tasks,
            isEnabled,
            icon,
            color
        });

        if (!scene) {
            res.status(404).json({ error: 'Scene not found' });
            return;
        }

        res.json({ scene });
    } catch (error) {
        logger.error('Error updating scene:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const deleteScene = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        const sceneId = parseInt(req.params.id);

        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const deleted = await sceneService.deleteScene(sceneId, userId);
        if (!deleted) {
            res.status(404).json({ error: 'Scene not found' });
            return;
        }

        res.json({ message: 'Scene deleted successfully' });
    } catch (error) {
        logger.error('Error deleting scene:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const toggleScene = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        const sceneId = parseInt(req.params.id);

        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const scene = await sceneService.toggleScene(sceneId, userId);
        if (!scene) {
            res.status(404).json({ error: 'Scene not found' });
            return;
        }

        res.json({ scene });
    } catch (error) {
        logger.error('Error toggling scene:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const executeScene = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        const sceneId = parseInt(req.params.id);

        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const result = await sceneService.executeScene(sceneId, userId);
        if (!result) {
            res.status(404).json({ error: 'Scene not found' });
            return;
        }

        res.json({ message: 'Scene executed successfully', result });
    } catch (error) {
        logger.error('Error executing scene:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const getSceneLogs = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const limit = parseInt(req.query.limit as string) || 50;
        const offset = parseInt(req.query.offset as string) || 0;
        const sceneId = req.query.sceneId ? parseInt(req.query.sceneId as string) : undefined;

        const logs = await sceneService.getSceneLogs(userId, limit, offset, sceneId);
        res.json({ logs });
    } catch (error) {
        logger.error('Error fetching scene logs:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const getSceneLogsById = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        const sceneId = parseInt(req.params.id);

        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        // Verify scene exists and belongs to user
        const scene = await sceneService.getSceneById(sceneId, userId);
        if (!scene) {
            res.status(404).json({ error: 'Scene not found' });
            return;
        }

        const limit = parseInt(req.query.limit as string) || 50;
        const offset = parseInt(req.query.offset as string) || 0;

        const logs = await sceneService.getSceneLogs(userId, limit, offset, sceneId);
        res.json({ logs });
    } catch (error) {
        logger.error('Error fetching scene logs:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};

export const reorderScenes = async (req: Request, res: Response): Promise<void> => {
    try {
        const userId = (req as any).user?.userId;
        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }

        const { sceneIds, type } = req.body;
        if (!Array.isArray(sceneIds) || sceneIds.length === 0) {
            res.status(400).json({ error: 'sceneIds array is required' });
            return;
        }

        const success = await sceneService.reorderScenes(userId, sceneIds, type);
        if (!success) {
            res.status(500).json({ error: 'Failed to reorder scenes' });
            return;
        }

        res.json({ message: 'Scenes reordered successfully' });
    } catch (error) {
        logger.error('Error reordering scenes:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
};
