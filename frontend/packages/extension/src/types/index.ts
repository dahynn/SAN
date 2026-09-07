import type { SourceType } from '@san/shared';

export interface PendingScrap {
  source_type: SourceType;
  source_url: string | null;
  raw_content: string | null;
  image_url: string | null;
  image_preview_url?: string | null;
  image_file_name?: string | null;
  image_mime_type?: string | null;
  image_blob_id?: string | null;
  title: string;
  domain: string;
  favicon: string | null;
}

export interface SavedInsight extends PendingScrap {
  id: string;
  card_id?: string | null;
  created_at: string;
}

export interface TilRecallSettings {
  enabled: boolean;
  time: string;
}

export type MessageType =
  | 'REQUEST_METADATA'
  | 'SCRAP_SELECTION'
  | 'PUSH_TO_SIDEPANEL'
  | 'SAN_AUTH_SYNC'
  | 'SAN_AUTH_CLEAR'
  | 'SAN_AUTH_STATE_CHANGED'
  | 'LOGIN_BRIDGE_TICKET'
  | 'GET_TIL_RECALL_SETTINGS'
  | 'SET_TIL_RECALL_SETTINGS'
  | 'GET_EXTENSION_SHORTCUTS'
  | 'OPEN_EXTENSION_SHORTCUT_SETTINGS';

export interface ExtensionMessage {
  type: MessageType;
  payload?: PendingScrap | TilRecallSettings;
  accessToken?: string;
  refreshToken?: string;
  sessionId?: string;
  clientType?: 'DASHBOARD' | 'EXTENSION';
  ticket?: string;
  isAuthenticated?: boolean;
}
