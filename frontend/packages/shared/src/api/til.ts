import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type {
  TilGenerateRequest,
  TilGenerationJobResponse,
  TilGithubCommitJobResponse,
  TilGithubContributionParams,
  TilGithubContributionResponse,
  TilRecallCardsResponse,
  TilResponse,
  TilSourcesResponse,
  TilUpdateRequest,
} from '../types';

export function createTilApi(apiClient: AxiosInstance) {
  return {
    generate: (payload: TilGenerateRequest): Promise<TilGenerationJobResponse> =>
      apiClient
        .post<ApiResponse<TilGenerationJobResponse>>('/tils', payload)
        .then((response) => unwrapApiResponse(response.data)),

    getByDate: (date: string): Promise<TilResponse[]> =>
      apiClient
        .get<ApiResponse<TilResponse[]>>('/tils', { params: { date } })
        .then((response) => unwrapApiResponse(response.data)),

    update: (summaryId: string, payload: TilUpdateRequest): Promise<TilResponse> =>
      apiClient
        .patch<ApiResponse<TilResponse>>(`/tils/${summaryId}`, payload)
        .then((response) => unwrapApiResponse(response.data)),

    delete: (summaryId: string): Promise<void> =>
      apiClient
        .delete<ApiResponse<void>>(`/tils/${summaryId}`)
        .then((response) => unwrapApiResponse(response.data)),

    getRecallCards: (summaryId: string): Promise<TilRecallCardsResponse> =>
      apiClient
        .get<ApiResponse<TilRecallCardsResponse>>(`/tils/${summaryId}/recall-cards`)
        .then((response) => unwrapApiResponse(response.data)),

    getSources: (summaryId: string): Promise<TilSourcesResponse> =>
      apiClient
        .get<ApiResponse<TilSourcesResponse>>(`/tils/${summaryId}/source`)
        .then((response) => unwrapApiResponse(response.data)),

    commitToGithub: (summaryId: string): Promise<TilGithubCommitJobResponse> =>
      apiClient
        .post<ApiResponse<TilGithubCommitJobResponse>>(`/til/${summaryId}/github-commit`)
        .then((response) => unwrapApiResponse(response.data)),

    getGithubContributions: (params?: TilGithubContributionParams): Promise<TilGithubContributionResponse> =>
      apiClient
        .get<ApiResponse<TilGithubContributionResponse>>('/til/github-commits/contributions', { params })
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type TilApi = ReturnType<typeof createTilApi>;
