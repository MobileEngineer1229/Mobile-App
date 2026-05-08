/**
 * Home invitation model interface
 */
export interface HomeInvitation {
  id: number;
  homeId: number;
  code: string;
  createdBy: number;
  expiresAt: Date | null;
  maxUses: number;
  currentUses: number;
  isActive: boolean;
  createdAt: Date;
}

/**
 * Home invitation creation input
 */
export interface CreateHomeInvitationInput {
  expiresAt?: Date | string;
  maxUses?: number;
}

/**
 * Home invitation response
 */
export interface HomeInvitationResponse {
  id: number;
  homeId: number;
  code: string;
  createdBy: number;
  expiresAt: Date | null;
  maxUses: number;
  currentUses: number;
  isActive: boolean;
  createdAt: Date;
  home?: {
    id: number;
    name: string;
    address?: string;
  };
}
