// packages/shared/src/api/index.ts
export { createApiClient, getApiErrorMessage, unwrapApiResponse } from './client';
export type { ApiResponse, AuthTokens, TokenProvider, TokenResponse } from './client';
export { createAuthApi } from './auth';
export type {
  AuthApi,
  AuthSession,
  AuthSessionsResponse,
  BridgeTicketResponse,
  BridgeTokenRequest,
  ClientType,
  GithubLoginRequest,
  GithubTokenExchangeRequest,
  LoginRequest,
  ReissueRequest,
  SignupRequest,
  SignupResponse,
  WithdrawRequest,
} from './auth';
export { createGithubApi } from './github';
export type {
  GithubApi,
} from './github';
export type {
  GithubAuthorizeUrlResponse,
  GithubLinkStatus,
  GithubRepository,
  GithubRepositoryConnectRequest,
  GithubStarRecommendation,
  GithubStarRecommendationCollectResponse,
  GithubStarRecommendationGenerationAcceptedResponse,
  GithubStarRecommendationGenerationResponse,
  GithubStarRecommendationsResponse,
} from '../types';
export { createFeedbackApi } from './feedback';
export type {
  FeedbackApi,
  FeedbackCreateRequest,
  FeedbackCreateResponse,
  FeedbackType,
} from './feedback';
export { createScrapsApi } from './scraps';
export type { ScrapsApi } from './scraps';
export { createCardsApi } from './cards';
export type { CardsApi } from './cards';
export { createArchiveApi } from './archive';
export type { ArchiveApi } from './archive';
export { createSearchApi } from './search';
export type { SearchApi } from './search';
export { createS3Api, getS3ImageFileValidationError } from './s3';
export type { S3Api } from './s3';
export { createAsyncJobsApi } from './async';
export type { AsyncJobsApi } from './async';
export { createTilApi } from './til';
export type { TilApi } from './til';
export { createRecallApi } from './recall';
export type { RecallApi } from './recall';
export { createStatisticsApi } from './statistics';
export type { StatisticsApi, StatisticsOverview } from './statistics';
