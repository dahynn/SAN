// packages/shared/src/hooks/useCards.ts
import { useMutation, useQuery, useQueryClient, type UseQueryOptions } from '@tanstack/react-query';
import { useApiContext } from '@san/shared';
import type { KnowledgeCardDetailResponse, KnowledgeCardListParams, RefinedContentUpdateRequest } from '../types';

export const cardKeys = {
  all: ['cards'] as const,
  list: (params?: KnowledgeCardListParams) => ['cards', 'list', params ?? {}] as const,
  detail: (cardId: string | null | undefined) => ['cards', 'detail', cardId] as const,
  similar: (cardId: string | null | undefined) => ['cards', 'similar', cardId] as const,
};

// ----------------------------
// 컴포넌트에서 cardsApi 인수 없이 바로 호출 가능
// const { data } = useCards()
// ----------------------------

export function useCards(params?: KnowledgeCardListParams, options?: { enabled?: boolean }) {
  const { cardsApi } = useApiContext();
  return useQuery({
    queryKey: cardKeys.list(params),
    queryFn: () => cardsApi.getAll(params),
    enabled: options?.enabled ?? true,
    staleTime: 1000 * 30,
  });
}

export function useCardDetail(
  cardId: string | null | undefined,
  options?: Omit<UseQueryOptions<KnowledgeCardDetailResponse>, 'queryKey' | 'queryFn'>
) {
  const { cardsApi } = useApiContext();
  return useQuery({
    queryKey: cardKeys.detail(cardId),
    queryFn: () => cardsApi.getDetail(cardId ?? ''),
    enabled: Boolean(cardId),
    staleTime: 1000 * 30,
    ...options,
  });
}

export function useSimilarCards(cardId: string | null | undefined) {
  const { cardsApi } = useApiContext();
  return useQuery({
    queryKey: cardKeys.similar(cardId),
    queryFn: () => cardsApi.getSimilarByCardId(cardId ?? ''),
    enabled: Boolean(cardId),
    staleTime: 1000 * 30,
  });
}

export function useUpdateRefinedContent(
  cardId: string | null | undefined,
  options?: {
    onSuccess?: (data: KnowledgeCardDetailResponse) => void;
    onError?: (error: unknown) => void;
  },
) {
  const { cardsApi } = useApiContext();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: RefinedContentUpdateRequest) => {
      if (!cardId) throw new Error('cardId is required');
      return cardsApi.updateRefinedContent(cardId, payload);
    },
    onSuccess: (data) => {
      queryClient.setQueryData(cardKeys.detail(cardId), data);
      void queryClient.invalidateQueries({ queryKey: cardKeys.list() });
      options?.onSuccess?.(data);
    },
    onError: options?.onError,
  });
}

export function useDeleteCard(
  cardId: string | null | undefined,
  options?: {
    onSuccess?: () => void;
    onError?: (error: unknown) => void;
  },
) {
  const { cardsApi } = useApiContext();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => {
      if (!cardId) throw new Error('cardId is required');
      return cardsApi.deleteCard(cardId);
    },
    onSuccess: () => {
      if (cardId) {
        queryClient.removeQueries({ queryKey: cardKeys.detail(cardId) });
        queryClient.removeQueries({ queryKey: cardKeys.similar(cardId) });
      }
      void queryClient.invalidateQueries({ queryKey: cardKeys.all });
      void queryClient.invalidateQueries({ queryKey: ['archive'] });
      void queryClient.invalidateQueries({ queryKey: ['archive-category-search'] });
      options?.onSuccess?.();
    },
    onError: options?.onError,
  });
}
