import { useQuery, type UseQueryOptions } from '@tanstack/react-query';
import { asyncJobsApi, recallApi, tilApi } from '@dashboard/api/client';
import type { RecallQuizListResponse, RecallQuizType, TilResponse } from '@san/shared';

export const tilKeys = {
  all: ['til'] as const,
  byDate: (date: string) => [...tilKeys.all, date] as const,
  recallCards: (summaryId: string | null | undefined) => [...tilKeys.all, 'recall-cards', summaryId] as const,
  recallQuizzes: (targetDate: string | null | undefined, quizType: RecallQuizType) =>
    [...tilKeys.all, 'recall-quizzes', targetDate, quizType] as const,
  sources: (summaryId: string | null | undefined) => [...tilKeys.all, 'sources', summaryId] as const,
  asyncJob: (jobId: string | null | undefined) => ['async-job', jobId] as const,
};

export function useTilByDate(
  date: string,
  options?: Omit<UseQueryOptions<TilResponse[]>, 'queryKey' | 'queryFn'>,
) {
  return useQuery({
    queryKey: tilKeys.byDate(date),
    queryFn: () => tilApi.getByDate(date),
    ...options,
  });
}

export function useTilRecallCards(summaryId: string | null | undefined) {
  return useQuery({
    queryKey: tilKeys.recallCards(summaryId),
    queryFn: () => tilApi.getRecallCards(summaryId ?? ''),
    enabled: Boolean(summaryId),
  });
}

export function useTilRecallQuizzes(
  targetDate: string | null | undefined,
  quizType: RecallQuizType = 'OX',
  enabled = true,
) {
  return useQuery<RecallQuizListResponse>({
    queryKey: tilKeys.recallQuizzes(targetDate, quizType),
    queryFn: () => recallApi.getQuizzes(targetDate ?? '', quizType),
    enabled: Boolean(targetDate) && enabled,
  });
}

export function useTilSources(summaryId: string | null | undefined) {
  return useQuery({
    queryKey: tilKeys.sources(summaryId),
    queryFn: () => tilApi.getSources(summaryId ?? ''),
    enabled: Boolean(summaryId),
  });
}

export function useTilAsyncJobStatus(jobId: string | null | undefined) {
  return useQuery({
    queryKey: tilKeys.asyncJob(jobId),
    queryFn: () => asyncJobsApi.getStatus(jobId ?? ''),
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'PENDING' || status === 'PROCESSING' ? 1500 : false;
    },
  });
}
