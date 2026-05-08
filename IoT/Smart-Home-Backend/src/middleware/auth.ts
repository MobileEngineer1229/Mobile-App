import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { env } from '../config/env';
import { UnauthorizedError } from '../utils/errors';
import { UserRepository } from '../repositories/user-repository';
import { getPool } from '../config/database';
import logger from '../utils/logger';

/**
 * JWT payload interface
 */
interface JwtPayload {
  userId: number;
  email: string;
}

/**
 * Authentication middleware
 * Verifies JWT token and attaches user to request
 */
export const authenticate = async (
  req: Request,
  _res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      logger.auth.unauthorized('No token provided', {
        path: req.path,
        method: req.method,
      });
      throw new UnauthorizedError('No token provided');
    }

    const token = authHeader.substring(7);

    const decoded = jwt.verify(token, env.jwtSecret) as JwtPayload;
    logger.auth.token('verified', {
      userId: decoded.userId,
      email: decoded.email,
    });

    const userRepository = new UserRepository(getPool());
    const user = await userRepository.findById(decoded.userId);

    if (!user) {
      logger.auth.unauthorized('User not found', {
        userId: decoded.userId,
        path: req.path,
      });
      throw new UnauthorizedError('User not found');
    }

    req.user = user;
    next();
  } catch (error) {
    if (error instanceof jwt.JsonWebTokenError) {
      logger.auth.unauthorized('Invalid token', {
        error: error.message,
        path: req.path,
      });
      next(new UnauthorizedError('Invalid token'));
      return;
    }
    next(error);
  }
};

