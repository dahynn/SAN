import type { ReactNode } from 'react';
import type { KnowledgeCardDetailResponse, KnowledgeCardResponse } from '@san/shared';
import type { SavedInsight } from '@extension/types';
import { RecentKnowledgeList } from './RecentKnowledgeList';

const TITLE = '\uC720\uC0AC \uC9C0\uC2DD';
const LOADING_MESSAGE = '\uC720\uC0AC\uD55C \uC9C0\uC2DD\uC744 \uCC3E\uB294 \uC911\uC774\uC5D0\uC694.';
const LOADING_DESCRIPTION = '\uC0DD\uC131\uB41C \uC9C0\uC2DD\uACFC \uBE44\uC2B7\uD55C \uCE74\uB4DC\uB97C \uBE44\uAD50\uD558\uACE0 \uC788\uC5B4\uC694.';
const ERROR_MESSAGE = '\uC720\uC0AC \uC9C0\uC2DD\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC5B4\uC694.';
const ERROR_DESCRIPTION = '\uC9C0\uC2DD \uCE74\uB4DC\uAC00 \uC0DD\uC131\uB41C \uB4A4 \uB2E4\uC2DC \uD655\uC778\uD574\uC8FC\uC138\uC694.';
const EMPTY_TITLE = '\uC720\uC0AC\uD55C \uC9C0\uC2DD\uC774 \uC5C6\uC5B4\uC694.';
const EMPTY_DESCRIPTION = '\uC218\uC9D1\uD55C \uC9C0\uC2DD\uC774 \uB298\uC5B4\uB098\uBA74 \uB354 \uC815\uD655\uD574\uC838\uC694.';

interface SimilarKnowledgeListProps {
  cards: KnowledgeCardResponse[];
  isLoading: boolean;
  error: string | null;
  action?: ReactNode;
  isScrollable?: boolean;
  title?: ReactNode;
  sourceByCardId?: Record<string, SavedInsight | undefined>;
  serverSourceByCardId?: Record<string, KnowledgeCardDetailResponse | undefined>;
  useServerSources?: boolean;
  onOpenCard?: (cardId: string) => void;
}

export function SimilarKnowledgeList({
  cards,
  isLoading,
  error,
  action,
  isScrollable = true,
  title,
  sourceByCardId,
  serverSourceByCardId,
  useServerSources,
  onOpenCard,
}: SimilarKnowledgeListProps) {
  return (
    <RecentKnowledgeList
      cards={cards}
      isLoading={isLoading}
      error={error}
      action={action}
      isScrollable={isScrollable}
      title={title ?? TITLE}
      sourceByCardId={sourceByCardId}
      serverSourceByCardId={serverSourceByCardId}
      useServerSources={useServerSources}
      onOpenCard={onOpenCard}
      loadingMessage={LOADING_MESSAGE}
      loadingDescription={LOADING_DESCRIPTION}
      errorMessage={ERROR_MESSAGE}
      errorDescription={ERROR_DESCRIPTION}
      emptyTitle={EMPTY_TITLE}
      emptyDescription={EMPTY_DESCRIPTION}
    />
  );
}
