import type { KnowledgeCardResponse } from '@san/shared';
import { ExternalLink } from 'lucide-react';
import { CurvedButton } from '@san/ui/components/Button/CurvedButton';
import { KnowledgeSummary } from './KnowledgeSummary';

interface RelatedCardsProps {
  cards: KnowledgeCardResponse[];
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  hasScrapContext: boolean;
  onLogin: () => void;
}

function formatDate(value?: string) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
  }).format(date);
}

export function RelatedCards({
  cards,
  isAuthenticated,
  isLoading,
  error,
  hasScrapContext,
  onLogin,
}: RelatedCardsProps) {
  if (!hasScrapContext) {
    return (
      <div className="rounded-leaf border border-text-secondary/20 bg-surface-low/50 p-popover-padding">
        <p className="text-body-main-bold text-text-secondary">유사 지식 카드</p>
        <p className="mt-1 text-caption leading-5 text-text-secondary/80">
          지식을 수집하시면 유사한 지식 카드를 보여드립니다. 웹에서 유용한 정보를 발견하면 스크랩해 보세요.
        </p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="rounded-leaf border border-primary-signal/20 bg-primary-signal/10 p-popover-padding">
        <p className="text-body-main-bold text-primary-signal">로그인하시면 연관 지식 카드를 확인할 수 있습니다.</p>
        <p className="mt-1 text-caption leading-5 text-text-secondary/80">
          지식 카드의 유사도를 높이려면 대시보드에서 더 많은 지식 카드를 저장해 보세요.
        </p>
        <CurvedButton onClick={onLogin} size="sm" className="mt-3">
          로그인
        </CurvedButton>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="rounded-leaf border border-text-secondary/20 bg-surface-low/50 p-popover-padding">
        <p className="text-body-main-bold text-text-secondary">유사 지식 카드를 찾는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-leaf border border-red-500/20 bg-red-500/10 p-popover-padding">
        <p className="text-body-main-bold text-red-300">유사 지식 카드를 불러올 수 없습니다</p>
        <p className="mt-1 text-caption leading-5 text-red-200/70">{error}</p>
      </div>
    );
  }

  if (cards.length === 0) {
    return (
      <div className="rounded-leaf border border-text-secondary/20 bg-surface-low/50 p-popover-padding">
        <p className="text-body-main-bold text-text-secondary">유사 지식 카드를 찾을 수 없습니다</p>
        <p className="mt-1 text-caption leading-5 text-text-secondary/80">
          대시보드에서 더 많은 지식 카드를 저장하여 유사도를 높여보세요.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {cards.map((card) => (
        <article
          key={card.cardId}
          className="flex flex-col gap-2.5 rounded-leaf border border-text-secondary/20 glass-card bg-surface-container/80 p-4"
        >
          <div className="flex items-start justify-between gap-3">
            <p className="text-body-main-bold text-text-primary line-clamp-1">{card.title}</p>
            <ExternalLink size={16} className="text-text-secondary" aria-hidden="true" />
          </div>

          <KnowledgeSummary
            summary={card.summary}
            className="max-h-10 overflow-hidden text-[13px] leading-5 text-text-secondary"
            paragraphClassName="mb-0.5 last:mb-0"
            listClassName="space-y-0.5"
            itemClassName="flex items-start gap-2"
          />

          {card.tags.length > 0 ? (
            <div className="flex flex-wrap gap-1.5 pt-1">
              {card.tags.slice(0, 3).map((tag) => (
                <span
                  key={tag.tagId}
                  className="inline-flex items-center gap-1 rounded-full border border-primary-signal/20 bg-primary-signal/5 px-2 py-0.5 text-[11px] font-bold uppercase text-primary-signal"
                >
                  {tag.tagName}
                </span>
              ))}
            </div>
          ) : null}
        </article>
      ))}
    </div>
  );
}
