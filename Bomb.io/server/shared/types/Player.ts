import type { Vector2, Direction } from './TerrainGrid';
import type { AbilityState } from './Abilities';
import type { ActiveCharacter } from './Character';

export interface PlayerAddress {
  host: string;
  port: number;
}

export type PlayerId = string;

export interface Player {
  id: PlayerId;
  username: string;
  sessionToken: string;
  address: PlayerAddress;

  activeCharacterId: number;
  characters: ActiveCharacter[];

  position: Vector2;
  facing: Direction;
  speed: number;
  blastRadius: number;
  bombsMax: number;
  bombsPlaced: number;

  teamId: number;        // 0 = no team (individual), 1 or 2 for TEAM mode
  isAlive: boolean;
  lastBombTime: number;
  pendingInput: PlayerInput | null;
  abilityStates: AbilityState[];

  lastSequence: number;
}

export interface PlayerInput {
  sequence: number;
  moveDir: Direction;
  placeBomb: boolean;
  abilitySlot: number;
  characterId: number;
}

export interface PlayerState {
  playerId: PlayerId;
  characterId: number;
  position: Vector2;
  facing: Direction;
  hp: number;
  maxHp: number;
  bombsAvailable: number;
  bombsMax: number;
  blastRadius: number;
  abilityStates: AbilityState[];
}
