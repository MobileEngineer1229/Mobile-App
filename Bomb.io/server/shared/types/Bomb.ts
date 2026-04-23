import type { Vector2 } from './TerrainGrid';

export interface Bomb {
  id: number;
  ownerPlayerId: number;
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
  hitPlayers: number[];
  triggeredBombIds: number[];
}
