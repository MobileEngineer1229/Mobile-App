export enum AbilityType {
  NONE            = 0,
  EXTRA_BOMB      = 1,
  SPEED_BOOST     = 2,
  BOMB_KICK       = 3,
  BLAST_SHIELD    = 4,
  REMOTE_DETONATE = 5,
  GHOST_WALK      = 6,
  BLAST_PLUS      = 7,
}

export interface AbilitySlot {
  abilityType: AbilityType;
  cooldownTicks: number;
  durationTicks: number;
  isPassive: boolean;
}

export interface AbilityState {
  abilityId: number;
  cooldownRemaining: number;
  isReady: boolean;
  activeTicksLeft: number;
}
