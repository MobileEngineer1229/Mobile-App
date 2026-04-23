import { SpatialGrid } from '../../../shared/utils';
import type { Player } from '../../../shared/types';

export class CollisionGrid {
  private spatial = new SpatialGrid(1);

  sync(players: Map<number, Player>): void {
    this.spatial.clear();
    players.forEach(p => {
      if (p.isAlive) {
        this.spatial.insert({ id: String(p.id), x: p.position.x, y: p.position.y, radius: 0.4 });
      }
    });
  }

  getPlayersAt(x: number, y: number): string[] {
    return [...this.spatial.getNearby(x, y, 0.5)];
  }

  getPlayersInCells(cells: { x: number; y: number }[]): Set<number> {
    const hit = new Set<number>();
    cells.forEach(({ x, y }) => {
      this.getPlayersAt(x, y).forEach(id => hit.add(Number(id)));
    });
    return hit;
  }
}
