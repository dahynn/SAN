// packages/shared/src/hooks/useScraps.ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useApiContext } from '../context/ApiContext';
import type { CreateScrapRequest } from '../types';

export const scrapKeys = {
  all: ['scraps'] as const,
  detail: (id: string) => ['scraps', id] as const,
};

export function useScrap(scrapId: string) {
  const { scrapsApi } = useApiContext();
  return useQuery({
    queryKey: scrapKeys.detail(scrapId),
    queryFn: () => scrapsApi.getById(scrapId),
    refetchInterval: (query) => {
      const status = query.state.data?.ai_status;
      if (status === 'PENDING' || status === 'PROCESSING') return 2_000;
      return false;
    },
    enabled: !!scrapId,
  });
}

export function useCreateScrap() {
  const { scrapsApi } = useApiContext();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateScrapRequest) => scrapsApi.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cards'] });
    },
  });
}

export function useDeleteScrap() {
  const { scrapsApi } = useApiContext();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (scrapId: string) => scrapsApi.delete(scrapId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cards'] });
    },
  });
}