// packages/extension/src/content/index.ts
import type { ExtensionMessage, PendingScrap } from '@extension/types/index';
import { extractMetadataFromDocument } from '@extension/content/metadata';

const DEBUG_PREFIX = '[SAN:content]';
const DASHBOARD_MESSAGE_SOURCE = 'SAN_DASHBOARD';
const EXTENSION_MESSAGE_SOURCE = 'SAN_EXTENSION';
const AUTH_SYNC_MESSAGE = 'SAN_AUTH_SYNC';
const AUTH_CLEAR_MESSAGE = 'SAN_AUTH_CLEAR';
const LOGIN_BRIDGE_TICKET_MESSAGE = 'LOGIN_BRIDGE_TICKET';
const GET_TIL_RECALL_SETTINGS_MESSAGE = 'GET_TIL_RECALL_SETTINGS';
const SET_TIL_RECALL_SETTINGS_MESSAGE = 'SET_TIL_RECALL_SETTINGS';
const GET_EXTENSION_SHORTCUTS_MESSAGE = 'GET_EXTENSION_SHORTCUTS';
const OPEN_EXTENSION_SHORTCUT_SETTINGS_MESSAGE = 'OPEN_EXTENSION_SHORTCUT_SETTINGS';
const isDebug = import.meta.env.DEV;

const allowedDashboardOrigins = new Set([
  'http://localhost:5173',
  'http://localhost:5174',
  'https://k14a309.p.ssafy.io',
]);

function debugLog(message: string, data?: unknown) {
  if (!isDebug) return;
  if (data === undefined) {
    console.debug(DEBUG_PREFIX, message);
    return;
  }
  console.debug(DEBUG_PREFIX, message, data);
}

debugLog('content script injected', { url: location.href });

function extractMetadata(): PendingScrap {
  return extractMetadataFromDocument(document, location, debugLog);
}

function isDashboardBridgeMessage(value: unknown): value is { source: string; requestId?: string; payload: unknown } {
  if (!value || typeof value !== 'object') return false;
  const maybe = value as { source?: unknown; payload?: unknown };
  if (maybe.source !== DASHBOARD_MESSAGE_SOURCE) return false;

  const payload = maybe.payload;
  if (!payload || typeof payload !== 'object') return false;
  const bridgeMessage = payload as { type?: unknown };
  return (
    bridgeMessage.type === AUTH_SYNC_MESSAGE
    || bridgeMessage.type === AUTH_CLEAR_MESSAGE
    || bridgeMessage.type === LOGIN_BRIDGE_TICKET_MESSAGE
    || bridgeMessage.type === GET_TIL_RECALL_SETTINGS_MESSAGE
    || bridgeMessage.type === SET_TIL_RECALL_SETTINGS_MESSAGE
    || bridgeMessage.type === GET_EXTENSION_SHORTCUTS_MESSAGE
    || bridgeMessage.type === OPEN_EXTENSION_SHORTCUT_SETTINGS_MESSAGE
  );
}

chrome.runtime.onMessage.addListener((message: ExtensionMessage, _sender, sendResponse) => {
  debugLog('runtime message received', message);
  if (message.type === 'REQUEST_METADATA') {
    sendResponse(extractMetadata());
  }
});

window.addEventListener('message', (event) => {
  if (event.source !== window || !allowedDashboardOrigins.has(event.origin)) {
    return;
  }

  if (!isDashboardBridgeMessage(event.data)) {
    return;
  }

  debugLog('dashboard bridge message relayed to background', {
    origin: event.origin,
    type: (event.data.payload as { type?: unknown }).type,
  });

  chrome.runtime
    .sendMessage(event.data.payload)
    .then((response) => {
      postBridgeResponse(event.origin, event.data.requestId, response);
    })
    .catch((error) => {
      console.warn(DEBUG_PREFIX, 'failed to relay dashboard bridge message', error);
      postBridgeResponse(event.origin, event.data.requestId, { ok: false });
    });
});

function postBridgeResponse(origin: string, requestId: string | undefined, response: unknown) {
  if (!requestId) return;

  window.postMessage(
    {
      source: EXTENSION_MESSAGE_SOURCE,
      requestId,
      response,
    },
    origin
  );
}
