import { mongoose } from '../connection';

const { Schema, model, models } = mongoose;

// ── Ability slot ──────────────────────────────────────────────────────────────
const AbilitySlotSchema = new Schema({
  abilityType: {
    type: String,
    enum: [
      'EXTRA_BOMB',       // +1 bomb capacity for duration
      'SPEED_BOOST',      // movement speed ×1.5 for duration
      'BOMB_KICK',        // kick placed bombs in facing direction
      'BLAST_SHIELD',     // absorbs 1 explosion hit
      'REMOTE_DETONATE',  // manually trigger all own bombs
      'GHOST_WALK',       // phase through soft blocks for duration
      'BLAST_PLUS',       // +1 blast radius for duration
      'BOMB_THROW',       // throw bomb to target tile (range = throwRange)
    ],
    required: true,
  },
  isPassive:       { type: Boolean, default: false },
  cooldownSeconds: { type: Number,  default: 10, min: 0 },
  durationSeconds: { type: Number,  default: 0,  min: 0 }, // 0 = instant
  description:     { type: String,  default: '' },
}, { _id: false });

// ── Cosmetics ────────────────────────────────────────────────────────────────
const CosmeticsSchema = new Schema({
  skinId:        { type: String, default: 'skin_default'     },
  bombSkinId:    { type: String, default: 'bomb_default'     },
  trailEffectId: { type: String, default: 'trail_none'       },
  victoryAnimId: { type: String, default: 'victory_default'  },
  tauntId:       { type: String, default: 'taunt_default'    },
}, { _id: false });

// ── Character ─────────────────────────────────────────────────────────────────
const CharacterSchema = new Schema({
  // Owner (null = template/global definition)
  playerId: { type: mongoose.Schema.Types.ObjectId, ref: 'Player', default: null, index: true },

  // Identity
  templateId:  { type: String,  required: true },          // e.g. "blaster", "speeder"
  name:        { type: String,  required: true, trim: true },
  class:       { type: String,  required: true,
    enum: ['BALANCED', 'ASSASSIN', 'TANK', 'TRICKSTER', 'ARTILLERY'] },
  lore:        { type: String,  default: '' },
  portraitId:  { type: String,  default: 'portrait_default' }, // asset key

  // ── Base Stats ──────────────────────────────────────────────────────────────
  stats: {
    // Movement
    speed:          { type: Number, required: true, min: 1.0, max: 5.0 },
    // Vitality
    maxHealth:      { type: Number, required: true, min: 1,   max: 5   },
    // Offence
    power:          { type: Number, required: true, min: 1,   max: 5   },  // blast damage multiplier
    // Defence
    defense:        { type: Number, required: true, min: 0,   max: 40  },  // % damage reduction
    // Mobility
    agility:        { type: Number, required: true, min: 0.0, max: 1.0 },  // reduces terrain speed penalty
  },

  // ── Bomb Stats ──────────────────────────────────────────────────────────────
  bombStats: {
    capacity:       { type: Number, required: true, min: 1,   max: 5   },  // simultaneous bombs
    blastRadius:    { type: Number, required: true, min: 1,   max: 8   },  // tiles in each direction
    fuseTime:       { type: Number, required: true, min: 1.0, max: 4.0 },  // seconds until explosion
    kickStrength:   { type: Number, default: 0,    min: 0,   max: 5   },  // tiles a kicked bomb slides
    throwRange:     { type: Number, default: 0,    min: 0,   max: 5   },  // 0 = cannot throw
    piercing:       { type: Boolean, default: false },                      // blast passes through 1 soft block
  },

  // ── Special Traits ──────────────────────────────────────────────────────────
  traits: {
    ghostWalkCharges:  { type: Number, default: 0, min: 0, max: 3 },  // phase through soft blocks N times per life
    shieldCharges:     { type: Number, default: 0, min: 0, max: 2 },  // absorbs N bomb hits before breaking
    remoteDetonation:  { type: Boolean, default: false },              // can manually trigger own bombs
    bombReturn:        { type: Boolean, default: false },              // bombs slide back to caster after kick
    areaExplosion:     { type: Boolean, default: false },              // diamond pattern instead of cross
    terrainImmunity:   {                                               // ignores terrain speed penalty
      type: [String],
      enum: ['FOREST', 'SNOW', 'WATER'],
      default: [],
    },
  },

  // ── Abilities (max 3 slots) ──────────────────────────────────────────────────
  abilities: {
    type: [AbilitySlotSchema],
    validate: [(v: unknown[]) => v.length <= 3, 'Max 3 ability slots'],
    default: [],
  },

  // ── Progression (per-player copy only) ──────────────────────────────────────
  progression: {
    level:          { type: Number, default: 1,  min: 1,  max: 50 },
    experience:     { type: Number, default: 0,  min: 0           },
    prestigeLevel:  { type: Number, default: 0,  min: 0,  max: 5  },
    // XP thresholds: level N requires N*100 XP
  },

  // ── Cosmetics ────────────────────────────────────────────────────────────────
  cosmetics: { type: CosmeticsSchema, default: () => ({}) },

  // ── Unlock config (template level, ignored on player copies) ────────────────
  unlock: {
    isDefault:           { type: Boolean, default: false }, // given to every new player
    unlockCost:          { type: Number,  default: 0    },  // coins
    requiredPlayerLevel: { type: Number,  default: 1    },
  },

  isActive: { type: Boolean, default: true }, // soft-delete
}, {
  timestamps: true,
  collection: 'characters',
});

// Indexes
CharacterSchema.index({ playerId: 1, templateId: 1 });
CharacterSchema.index({ templateId: 1, 'unlock.isDefault': 1 });

export interface IAbilitySlot {
  abilityType: string;
  isPassive: boolean;
  cooldownSeconds: number;
  durationSeconds: number;
  description: string;
}

export interface ICharacter {
  _id: mongoose.Types.ObjectId;
  playerId: mongoose.Types.ObjectId | null;
  templateId: string;
  name: string;
  class: string;
  lore: string;
  portraitId: string;
  stats: {
    speed: number;
    maxHealth: number;
    power: number;
    defense: number;
    agility: number;
  };
  bombStats: {
    capacity: number;
    blastRadius: number;
    fuseTime: number;
    kickStrength: number;
    throwRange: number;
    piercing: boolean;
  };
  traits: {
    ghostWalkCharges: number;
    shieldCharges: number;
    remoteDetonation: boolean;
    bombReturn: boolean;
    areaExplosion: boolean;
    terrainImmunity: string[];
  };
  abilities: IAbilitySlot[];
  progression: {
    level: number;
    experience: number;
    prestigeLevel: number;
  };
  cosmetics: {
    skinId: string;
    bombSkinId: string;
    trailEffectId: string;
    victoryAnimId: string;
    tauntId: string;
  };
  unlock: {
    isDefault: boolean;
    unlockCost: number;
    requiredPlayerLevel: number;
  };
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export const Character = models.Character ?? model<ICharacter>('Character', CharacterSchema);
