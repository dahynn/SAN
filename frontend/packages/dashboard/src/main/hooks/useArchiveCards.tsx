import {
  toKnowledgeCardView,
  useCards,
  type KnowledgeCardListParams,
  type KnowledgeCardView,
} from '@san/shared';

export interface ArchiveCardsParams {
  limit?: number;
  tag?: string;
  tags?: string[];
  search?: string;
  date?: string;
  from?: string;
  to?: string;
}

interface UseArchiveCardsResult {
  cards: KnowledgeCardView[];
  isPending: boolean;
  isError: boolean;
}

interface UseArchiveCardsOptions {
  enabled?: boolean;
}

export function useArchiveCards(
  params?: ArchiveCardsParams,
  options?: UseArchiveCardsOptions
): UseArchiveCardsResult {
  const serverParams = toKnowledgeCardListParams(params);
  const query = useCards(serverParams, { enabled: options?.enabled });

  return {
    cards: filterArchiveCards(query.data?.cards.map(toKnowledgeCardView) ?? [], {
      ...params,
      limit: params?.search ? params.limit : undefined,
    }),
    isPending: query.isPending,
    isError: query.isError,
  };
}

function toKnowledgeCardListParams(params?: ArchiveCardsParams): KnowledgeCardListParams | undefined {
  if (!params) return undefined;

  const selectedTags = normalizeTags(params.tags ?? (params.tag ? [params.tag] : []));

  return compactParams({
    tag: selectedTags[0],
    fromDate: params.date ?? params.from,
    toDate: params.date ?? params.to,
    limit: params.search ? undefined : params.limit,
  });
}

function compactParams(params: KnowledgeCardListParams): KnowledgeCardListParams | undefined {
  const compacted = Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== '')
  ) as KnowledgeCardListParams;

  return Object.keys(compacted).length > 0 ? compacted : undefined;
}

function filterArchiveCards(cards: KnowledgeCardView[], params?: ArchiveCardsParams) {
  if (!params) return cards;

  const search = params.search?.trim().toLowerCase();
  const selectedTags = normalizeTags(params.tags ?? (params.tag ? [params.tag] : []));

  const filtered = cards.filter((card) => {
    const matchesSearch = search
      ? [card.title, card.summary, card.category_name]
          .filter(Boolean)
          .some((value) => value?.toLowerCase().includes(search))
      : true;

    const cardTags = card.tags.map((tag) => tag.name.toLowerCase());
    const matchesTags =
      selectedTags.length > 0
        ? selectedTags.every((tag) => cardTags.includes(tag.toLowerCase()))
        : true;

    const createdDate = card.created_at.slice(0, 10);
    const matchesDate = params.date ? createdDate === params.date : true;
    const matchesFrom = params.from ? createdDate >= params.from : true;
    const matchesTo = params.to ? createdDate <= params.to : true;

    return matchesSearch && matchesTags && matchesDate && matchesFrom && matchesTo;
  });

  return typeof params.limit === 'number' ? filtered.slice(0, params.limit) : filtered;
}

function normalizeTags(tags: string[]) {
  return tags.flatMap((tag) => tag.split(',')).map((tag) => tag.trim()).filter(Boolean);
}
