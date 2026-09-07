// packages/extension/src/background/index.ts
import type { ExtensionMessage, PendingScrap, TilRecallSettings } from '@extension/types/index';
import { createLinkScrap, isHttpUrl } from '@extension/utils/scrap';

const DEBUG_PREFIX = '[SAN:background]';
const baseURL = normalizeApiBaseURL(getApiBaseURL());
const dashboardBaseUrl = getDashboardBaseUrl();
const PENDING_STORAGE_KEY = 'san:pending-scrap';
const ACCESS_TOKEN_KEY = 'san_access_token';
const REFRESH_TOKEN_KEY = 'san_refresh_token';
const SESSION_ID_KEY = 'san_session_id';
const CLIENT_TYPE_KEY = 'san_client_type';
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'san_access_token_expires_at';
const TIL_RECALL_SETTINGS_KEY = 'san:til-recall-settings';
const TIL_RECALL_LAST_NOTIFIED_KEY = 'san:til-recall-last-notified-date';
const TIL_RECALL_NOTIFICATION_TARGETS_KEY = 'san:til-recall-notification-targets';
const TIL_RECALL_ALARM_NAME = 'san:til-recall';
const DEFAULT_TIL_RECALL_TIME = '07:00';
const NOTIFICATION_ICON_URL = chrome.runtime.getURL('SAN_LOGO.png');
const TIL_RECALL_OFFSETS = [7, 3, 1] as const;
const AUTH_SYNC_MESSAGE = 'SAN_AUTH_SYNC';
const AUTH_CLEAR_MESSAGE = 'SAN_AUTH_CLEAR';
const AUTH_STATE_CHANGED_MESSAGE = 'SAN_AUTH_STATE_CHANGED';
const LOGIN_BRIDGE_TICKET_MESSAGE = 'LOGIN_BRIDGE_TICKET';
const GET_TIL_RECALL_SETTINGS_MESSAGE = 'GET_TIL_RECALL_SETTINGS';
const SET_TIL_RECALL_SETTINGS_MESSAGE = 'SET_TIL_RECALL_SETTINGS';
const GET_EXTENSION_SHORTCUTS_MESSAGE = 'GET_EXTENSION_SHORTCUTS';
const OPEN_EXTENSION_SHORTCUT_SETTINGS_MESSAGE = 'OPEN_EXTENSION_SHORTCUT_SETTINGS';
const isDebug = import.meta.env.DEV;
let lastFocusedWindowId: number | undefined;

chrome.windows.onFocusChanged.addListener((windowId) => {
  if (windowId !== chrome.windows.WINDOW_ID_NONE) {
    lastFocusedWindowId = windowId;
  }
});

chrome.tabs.onActivated.addListener((activeInfo) => {
  lastFocusedWindowId = activeInfo.windowId;
});

void chrome.windows.getLastFocused().then((window) => {
  lastFocusedWindowId = window.id;
});

function getApiBaseURL() {
  if (import.meta.env.VITE_API_BASE_URL) return import.meta.env.VITE_API_BASE_URL;
  if (!import.meta.env.PROD) return 'http://localhost:8080/api';
  throw new Error('Missing VITE_API_BASE_URL for production extension build');
}

function getDashboardBaseUrl() {
  if (import.meta.env.VITE_DASHBOARD_BASE_URL) {
    return import.meta.env.VITE_DASHBOARD_BASE_URL.replace(/\/$/, '');
  }
  if (!import.meta.env.PROD) return 'http://localhost:5173';
  throw new Error('Missing VITE_DASHBOARD_BASE_URL for production extension build');
}

function normalizeApiBaseURL(value: string) {
  const trimmed = value.replace(/\/$/, '');
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`;
}

interface AuthSyncMessage {
  type: typeof AUTH_SYNC_MESSAGE;
  accessToken?: string;
  refreshToken?: string;
  sessionId?: string;
  clientType?: 'DASHBOARD' | 'EXTENSION';
  expiresIn?: number;
}

interface AuthClearMessage {
  type: typeof AUTH_CLEAR_MESSAGE;
}

interface LoginBridgeTicketMessage {
  type: typeof LOGIN_BRIDGE_TICKET_MESSAGE;
  ticket?: string;
}

interface ApiResponse<T> {
  ok: boolean;
  data?: T;
  error?: string;
  message?: string;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  sessionId: string;
  expiresIn?: number;
}

interface TilResponse {
  summaryId: string;
  targetDate: string;
  title: string | null;
  content: string | null;
  createdAt: string;
  updatedAt: string;
}

function isAuthSyncMessage(message: unknown): message is AuthSyncMessage {
  if (!message || typeof message !== 'object') return false;
  const maybe = message as Partial<AuthSyncMessage>;
  return maybe.type === AUTH_SYNC_MESSAGE;
}

function isAuthClearMessage(message: unknown): message is AuthClearMessage {
  if (!message || typeof message !== 'object') return false;
  const maybe = message as Partial<AuthClearMessage>;
  return maybe.type === AUTH_CLEAR_MESSAGE;
}

function isLoginBridgeTicketMessage(message: unknown): message is LoginBridgeTicketMessage {
  if (!message || typeof message !== 'object') return false;
  const maybe = message as Partial<LoginBridgeTicketMessage>;
  return maybe.type === LOGIN_BRIDGE_TICKET_MESSAGE;
}

function isGetTilRecallSettingsMessage(message: unknown): message is { type: typeof GET_TIL_RECALL_SETTINGS_MESSAGE } {
  if (!message || typeof message !== 'object') return false;
  const maybe = message as { type?: unknown };
  return maybe.type === GET_TIL_RECALL_SETTINGS_MESSAGE;
}

function isSetTilRecallSettingsMessage(
  message: unknown,
): message is { type: typeof SET_TIL_RECALL_SETTINGS_MESSAGE; payload: TilRecallSettings } {
  if (!message || typeof message !== 'object') return false;
  const maybe = message as { type?: unknown; payload?: unknown };
  return maybe.type === SET_TIL_RECALL_SETTINGS_MESSAGE && isTilRecallSettings(maybe.payload);
}

function isGetExtensionShortcutsMessage(
  message: unknown,
): message is { type: typeof GET_EXTENSION_SHORTCUTS_MESSAGE } {
  if (!message || typeof message !== 'object') return false;
  const maybe = message as { type?: unknown };
  return maybe.type === GET_EXTENSION_SHORTCUTS_MESSAGE;
}

function isOpenExtensionShortcutSettingsMessage(
  message: unknown,
): message is { type: typeof OPEN_EXTENSION_SHORTCUT_SETTINGS_MESSAGE } {
  if (!message || typeof message !== 'object') return false;
  const maybe = message as { type?: unknown };
  return maybe.type === OPEN_EXTENSION_SHORTCUT_SETTINGS_MESSAGE;
}

function isTilRecallSettings(value: unknown): value is TilRecallSettings {
  if (!value || typeof value !== 'object') return false;
  const maybe = value as Partial<TilRecallSettings>;
  return typeof maybe.enabled === 'boolean' && isValidTime(maybe.time);
}

function isValidTime(value: unknown): value is string {
  return typeof value === 'string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(value);
}

function debugLog(message: string, data?: unknown) {
  if (!isDebug) return;
  if (data === undefined) {
    console.debug(DEBUG_PREFIX, message);
    return;
  }
  console.debug(DEBUG_PREFIX, message, data);
}

debugLog('service worker loaded');

chrome.runtime.onInstalled.addListener(() => {
  debugLog('onInstalled');

  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: 'san-scrap-page',
      title: 'SAN: Save this page',
      contexts: ['page'],
    });

    chrome.contextMenus.create({
      id: 'san-scrap-selection',
      title: 'SAN: Save selected text',
      contexts: ['selection'],
    });
  });

  void ensureTilRecallSettings();
  void scheduleNextTilRecallAlarm();
});

chrome.runtime.onStartup.addListener(() => {
  void ensureTilRecallSettings();
  void scheduleNextTilRecallAlarm();
});

chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name !== TIL_RECALL_ALARM_NAME) return;

  void runTilRecallCheck()
    .catch((error) => {
      console.error(DEBUG_PREFIX, 'failed to run TIL recall check', error);
    })
    .finally(() => {
      void scheduleNextTilRecallAlarm();
    });
});

chrome.notifications.onClicked.addListener((notificationId) => {
  void openTilRecallNotification(notificationId);
});

chrome.storage.onChanged.addListener((changes, areaName) => {
  if (areaName !== 'local' || !changes[TIL_RECALL_SETTINGS_KEY]) return;
  void scheduleNextTilRecallAlarm();
});

chrome.action.onClicked.addListener((tab) => {
  debugLog('action clicked', { tabId: tab.id, url: tab.url });
  if (!tab.id) return;
  chrome.sidePanel.open({ tabId: tab.id });
});

chrome.commands.onCommand.addListener(async (command) => {
  debugLog('command received', command);

  if (command === 'capture_image') {
    try {
      if (lastFocusedWindowId !== undefined) {
        await chrome.sidePanel.open({ windowId: lastFocusedWindowId });
      }

      const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
      if (!tab?.id) return;

      let dataUrl: string;
      try {
        dataUrl = await chrome.tabs.captureVisibleTab();
      } catch (error) {
        console.error(DEBUG_PREFIX, 'failed to capture visible tab for capture_image', {
          error,
          tabId: tab.id,
          url: tab.url,
        });
        throw error;
      }

      let metadata: PendingScrap;
      try {
        metadata = await chrome.tabs.sendMessage<ExtensionMessage, PendingScrap>(tab.id, { type: 'REQUEST_METADATA' });
      } catch (error) {
        metadata = {
          source_type: 'IMAGE',
          source_url: tab.url ?? null,
          raw_content: tab.title ?? 'Captured image',
          image_url: null,
          title: tab.title ?? 'Captured image',
          domain: tab.url ? new URL(tab.url).hostname : '',
          favicon: tab.favIconUrl ?? null,
        };
      }

      pushToSidePanel({
        ...metadata,
        source_type: 'IMAGE',
        image_preview_url: dataUrl,
        raw_content: tab.title ?? 'Captured image',
      });
    } catch (error) {
      console.error(DEBUG_PREFIX, 'failed to capture image or open side panel', error);
    }
  }
});

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  debugLog('context menu clicked', { menuItemId: info.menuItemId, tabId: tab?.id, url: tab?.url });
  if (!tab?.id) return;

  try {
    await chrome.sidePanel.open({ tabId: tab.id });
  } catch (error) {
    console.error(DEBUG_PREFIX, 'failed to open side panel', error);
    return;
  }

  let metadata: PendingScrap;
  try {
    metadata = await chrome.tabs.sendMessage<ExtensionMessage, PendingScrap>(tab.id, { type: 'REQUEST_METADATA' });
    debugLog('received metadata from content script', metadata);
  } catch (error) {
    console.error(
      DEBUG_PREFIX,
      'failed to request metadata. Check that the content script is injected into this page.',
      error,
    );
    return;
  }

  if (info.menuItemId === 'san-scrap-page') {
    pushToSidePanel(metadata);
  } else if (info.menuItemId === 'san-scrap-selection' && info.selectionText) {
    const selectionText = info.selectionText.trim();
    pushToSidePanel(
      isHttpUrl(selectionText)
        ? createLinkScrap(selectionText)
        : {
            ...metadata,
            source_type: 'TEXT',
            raw_content: info.selectionText,
          }
    );
  }
});

chrome.runtime.onMessage.addListener((message: ExtensionMessage, sender, sendResponse) => {
  debugLog('runtime message received', { message, tabId: sender.tab?.id, url: sender.tab?.url });
  if (message.type === 'SCRAP_SELECTION' && sender.tab?.id && message.payload) {
    pushToSidePanel(message.payload as PendingScrap);
    return;
  }

  if (isGetTilRecallSettingsMessage(message)) {
    getTilRecallSettings()
      .then((settings) => {
        sendResponse({ ok: true, settings });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to read TIL recall settings', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isSetTilRecallSettingsMessage(message)) {
    chrome.storage.local
      .set({ [TIL_RECALL_SETTINGS_KEY]: message.payload })
      .then(() => {
        sendResponse({ ok: true, settings: message.payload });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to save TIL recall settings', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isGetExtensionShortcutsMessage(message)) {
    getExtensionShortcuts()
      .then((shortcuts) => {
        sendResponse({ ok: true, shortcuts });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to read extension shortcuts', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isOpenExtensionShortcutSettingsMessage(message)) {
    openExtensionShortcutSettings()
      .then(() => {
        sendResponse({ ok: true });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to open extension shortcut settings', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isAuthClearMessage(message)) {
    clearAuthTokens()
      .then(() => {
        debugLog('auth tokens cleared');
        sendResponse({ ok: true, hasAccessToken: false, hasRefreshToken: false });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to clear auth tokens', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isAuthSyncMessage(message)) {
    syncAuthTokens(message)
      .then((authState) => {
        debugLog('auth tokens synced');
        sendResponse({ ok: true, ...authState });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to sync auth tokens', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isLoginBridgeTicketMessage(message)) {
    exchangeAndSyncBridgeToken(message)
      .then((authState) => {
        debugLog('bridge ticket exchanged');
        sendResponse({ ok: true, ...authState });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to exchange bridge ticket', error);
        sendResponse({ ok: false });
      });
    return true;
  }
});

chrome.runtime.onMessageExternal.addListener((message, sender, sendResponse) => {
  debugLog('external message received', { message, origin: sender.origin, url: sender.url });

  if (isGetTilRecallSettingsMessage(message)) {
    getTilRecallSettings()
      .then((settings) => {
        sendResponse({ ok: true, settings });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to read TIL recall settings from dashboard', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isSetTilRecallSettingsMessage(message)) {
    chrome.storage.local
      .set({ [TIL_RECALL_SETTINGS_KEY]: message.payload })
      .then(() => {
        sendResponse({ ok: true, settings: message.payload });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to save TIL recall settings from dashboard', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isGetExtensionShortcutsMessage(message)) {
    getExtensionShortcuts()
      .then((shortcuts) => {
        sendResponse({ ok: true, shortcuts });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to read extension shortcuts from dashboard', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (isOpenExtensionShortcutSettingsMessage(message)) {
    openExtensionShortcutSettings()
      .then(() => {
        sendResponse({ ok: true });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to open extension shortcut settings from dashboard', error);
        sendResponse({ ok: false });
      });
    return true;
  }

  if (!isAuthSyncMessage(message)) {
    if (isLoginBridgeTicketMessage(message)) {
      exchangeAndSyncBridgeToken(message)
        .then((authState) => {
          debugLog('bridge ticket exchanged from dashboard');
          sendResponse({ ok: true, ...authState });
        })
        .catch((error) => {
          console.error(DEBUG_PREFIX, 'failed to exchange bridge ticket', error);
          sendResponse({ ok: false });
        });

      return true;
    }

    if (!isAuthClearMessage(message)) {
      return;
    }

    clearAuthTokens()
      .then(() => {
        debugLog('auth tokens cleared from dashboard');
        sendResponse({ ok: true, hasAccessToken: false, hasRefreshToken: false });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to clear auth tokens', error);
        sendResponse({ ok: false });
      });

    return true;
  }

  if (!message.accessToken || !message.refreshToken) {
    sendResponse({ ok: false });
    return;
  }

  syncAuthTokens(message)
    .then((authState) => {
      debugLog('auth tokens synced from dashboard');
      sendResponse({ ok: true, ...authState });
    })
    .catch((error) => {
      console.error(DEBUG_PREFIX, 'failed to sync auth tokens', error);
      sendResponse({ ok: false });
    });

  return true;
});

async function ensureTilRecallSettings() {
  const stored = await chrome.storage.local.get(TIL_RECALL_SETTINGS_KEY);
  const settings = stored[TIL_RECALL_SETTINGS_KEY];

  if (isTilRecallSettings(settings)) {
    return settings;
  }

  const defaultSettings = getDefaultTilRecallSettings();
  await chrome.storage.local.set({ [TIL_RECALL_SETTINGS_KEY]: defaultSettings });
  return defaultSettings;
}

async function getTilRecallSettings(): Promise<TilRecallSettings> {
  const stored = await chrome.storage.local.get(TIL_RECALL_SETTINGS_KEY);
  const settings = stored[TIL_RECALL_SETTINGS_KEY];

  if (isTilRecallSettings(settings)) {
    return settings;
  }

  return getDefaultTilRecallSettings();
}

function getDefaultTilRecallSettings(): TilRecallSettings {
  return {
    enabled: true,
    time: DEFAULT_TIL_RECALL_TIME,
  };
}

async function scheduleNextTilRecallAlarm() {
  const settings = await getTilRecallSettings();
  await chrome.alarms.clear(TIL_RECALL_ALARM_NAME);

  if (!settings.enabled) {
    debugLog('TIL recall alarm disabled');
    return;
  }

  const nextAlarmAt = getNextAlarmTime(settings.time);
  chrome.alarms.create(TIL_RECALL_ALARM_NAME, {
    when: nextAlarmAt.getTime(),
  });
  debugLog('TIL recall alarm scheduled', { time: settings.time, nextAlarmAt: nextAlarmAt.toISOString() });
}

async function runTilRecallCheck() {
  const settings = await getTilRecallSettings();
  if (!settings.enabled) return;

  const today = formatLocalDate(new Date());
  const stored = await chrome.storage.local.get(TIL_RECALL_LAST_NOTIFIED_KEY);
  if (stored[TIL_RECALL_LAST_NOTIFIED_KEY] === today) {
    debugLog('TIL recall already notified today', today);
    return;
  }

  const accessToken = await getValidAccessToken();
  if (!accessToken) {
    debugLog('TIL recall skipped because access token is missing');
    return;
  }

  const target = await findTilRecallTarget(accessToken);
  if (!target) {
    debugLog('TIL recall skipped because target TIL was not found');
    return;
  }

  await createTilRecallNotification(target.targetDate, target.til, target.offset);
}

async function findTilRecallTarget(accessToken: string) {
  for (const offset of TIL_RECALL_OFFSETS) {
    const targetDate = getDateBefore(offset);
    const tils = await getTilsByDate(accessToken, targetDate);

    if (tils.length > 0) {
      return {
        offset,
        targetDate,
        til: tils[0],
      };
    }
  }

  return null;
}

async function getTilsByDate(accessToken: string, date: string): Promise<TilResponse[]> {
  let response = await fetch(`${baseURL}/tils?date=${encodeURIComponent(date)}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
  if (response.status === 401) {
    const refreshedAccessToken = await reissueStoredTokens();
    if (!refreshedAccessToken) {
      throw new Error('Missing valid auth tokens');
    }

    response = await fetch(`${baseURL}/tils?date=${encodeURIComponent(date)}`, {
      headers: {
        Authorization: `Bearer ${refreshedAccessToken}`,
      },
    });
  }

  const payload = await parseApiResponse<TilResponse[]>(
    response,
    `Failed to parse TIL response for ${date}`,
  );

  if (!response.ok || !payload.ok || !payload.data) {
    if (response.status === 401) {
      await clearAuthTokens();
    }
    throw new Error(payload.message ?? payload.error ?? `Failed to load TIL for ${date}`);
  }

  return payload.data;
}

async function createTilRecallNotification(targetDate: string, til: TilResponse, offset: number) {
  const notifications = (
    chrome as typeof chrome & { notifications?: typeof chrome.notifications }
  ).notifications;

  if (!notifications?.create) {
    console.error(
      DEBUG_PREFIX,
      'chrome.notifications API is unavailable. Check the loaded extension manifest has the notifications permission and reload the extension.',
    );
    return;
  }

  const notificationId = `san-til-recall-${targetDate}`;
  await saveNotificationTarget(notificationId, targetDate);

  notifications.create(
    notificationId,
    {
      type: 'basic',
      iconUrl: NOTIFICATION_ICON_URL,
      title: '맞다, TIL 보러 가야지!',
      message: til.title
        ? `${offset}일 전 <${til.title}> 을 다시 살펴볼 시간이에요.`
        : `${offset}일 전 TIL을 다시 살펴볼 시간이에요.`,
      priority: 2,
    },
    (createdNotificationId?: string) => {
      if (chrome.runtime.lastError) {
        console.error(DEBUG_PREFIX, 'notification failed', chrome.runtime.lastError.message);
        void removeNotificationTarget(notificationId);
        return;
      }
      if (createdNotificationId) {
        void chrome.storage.local.set({ [TIL_RECALL_LAST_NOTIFIED_KEY]: formatLocalDate(new Date()) });
      }
      debugLog('notification created', createdNotificationId);
    },
  );
}

async function saveNotificationTarget(notificationId: string, targetDate: string) {
  const stored = await chrome.storage.local.get(TIL_RECALL_NOTIFICATION_TARGETS_KEY);
  const targets = typeof stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] === 'object'
    && stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] !== null
    ? stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] as Record<string, string>
    : {};

  await chrome.storage.local.set({
    [TIL_RECALL_NOTIFICATION_TARGETS_KEY]: {
      ...targets,
      [notificationId]: targetDate,
    },
  });
}

async function removeNotificationTarget(notificationId: string) {
  const stored = await chrome.storage.local.get(TIL_RECALL_NOTIFICATION_TARGETS_KEY);
  const targets = typeof stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] === 'object'
    && stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] !== null
    ? stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] as Record<string, string>
    : {};

  if (!targets[notificationId]) return;

  const nextTargets = { ...targets };
  delete nextTargets[notificationId];
  await chrome.storage.local.set({ [TIL_RECALL_NOTIFICATION_TARGETS_KEY]: nextTargets });
}

async function openTilRecallNotification(notificationId: string) {
  const stored = await chrome.storage.local.get(TIL_RECALL_NOTIFICATION_TARGETS_KEY);
  const targets = typeof stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] === 'object'
    && stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] !== null
    ? stored[TIL_RECALL_NOTIFICATION_TARGETS_KEY] as Record<string, string>
    : {};
  const targetDate = targets[notificationId];
  if (!targetDate) return;

  const nextTargets = { ...targets };
  delete nextTargets[notificationId];
  await chrome.storage.local.set({ [TIL_RECALL_NOTIFICATION_TARGETS_KEY]: nextTargets });
  await chrome.notifications.clear(notificationId);
  await chrome.tabs.create({ url: `${dashboardBaseUrl}/til?date=${encodeURIComponent(targetDate)}` });
}

async function openExtensionShortcutSettings() {
  await chrome.tabs.create({ url: 'chrome://extensions/shortcuts' });
}

async function getExtensionShortcuts() {
  const commands = await chrome.commands.getAll();
  const commandByName = Object.fromEntries(commands.map((command) => [command.name, command.shortcut ?? '']));

  return {
    openSidePanel: commandByName._execute_action ?? '',
    captureImage: commandByName.capture_image ?? '',
  };
}

function getNextAlarmTime(time: string) {
  const [hours, minutes] = time.split(':').map(Number);
  const next = new Date();
  next.setHours(hours, minutes, 0, 0);

  if (next.getTime() <= Date.now()) {
    next.setDate(next.getDate() + 1);
  }

  return next;
}

function getDateBefore(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return formatLocalDate(date);
}

function formatLocalDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

async function getStoredAccessToken() {
  const stored = await chrome.storage.local.get(ACCESS_TOKEN_KEY);
  return typeof stored[ACCESS_TOKEN_KEY] === 'string' ? stored[ACCESS_TOKEN_KEY] : null;
}

async function getStoredRefreshToken() {
  const stored = await chrome.storage.local.get(REFRESH_TOKEN_KEY);
  return typeof stored[REFRESH_TOKEN_KEY] === 'string' ? stored[REFRESH_TOKEN_KEY] : null;
}

async function getValidAccessToken() {
  return getStoredAccessToken();
}

async function reissueStoredTokens() {
  const refreshToken = await getStoredRefreshToken();
  if (!refreshToken) {
    await clearAuthTokens();
    return null;
  }

  const response = await fetch(`${baseURL}/auth/reissue`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });
  const payload = await parseApiResponse<TokenResponse>(
    response,
    'Failed to parse token reissue response',
  );

  if (!response.ok || !payload.ok || !payload.data) {
    if (response.status === 401 || response.status === 403) {
      await clearAuthTokens();
    }
    return null;
  }

  await chrome.storage.local.set({
    [ACCESS_TOKEN_KEY]: payload.data.accessToken,
    [REFRESH_TOKEN_KEY]: payload.data.refreshToken,
    [CLIENT_TYPE_KEY]: 'EXTENSION',
    ...(payload.data.sessionId ? { [SESSION_ID_KEY]: payload.data.sessionId } : {}),
  });
  await updateAccessTokenExpiresAt(payload.data.expiresIn);

  return payload.data.accessToken;
}

async function exchangeAndSyncBridgeToken(message: LoginBridgeTicketMessage) {
  if (!message.ticket) {
    throw new Error('Missing bridge ticket');
  }

  const response = await fetch(`${baseURL}/auth/bridge/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ticket: message.ticket }),
  });

  const payload = await parseApiResponse<TokenResponse>(
    response,
    'Failed to parse bridge token response',
  );

  if (!response.ok || !payload.ok || !payload.data) {
    throw new Error(payload.message ?? payload.error ?? 'Bridge token exchange failed');
  }

  return syncAuthTokens({
    type: AUTH_SYNC_MESSAGE,
    accessToken: payload.data.accessToken,
    refreshToken: payload.data.refreshToken,
    sessionId: payload.data.sessionId,
    clientType: 'EXTENSION',
    expiresIn: payload.data.expiresIn,
  });
}

async function syncAuthTokens(message: AuthSyncMessage) {
  if (!message.accessToken || !message.refreshToken) {
    throw new Error('Missing auth tokens');
  }

  await chrome.storage.local.set({
    [ACCESS_TOKEN_KEY]: message.accessToken,
    [REFRESH_TOKEN_KEY]: message.refreshToken,
    [CLIENT_TYPE_KEY]: message.clientType ?? 'EXTENSION',
    ...(message.sessionId ? { [SESSION_ID_KEY]: message.sessionId } : {}),
  });
  await updateAccessTokenExpiresAt(message.expiresIn);
  notifyAuthStateChanged(true);
  void ensureTilRecallSettings();
  void scheduleNextTilRecallAlarm();

  return readStoredAuthState();
}

async function updateAccessTokenExpiresAt(expiresIn?: number) {
  if (expiresIn) {
    await chrome.storage.local.set({
      [ACCESS_TOKEN_EXPIRES_AT_KEY]: String(Date.now() + expiresIn * 1000),
    });
    return;
  }

  await chrome.storage.local.remove(ACCESS_TOKEN_EXPIRES_AT_KEY);
}

async function clearAuthTokens() {
  await chrome.storage.local.remove([
    ACCESS_TOKEN_KEY,
    REFRESH_TOKEN_KEY,
    SESSION_ID_KEY,
    CLIENT_TYPE_KEY,
    ACCESS_TOKEN_EXPIRES_AT_KEY,
  ]);
  notifyAuthStateChanged(false);
}

async function readStoredAuthState() {
  const stored = await chrome.storage.local.get([
    ACCESS_TOKEN_KEY,
    REFRESH_TOKEN_KEY,
    SESSION_ID_KEY,
    CLIENT_TYPE_KEY,
  ]);

  return {
    hasAccessToken: typeof stored[ACCESS_TOKEN_KEY] === 'string',
    hasRefreshToken: typeof stored[REFRESH_TOKEN_KEY] === 'string',
    hasSessionId: typeof stored[SESSION_ID_KEY] === 'string',
    clientType: typeof stored[CLIENT_TYPE_KEY] === 'string' ? stored[CLIENT_TYPE_KEY] : null,
  };
}

function notifyAuthStateChanged(isAuthenticated: boolean) {
  chrome.runtime
    .sendMessage({
      type: AUTH_STATE_CHANGED_MESSAGE,
      isAuthenticated,
    })
    .catch((error) => {
      debugLog('auth state change broadcast skipped', error);
    });
}

async function parseApiResponse<T>(response: Response, fallbackMessage: string): Promise<ApiResponse<T>> {
  const text = await response.text().catch((error) => {
    console.error(DEBUG_PREFIX, 'failed to read API response body', {
      error,
      status: response.status,
      url: response.url,
    });
    return '';
  });

  if (!text.trim()) {
    console.error(DEBUG_PREFIX, 'empty API response body', {
      status: response.status,
      url: response.url,
    });
    return { ok: false, message: fallbackMessage };
  }

  try {
    return JSON.parse(text) as ApiResponse<T>;
  } catch (error) {
    console.error(DEBUG_PREFIX, 'failed to parse API response JSON', {
      error,
      status: response.status,
      url: response.url,
    });
    return { ok: false, message: fallbackMessage };
  }
}

function pushToSidePanel(payload: PendingScrap) {
  debugLog('push to side panel', payload);
  chrome.storage.local.set({ [PENDING_STORAGE_KEY]: payload }).catch((error) => {
    console.error(DEBUG_PREFIX, 'failed to persist pending scrap', error);
  });
  chrome.runtime.sendMessage({ type: 'PUSH_TO_SIDEPANEL', payload }).catch((error) => {
    debugLog('side panel is not ready to receive messages yet', error);
  });
}
