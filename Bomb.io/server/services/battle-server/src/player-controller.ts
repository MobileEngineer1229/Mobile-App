import type { Player, PlayerInput, Bomb } from '../../../shared/types';
import type { TerrainSystem } from './terrain-system';
import type { AbilitySystem } from './ability-system';
import type { BombSystem } from './bomb-system';
import { AbilityType } from '../../../shared/types';

export class PlayerController {
  constructor(private terrain: TerrainSystem, private abilities: AbilitySystem) {}

  processInput(
    player: Player,
    input: PlayerInput,
    bombs: Map<number, Bomb>,
    bombSystem: BombSystem,
  ): void {
    if (!player.isAlive) return;

    // Sequence guard — discard out-of-order packets
    if (input.sequence <= player.lastSequence) return;
    player.lastSequence = input.sequence;

    player.facing = input.moveDir;

    if (input.moveDir !== 0) {
      const newPos = this.terrain.getNewPosition(player.position, input.moveDir, player.speed / 20);
      const nx = Math.round(newPos.x);
      const ny = Math.round(newPos.y);

      const ghostActive = player.abilityStates.some(
        (s, i) => {
          const char = player.characters.find(c => c.characterId === player.activeCharacterId);
          return char?.abilities[i]?.abilityType === AbilityType.GHOST_WALK && s.activeTicksLeft > 0;
        }
      );

      if (ghostActive || this.terrain.isWalkable(nx, ny)) {
        player.position = { x: nx, y: ny };
      }
    }

    if (input.characterId && input.characterId !== player.activeCharacterId) {
      const hasChar = player.characters.some(c => c.characterId === input.characterId);
      if (hasChar) {
        player.activeCharacterId = input.characterId;
        player.abilityStates = this.abilities.buildAbilityStates(player);
        this.syncStatsFromCharacter(player);
      }
    }

    if (input.abilitySlot > 0) {
      this.abilities.useAbility(player, input.abilitySlot, bombSystem, bombs);
    }
  }

  private syncStatsFromCharacter(player: Player): void {
    const char = player.characters.find(c => c.characterId === player.activeCharacterId);
    if (!char) return;
    player.speed = char.baseSpeed;
    player.blastRadius = char.baseBlastRadius;
    player.bombsMax = char.baseBombCount;
  }
}
