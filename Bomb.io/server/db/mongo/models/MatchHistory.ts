import { mongoose } from '../connection';

const { Schema, model, models } = mongoose;

// Snapshot of a single player's performance in a match.
// Written ONCE at match end — never during a live tick.
const PlayerResultSchema = new Schema({
  playerId:    { type: mongoose.Schema.Types.ObjectId, ref: 'Player', required: true },
  username:    { type: String, required: true },
  characterId: { type: mongoose.Schema.Types.ObjectId, ref: 'Character', required: true },
  characterName: { type: String },

  placement:   { type: Number, required: true },  // 1 = winner
  kills:       { type: Number, default: 0 },
  deaths:      { type: Number, default: 0 },
  bombsPlaced: { type: Number, default: 0 },
  damageDealt: { type: Number, default: 0 },

  // ELO before/after this match
  eloBefore: { type: Number },
  eloAfter:  { type: Number },
  eloChange: { type: Number },

  // Survival time in seconds
  survivalTime: { type: Number, default: 0 },
}, { _id: false });

const MatchHistorySchema = new Schema({
  roomId:    { type: String, required: true, index: true },
  mapId:     { type: String, required: true },
  mapName:   { type: String },

  // All participants
  players: { type: [PlayerResultSchema], required: true },

  // Match metadata
  winnerId:       { type: mongoose.Schema.Types.ObjectId, ref: 'Player', default: null },
  winnerUsername: { type: String, default: null },

  startedAt:   { type: Date, required: true },
  endedAt:     { type: Date, required: true },
  durationSec: { type: Number, required: true },

  totalFrames:     { type: Number },
  totalBombsPlaced:{ type: Number, default: 0 },
  totalExplosions: { type: Number, default: 0 },

  // Replay hook — store compressed frame snapshots if needed (optional)
  replayRef: { type: String, default: null }, // e.g. S3 key or GridFS id
}, {
  timestamps: true,
  collection: 'match_history',
});

MatchHistorySchema.index({ 'players.playerId': 1, endedAt: -1 });
MatchHistorySchema.index({ winnerId: 1 });
MatchHistorySchema.index({ endedAt: -1 });

export interface IPlayerResult {
  playerId: mongoose.Types.ObjectId;
  username: string;
  characterId: mongoose.Types.ObjectId;
  characterName?: string;
  placement: number;
  kills: number;
  deaths: number;
  bombsPlaced: number;
  damageDealt: number;
  eloBefore?: number;
  eloAfter?: number;
  eloChange?: number;
  survivalTime: number;
}

export interface IMatchHistory {
  _id: mongoose.Types.ObjectId;
  roomId: string;
  mapId: string;
  mapName?: string;
  players: IPlayerResult[];
  winnerId: mongoose.Types.ObjectId | null;
  winnerUsername: string | null;
  startedAt: Date;
  endedAt: Date;
  durationSec: number;
  totalFrames?: number;
  totalBombsPlaced: number;
  totalExplosions: number;
  replayRef: string | null;
  createdAt: Date;
}

export const MatchHistory = models.MatchHistory ?? model<IMatchHistory>('MatchHistory', MatchHistorySchema);
