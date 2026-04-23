import type { Vector2 } from '../types';

export function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

export function manhattanDistance(a: Vector2, b: Vector2): number {
  return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
}

export function euclideanDistance(a: Vector2, b: Vector2): number {
  return Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2);
}

export function addVec(a: Vector2, b: Vector2): Vector2 {
  return { x: a.x + b.x, y: a.y + b.y };
}

export function scaleVec(v: Vector2, s: number): Vector2 {
  return { x: v.x * s, y: v.y * s };
}

export function vecEqual(a: Vector2, b: Vector2): boolean {
  return a.x === b.x && a.y === b.y;
}

export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

export function randomInt(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}
