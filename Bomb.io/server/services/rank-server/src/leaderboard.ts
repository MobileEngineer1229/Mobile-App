import { MatchHistory } from '../../../db/mongo/models';
import type { IPlayerResult } from '../../../db/mongo/models';

const LEADERBOARD_KEY = 'leaderboard:elo';
const K_FACTOR        = 32;
const DEFAULT_ELO     = 1000;

export interface PlayerStats {
  playerId:    string;
  elo:         number;
  wins:        number;
  losses:      number;
  kills:       number;
  gamesPlayed: number;
}

export class Leaderboard {
  constructor(private redis: any) {}

  async getStats(playerId: string): Promise<PlayerStats> {
    const raw = await this.redis.hgetall(`stats:${playerId}`);
    if (!raw?.playerId) {
      return { playerId, elo: DEFAULT_ELO, wins: 0, losses: 0, kills: 0, gamesPlayed: 0 };
    }
    return {
      playerId:    raw.playerId,
      elo:         Number(raw.elo         ?? DEFAULT_ELO),
      wins:        Number(raw.wins        ?? 0),
      losses:      Number(raw.losses      ?? 0),
      kills:       Number(raw.kills       ?? 0),
      gamesPlayed: Number(raw.gamesPlayed ?? 0),
    };
  }

  async recordResult(
    winnerId: string,
    results: IPlayerResult[],
  ): Promise<void> {
    const winnerStats = await this.getStats(winnerId);
    winnerStats.wins++;
    winnerStats.gamesPlayed++;

    for (const r of results) {
      const pid = String(r.playerId);
      if (pid === winnerId) {
        winnerStats.kills += r.kills;
        continue;
      }
      const loserStats = await this.getStats(pid);
      const [newWElo, newLElo] = this.calcElo(winnerStats.elo, loserStats.elo);
      winnerStats.elo = newWElo;
      loserStats.elo  = newLElo;
      loserStats.losses++;
      loserStats.gamesPlayed++;
      loserStats.kills += r.kills;
      await this.saveStats(loserStats);
    }

    await this.saveStats(winnerStats);
  }

  async getTopPlayers(count = 50): Promise<{ playerId: string; elo: number; rank: number }[]> {
    const entries = await this.redis.zrevrange(LEADERBOARD_KEY, 0, count - 1, 'WITHSCORES');
    const result: { playerId: string; elo: number; rank: number }[] = [];
    for (let i = 0; i < entries.length; i += 2) {
      result.push({ playerId: entries[i], elo: Number(entries[i + 1]), rank: result.length + 1 });
    }
    return result;
  }

  async getRank(playerId: string): Promise<number | null> {
    const rank = await this.redis.zrevrank(LEADERBOARD_KEY, playerId);
    return rank !== null ? rank + 1 : null;
  }

  private calcElo(winnerElo: number, loserElo: number): [number, number] {
    const expected = 1 / (1 + Math.pow(10, (loserElo - winnerElo) / 400));
    return [
      Math.round(winnerElo + K_FACTOR * (1 - expected)),
      Math.round(loserElo  + K_FACTOR * (0 - (1 - expected))),
    ];
  }

  private async saveStats(s: PlayerStats): Promise<void> {
    await this.redis.hset(`stats:${s.playerId}`, {
      playerId:    s.playerId,
      elo:         s.elo,
      wins:        s.wins,
      losses:      s.losses,
      kills:       s.kills,
      gamesPlayed: s.gamesPlayed,
    });
    await this.redis.zadd(LEADERBOARD_KEY, s.elo, s.playerId);
  }
}
