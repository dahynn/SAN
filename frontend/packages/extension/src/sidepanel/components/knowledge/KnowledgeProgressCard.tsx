import type { KnowledgeCardResponse, KnowledgeCardView } from '@san/shared';
import { CreatedKnowledgeCard } from './CreatedKnowledgeCard';
import { KnowledgeLoadingCard } from './KnowledgeLoadingCard';
import { RelatedCards } from './RelatedCards';

interface KnowledgeProgressCardProps {
  cards: KnowledgeCardResponse[];
  isLoading: boolean;
  error: string | null;
  hasScrapContext: boolean;
  createdCard: KnowledgeCardView | null;
}

export default function KnowledgeProgressCard({
  cards,
  isLoading,
  error,
  hasScrapContext,
  createdCard,
}: KnowledgeProgressCardProps) {
  if (!hasScrapContext && !createdCard) {
    return null;
  }

  if (isLoading) {
    return <KnowledgeLoadingCard />;
  }

  return (
    <section>
      <div className="mb-3 text-caption font-medium uppercase tracking-[0.14em] text-text-secondary">
        {isLoading ? 'Creating knowledge card' : 'Knowledge result'}
      </div>
      <div className="glass-panel space-y-3 rounded-leaf border border-text-secondary/20 bg-surface-container/90 p-popover-padding backdrop-blur-md">
        {createdCard ? <CreatedKnowledgeCard card={createdCard} /> : null}
        <RelatedCards
          cards={cards}
          isAuthenticated
          isLoading={isLoading}
          error={error}
          hasScrapContext={hasScrapContext || Boolean(createdCard)}
          onLogin={() => undefined}
        />
      </div>
    </section>
  );
}
