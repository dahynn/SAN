import {
  createApiClient,
  createAsyncJobsApi,
  createArchiveApi,
  createAuthApi,
  createCardsApi,
  createFeedbackApi,
  createGithubApi,
  createRecallApi,
  createSearchApi,
  createScrapsApi,
  createStatisticsApi,
  createTilApi,
  type AuthTokens,
} from '@san/shared';
import { authTokenStorage as localAuthTokenStorage } from './tokenStorage';

const defaultBaseURL = import.meta.env.PROD
  ? '/api'
  : 'http://localhost:8080/api';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? defaultBaseURL;
const githubAuthBaseURL = import.meta.env.VITE_GITHUB_AUTH_API_BASE_URL
  ?? baseURL;

const tokenProvider = {
  getToken: localAuthTokenStorage.getToken,
  getRefreshToken: localAuthTokenStorage.getRefreshToken,
  getAccessTokenExpiresAt: localAuthTokenStorage.getAccessTokenExpiresAt,
  getUsername: localAuthTokenStorage.getUsername,
  setUsername: localAuthTokenStorage.setUsername,
  setTokens: async (tokens: AuthTokens) => {
    await localAuthTokenStorage.setTokens(tokens);
  },
  clearToken: async () => {
    await localAuthTokenStorage.clearToken();
  },
};

const apiClient = createApiClient(baseURL, tokenProvider);
const githubAuthApiClient = createApiClient(githubAuthBaseURL, tokenProvider);

export const authApi = createAuthApi(apiClient);
export const githubAuthApi = createAuthApi(githubAuthApiClient);
export const githubApi = createGithubApi(apiClient);
export const feedbackApi = createFeedbackApi(apiClient);
export const searchApi = createSearchApi(apiClient);
export const scrapsApi = createScrapsApi(apiClient);
export const cardsApi = createCardsApi(apiClient);
export const archiveApi = createArchiveApi(apiClient);
export const tilApi = createTilApi(apiClient);
export const recallApi = createRecallApi(apiClient);
export const asyncJobsApi = createAsyncJobsApi(apiClient);
export const statisticsApi = createStatisticsApi(apiClient);
export const authTokenStorage = tokenProvider;
