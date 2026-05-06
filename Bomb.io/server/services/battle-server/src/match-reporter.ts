import type { Player, PlayerId } from '../../../shared/types';

interface MatchResultEvent {
  roomId:         string;
  mapId:          string;
  winnerId:       string | null;
  winnerUsername: string | null;
  startedAt:      number;
  endedAt:        number;
  totalFrames:    number;
  players: Array<{
    playerId:      string;
    username:      string;
    characterId:   string;
    characterName: string;
    placement:     number;
    kills:         number;
    deaths:        number;
    bombsPlaced:   number;
    damageDealt:   number;
    survivalTime:  number;
  }>;
}

export class MatchReporter {
  constructor(private redis: any) {}

  async publish(
    roomId: string,
    mapId: string,
    players: Map<PlayerId, Player & { kills: number; deaths: number; damageDealt: number; eliminatedAtFrame: number }>,
    winnerId: PlayerId | null,
    startedAt: number,
    totalFrames: number,
  ): Promise<void> {
    const endedAt = Date.now();

    // Sort by survival — winner first (placement 1), then by last-eliminated
    const ranked = [...players.values()].sort((a, b) => {
      if (a.id === winnerId) return -1;
      if (b.id === winnerId) return  1;
      return b.eliminatedAtFrame - a.eliminatedAtFrame;
    });

    const event: MatchResultEvent = {
      roomId,
      mapId,
      winnerId:       winnerId !== null ? String(winnerId) : null,
      winnerUsername: winnerId !== null ? (players.get(winnerId)?.username ?? winnerId) : null,
      startedAt,
      endedAt,
      totalFrames,
      players: ranked.map((p, idx) => {
        const char = p.characters.find(c => c.characterId === p.activeCharacterId);
        return {
          playerId:      String(p.id),
          username:      p.username,
          characterId:   String(p.activeCharacterId),
          characterName: char?.name ?? 'Unknown',
          placement:     idx + 1,
          kills:         p.kills,
          deaths:        p.deaths,
          bombsPlaced:   p.bombsPlaced,
          damageDealt:   p.damageDealt,
          survivalTime:  Math.round((endedAt - startedAt) / 1000),
        };
      }),
    };

    await this.redis.publish('match:result', JSON.stringify(event));
    console.log(`[Battle] Published match result for room ${roomId}`);
  }
}
