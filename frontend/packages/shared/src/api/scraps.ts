import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type { CreateScrapRequest, CreateScrapResponse, Scrap } from '../types';

export function createScrapsApi(apiClient: AxiosInstance) {
  return {
    create: (payload: CreateScrapRequest): Promise<CreateScrapResponse> =>
      apiClient
        .post<ApiResponse<CreateScrapResponse>>('/scraps', payload)
        .then((response) => unwrapApiResponse(response.data)),

    getById: (scrapId: string): Promise<Scrap> =>
      apiClient
        .get<ApiResponse<Scrap>>(`/scraps/${scrapId}`)
        .then((response) => unwrapApiResponse(response.data)),

    delete: (scrapId: string): Promise<void> =>
      apiClient.delete<ApiResponse<void>>(`/scraps/${scrapId}`).then(() => undefined),
  };
}

export type ScrapsApi = ReturnType<typeof createScrapsApi>;
