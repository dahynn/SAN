import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type {
  ArchiveCardTagRelationResponse,
  ArchiveCategoryCardListResponse,
  ArchiveCategoryListResponse,
} from '../types';

export function createArchiveApi(apiClient: AxiosInstance) {
  return {
    getCategories: (): Promise<ArchiveCategoryListResponse> =>
      apiClient
        .get<ApiResponse<ArchiveCategoryListResponse>>('/archives/categories')
        .then((response) => unwrapApiResponse(response.data)),

    getCategoryCards: (categoryId: string): Promise<ArchiveCategoryCardListResponse> =>
      apiClient
        .get<ApiResponse<ArchiveCategoryCardListResponse>>(`/archives/categories/${categoryId}/cards`)
        .then((response) => unwrapApiResponse(response.data)),

    getCardTagRelations: (cardId: string): Promise<ArchiveCardTagRelationResponse> =>
      apiClient
        .get<ApiResponse<ArchiveCardTagRelationResponse>>(`/archives/cards/${cardId}/tag-relations`)
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type ArchiveApi = ReturnType<typeof createArchiveApi>;
