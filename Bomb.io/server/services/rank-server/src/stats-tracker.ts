import { MatchHistory } from '../../../db/mongo/models';
import type { Leaderboard } from './leaderboard';

interface MatchResultEvent {
  roomId:      string;
  mapId:       string;
  mapName?:    string;
  winnerId:    string;
  winnerUsername: string;
  startedAt:   number; // unix ms
  endedAt:     number;
  totalFrames: number;
  players: Array<{
    playerId:     string;
    username:     string;
    characterId:  string;
    characterName:string;
    placement:    number;
    kills:        number;
    deaths:       number;
    bombsPlaced:  number;
    damageDealt:  number;
    survivalTime: number;
  }>;
}

export class StatsTracker {
  constructor(private redis: any, private leaderboard: Leaderboard) {
    this.listenForResults();
  }

  private listenForResults(): void {
    const sub = this.redis.duplicate();
    sub.subscribe('match:result', (err: Error | null) => {
      if (err) console.error('[Rank] Subscribe error:', err);
      else console.log('[Rank] Subscribed to match:result');
    });

    sub.on('message', async (_channel: string, message: string) => {
      try {
        const event: MatchResultEvent = JSON.parse(message);
        await this.processResult(event);
      } catch (err) {
        console.error('[Rank] Failed to process result:', err);
      }
    });
  }

  private async processResult(event: MatchResultEvent): Promise<void> {
    const startedAt  = new Date(event.startedAt);
    const endedAt    = new Date(event.endedAt);
    const durationSec = (event.endedAt - event.startedAt) / 1000;

    // Persist to MongoDB — full match record
    const match = await MatchHistory.create({
      roomId:          event.roomId,
      mapId:           event.mapId,
      mapName:         event.mapName,
      winnerId:        event.winnerId,
      winnerUsername:  event.winnerUsername,
      startedAt,
      endedAt,
      durationSec,
      totalFrames:     event.totalFrames,
      totalBombsPlaced: event.players.reduce((s, p) => s + p.bombsPlaced, 0),
      players: event.players.map(p => ({
        playerId:     p.playerId,
        username:     p.username,
        characterId:  p.characterId,
        characterName:p.characterName,
        placement:    p.placement,
        kills:        p.kills,
        deaths:       p.deaths,
        bombsPlaced:  p.bombsPlaced,
        damageDealt:  p.damageDealt,
        survivalTime: p.survivalTime,
      })),
    });

    // Update Redis ELO + counters
    await this.leaderboard.recordResult(event.winnerId, match.players as any);

    console.log(`[Rank] Match ${event.roomId} persisted — winner: ${event.winnerUsername}`);
  }

  async getHistory(playerId: string, limit = 20): Promise<any[]> {
    return MatchHistory
      .find({ 'players.playerId': playerId })
      .sort({ endedAt: -1 })
      .limit(limit)
      .lean();
  }
}
