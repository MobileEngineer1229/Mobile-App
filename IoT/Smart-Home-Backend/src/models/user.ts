/**
 * User model interface
 */
export interface User {
  id: number;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  notificationPreferences?: Record<string, boolean>;
  securitySettings?: Record<string, boolean | Record<string, any>>;
  profileMetadata?: {
    gender?: string;
    birthdate?: string;
    profilePictureUrl?: string;
    appAppearance?: {
      theme?: string;
      language?: string;
    };
    [key: string]: any;
  };
  additionalSettings?: Record<string, any>;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * User creation input
 */
export interface CreateUserInput {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

/**
 * User update input
 */
export interface UpdateUserInput {
  firstName?: string;
  lastName?: string;
  phone?: string;
}

/**
 * User response (without password)
 */
export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  notificationPreferences?: Record<string, boolean>;
  securitySettings?: Record<string, boolean | Record<string, any>>;
  profileMetadata?: {
    gender?: string;
    birthdate?: string;
    profilePictureUrl?: string;
    appAppearance?: {
      theme?: string;
      language?: string;
    };
    [key: string]: any;
  };
  additionalSettings?: Record<string, any>;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Login input
 */
export interface LoginInput {
  email: string;
  password: string;
}

/**
 * Login response
 */
export interface LoginResponse {
  user: UserResponse;
  token: string;
}

