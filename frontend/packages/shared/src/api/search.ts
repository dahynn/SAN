import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type { SearchParams, SearchResponse } from '../types';

export function createSearchApi(apiClient: AxiosInstance) {
  return {
    search: (params: SearchParams): Promise<SearchResponse> =>
      apiClient
        .get<ApiResponse<SearchResponse>>('/search', { params })
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type SearchApi = ReturnType<typeof createSearchApi>;
