import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type {
  GithubAuthorizeUrlResponse,
  GithubLinkStatus,
  GithubRepository,
  GithubRepositoryConnectRequest,
  GithubStarRecommendationCollectResponse,
  GithubStarRecommendationGenerationResponse,
  GithubStarRecommendationsResponse,
} from '../types';

export function createGithubApi(apiClient: AxiosInstance) {
  return {
    getLinkAuthorizeUrl: (): Promise<string> =>
      apiClient
        .get<ApiResponse<GithubAuthorizeUrlResponse>>('/github/link/authorize-url')
        .then((response) => unwrapApiResponse(response.data).redirectUrl),

    unlinkAccount: (): Promise<void> =>
      apiClient.delete<ApiResponse<void>>('/github/link').then(() => undefined),

    getLinkStatus: (): Promise<GithubLinkStatus> =>
      apiClient
        .get<ApiResponse<GithubLinkStatus>>('/github/link/status')
        .then((response) => unwrapApiResponse(response.data)),

    getRepositories: (): Promise<GithubRepository[]> =>
      apiClient
        .get<ApiResponse<GithubRepository[]>>('/github/repositories')
        .then((response) => unwrapApiResponse(response.data)),

    getStarRecommendations: (): Promise<GithubStarRecommendationsResponse> =>
      apiClient
        .get<ApiResponse<GithubStarRecommendationsResponse>>('/github/star-recommendations')
        .then((response) => unwrapApiResponse(response.data)),

    requestStarRecommendations: async (): Promise<GithubStarRecommendationGenerationResponse> => {
      const response = await apiClient.post<ApiResponse<GithubStarRecommendationGenerationResponse>>('/github/star-recommendations');
      const data = unwrapApiResponse(response.data);

      if (response.status === 202 && 'jobId' in data) {
        return { jobId: data.jobId };
      }

      return { recommendations: (data as GithubStarRecommendationsResponse).recommendations };
    },

    collectStarRecommendation: (recommendationId: string): Promise<GithubStarRecommendationCollectResponse> =>
      apiClient
        .post<ApiResponse<GithubStarRecommendationCollectResponse>>(`/github/star-recommendations/${recommendationId}/collect`)
        .then((response) => unwrapApiResponse(response.data)),

    getConnectedRepositories: (): Promise<GithubRepository[]> =>
      apiClient
        .get<ApiResponse<GithubLinkStatus>>('/github/link/status')
        .then((response) => {
          const status = unwrapApiResponse(response.data);
          return status.connectedRepository ? [status.connectedRepository] : [];
        }),

    connectRepository: (
      payload: GithubRepositoryConnectRequest
    ): Promise<GithubRepository> =>
      apiClient
        .post<ApiResponse<GithubRepository>>('/github/repositories/connect', payload)
        .then((response) => unwrapApiResponse(response.data)),

    disconnectRepository: (repositoryId: number): Promise<void> =>
      apiClient.delete<ApiResponse<void>>(`/github/repositories/${repositoryId}`).then(() => undefined),
  };
}

export type GithubApi = ReturnType<typeof createGithubApi>;
