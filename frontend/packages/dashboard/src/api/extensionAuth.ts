import type { AuthTokens } from '@san/shared';
const AUTH_SYNC_MESSAGE = 'SAN_AUTH_SYNC';
const AUTH_CLEAR_MESSAGE = 'SAN_AUTH_CLEAR';
const LOGIN_BRIDGE_TICKET_MESSAGE = 'LOGIN_BRIDGE_TICKET';
const GET_TIL_RECALL_SETTINGS_MESSAGE = 'GET_TIL_RECALL_SETTINGS';
const SET_TIL_RECALL_SETTINGS_MESSAGE = 'SET_TIL_RECALL_SETTINGS';
const GET_EXTENSION_SHORTCUTS_MESSAGE = 'GET_EXTENSION_SHORTCUTS';
const OPEN_EXTENSION_SHORTCUT_SETTINGS_MESSAGE = 'OPEN_EXTENSION_SHORTCUT_SETTINGS';
const DEBUG_PREFIX = '[SAN:extension-auth]';
const DASHBOARD_MESSAGE_SOURCE = 'SAN_DASHBOARD';
const EXTENSION_MESSAGE_SOURCE = 'SAN_EXTENSION';
const BRIDGE_TIMEOUT_MS = 3000;

interface ChromeRuntimeBridge {
  runtime?: {
    sendMessage?: (
      extensionId: string,
      message: unknown,
      callback?: (response?: unknown) => void
    ) => void;
    lastError?: { message?: string };
  };
}

interface ExtensionMessageResponse {
  ok?: boolean;
  hasAccessToken?: boolean;
  hasRefreshToken?: boolean;
  settings?: TilRecallSettings;
  shortcuts?: ExtensionShortcuts;
}

interface ExtensionBridgeResponseMessage {
  source?: unknown;
  requestId?: unknown;
  response?: unknown;
}

declare global {
  interface Window {
    chrome?: ChromeRuntimeBridge;
  }
}

export interface TilRecallSettings {
  enabled: boolean;
  time: string;
}

export interface ExtensionShortcuts {
  openSidePanel: string;
  captureImage: string;
}

export async function syncExtensionAuth(tokens: AuthTokens): Promise<void> {
  const message = {
    type: AUTH_SYNC_MESSAGE,
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    sessionId: tokens.sessionId,
    clientType: tokens.clientType ?? 'EXTENSION',
    expiresIn: tokens.expiresIn,
  };

  await deliverExtensionAuthMessage(message, true);
}

export async function clearExtensionAuth(): Promise<void> {
  const message = { type: AUTH_CLEAR_MESSAGE };

  await deliverExtensionAuthMessage(message, false);
}

export async function syncExtensionBridgeTicket(ticket: string): Promise<void> {
  await deliverExtensionAuthMessage(
    {
      type: LOGIN_BRIDGE_TICKET_MESSAGE,
      ticket,
    },
    true
  );
}

export async function getExtensionTilRecallSettings(): Promise<TilRecallSettings> {
  const response = await deliverExtensionMessageWithResponse(
    { type: GET_TIL_RECALL_SETTINGS_MESSAGE },
    isConfirmedTilRecallSettingsResponse
  );

  return response.settings;
}

export async function setExtensionTilRecallSettings(settings: TilRecallSettings): Promise<TilRecallSettings> {
  const response = await deliverExtensionMessageWithResponse(
    {
      type: SET_TIL_RECALL_SETTINGS_MESSAGE,
      payload: settings,
    },
    isConfirmedTilRecallSettingsResponse
  );

  return response.settings;
}

export async function getExtensionShortcuts(): Promise<ExtensionShortcuts> {
  const response = await deliverExtensionMessageWithResponse(
    { type: GET_EXTENSION_SHORTCUTS_MESSAGE },
    isConfirmedExtensionShortcutsResponse
  );

  return response.shortcuts;
}

export async function openExtensionShortcutSettings(): Promise<void> {
  await deliverExtensionMessageWithResponse(
    { type: OPEN_EXTENSION_SHORTCUT_SETTINGS_MESSAGE },
    isConfirmedOkResponse
  );
}

function isConfirmedOkResponse(response: ExtensionMessageResponse | undefined): { ok: true } | null {
  return response?.ok === true ? { ok: true } : null;
}

async function deliverExtensionAuthMessage(message: unknown, expectStoredTokens: boolean): Promise<void> {
  console.info(DEBUG_PREFIX, 'extension auth sync started', {
    directRuntimeAvailable: typeof window.chrome?.runtime?.sendMessage === 'function',
    origin: window.location.origin,
  });

  const attempts = [
    sendExtensionMessage(message, expectStoredTokens),
    postDashboardMessage(message, expectStoredTokens),
  ];

  try {
    await waitForFirstConfirmed(attempts);
    console.info(DEBUG_PREFIX, 'extension auth sync confirmed');
  } catch (error) {
    console.warn(DEBUG_PREFIX, 'extension auth sync was not confirmed', {
      errors: error instanceof AuthSyncError ? error.errors : [normalizeError(error)],
    });
    throw error;
  }
}

async function deliverExtensionMessageWithResponse<T>(
  message: unknown,
  confirmResponse: (response: ExtensionMessageResponse | undefined) => T | null
): Promise<T> {
  const attempts = [
    sendExtensionMessageWithResponse(message, confirmResponse),
    postDashboardMessageWithResponse(message, confirmResponse),
  ];

  return waitForFirstConfirmedResponse(attempts);
}

function waitForFirstConfirmed(attempts: Promise<void>[]): Promise<void> {
  return new Promise((resolve, reject) => {
    const errors: string[] = [];
    let rejectedCount = 0;
    let settled = false;

    attempts.forEach((attempt) => {
      attempt
        .then(() => {
          if (settled) {
            return;
          }

          settled = true;
          resolve();
        })
        .catch((error: unknown) => {
          if (settled) {
            return;
          }

          rejectedCount += 1;
          errors.push(normalizeError(error));

          if (rejectedCount === attempts.length) {
            settled = true;
            reject(new AuthSyncError(errors));
          }
        });
    });
  });
}

function waitForFirstConfirmedResponse<T>(attempts: Promise<T>[]): Promise<T> {
  return new Promise((resolve, reject) => {
    const errors: string[] = [];
    let rejectedCount = 0;
    let settled = false;

    attempts.forEach((attempt) => {
      attempt
        .then((response) => {
          if (settled) {
            return;
          }

          settled = true;
          resolve(response);
        })
        .catch((error: unknown) => {
          if (settled) {
            return;
          }

          rejectedCount += 1;
          errors.push(normalizeError(error));

          if (rejectedCount === attempts.length) {
            settled = true;
            reject(new AuthSyncError(errors));
          }
        });
    });
  });
}

async function postDashboardMessage(message: unknown, expectStoredTokens: boolean): Promise<void> {
  const requestId = createRequestId();

  await new Promise<void>((resolve, reject) => {
    const timeoutId = window.setTimeout(() => {
      window.removeEventListener('message', handleBridgeResponse);
      reject(new Error('Timed out waiting for extension content-script bridge'));
    }, BRIDGE_TIMEOUT_MS);

    const handleBridgeResponse = (event: MessageEvent<ExtensionBridgeResponseMessage>) => {
      if (event.source !== window || event.origin !== window.location.origin) {
        return;
      }

      if (
        event.data?.source !== EXTENSION_MESSAGE_SOURCE
        || event.data.requestId !== requestId
      ) {
        return;
      }

      window.clearTimeout(timeoutId);
      window.removeEventListener('message', handleBridgeResponse);

      const response = event.data.response as ExtensionMessageResponse | undefined;
      if (!isConfirmedExtensionResponse(response, expectStoredTokens)) {
        reject(new Error('Extension content-script bridge did not confirm auth storage'));
        return;
      }

      resolve();
    };

    window.addEventListener('message', handleBridgeResponse);
    window.postMessage(
      {
        source: DASHBOARD_MESSAGE_SOURCE,
        requestId,
        payload: message,
      },
      window.location.origin
    );
  });
}

async function postDashboardMessageWithResponse<T>(
  message: unknown,
  confirmResponse: (response: ExtensionMessageResponse | undefined) => T | null
): Promise<T> {
  const requestId = createRequestId();

  return new Promise<T>((resolve, reject) => {
    const timeoutId = window.setTimeout(() => {
      window.removeEventListener('message', handleBridgeResponse);
      reject(new Error('Timed out waiting for extension content-script bridge'));
    }, BRIDGE_TIMEOUT_MS);

    const handleBridgeResponse = (event: MessageEvent<ExtensionBridgeResponseMessage>) => {
      if (event.source !== window || event.origin !== window.location.origin) {
        return;
      }

      if (
        event.data?.source !== EXTENSION_MESSAGE_SOURCE
        || event.data.requestId !== requestId
      ) {
        return;
      }

      window.clearTimeout(timeoutId);
      window.removeEventListener('message', handleBridgeResponse);

      const response = event.data.response as ExtensionMessageResponse | undefined;
      const confirmedResponse = confirmResponse(response);
      if (!confirmedResponse) {
        reject(new Error('Extension content-script bridge did not confirm message'));
        return;
      }

      resolve(confirmedResponse);
    };

    window.addEventListener('message', handleBridgeResponse);
    window.postMessage(
      {
        source: DASHBOARD_MESSAGE_SOURCE,
        requestId,
        payload: message,
      },
      window.location.origin
    );
  });
}

async function sendExtensionMessage(message: unknown, expectStoredTokens: boolean): Promise<void> {
  const extensionId = import.meta.env.VITE_SAN_EXTENSION_ID;

  if (!extensionId || typeof window.chrome?.runtime?.sendMessage !== 'function') {
    throw new Error('Chrome runtime bridge is unavailable');
  }

  await new Promise<void>((resolve, reject) => {
    const timeoutId = window.setTimeout(() => {
      reject(new Error('Timed out waiting for extension runtime bridge'));
    }, BRIDGE_TIMEOUT_MS);

    window.chrome?.runtime?.sendMessage?.(
      extensionId,
      message,
      (response?: unknown) => {
        window.clearTimeout(timeoutId);
        const lastError = window.chrome?.runtime?.lastError;

        if (lastError?.message) {
          reject(new Error(lastError.message));
          return;
        }

        const extensionResponse = response as ExtensionMessageResponse | undefined;

        if (!isConfirmedExtensionResponse(extensionResponse, expectStoredTokens)) {
          reject(new Error('Extension did not confirm auth storage'));
          return;
        }

        resolve();
      }
    );
  });
}

async function sendExtensionMessageWithResponse<T>(
  message: unknown,
  confirmResponse: (response: ExtensionMessageResponse | undefined) => T | null
): Promise<T> {
  const extensionId = import.meta.env.VITE_SAN_EXTENSION_ID;

  if (!extensionId || typeof window.chrome?.runtime?.sendMessage !== 'function') {
    throw new Error('Chrome runtime bridge is unavailable');
  }

  return new Promise<T>((resolve, reject) => {
    const timeoutId = window.setTimeout(() => {
      reject(new Error('Timed out waiting for extension runtime bridge'));
    }, BRIDGE_TIMEOUT_MS);

    window.chrome?.runtime?.sendMessage?.(
      extensionId,
      message,
      (response?: unknown) => {
        window.clearTimeout(timeoutId);
        const lastError = window.chrome?.runtime?.lastError;

        if (lastError?.message) {
          reject(new Error(lastError.message));
          return;
        }

        const confirmedResponse = confirmResponse(response as ExtensionMessageResponse | undefined);

        if (!confirmedResponse) {
          reject(new Error('Extension did not confirm message'));
          return;
        }

        resolve(confirmedResponse);
      }
    );
  });
}

function createRequestId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }

  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function normalizeError(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

function isConfirmedExtensionResponse(
  response: ExtensionMessageResponse | undefined,
  expectStoredTokens: boolean
) {
  if (response?.ok !== true) {
    return false;
  }

  if (!expectStoredTokens) {
    return true;
  }

  return response.hasAccessToken === true && response.hasRefreshToken === true;
}

function isConfirmedTilRecallSettingsResponse(
  response: ExtensionMessageResponse | undefined
): { settings: TilRecallSettings } | null {
  if (response?.ok !== true || !isTilRecallSettings(response.settings)) {
    return null;
  }

  return { settings: response.settings };
}

function isConfirmedExtensionShortcutsResponse(
  response: ExtensionMessageResponse | undefined
): { shortcuts: ExtensionShortcuts } | null {
  if (response?.ok !== true || !isExtensionShortcuts(response.shortcuts)) {
    return null;
  }

  return { shortcuts: response.shortcuts };
}

function isExtensionShortcuts(value: unknown): value is ExtensionShortcuts {
  if (!value || typeof value !== 'object') return false;
  const maybe = value as Partial<ExtensionShortcuts>;
  return typeof maybe.openSidePanel === 'string'
    && typeof maybe.captureImage === 'string';
}

function isTilRecallSettings(value: unknown): value is TilRecallSettings {
  if (!value || typeof value !== 'object') return false;
  const maybe = value as Partial<TilRecallSettings>;
  return typeof maybe.enabled === 'boolean'
    && typeof maybe.time === 'string'
    && /^([01]\d|2[0-3]):[0-5]\d$/.test(maybe.time);
}

class AuthSyncError extends Error {
  errors: string[];

  constructor(errors: string[]) {
    super('Extension auth sync was not confirmed');
    this.errors = errors;
  }
}
