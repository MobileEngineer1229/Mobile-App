import type { AbilitySlot, AbilityState } from './Abilities';

export interface CharacterData {
  characterId: number;
  name: string;
  level: number;
  experience: number;
  baseHp: number;
  baseSpeed: number;
  baseBombCount: number;
  baseBlastRadius: number;
  abilities: AbilitySlot[];
  skinId: string;
  bombEffectId: string;
}

export interface ActiveCharacter extends CharacterData {
  currentHp: number;
  abilityStates: AbilityState[];
}

export const DEFAULT_CHARACTERS: Omit<CharacterData, 'characterId'>[] = [
  {
    name: 'Blaster',
    level: 1, experience: 0,
    baseHp: 3, baseSpeed: 3.0, baseBombCount: 1, baseBlastRadius: 2,
    abilities: [], skinId: 'blaster_default', bombEffectId: 'bomb_default',
  },
  {
    name: 'Speeder',
    level: 1, experience: 0,
    baseHp: 2, baseSpeed: 4.5, baseBombCount: 1, baseBlastRadius: 1,
    abilities: [], skinId: 'speeder_default', bombEffectId: 'bomb_default',
  },
  {
    name: 'Tanker',
    level: 1, experience: 0,
    baseHp: 5, baseSpeed: 2.0, baseBombCount: 2, baseBlastRadius: 2,
    abilities: [], skinId: 'tanker_default', bombEffectId: 'bomb_default',
  },
  {
    name: 'Phantom',
    level: 1, experience: 0,
    baseHp: 2, baseSpeed: 3.5, baseBombCount: 1, baseBlastRadius: 3,
    abilities: [], skinId: 'phantom_default', bombEffectId: 'bomb_ghost',
  },
  {
    name: 'Bomber',
    level: 1, experience: 0,
    baseHp: 3, baseSpeed: 3.0, baseBombCount: 3, baseBlastRadius: 4,
    abilities: [], skinId: 'bomber_default', bombEffectId: 'bomb_mega',
  },
];
