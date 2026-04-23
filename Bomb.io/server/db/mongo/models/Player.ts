import { mongoose } from '../connection';

const { Schema, model, models } = mongoose;

// ── Inventory item ────────────────────────────────────────────────────────────
const InventoryItemSchema = new Schema({
  itemType: {
    type: String,
    enum: ['SKIN', 'BOMB_SKIN', 'TRAIL', 'VICTORY_ANIM', 'TAUNT'],
    required: true,
  },
  itemId:     { type: String, required: true },
  acquiredAt: { type: Date,   default: Date.now },
  source:     { type: String, enum: ['PURCHASE', 'REWARD', 'EVENT', 'DEFAULT'], default: 'DEFAULT' },
}, { _id: false });

// ── Player (auth + account) ───────────────────────────────────────────────────
const PlayerSchema = new Schema({
  username:     { type: String, required: true, unique: true, trim: true, minlength: 3, maxlength: 32 },
  email:        { type: String, required: true, unique: true, trim: true, lowercase: true },
  passwordHash: { type: String, required: true },

  // Account state
  isActive:    { type: Boolean, default: true  },
  isBanned:    { type: Boolean, default: false },
  banReason:   { type: String,  default: null  },
  banExpiresAt:{ type: Date,    default: null  },

  // Currency
  coins:       { type: Number, default: 0, min: 0 },
  gems:        { type: Number, default: 0, min: 0 },

  // Player-level progression (global, not per-character)
  playerLevel: { type: Number, default: 1,  min: 1 },
  playerXp:    { type: Number, default: 0,  min: 0 },

  // Cosmetic inventory
  inventory: { type: [InventoryItemSchema], default: [] },

  // Timestamps
  lastLoginAt: { type: Date, default: null },
}, {
  timestamps: true, // adds createdAt, updatedAt
  collection: 'players',
});

// Give every new player default cosmetics
PlayerSchema.pre('save', function (next) {
  if (!this.isNew) return next();
  this.inventory = [
    { itemType: 'SKIN',         itemId: 'skin_default',     source: 'DEFAULT' },
    { itemType: 'BOMB_SKIN',    itemId: 'bomb_default',     source: 'DEFAULT' },
    { itemType: 'TRAIL',        itemId: 'trail_none',       source: 'DEFAULT' },
    { itemType: 'VICTORY_ANIM', itemId: 'victory_default',  source: 'DEFAULT' },
    { itemType: 'TAUNT',        itemId: 'taunt_default',    source: 'DEFAULT' },
  ] as any;
  next();
});

export interface IPlayer {
  _id: mongoose.Types.ObjectId;
  username: string;
  email: string;
  passwordHash: string;
  isActive: boolean;
  isBanned: boolean;
  banReason: string | null;
  banExpiresAt: Date | null;
  coins: number;
  gems: number;
  playerLevel: number;
  playerXp: number;
  inventory: { itemType: string; itemId: string; acquiredAt: Date; source: string }[];
  lastLoginAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
}

export const Player = models.Player ?? model<IPlayer>('Player', PlayerSchema);
