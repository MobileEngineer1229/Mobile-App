export enum TerrainType {
  GRASS         = 0,
  FOREST        = 1,
  SNOW          = 2,
  WATER         = 3,
  MOUNTAIN      = 4,
  DESTRUCTIBLE  = 5,
  INDESTRUCTIBLE = 6,
}

export enum Direction {
  NONE  = 0,
  UP    = 1,
  DOWN  = 2,
  LEFT  = 3,
  RIGHT = 4,
}

export interface Vector2 {
  x: number;
  y: number;
}

export interface TerrainProperties {
  walkable: boolean;
  speedMultiplier: number;
  destructible: boolean;
  blocksExplosion: boolean;
}

export const TERRAIN_PROPS: Record<TerrainType, TerrainProperties> = {
  [TerrainType.GRASS]:          { walkable: true,  speedMultiplier: 1.0,  destructible: false, blocksExplosion: false },
  [TerrainType.FOREST]:         { walkable: true,  speedMultiplier: 0.7,  destructible: false, blocksExplosion: true  },
  [TerrainType.SNOW]:           { walkable: true,  speedMultiplier: 0.85, destructible: false, blocksExplosion: false },
  [TerrainType.WATER]:          { walkable: false, speedMultiplier: 0,    destructible: false, blocksExplosion: false },
  [TerrainType.MOUNTAIN]:       { walkable: false, speedMultiplier: 0,    destructible: false, blocksExplosion: true  },
  [TerrainType.DESTRUCTIBLE]:   { walkable: false, speedMultiplier: 0,    destructible: true,  blocksExplosion: true  },
  [TerrainType.INDESTRUCTIBLE]: { walkable: false, speedMultiplier: 0,    destructible: false, blocksExplosion: true  },
};

export interface TerrainUpdate {
  position: Vector2;
  newType: TerrainType;
}

export interface MapData {
  mapId: string;
  name: string;
  width: number;
  height: number;
  tiles: TerrainType[][];
  spawnPoints: Vector2[];
}
