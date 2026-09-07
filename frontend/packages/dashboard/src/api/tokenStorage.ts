import type { AuthTokens } from '@san/shared';

const ACCESS_TOKEN_KEY = 'san_access_token';
const REFRESH_TOKEN_KEY = 'san_refresh_token';
const SESSION_ID_KEY = 'san_session_id';
const CLIENT_TYPE_KEY = 'san_client_type';
const USERNAME_KEY = 'san_username';
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'san_access_token_expires_at';

function getAccessTokenExpiresAt(expiresIn?: number) {
  return expiresIn ? String(Date.now() + expiresIn * 1000) : null;
}

export const authTokenStorage = {
  getToken: async () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: async () => localStorage.getItem(REFRESH_TOKEN_KEY),
  getAccessTokenExpiresAt: async () => {
    const value = localStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
    return value ? Number(value) : null;
  },
  getUsername: async () => localStorage.getItem(USERNAME_KEY),
  setTokens: async ({ accessToken, refreshToken, sessionId, clientType, expiresIn }: AuthTokens) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(CLIENT_TYPE_KEY, clientType ?? 'DASHBOARD');
    const expiresAt = getAccessTokenExpiresAt(expiresIn);
    if (expiresAt) {
      localStorage.setItem(ACCESS_TOKEN_EXPIRES_AT_KEY, expiresAt);
    } else {
      localStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
    }
    if (sessionId) {
      localStorage.setItem(SESSION_ID_KEY, sessionId);
    }
  },
  setUsername: async (username: string) => {
    localStorage.setItem(USERNAME_KEY, username);
  },
  clearToken: async () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(SESSION_ID_KEY);
    localStorage.removeItem(CLIENT_TYPE_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
  },
};
