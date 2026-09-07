import type { KnowledgeCardView } from '@san/shared';
import { KnowledgeSummary } from './KnowledgeSummary';

interface CreatedKnowledgeCardProps {
  card: KnowledgeCardView;
  onOpenCard?: (cardId: string) => void;
}

export function CreatedKnowledgeCard({ card, onOpenCard }: CreatedKnowledgeCardProps) {
  const canOpenCard = Boolean(onOpenCard);

  const handleOpenCard = () => {
    onOpenCard?.(card.card_id);
  };

  return (
    <div className="px-1">
      <div className="mb-2 flex h-11 items-center text-body-sm font-bold text-text-secondary/85">
        지식카드 생성
      </div>

      <article
        role={canOpenCard ? 'button' : undefined}
        tabIndex={canOpenCard ? 0 : undefined}
        aria-label={canOpenCard ? '지식카드 상세보기' : undefined}
        title={canOpenCard ? '지식카드 상세보기' : undefined}
        onClick={canOpenCard ? handleOpenCard : undefined}
        onKeyDown={canOpenCard ? (event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handleOpenCard();
          }
        } : undefined}
        className={[
          'glass-card relative flex h-[132px] w-full gap-3 overflow-hidden rounded-leaf border-t border-l border-text-secondary/20 bg-surface-container/90 p-3.5 backdrop-blur-xl shadow-neon-sm transition',
          canOpenCard
            ? 'cursor-pointer hover:border-action-accent/35 hover:bg-surface-container focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-action-accent/55 active:translate-y-px'
            : '',
        ].join(' ')}
      >
        <div className="absolute -left-4 -top-4 h-24 w-24 rounded-full bg-action-accent/5 blur-2xl" aria-hidden="true" />

        <div className="relative flex h-12 w-12 shrink-0 items-center justify-center rounded-leaf border border-action-accent/20 bg-surface-low shadow-neon-sm">
          <svg
            width={20}
            height={20}
            viewBox="0 0 22 22"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            className="text-action-accent"
          >
            <path
              d="M10.3125 21.2809C9.625 21.2809 8.93229 21.2028 8.23438 21.0465C7.53646 20.8903 6.82292 20.6663 6.09375 20.3746C6.34375 17.8538 7.07292 15.4996 8.28125 13.3121C9.48958 11.1246 11.0417 9.19757 12.9375 7.5309C10.6458 8.69757 8.66146 10.2392 6.98438 12.1559C5.30729 14.0726 4.13542 16.2601 3.46875 18.7184C3.38542 18.6559 3.30729 18.5882 3.23438 18.5153C3.16146 18.4424 3.08333 18.3642 3 18.2809C2.02083 17.3017 1.27604 16.208 0.765625 14.9996C0.255208 13.7913 0 12.5309 0 11.2184C0 9.80173 0.28125 8.44757 0.84375 7.1559C1.40625 5.86423 2.1875 4.7184 3.1875 3.7184C4.875 2.0309 7.0625 0.931941 9.75 0.421524C12.4375 -0.0888928 16.2083 -0.135768 21.0625 0.280899C21.4375 5.26007 21.375 9.05694 20.875 11.6715C20.375 14.2861 19.2917 16.4267 17.625 18.0934C16.6042 19.1142 15.4635 19.9007 14.2031 20.4528C12.9427 21.0049 11.6458 21.2809 10.3125 21.2809Z"
              fill="currentColor"
            />
          </svg>
          <div className="absolute -bottom-1 -left-1 flex items-center justify-center rounded-full bg-action-accent px-1.5 py-0.5 shadow-neon-sm">
            <span className="text-[7px] font-bold uppercase leading-none text-surface-lowest">SPROUT</span>
          </div>
        </div>

        <div className="flex flex-1 flex-col justify-start overflow-hidden">
          <h3 className="line-clamp-1 text-body-main-bold text-text-primary">
            {card.title}
          </h3>
          <div className="mt-1 flex-1 overflow-y-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            <KnowledgeSummary
              summary={card.summary}
              className="text-[12px] leading-[18px] text-text-secondary/90"
              paragraphClassName="mb-0.5 last:mb-0"
              listClassName="space-y-0.5"
              itemClassName="flex items-start gap-2"
            />
          </div>

          {card.tags.length > 0 ? (
            <div className="mt-1.5 flex shrink-0 flex-wrap gap-1">
              {card.tags.slice(0, 2).map((tag) => (
                <span
                  key={tag.tag_id}
                  className="rounded-full border border-action-accent/15 bg-background/40 px-1.5 py-0.5 text-[9px] font-bold leading-none text-action-accent"
                >
                  #{tag.name}
                </span>
              ))}
            </div>
          ) : null}
        </div>
      </article>
    </div>
  );
}
