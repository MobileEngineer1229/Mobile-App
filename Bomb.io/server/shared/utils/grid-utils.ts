import type { Vector2, MapData } from '../types';
import { TERRAIN_PROPS } from '../types';

interface AStarNode {
  pos: Vector2;
  g: number;
  h: number;
  f: number;
  parent: AStarNode | null;
}

export function findPath(
  map: MapData,
  start: Vector2,
  goal: Vector2,
  maxSteps = 200,
): Vector2[] {
  const key = (p: Vector2) => `${p.x},${p.y}`;
  const isWalkable = (x: number, y: number) => {
    if (x < 0 || y < 0 || x >= map.width || y >= map.height) return false;
    return TERRAIN_PROPS[map.tiles[y][x]].walkable;
  };

  const h = (p: Vector2) => Math.abs(p.x - goal.x) + Math.abs(p.y - goal.y);

  const open = new Map<string, AStarNode>();
  const closed = new Set<string>();

  const startNode: AStarNode = { pos: start, g: 0, h: h(start), f: h(start), parent: null };
  open.set(key(start), startNode);

  let steps = 0;
  while (open.size > 0 && steps++ < maxSteps) {
    let current: AStarNode = [...open.values()].reduce((a, b) => a.f < b.f ? a : b);
    if (current.pos.x === goal.x && current.pos.y === goal.y) {
      const path: Vector2[] = [];
      let node: AStarNode | null = current;
      while (node) { path.unshift(node.pos); node = node.parent; }
      return path;
    }

    open.delete(key(current.pos));
    closed.add(key(current.pos));

    for (const [dx, dy] of [[0,-1],[0,1],[-1,0],[1,0]]) {
      const nx = current.pos.x + dx;
      const ny = current.pos.y + dy;
      if (!isWalkable(nx, ny) || closed.has(`${nx},${ny}`)) continue;

      const terrain = map.tiles[ny][nx];
      const cost = 1 / (TERRAIN_PROPS[terrain].speedMultiplier || 1);
      const g = current.g + cost;
      const neighbor: AStarNode = { pos: { x: nx, y: ny }, g, h: h({ x: nx, y: ny }), f: 0, parent: current };
      neighbor.f = neighbor.g + neighbor.h;

      const existing = open.get(`${nx},${ny}`);
      if (!existing || g < existing.g) open.set(`${nx},${ny}`, neighbor);
    }
  }
  return [];
}

export function getDirectionDelta(dir: number): Vector2 {
  switch (dir) {
    case 1: return { x: 0, y: -1 };  // UP
    case 2: return { x: 0, y:  1 };  // DOWN
    case 3: return { x: -1, y: 0 };  // LEFT
    case 4: return { x:  1, y: 0 };  // RIGHT
    default: return { x: 0, y: 0 };
  }
}

export function isInBounds(x: number, y: number, width: number, height: number): boolean {
  return x >= 0 && y >= 0 && x < width && y < height;
}
