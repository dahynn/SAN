import type { BaseEntity } from './common';

export type SourceType = 'LINK' | 'TEXT' | 'IMAGE';
export type ScrapOriginStatus = 'CREATED' | 'EXISTING';
export type ScrapRefineStatus = 'REFINE_IN_PROGRESS' | 'REFINE_COMPLETED';
export type ScrapCardCreationStatus = 'ANALYSIS_IN_PROGRESS' | 'CARD_READY';

export interface Scrap extends BaseEntity {
  scrapId: string;
  sourceType: SourceType;
  sourceUrl: string | null;
  rawContent: string | null;
  imageUrl: string | null;
  imageObjectKey: string | null;
  ai_status?: AiStatus;
}

export interface CreateScrapRequest {
  sourceUrl?: string | null;
  rawContent: string;
  imageObjectKey?: string | null;
}

export interface CreateScrapResponse extends Scrap {
  analysisJobId?: string | null;
  refineJobId?: string | null;
  cardId?: string | null;
  originStatus: ScrapOriginStatus;
  refineStatus: ScrapRefineStatus;
  cardCreationStatus: ScrapCardCreationStatus;
  duplicated?: boolean;
}

export type AiStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface PendingScrapView {
  source_type: SourceType;
  source_url?: string | null;
  raw_content?: string | null;
  image_url?: string | null;
}
