import { useQuery } from '@tanstack/react-query';
import { useApiContext } from '@san/shared';

export const archiveKeys = {
  all: ['archive'] as const,
  categories: () => [...archiveKeys.all, 'categories'] as const,
  categoryCards: (categoryId: string | null | undefined) => [...archiveKeys.all, 'category-cards', categoryId] as const,
  cardRelations: (cardId: string | null | undefined) => [...archiveKeys.all, 'card-relations', cardId] as const,
};

export function useArchiveCategories(options?: { enabled?: boolean }) {
  const { archiveApi } = useApiContext();

  return useQuery({
    queryKey: archiveKeys.categories(),
    queryFn: () => archiveApi.getCategories(),
    enabled: options?.enabled ?? true,
  });
}

export function useArchiveCategoryCards(categoryId: string | null | undefined) {
  const { archiveApi } = useApiContext();

  return useQuery({
    queryKey: archiveKeys.categoryCards(categoryId),
    queryFn: () => archiveApi.getCategoryCards(categoryId ?? ''),
    enabled: Boolean(categoryId),
  });
}

export function useArchiveCardTagRelations(cardId: string | null | undefined) {
  const { archiveApi } = useApiContext();

  return useQuery({
    queryKey: archiveKeys.cardRelations(cardId),
    queryFn: () => archiveApi.getCardTagRelations(cardId ?? ''),
    enabled: Boolean(cardId),
  });
}
