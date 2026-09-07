import type { SavedInsight } from '@extension/types';
import { PackageOpen } from 'lucide-react';
import ArchiveItem from './ArchiveItem';

interface CardListProps {
  cards: SavedInsight[];
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
  }).format(new Date(value));
}

function getPreviewContent(card: SavedInsight) {
  return card.raw_content ?? card.source_url ?? card.image_file_name ?? card.domain ?? null;
}

export const CardList = ({ cards }: CardListProps) => {
  if (cards.length === 0) {
    return (
      <div className="flex min-h-[220px] flex-col items-center justify-center gap-3 pt-6 text-center">
        <div className="extension-primary-drop-glow text-primary-signal/80">
          <PackageOpen size={44} strokeWidth={1.6} aria-hidden="true" />
        </div>
        <div className="space-y-1">
          <p className="text-body-sm font-medium text-text-secondary">
            아직 저장된 원본이 없어요.
          </p>
          <p className="text-caption text-text-secondary/60">
            수집한 내용을 저장하면 이곳에 표시돼요.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4 pb-4">
      {cards.map((card) => (
        <ArchiveItem
          key={card.id}
          title={card.title || card.domain || 'Untitled'}
          meta={formatTime(card.created_at)}
          sourceType={card.source_type}
          content={getPreviewContent(card)}
        />
      ))}
    </div>
  );
};
