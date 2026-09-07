export interface GithubLinkStatus {
  linked: boolean;
  githubUsername: string | null;
  repositoryConnected: boolean;
  connectedRepository: GithubRepository | null;
}

export interface GithubRepository {
  githubRepositoryId: number;
  name: string;
  fullName: string;
  privateRepository: boolean;
  defaultBranch: string;
  htmlUrl: string;
}

export interface GithubRepositoryConnectRequest {
  githubRepositoryId: number;
}

export interface GithubAuthorizeUrlResponse {
  redirectUrl: string;
}

export interface GithubStarRecommendation {
  recommendationId: string;
  title: string;
  tagList: string[];
  recommendationUrl: string;
  collected: boolean;
}

export interface GithubStarRecommendationsResponse {
  recommendations: GithubStarRecommendation[];
}

export interface GithubStarRecommendationGenerationAcceptedResponse {
  jobId: string;
}

export interface GithubStarRecommendationCollectResponse {
  scrapId: string;
  cardId: string;
  collected: boolean;
}

export type GithubStarRecommendationGenerationResponse =
  | GithubStarRecommendationsResponse
  | GithubStarRecommendationGenerationAcceptedResponse;

