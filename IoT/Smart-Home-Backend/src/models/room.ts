/**
 * Room model interface
 */
export interface Room {
  id: number;
  userId: number;
  homeId?: number | null;
  name: string;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Room creation input
 */
export interface CreateRoomInput {
  name: string;
  homeId?: number;
}

/**
 * Room update input
 */
export interface UpdateRoomInput {
  name?: string;
  homeId?: number | null;
}

/**
 * Room response
 */
export interface RoomResponse {
  id: number;
  userId: number;
  homeId?: number | null;
  name: string;
  createdAt: Date;
  updatedAt: Date;
}

