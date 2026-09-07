import type { ClientType } from '@san/shared';

export const AUTH_CLIENT_TYPE_PARAM = 'clientType';
export const DEFAULT_AUTH_CLIENT_TYPE: ClientType = 'DASHBOARD';
const AUTH_CLIENT_TYPE_STORAGE_KEY = 'san:auth-client-type';

export function getAuthClientType(
  searchParams: URLSearchParams,
  fallback: ClientType = DEFAULT_AUTH_CLIENT_TYPE
): ClientType {
  return normalizeAuthClientType(searchParams.get(AUTH_CLIENT_TYPE_PARAM)) ?? fallback;
}

export function withAuthClientType(path: string, clientType: ClientType): string {
  if (clientType === DEFAULT_AUTH_CLIENT_TYPE) {
    return path;
  }

  return `${path}?${AUTH_CLIENT_TYPE_PARAM}=${clientType}`;
}

export function rememberAuthClientType(clientType: ClientType) {
  sessionStorage.setItem(AUTH_CLIENT_TYPE_STORAGE_KEY, clientType);
}

export function consumeRememberedAuthClientType(): ClientType | null {
  const clientType = normalizeAuthClientType(sessionStorage.getItem(AUTH_CLIENT_TYPE_STORAGE_KEY));
  sessionStorage.removeItem(AUTH_CLIENT_TYPE_STORAGE_KEY);
  return clientType;
}

function normalizeAuthClientType(value: string | null): ClientType | null {
  const normalized = value?.trim().toUpperCase();
  return normalized === 'DASHBOARD' || normalized === 'EXTENSION' ? normalized : null;
}
