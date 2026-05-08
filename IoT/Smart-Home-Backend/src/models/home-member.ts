/**
 * Home member role enum
 */
export enum HomeMemberRole {
  OWNER = 'owner',
  ADMIN = 'admin',
  MEMBER = 'member',
}

/**
 * Home member model interface
 */
export interface HomeMember {
  id: number;
  homeId: number;
  userId: number;
  role: HomeMemberRole;
  addedBy: number;
  createdAt: Date;
}

/**
 * Home member creation input
 */
export interface CreateHomeMemberInput {
  userId?: number;
  email?: string;
  role?: HomeMemberRole;
}

/**
 * Home member update input
 */
export interface UpdateHomeMemberInput {
  role?: HomeMemberRole;
}

/**
 * Home member response (includes user info)
 */
export interface HomeMemberResponse {
  id: number;
  homeId: number;
  userId: number;
  role: HomeMemberRole;
  addedBy: number;
  createdAt: Date;
  user?: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
  };
}
