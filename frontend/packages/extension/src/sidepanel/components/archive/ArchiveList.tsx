import type { SavedInsight } from '@extension/types';
import { CardList } from './CardList';

interface ArchiveListProps {
  cards: SavedInsight[];
}

export function ArchiveList({ cards }: ArchiveListProps) {
  return (
    <section className="shrink-0">
      <div className="mb-3 flex h-11 shrink-0 items-center justify-between gap-3">
        <div className="shrink-0 text-body-sm font-medium text-text-secondary/85">
          최근 아카이브
        </div>
      </div>
      <CardList cards={cards} />
    </section>
  );
}
