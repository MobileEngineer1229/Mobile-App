export interface Entity {
  id: string;
  x: number;
  y: number;
  radius?: number;
}

export class SpatialGrid {
  private grid: Map<string, Set<string>> = new Map();
  private entityPositions: Map<string, string[]> = new Map();
  private cellSize: number;

  constructor(cellSize: number = 2) {
    this.cellSize = cellSize;
  }

  private cellKey(cx: number, cy: number): string {
    return `${cx}:${cy}`;
  }

  private getCellsForEntity(entity: Entity): string[] {
    const r = entity.radius ?? 0.5;
    const startX = Math.floor((entity.x - r) / this.cellSize);
    const endX   = Math.floor((entity.x + r) / this.cellSize);
    const startY = Math.floor((entity.y - r) / this.cellSize);
    const endY   = Math.floor((entity.y + r) / this.cellSize);

    const cells: string[] = [];
    for (let cx = startX; cx <= endX; cx++) {
      for (let cy = startY; cy <= endY; cy++) {
        cells.push(this.cellKey(cx, cy));
      }
    }
    return cells;
  }

  insert(entity: Entity): void {
    this.remove(entity.id);
    const cells = this.getCellsForEntity(entity);
    cells.forEach(key => {
      if (!this.grid.has(key)) this.grid.set(key, new Set());
      this.grid.get(key)!.add(entity.id);
    });
    this.entityPositions.set(entity.id, cells);
  }

  remove(id: string): void {
    const cells = this.entityPositions.get(id);
    if (!cells) return;
    cells.forEach(key => this.grid.get(key)?.delete(id));
    this.entityPositions.delete(id);
  }

  getNearby(x: number, y: number, radius: number): Set<string> {
    const nearby = new Set<string>();
    const startX = Math.floor((x - radius) / this.cellSize);
    const endX   = Math.floor((x + radius) / this.cellSize);
    const startY = Math.floor((y - radius) / this.cellSize);
    const endY   = Math.floor((y + radius) / this.cellSize);

    for (let cx = startX; cx <= endX; cx++) {
      for (let cy = startY; cy <= endY; cy++) {
        const cell = this.grid.get(this.cellKey(cx, cy));
        if (cell) cell.forEach(id => nearby.add(id));
      }
    }
    return nearby;
  }

  clear(): void {
    this.grid.clear();
    this.entityPositions.clear();
  }
}
