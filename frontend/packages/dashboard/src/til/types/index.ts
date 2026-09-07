import type { UseMutationResult, UseQueryResult } from '@tanstack/react-query';
import type {
  AsyncJobStatusResponse,
  RecallQuizListResponse,
  RecallQuizSubmitResponse,
  TilGenerationJobResponse,
  TilGithubCommitJobResponse,
  TilRecallCardsResponse,
  TilResponse,
  TilSourcesResponse,
} from '@san/shared';

export type TilDateString = string;

export type TilJobTone = 'idle' | 'pending' | 'success' | 'error';

export type TilListQuery = UseQueryResult<TilResponse[]>;
export type TilRecallCardsQuery = UseQueryResult<TilRecallCardsResponse>;
export type TilRecallQuizzesQuery = UseQueryResult<RecallQuizListResponse>;
export type TilSourcesQuery = UseQueryResult<TilSourcesResponse>;
export type TilJobStatusQuery = UseQueryResult<AsyncJobStatusResponse>;

export type TilGenerateMutation = UseMutationResult<TilGenerationJobResponse, Error, void, unknown>;
export type TilGithubCommitMutation = UseMutationResult<TilGithubCommitJobResponse, Error, string, unknown>;
export type TilUpdateMutation = UseMutationResult<TilResponse, Error, { summaryId: string; title: string; content: string }, unknown>;
export type TilDeleteMutation = UseMutationResult<void, Error, string, unknown>;
export type RecallQuizSubmitMutation = UseMutationResult<RecallQuizSubmitResponse, Error, { quizId: string; answer: string }, unknown>;

export interface TilSourceContent {
  inputType: 'text' | 'url' | 'image' | string;
  content: string;
}

export interface TilSourceResponse {
  targetDate: TilDateString;
  contents: TilSourceContent[];
}

export interface TilPageLogic {
  selectedDate: TilDateString;
  setSelectedDate: (date: TilDateString) => void;
  selectedSummaryId: string | null;
  setSelectedSummaryId: (summaryId: string | null) => void;
  title: string;
  setTitle: (value: string) => void;
  draft: string;
  setDraft: (value: string) => void;
  tilList: TilResponse[];
  selectedTil: TilResponse | null;
  tilQuery: TilListQuery;
  recallCardsQuery: TilRecallCardsQuery;
  sourcesQuery: TilSourcesQuery;
  generationStatusQuery: TilJobStatusQuery;
  commitStatusQuery: TilJobStatusQuery;
  generateMutation: TilGenerateMutation;
  updateMutation: TilUpdateMutation;
  deleteMutation: TilDeleteMutation;
  commitMutation: TilGithubCommitMutation;
  generationTone: TilJobTone;
  commitTone: TilJobTone;
  generationMessage: string | null;
  commitMessage: string | null;
}
