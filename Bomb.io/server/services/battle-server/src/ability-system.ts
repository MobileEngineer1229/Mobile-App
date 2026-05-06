import { AbilityType } from '../../../shared/types';
import type { Player, PlayerId, AbilityState, Bomb } from '../../../shared/types';
import type { BombSystem } from './bomb-system';

export class AbilitySystem {
  updateCooldowns(players: Map<PlayerId, Player>): void {
    players.forEach(player => {
      player.abilityStates.forEach(state => {
        if (state.cooldownRemaining > 0) {
          state.cooldownRemaining--;
          if (state.cooldownRemaining === 0) state.isReady = true;
        }
        if (state.activeTicksLeft > 0) state.activeTicksLeft--;
      });
    });
  }

  useAbility(player: Player, slotIndex: number, bombSystem: BombSystem, bombs: Map<number, Bomb>): boolean {
    const character = player.characters.find(c => c.characterId === player.activeCharacterId);
    if (!character) return false;

    const slot = character.abilities[slotIndex - 1];
    if (!slot) return false;

    const state = player.abilityStates[slotIndex - 1];
    if (!state || !state.isReady) return false;

    state.isReady = false;
    state.cooldownRemaining = slot.cooldownTicks;

    switch (slot.abilityType) {
      case AbilityType.EXTRA_BOMB:
        player.bombsMax += 1;
        state.activeTicksLeft = slot.durationTicks;
        break;
      case AbilityType.SPEED_BOOST:
        player.speed *= 1.5;
        state.activeTicksLeft = slot.durationTicks;
        break;
      case AbilityType.BLAST_PLUS:
        player.blastRadius += 1;
        state.activeTicksLeft = slot.durationTicks;
        break;
      case AbilityType.REMOTE_DETONATE:
        bombSystem.triggerRemote(player.id, bombs);
        break;
      case AbilityType.GHOST_WALK:
        state.activeTicksLeft = slot.durationTicks;
        break;
      default:
        break;
    }

    return true;
  }

  buildAbilityStates(player: Player): AbilityState[] {
    const character = player.characters.find(c => c.characterId === player.activeCharacterId);
    if (!character) return [];

    return character.abilities.map((slot, i) => ({
      abilityId: i + 1,
      cooldownRemaining: 0,
      isReady: true,
      activeTicksLeft: 0,
    }));
  }
}
