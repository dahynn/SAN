import { useMutation } from '@tanstack/react-query';
import type {
  AsyncJobStatusResponse,
  RecallQuizGenerateRequest,
  RecallQuizGenerationJobResponse,
  RecallQuizSubmitRequest,
  RecallQuizSubmitResponse,
  TilGenerationJobResponse,
  TilGithubCommitJobResponse,
  TilResponse,
  TilUpdateRequest,
} from '@san/shared';
import { asyncJobsApi, recallApi, tilApi } from '@dashboard/api/client';

export function useTilGenerationRetryMutation(onSuccess?: (response: AsyncJobStatusResponse) => void) {
  return useMutation({
    mutationFn: (jobId: string) => asyncJobsApi.retryTilGeneration(jobId),
    onSuccess,
  });
}

interface UseTilGenerateMutationOptions {
  targetDate: string;
  onSuccess?: (response: TilGenerationJobResponse) => void;
}

export function useTilGenerateMutation({ targetDate, onSuccess }: UseTilGenerateMutationOptions) {
  return useMutation({
    mutationFn: (aiTransmissionConfirmed: boolean) => tilApi.generate({ targetDate, aiTransmissionConfirmed }),
    onSuccess,
  });
}

interface UseRecallQuizGenerateMutationOptions {
  onSuccess?: (response: RecallQuizGenerationJobResponse) => void;
  onError?: (error: unknown) => void;
}

export function useRecallQuizGenerateMutation({ onSuccess, onError }: UseRecallQuizGenerateMutationOptions = {}) {
  return useMutation({
    mutationFn: (payload: RecallQuizGenerateRequest) => recallApi.requestQuizGeneration(payload),
    onSuccess,
    onError,
  });
}

interface UseTilGithubCommitMutationOptions {
  onSuccess?: (response: TilGithubCommitJobResponse) => void;
}

export function useTilGithubCommitMutation({ onSuccess }: UseTilGithubCommitMutationOptions = {}) {
  return useMutation({
    mutationFn: (summaryId: string) => tilApi.commitToGithub(summaryId),
    onSuccess,
  });
}

interface UpdateTilVariables extends TilUpdateRequest {
  summaryId: string;
}

interface UseTilUpdateMutationOptions {
  onSuccess?: (response: TilResponse) => void;
}

export function useTilUpdateMutation({ onSuccess }: UseTilUpdateMutationOptions = {}) {
  return useMutation({
    mutationFn: ({ summaryId, title, content }: UpdateTilVariables) =>
      tilApi.update(summaryId, { title, content }),
    onSuccess,
  });
}

interface UseTilDeleteMutationOptions {
  onSuccess?: (_response: void, summaryId: string) => void;
}

export function useTilDeleteMutation({ onSuccess }: UseTilDeleteMutationOptions = {}) {
  return useMutation({
    mutationFn: (summaryId: string) => tilApi.delete(summaryId),
    onSuccess,
  });
}

interface UseRecallQuizSubmitMutationOptions {
  onSuccess?: (response: RecallQuizSubmitResponse, variables: { quizId: string; answer: string }) => void;
}

export function useRecallQuizSubmitMutation({ onSuccess }: UseRecallQuizSubmitMutationOptions = {}) {
  return useMutation({
    mutationFn: ({ quizId, answer }: { quizId: string; answer: string }) =>
      recallApi.submitQuiz(quizId, { answer } satisfies RecallQuizSubmitRequest),
    onSuccess,
  });
}
