import type { Bomb, ExplosionResult, PlayerId } from '../../../shared/types';
import type { TerrainSystem } from './terrain-system';
import type { CollisionGrid } from './collision-grid';

let bombIdCounter = 1;

export class BombSystem {
  private recentExplosions: ExplosionResult[] = [];

  constructor(
    private terrain: TerrainSystem,
    private collision: CollisionGrid,
  ) {}

  update(bombs: Map<number, Bomb>): ExplosionResult[] {
    const explosions: ExplosionResult[] = [];
    const toDetonate: number[] = [];

    bombs.forEach((bomb, id) => {
      bomb.fuseTicks--;
      if (bomb.fuseTicks <= 0) toDetonate.push(id);
    });

    const detonated = new Set<number>();
    const detonateChain = (id: number) => {
      if (detonated.has(id) || !bombs.has(id)) return;
      detonated.add(id);
      const bomb = bombs.get(id)!;
      const result = this.detonate(bomb, bombs);
      explosions.push(result);
      result.triggeredBombIds.forEach(detonateChain);
    };

    toDetonate.forEach(detonateChain);
    detonated.forEach(id => bombs.delete(id));

    this.recentExplosions = explosions;
    return explosions;
  }

  private detonate(bomb: Bomb, allBombs: Map<number, Bomb>): ExplosionResult {
    const affectedCells: { x: number; y: number }[] = [];
    const destroyedBlocks: { x: number; y: number }[] = [];
    const triggeredBombIds: number[] = [];

    const dirs = [[0,-1],[0,1],[-1,0],[1,0]];
    dirs.forEach(([dx, dy]) => {
      for (let i = 1; i <= bomb.blastRadius; i++) {
        const nx = bomb.x + dx * i;
        const ny = bomb.y + dy * i;

        allBombs.forEach((other, _id) => {
          if (other.id !== bomb.id && other.x === nx && other.y === ny) {
            triggeredBombIds.push(other.id);
          }
        });

        if (this.terrain.blocksExplosion(nx, ny)) {
          if (this.terrain.destroyTile(nx, ny)) {
            destroyedBlocks.push({ x: nx, y: ny });
            affectedCells.push({ x: nx, y: ny });
          }
          break;
        }

        affectedCells.push({ x: nx, y: ny });
      }
    });

    // Include bomb's own cell
    affectedCells.push({ x: bomb.x, y: bomb.y });

    const hitPlayers = [...this.collision.getPlayersInCells(affectedCells)];

    return { bombId: bomb.id, affectedCells, destroyedBlocks, hitPlayers, triggeredBombIds };
  }

  placeBomb(playerId: PlayerId, x: number, y: number, blastRadius: number, isRemote = false): Bomb {
    return {
      id: bombIdCounter++,
      ownerPlayerId: playerId,
      x: Math.round(x),
      y: Math.round(y),
      fuseTicks: 60, // 3 s at 20 Hz
      blastRadius,
      isRemote,
    };
  }

  triggerRemote(playerId: PlayerId, bombs: Map<number, Bomb>): void {
    bombs.forEach((bomb, id) => {
      if (bomb.ownerPlayerId === playerId && bomb.isRemote) {
        bomb.fuseTicks = 1;
      }
    });
  }

  getRecentExplosions(): ExplosionResult[] {
    return this.recentExplosions;
  }
}
