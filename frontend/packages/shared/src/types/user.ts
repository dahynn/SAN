import type { BaseEntity } from './common';

export type AuthProvider = 'LOCAL' | 'GITHUB';
export type UserStatus = 'ACTIVE' | 'LOCKED' | 'WITHDRAWN';

export interface User extends BaseEntity {
  userId: string;
  username: string;
  provider: AuthProvider | string;
  providerId?: string | null;
  status: UserStatus | string;
  lockedUntil?: string | null;
}

