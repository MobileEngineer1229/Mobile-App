export type RoomStatus = 'WAITING' | 'STARTING' | 'IN_GAME' | 'FINISHED';

export enum RoomType {
  LARGE  = 'LARGE',   // server-created, large map, high player cap, individual only
  CUSTOM = 'CUSTOM',  // player-created, configurable map/mode/size
}

export enum GameMode {
  INDIVIDUAL = 'INDIVIDUAL',  // free-for-all, last player standing
  TEAM       = 'TEAM',        // 2 teams randomly assigned, last team standing
}

export interface Team {
  teamId:    1 | 2;
  playerIds: number[];
}

export interface RoomInfo {
  roomId:           string;
  type:             RoomType;
  status:           RoomStatus;
  gameMode:         GameMode;
  playerIds:        number[];
  maxPlayers:       number;
  mapId:            string;
  teams:            Team[];          // populated at start for TEAM mode, empty otherwise
  creatorPlayerId:  number | null;   // null for LARGE rooms
  battleServerHost: string;
  battleServerPort: number;
  createdAt:        number;
}
