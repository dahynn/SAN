import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type {
  KnowledgeCardAnalysisJobResponse,
  KnowledgeCardByScrapResponse,
  KnowledgeCardCreateRequest,
  KnowledgeCardDetailResponse,
  KnowledgeCardListResponse,
  KnowledgeCardListParams,
  KnowledgeCardSimilarCardsResponse,
  RefinedContentUpdateRequest,
} from '../types';

export function createCardsApi(apiClient: AxiosInstance) {
  return {
    create: (payload: KnowledgeCardCreateRequest): Promise<KnowledgeCardAnalysisJobResponse> =>
      apiClient
        .post<ApiResponse<KnowledgeCardAnalysisJobResponse>>('/cards', payload)
        .then((response) => unwrapApiResponse(response.data)),

    getAll: (params?: KnowledgeCardListParams): Promise<KnowledgeCardListResponse> =>
      apiClient
        .get<ApiResponse<KnowledgeCardListResponse>>('/cards', { params })
        .then((response) => unwrapApiResponse(response.data)),

    getByScrapId: (scrapId: string): Promise<KnowledgeCardByScrapResponse> =>
      apiClient
        .get<ApiResponse<KnowledgeCardByScrapResponse>>(`/cards/${scrapId}`)
        .then((response) => unwrapApiResponse(response.data)),

    getDetail: (cardId: string): Promise<KnowledgeCardDetailResponse> =>
      apiClient
        .get<ApiResponse<KnowledgeCardDetailResponse>>(`/cards/${cardId}/detail`)
        .then((response) => unwrapApiResponse(response.data)),

    updateRefinedContent: (cardId: string, payload: RefinedContentUpdateRequest): Promise<KnowledgeCardDetailResponse> =>
      apiClient
        .patch<ApiResponse<KnowledgeCardDetailResponse>>(`/cards/${cardId}/refined-content`, payload)
        .then((response) => unwrapApiResponse(response.data)),

    deleteCard: (cardId: string): Promise<void> =>
      apiClient.delete<ApiResponse<void>>(`/cards/${cardId}`).then(() => undefined),

    getSimilarByCardId: (cardId: string): Promise<KnowledgeCardSimilarCardsResponse> =>
      apiClient
        .get<ApiResponse<KnowledgeCardSimilarCardsResponse>>(`/cards/${cardId}/similar-cards`)
        .then((response) => unwrapApiResponse(response.data)),

  };
}

export type CardsApi = ReturnType<typeof createCardsApi>;
