import axios, { AxiosHeaders, type AxiosError, type InternalAxiosRequestConfig } from 'axios';

export const SKIP_AUTH_HEADER = 'X-SAN-Skip-Auth';

export type AuthClientType = 'DASHBOARD' | 'EXTENSION';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  sessionId?: string;
  clientType?: AuthClientType;
  expiresIn?: number;
}

export interface TokenProvider {
  getToken: () => Promise<string | null>;
  getRefreshToken?: () => Promise<string | null>;
  getAccessTokenExpiresAt?: () => Promise<number | null>;
  setTokens?: (tokens: AuthTokens) => Promise<void>;
  clearToken: () => Promise<void>;
}

export interface ApiResponse<T> {
  ok: boolean;
  data?: T;
  error?: string;
  message?: string;
  timestamp: string;
}

export interface TokenResponse extends AuthTokens {
  tokenType: 'Bearer' | string;
  expiresIn: number;
  sessionId: string;
}

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  _skipAuth?: boolean;
}

const ACCESS_TOKEN_REFRESH_LEEWAY_MS = 60_000;

export function createApiClient(baseURL: string, tokenProvider: TokenProvider) {
  const client = axios.create({
    baseURL,
    timeout: 10_000,
    withCredentials: true,
  });
  let refreshPromise: Promise<TokenResponse> | null = null;

  async function reissueAccessToken() {
    if (!tokenProvider.getRefreshToken || !tokenProvider.setTokens) {
      throw new Error('Token refresh is unavailable');
    }

    if (!refreshPromise) {
      refreshPromise = tokenProvider.getRefreshToken()
        .then((refreshToken) => {
          if (!refreshToken) {
            throw new Error('Missing refresh token');
          }

          return axios.post<ApiResponse<TokenResponse>>(
            '/auth/reissue',
            { refreshToken },
            {
              baseURL,
              headers: { 'Content-Type': 'application/json' },
            }
          );
        })
        .then((response) => unwrapApiResponse(response.data))
        .then(async (tokens) => {
          await tokenProvider.setTokens?.(tokens);
          return tokens;
        })
        .finally(() => {
          refreshPromise = null;
        });
    }

    return refreshPromise;
  }

  client.interceptors.request.use(
    async (config) => {
      const headers = config.headers;
      const skipAuth = headers instanceof AxiosHeaders
        ? headers.get(SKIP_AUTH_HEADER)
        : headers?.[SKIP_AUTH_HEADER];
      if (skipAuth) {
        (config as RetriableRequestConfig)._skipAuth = true;
        if (headers instanceof AxiosHeaders) {
          headers.delete(SKIP_AUTH_HEADER);
        } else if (headers) {
          delete headers[SKIP_AUTH_HEADER];
        }
        return config;
      }

      let token = await tokenProvider.getToken();
      const expiresAt = await tokenProvider.getAccessTokenExpiresAt?.();
      const shouldRefresh =
        token &&
        expiresAt !== null &&
        expiresAt !== undefined &&
        expiresAt - Date.now() <= ACCESS_TOKEN_REFRESH_LEEWAY_MS;

      if (shouldRefresh && tokenProvider.getRefreshToken && tokenProvider.setTokens) {
        try {
          token = (await reissueAccessToken()).accessToken;
        } catch {
          await tokenProvider.clearToken();
          token = null;
        }
      }

      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as RetriableRequestConfig | undefined;

      if (originalRequest?._skipAuth) {
        return Promise.reject(error);
      }

      if (
        error.response?.status === 401 &&
        originalRequest &&
        !originalRequest._retry &&
        tokenProvider.getRefreshToken &&
        tokenProvider.setTokens
      ) {
        originalRequest._retry = true;

        try {
          const tokens = await reissueAccessToken();
          originalRequest.headers.Authorization = `Bearer ${tokens.accessToken}`;

          return client(originalRequest);
        } catch (refreshError) {
          await tokenProvider.clearToken();
          return Promise.reject(refreshError);
        }
      }

      if (error.response?.status === 401) {
        await tokenProvider.clearToken();
      }

      return Promise.reject(error);
    }
  );

  return client;
}

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  if (!response.ok || response.data === undefined) {
    throw new Error(response.message ?? response.error ?? 'API request failed');
  }

  return response.data;
}

export function getApiErrorMessage(
  error: unknown,
  fallback = 'Request failed',
  errorMessages: Record<string, string> = {}
) {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) {
    const response = error.response?.data;
    const errorCode = response?.error;
    return (errorCode ? errorMessages[errorCode] : undefined)
      ?? response?.message
      ?? errorCode
      ?? error.message
      ?? fallback;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}
