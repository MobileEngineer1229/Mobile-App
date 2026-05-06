import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { createHash } from 'node:crypto';
import { Player } from '../../../db/mongo/models';
import type { IPlayer } from '../../../db/mongo/models';

const JWT_SECRET  = process.env.JWT_SECRET ?? 'dev-secret';
const SESSION_TTL = 60 * 60 * 24; // 24 h

export interface AuthPayload {
  playerId: string;
  username: string;
}

async function sha256(input: string): Promise<string> {
  return createHash('sha256').update(input).digest('hex').slice(0, 32);
}

export class AuthHandler {
  constructor(private redis: any) {}

  async register(
    username: string,
    password: string,
    email?: string,
  ): Promise<{ token: string; playerId: string; username: string } | { error: string }> {
    const resolvedEmail = email?.trim() || `${username.toLowerCase()}@bomb.io`;
    const existing = await Player.findOne({ $or: [{ username }, { email: resolvedEmail }] });
    if (existing) {
      return { error: existing.username === username ? 'Username already taken' : 'Email already registered' };
    }
    const passwordHash = await bcrypt.hash(password, 12);
    const player = await Player.create({ username, email: resolvedEmail, passwordHash });
    const token  = this.signToken({ playerId: String(player._id), username });
    await this.cacheSession(token, String(player._id));
    return { token, playerId: String(player._id), username };
  }

  async login(
    usernameOrEmail: string,
    password: string,
  ): Promise<{ token: string; playerId: string; username: string } | { error: string }> {
    const player = await Player.findOne({
      $or: [{ username: usernameOrEmail }, { email: usernameOrEmail }],
      isActive: true,
    });
    if (!player) return { error: 'Invalid credentials' };

    if (player.isBanned) {
      const expired = player.banExpiresAt && player.banExpiresAt < new Date();
      if (!expired) return { error: `Account banned: ${player.banReason ?? 'violation'}` };
      await Player.updateOne({ _id: player._id }, { isBanned: false, banReason: null, banExpiresAt: null });
    }

    const valid = await bcrypt.compare(password, player.passwordHash);
    if (!valid) return { error: 'Invalid credentials' };

    await Player.updateOne({ _id: player._id }, { lastLoginAt: new Date() });
    const token = this.signToken({ playerId: String(player._id), username: player.username });
    await this.cacheSession(token, String(player._id));
    return { token, playerId: String(player._id), username: player.username };
  }

  async validateToken(token: string): Promise<AuthPayload | null> {
    try {
      const payload = jwt.verify(token, JWT_SECRET) as AuthPayload;
      const key     = await sha256(token);
      const exists  = await this.redis.exists(`session:${key}`);
      return exists ? payload : null;
    } catch {
      return null;
    }
  }

  async logout(token: string): Promise<void> {
    await this.redis.del(`session:${await sha256(token)}`);
  }

  async getPlayer(playerId: string): Promise<IPlayer | null> {
    return Player.findById(playerId).lean() as Promise<IPlayer | null>;
  }

  private signToken(payload: AuthPayload): string {
    return jwt.sign(payload, JWT_SECRET, { expiresIn: '24h' });
  }

  private async cacheSession(token: string, playerId: string): Promise<void> {
    await this.redis.setex(`session:${await sha256(token)}`, SESSION_TTL, playerId);
  }
}
