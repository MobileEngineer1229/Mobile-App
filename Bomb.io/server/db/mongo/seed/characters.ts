import { connectMongo } from '../connection';
import { Character } from '../models/Character';
import type { ICharacter } from '../models/Character';

// ── Character templates ───────────────────────────────────────────────────────
// Rules:
//   • speed      1.0–5.0  (tiles/sec)
//   • maxHealth  1–5      (HP before elimination)
//   • power      1–5      (blast damage multiplier)
//   • defense    0–40     (% damage reduction from explosions)
//   • agility    0.0–1.0  (fraction of terrain speed penalty removed)
//   • blastRadius 1–8     (tiles in each cardinal direction)
//   • fuseTime   1.0–4.0  (seconds before detonation)
//   • capacity   1–5      (simultaneous live bombs)

type CharacterSeed = Omit<ICharacter, '_id' | 'playerId' | 'createdAt' | 'updatedAt' | 'progression' | 'cosmetics' | 'isActive'>;

const templates: CharacterSeed[] = [

  // ── 1. BLASTER — The Balanced Bomber ───────────────────────────────────────
  {
    templateId: 'blaster',
    name:       'Blaster',
    class:      'BALANCED',
    lore:       'A seasoned veteran who has mastered the art of the controlled explosion. Neither the fastest nor the toughest, but always the most prepared.',
    portraitId: 'portrait_blaster',

    stats: {
      speed:     3.0,  // comfortable running speed
      maxHealth: 3,    // survives 3 direct hits
      power:     3,    // standard blast damage
      defense:   10,   // slight blast resistance from thick gear
      agility:   0.50, // handles terrain changes well
    },
    bombStats: {
      capacity:     2,    // can juggle 2 bombs at once
      blastRadius:  3,    // solid 3-tile reach
      fuseTime:     2.5,  // standard 2.5 s fuse
      kickStrength: 2,    // can nudge bombs 2 tiles
      throwRange:   0,
      piercing:     false,
    },
    traits: {
      ghostWalkCharges:  0,
      shieldCharges:     0,
      remoteDetonation:  false,
      bombReturn:        false,
      areaExplosion:     false,
      terrainImmunity:   [],
    },
    abilities: [
      {
        abilityType:     'SPEED_BOOST',
        isPassive:       false,
        cooldownSeconds: 12,
        durationSeconds: 4,
        description:     'Sprint at 1.5× speed for 4 seconds.',
      },
      {
        abilityType:     'EXTRA_BOMB',
        isPassive:       false,
        cooldownSeconds: 15,
        durationSeconds: 6,
        description:     'Temporarily carry +1 extra bomb for 6 seconds.',
      },
      {
        abilityType:     'BLAST_PLUS',
        isPassive:       true,
        cooldownSeconds: 0,
        durationSeconds: 0,
        description:     'Passively adds +1 to blast radius at all times.',
      },
    ],
    unlock: { isDefault: true, unlockCost: 0, requiredPlayerLevel: 1 },
  },

  // ── 2. SPEEDER — The Assassin ───────────────────────────────────────────────
  {
    templateId: 'speeder',
    name:       'Speeder',
    class:      'ASSASSIN',
    lore:       'A ghost on the battlefield. Speeder places a bomb and vanishes before the smoke clears. Fragile, but near-impossible to catch.',
    portraitId: 'portrait_speeder',

    stats: {
      speed:     5.0,  // fastest character in the game
      maxHealth: 2,    // glass cannon — 2 hits and out
      power:     2,    // weaker blasts compensated by speed
      defense:   0,    // zero armour — relies on dodging
      agility:   0.80, // barely slowed by terrain
    },
    bombStats: {
      capacity:     1,    // only 1 bomb at a time
      blastRadius:  2,    // short range — must get in close
      fuseTime:     2.0,  // faster fuse — keep moving!
      kickStrength: 4,    // powerful kick to create distance
      throwRange:   0,
      piercing:     false,
    },
    traits: {
      ghostWalkCharges:  1,    // 1 free phase through soft blocks per life
      shieldCharges:     0,
      remoteDetonation:  false,
      bombReturn:        false,
      areaExplosion:     false,
      terrainImmunity:   ['SNOW'], // ice doesn't slow them down
    },
    abilities: [
      {
        abilityType:     'GHOST_WALK',
        isPassive:       false,
        cooldownSeconds: 20,
        durationSeconds: 3,
        description:     'Phase through soft blocks and bombs for 3 seconds.',
      },
      {
        abilityType:     'SPEED_BOOST',
        isPassive:       true,
        cooldownSeconds: 0,
        durationSeconds: 0,
        description:     'Passively moves 15% faster than base speed.',
      },
      {
        abilityType:     'BOMB_KICK',
        isPassive:       true,
        cooldownSeconds: 0,
        durationSeconds: 0,
        description:     'Passively kick any bomb by walking into it.',
      },
    ],
    unlock: { isDefault: true, unlockCost: 0, requiredPlayerLevel: 1 },
  },

  // ── 3. TANKER — The Fortress ────────────────────────────────────────────────
  {
    templateId: 'tanker',
    name:       'Tanker',
    class:      'TANK',
    lore:       'Built like a concrete bunker and just as stubborn. Tanker doesn\'t dodge — Tanker endures. Their bombs cover wide areas and their shield turns deathtraps into opportunities.',
    portraitId: 'portrait_tanker',

    stats: {
      speed:     2.0,  // slowest character — trades mobility for durability
      maxHealth: 5,    // takes 5 hits before going down
      power:     2,    // average blast — relies on zone control
      defense:   40,   // absorbs 40% of all explosion damage
      agility:   0.20, // terrain really slows them down
    },
    bombStats: {
      capacity:     3,    // 3 bombs at once — great zone control
      blastRadius:  2,
      fuseTime:     3.0,  // slow fuse — gives time to get clear
      kickStrength: 0,    // too heavy to kick bombs
      throwRange:   0,
      piercing:     false,
    },
    traits: {
      ghostWalkCharges:  0,
      shieldCharges:     1,    // 1 free hit absorbed by armour per life
      remoteDetonation:  false,
      bombReturn:        false,
      areaExplosion:     false,
      terrainImmunity:   ['FOREST'], // forest doesn't slow their march
    },
    abilities: [
      {
        abilityType:     'BLAST_SHIELD',
        isPassive:       false,
        cooldownSeconds: 25,
        durationSeconds: 5,
        description:     'Become immune to all explosion damage for 5 seconds.',
      },
      {
        abilityType:     'EXTRA_BOMB',
        isPassive:       true,
        cooldownSeconds: 0,
        durationSeconds: 0,
        description:     'Passively carries +1 bomb capacity at all times.',
      },
      {
        abilityType:     'BLAST_PLUS',
        isPassive:       true,
        cooldownSeconds: 0,
        durationSeconds: 0,
        description:     'Passively adds +1 to blast radius at all times.',
      },
    ],
    unlock: { isDefault: true, unlockCost: 0, requiredPlayerLevel: 1 },
  },

  // ── 4. PHANTOM — The Trickster ──────────────────────────────────────────────
  {
    templateId: 'phantom',
    name:       'Phantom',
    class:      'TRICKSTER',
    lore:       'A mischief-maker who thrives in chaos. Phantom sets traps from afar, triggers them at will, and slips through walls like smoke. Opponents never know where the next bomb will land.',
    portraitId: 'portrait_phantom',

    stats: {
      speed:     3.5,  // above-average — needs to reposition
      maxHealth: 2,    // fragile — lives by wits, not brawn
      power:     3,    // respectable blast power
      defense:   10,
      agility:   0.60,
    },
    bombStats: {
      capacity:     2,
      blastRadius:  3,
      fuseTime:     2.5,
      kickStrength: 0,
      throwRange:   4,    // can THROW bombs up to 4 tiles away
      piercing:     false,
    },
    traits: {
      ghostWalkCharges:  2,    // 2 phase-through charges per life
      shieldCharges:     0,
      remoteDetonation:  true, // signature trait — trigger bombs manually
      bombReturn:        false,
      areaExplosion:     false,
      terrainImmunity:   [],
    },
    abilities: [
      {
        abilityType:     'REMOTE_DETONATE',
        isPassive:       false,
        cooldownSeconds: 8,
        durationSeconds: 0,
        description:     'Instantly detonate all your placed bombs.',
      },
      {
        abilityType:     'GHOST_WALK',
        isPassive:       false,
        cooldownSeconds: 18,
        durationSeconds: 2,
        description:     'Phase through soft blocks and bombs for 2 seconds.',
      },
      {
        abilityType:     'BOMB_THROW',
        isPassive:       true,
        cooldownSeconds: 0,
        durationSeconds: 0,
        description:     'Passively allows throwing bombs up to throwRange tiles.',
      },
    ],
    unlock: { isDefault: false, unlockCost: 800, requiredPlayerLevel: 5 },
  },

  // ── 5. BOMBER — The Artillery ───────────────────────────────────────────────
  {
    templateId: 'bomber',
    name:       'Bomber',
    class:      'ARTILLERY',
    lore:       'A walking arsenal. Bomber exists for one purpose: maximum destruction. Five simultaneous bombs, radius that clears entire corridors, and blasts that punch clean through walls.',
    portraitId: 'portrait_bomber',

    stats: {
      speed:     2.5,  // slow — all that ordnance is heavy
      maxHealth: 3,
      power:     5,    // MAXIMUM power — highest damage multiplier
      defense:   15,
      agility:   0.30,
    },
    bombStats: {
      capacity:     5,    // 5 simultaneous bombs — carpet bomb whole areas
      blastRadius:  5,    // wide devastation radius
      fuseTime:     3.0,  // long fuse — time to place all 5
      kickStrength: 1,
      throwRange:   2,    // short throw for bomb positioning
      piercing:     true, // blasts punch through 1 soft block per direction
    },
    traits: {
      ghostWalkCharges:  0,
      shieldCharges:     0,
      remoteDetonation:  false,
      bombReturn:        false,
      areaExplosion:     true, // diamond blast pattern instead of cross
      terrainImmunity:   [],
    },
    abilities: [
      {
        abilityType:     'BLAST_PLUS',
        isPassive:       false,
        cooldownSeconds: 20,
        durationSeconds: 8,
        description:     'Adds +2 to blast radius for 8 seconds.',
      },
      {
        abilityType:     'REMOTE_DETONATE',
        isPassive:       false,
        cooldownSeconds: 30,
        durationSeconds: 0,
        description:     'Detonate all placed bombs simultaneously for chain reactions.',
      },
      {
        abilityType:     'EXTRA_BOMB',
        isPassive:       true,
        cooldownSeconds: 0,
        durationSeconds: 0,
        description:     'Passively carries +1 bomb capacity (total 5+1=6 at max).',
      },
    ],
    unlock: { isDefault: false, unlockCost: 1200, requiredPlayerLevel: 8 },
  },
];

// ── Seed runner ───────────────────────────────────────────────────────────────
export async function seedCharacters(): Promise<void> {
  await connectMongo();

  for (const template of templates) {
    await Character.findOneAndUpdate(
      { templateId: template.templateId, playerId: null },
      {
        $setOnInsert: {
          ...template,
          playerId: null,
          isActive: true,
          progression: { level: 1, experience: 0, prestigeLevel: 0 },
          cosmetics: {
            skinId:        `skin_${template.templateId}`,
            bombSkinId:    'bomb_default',
            trailEffectId: 'trail_none',
            victoryAnimId: 'victory_default',
            tauntId:       'taunt_default',
          },
        },
      },
      { upsert: true, new: true },
    );
    console.log(`[Seed] Character template "${template.name}" ready`);
  }

  console.log('[Seed] All character templates seeded.');
}

// Run directly: bun server/db/mongo/seed/characters.ts
if (import.meta.main) {
  seedCharacters()
    .then(() => process.exit(0))
    .catch(err => { console.error(err); process.exit(1); });
}
