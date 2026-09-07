import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';

export interface StatisticsOverview {
  totalKnowledgeCardCount: number;
  todayKnowledgeCardCount: number;
  totalTilCount: number;
}

export function createStatisticsApi(apiClient: AxiosInstance) {
  return {
    getOverview: (): Promise<StatisticsOverview> =>
      apiClient
        .get<ApiResponse<StatisticsOverview>>('/statistics/overview')
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type StatisticsApi = ReturnType<typeof createStatisticsApi>;
