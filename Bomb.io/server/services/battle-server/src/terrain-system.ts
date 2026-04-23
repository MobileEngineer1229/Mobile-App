import { TerrainType, TERRAIN_PROPS, type Vector2, type TerrainUpdate } from '../../../shared/types';

export class TerrainSystem {
  private grid: TerrainType[][];
  private pendingUpdates: TerrainUpdate[] = [];
  readonly width: number;
  readonly height: number;

  constructor(grid: TerrainType[][]) {
    this.grid = grid;
    this.height = grid.length;
    this.width = grid[0]?.length ?? 0;
  }

  isWalkable(x: number, y: number): boolean {
    if (!this.inBounds(x, y)) return false;
    return TERRAIN_PROPS[this.grid[y][x]].walkable;
  }

  getSpeedMultiplier(x: number, y: number): number {
    if (!this.inBounds(x, y)) return 1.0;
    return TERRAIN_PROPS[this.grid[y][x]].speedMultiplier;
  }

  blocksExplosion(x: number, y: number): boolean {
    if (!this.inBounds(x, y)) return true;
    return TERRAIN_PROPS[this.grid[y][x]].blocksExplosion;
  }

  destroyTile(x: number, y: number): boolean {
    if (!this.inBounds(x, y)) return false;
    if (!TERRAIN_PROPS[this.grid[y][x]].destructible) return false;
    this.grid[y][x] = TerrainType.GRASS;
    this.pendingUpdates.push({ position: { x, y }, newType: TerrainType.GRASS });
    return true;
  }

  getTile(x: number, y: number): TerrainType {
    if (!this.inBounds(x, y)) return TerrainType.INDESTRUCTIBLE;
    return this.grid[y][x];
  }

  flushUpdates(): TerrainUpdate[] {
    const updates = this.pendingUpdates;
    this.pendingUpdates = [];
    return updates;
  }

  getNewPosition(pos: Vector2, dir: number, speed: number): Vector2 {
    const deltas: Record<number, Vector2> = {
      1: { x: 0, y: -1 },
      2: { x: 0, y:  1 },
      3: { x: -1, y: 0 },
      4: { x:  1, y: 0 },
    };
    const delta = deltas[dir] ?? { x: 0, y: 0 };
    const multiplier = this.getSpeedMultiplier(pos.x + delta.x, pos.y + delta.y);
    return {
      x: pos.x + delta.x * speed * multiplier,
      y: pos.y + delta.y * speed * multiplier,
    };
  }

  private inBounds(x: number, y: number): boolean {
    return x >= 0 && y >= 0 && x < this.width && y < this.height;
  }
}
