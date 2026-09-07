import type { AiStatus, SourceType } from './scraps';

export interface TagResponse {
  tagId: string;
  tagName: string;
}

export interface CategoryResponse {
  categoryId: string;
  categoryName: string;
}

export interface KnowledgeCardResponse {
  cardId: string;
  title: string;
  summary: string | null;
  category: CategoryResponse | null;
  tags: TagResponse[];
  createdAt: string;
}

export interface KnowledgeCardListResponse {
  cards: KnowledgeCardResponse[];
}

export interface KnowledgeCardListParams {
  tag?: string;
  fromDate?: string;
  toDate?: string;
  limit?: number;
}

export interface KnowledgeCardCreateRequest {
  scrapId: string;
}

export interface RefinedContentUpdateRequest {
  refinedContent: string;
}

export interface KnowledgeCardByScrapResponse {
  scrapId: string;
  cardId: string;
}

export interface KnowledgeCardAnalysisJobResponse {
  jobId: string;
}

export interface KnowledgeCardSimilarCardsResponse {
  similarCards: KnowledgeCardResponse[];
}

export interface KnowledgeCardDetailResponse {
  title: string;
  categoryId: string;
  categoryName: string;
  sourceType: SourceType;
  sourceContent: string | null;
  refinedContent: string | null;
  summary: string | null;
  tags: string[];
  collectedAt: string | null;
}

export interface Tag {
  tag_id: string;
  name: string;
}

export interface KnowledgeCardView {
  card_id: string;
  scrap_id?: string;
  category_id?: string;
  title: string;
  summary: string | null;
  tags: Tag[];
  source_url?: string | null;
  source_type: SourceType;
  ai_status: AiStatus;
  category_name: string | null;
  createdAt?: string;
  created_at: string;
  updated_at?: string | null;
  is_deleted?: boolean | null;
}

export interface SearchParams {
  keyword: string;
  tag?: string;
  category?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface SearchCardResult {
  cardId: string;
  title: string;
  summary: string | null;
}

export interface SearchResponse {
  keyword: string;
  page: number;
  size: number;
  totalCount: number;
  hasNext: boolean;
  results: SearchCardResult[];
}

export function toKnowledgeCardView(card: KnowledgeCardResponse): KnowledgeCardView {
  return {
    card_id: card.cardId,
    title: card.title,
    summary: card.summary,
    tags: card.tags.map((tag) => ({
      tag_id: tag.tagId,
      name: tag.tagName,
    })),
    source_type: 'TEXT',
    ai_status: 'COMPLETED',
    category_name: card.category?.categoryName ?? null,
    createdAt: card.createdAt,
    created_at: card.createdAt,
  };
}
