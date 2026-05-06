import type { Vector2 } from './TerrainGrid';
import type { PlayerId } from './Player';

export interface Bomb {
  id: number;
  ownerPlayerId: PlayerId;
  x: number;
  y: number;
  fuseTicks: number;
  blastRadius: number;
  isRemote: boolean;
}

export interface ExplosionResult {
  bombId: number;
  affectedCells: Vector2[];
  destroyedBlocks: Vector2[];
  hitPlayers: PlayerId[];
  triggeredBombIds: number[];
}
