import type { KnowledgeCardResponse } from './knowledge';
import type { CategoryResponse } from './knowledge';
import type { SourceType } from './scraps';

export interface TilGenerateRequest {
  targetDate: string;
  aiTransmissionConfirmed: boolean;
}

export interface TilUpdateRequest {
  title: string;
  content: string;
}

export interface TilGenerationJobResponse {
  summaryId: string;
  jobId: string;
  targetDate: string;
}

export interface TilResponse {
  summaryId: string;
  targetDate: string;
  title: string | null;
  content: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TilSourceContentResponse {
  referenceId: string;
  cardId: string;
  scrapId: string;
  title: string;
  sourceType: SourceType;
  rawContent: string | null;
  sourceUrl: string | null;
  imageUrl: string | null;
  category: CategoryResponse | null;
  createdAt: string;
}

export interface TilRecallCardsResponse {
  recallCards: KnowledgeCardResponse[];
}

export interface TilSourcesResponse {
  sources: TilSourceContentResponse[];
  evidenceSnapshot: boolean;
  evidenceScope: 'TIL_INPUT_SNAPSHOT' | 'UNAVAILABLE';
  dataProtection: TilDataProtectionResponse | null;
  reviewedBlockIds: string[];
}

export interface TilDataProtectionResponse {
  policyVersion: string;
  maskedItemCount: number;
  transmissionConfirmed: boolean;
  transmissionConfirmedAt: string | null;
}

export type TilGithubCommitStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface TilGithubCommitJobResponse {
  commitId: string;
  jobId: string;
  summaryId: string;
  status: TilGithubCommitStatus;
}

export interface TilGithubContributionDayResponse {
  date: string;
  count: number;
  level: number;
}

export interface TilGithubContributionRepositoryResponse {
  githubRepositoryId: number;
  name: string;
  fullName: string;
  htmlUrl: string;
  count: number;
}

export interface TilGithubContributionCommitResponse {
  commitId: string;
  summaryId: string;
  githubRepositoryId: number;
  repositoryName: string;
  repositoryFullName: string;
  branch: string;
  filePath: string;
  title: string;
  commitSha: string;
  commitUrl: string;
  pushedAt: string;
}

export interface TilGithubContributionResponse {
  from: string;
  to: string;
  totalCommits: number;
  activeDays: number;
  currentStreakDays: number;
  longestStreakDays: number;
  days: TilGithubContributionDayResponse[];
  repositories: TilGithubContributionRepositoryResponse[];
  commits: TilGithubContributionCommitResponse[];
}

export interface TilGithubContributionParams {
  from?: string;
  to?: string;
  githubRepositoryId?: number;
}
