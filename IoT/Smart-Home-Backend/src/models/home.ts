/**
 * Home model interface
 */
export interface Home {
  id: number;
  userId: number;
  name: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  country?: string;
  isPrimary: boolean;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Home creation input
 */
export interface CreateHomeInput {
  name: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  country?: string;
  isPrimary?: boolean;
}

/**
 * Home update input
 */
export interface UpdateHomeInput {
  name?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  country?: string;
  isPrimary?: boolean;
}

/**
 * Home response
 */
export interface HomeResponse {
  id: number;
  userId: number;
  name: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  country?: string;
  isPrimary: boolean;
  createdAt: Date;
  updatedAt: Date;
}
