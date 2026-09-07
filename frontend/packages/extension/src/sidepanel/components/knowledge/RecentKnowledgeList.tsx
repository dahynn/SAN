import type { ReactNode } from 'react';
import { useState } from 'react';
import type { KnowledgeCardDetailResponse, KnowledgeCardResponse } from '@san/shared';
import type { SavedInsight } from '@extension/types';
import { Check, ChevronsDownUp, Copy, FileText, Image, Link, Loader2, PackageOpen } from 'lucide-react';
import { KnowledgeSummary } from './KnowledgeSummary';

const RECENT_TITLE = '\uCD5C\uADFC \uC9C0\uC2DD';
const LOADING_MESSAGE = '\uC800\uC7A5\uD55C \uC9C0\uC2DD\uC744 \uBD88\uB7EC\uC624\uB294 \uC911\uC774\uC5D0\uC694.';
const LOADING_DESCRIPTION = '\uC7A0\uC2DC\uB9CC \uAE30\uB2E4\uB824\uC8FC\uC138\uC694.';
const ERROR_MESSAGE = '\uCD5C\uADFC \uC9C0\uC2DD\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC5B4\uC694.';
const ERROR_DESCRIPTION = '\uC7A0\uC2DC \uD6C4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574\uC8FC\uC138\uC694.';
const EMPTY_TITLE = '\uC544\uC9C1 \uC800\uC7A5\uB41C \uC9C0\uC2DD\uC774 \uC5C6\uC5B4\uC694.';
const EMPTY_DESCRIPTION = '\uC218\uC9D1\uD55C \uB0B4\uC6A9\uC744 \uC800\uC7A5\uD558\uBA74 \uC774\uACF3\uC5D0 \uD45C\uC2DC\uB3FC\uC694.';

interface RecentKnowledgeListProps {
  cards: KnowledgeCardResponse[];
  isLoading: boolean;
  error: string | null;
  action?: ReactNode;
  isScrollable?: boolean;
  title?: ReactNode;
  loadingMessage?: string;
  loadingDescription?: string;
  errorMessage?: string;
  errorDescription?: string;
  emptyTitle?: string;
  emptyDescription?: string;
  sourceByCardId?: Record<string, SavedInsight | undefined>;
  serverSourceByCardId?: Record<string, KnowledgeCardDetailResponse | undefined>;
  useServerSources?: boolean;
  onOpenCard?: (cardId: string) => void;
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

function getSourceContent(source: SavedInsight) {
  return source.raw_content ?? source.source_url ?? source.image_file_name ?? source.title;
}

function getSourceMeta(source: SavedInsight) {
  return source.source_url ?? source.domain ?? source.image_file_name ?? source.title;
}

function getSourceIcon(source: SavedInsight) {
  if (source.source_type === 'IMAGE') return Image;
  if (source.source_type === 'LINK') return Link;
  return FileText;
}

function getSourceImageUrl(source: SavedInsight) {
  if (source.source_type !== 'IMAGE') return null;
  return source.image_preview_url ?? source.image_url ?? null;
}

function getServerSourceContent(source: KnowledgeCardDetailResponse) {
  return source.sourceContent;
}

function getServerSourceMeta(source: KnowledgeCardDetailResponse) {
  return source.sourceType === 'LINK' ? source.sourceContent : null;
}

function getServerSourceIcon(source: KnowledgeCardDetailResponse) {
  if (source.sourceType === 'IMAGE') return Image;
  if (source.sourceType === 'LINK') return Link;
  return FileText;
}

function getServerSourceImageUrl(source: KnowledgeCardDetailResponse) {
  if (source.sourceType !== 'IMAGE') return null;
  return source.sourceContent;
}

async function copyCard(card: KnowledgeCardResponse) {
  const tags = card.tags.map((tag) => `#${tag.tagName}`).join(' ');
  const lines = [
    card.title,
    card.summary,
    card.category ? `Category: ${card.category.categoryName}` : null,
    tags || null,
  ].filter(Boolean);

  await navigator.clipboard.writeText(lines.join('\n'));
}

function KnowledgeCardArticle({
  card,
  source,
  isCopied,
  onCopy,
  serverSource,
  useServerSources,
  onOpenCard,
}: {
  card: KnowledgeCardResponse;
  source?: SavedInsight;
  isCopied: boolean;
  onCopy: (card: KnowledgeCardResponse) => void;
  serverSource?: KnowledgeCardDetailResponse;
  useServerSources?: boolean;
  onOpenCard?: (cardId: string) => void;
}) {
  const [isSourceOpen, setIsSourceOpen] = useState(false);
  const sourceContent = useServerSources
    ? (serverSource ? getServerSourceContent(serverSource) : null)
    : (source ? getSourceContent(source) : null);
  const sourceMeta = useServerSources
    ? (serverSource ? getServerSourceMeta(serverSource) : null)
    : (source ? getSourceMeta(source) : null);
  const sourceImageUrl = useServerSources
    ? (serverSource ? getServerSourceImageUrl(serverSource) : null)
    : (source ? getSourceImageUrl(source) : null);
  const hasSource = useServerSources
    ? Boolean(serverSource && (sourceImageUrl || sourceContent || sourceMeta))
    : Boolean(source && (sourceImageUrl || sourceContent || sourceMeta));
  const SourceIcon = useServerSources
    ? (serverSource ? getServerSourceIcon(serverSource) : FileText)
    : (source ? getSourceIcon(source) : FileText);

  return (
    <article className="rounded-leaf border border-text-secondary/12 glass-card bg-surface-container/80 px-4 py-4 transition hover:border-primary-signal/25 hover:bg-surface-container">
        <div className="flex items-start justify-between gap-3">
          <h3
            className={[
            'line-clamp-1 select-text text-body-main-bold font-bold text-text-primary',
            'cursor-text',
          ].join(' ')}
        >
          {card.title}
        </h3>
        <div className="flex shrink-0 items-center gap-1">
          {onOpenCard ? (
            <button
              type="button"
              onClick={(event) => {
                event.stopPropagation();
                onOpenCard(card.cardId);
              }}
              className="inline-flex items-center gap-1 rounded-full px-2 py-1 text-caption-bold text-text-secondary/75 transition hover:bg-text-primary/5 hover:text-action-accent active:translate-y-px"
              aria-label="Open card detail"
              title="Open card detail"
            >
              열기
            </button>
          ) : null}
          {hasSource ? (
            <button
              type="button"
              onClick={(event) => {
                event.stopPropagation();
                setIsSourceOpen((current) => !current);
              }}
              className="rounded-full p-1 text-text-secondary/75 transition hover:bg-text-primary/5 hover:text-action-accent active:translate-y-px"
              aria-label={isSourceOpen ? '원본 데이터 닫기' : '원본 데이터 열기'}
              aria-expanded={isSourceOpen}
              title="원본 데이터"
            >
              <ChevronsDownUp
                size={15}
                className={['transition-transform', isSourceOpen ? 'rotate-180' : ''].join(' ')}
                aria-hidden="true"
              />
            </button>
          ) : null}
          <button
            type="button"
            onClick={(event) => {
              event.stopPropagation();
              onCopy(card);
            }}
            className={[
              'rounded-full p-1 transition hover:bg-text-primary/5 active:translate-y-px',
              isCopied ? 'text-action-accent' : 'text-text-secondary/75 hover:text-action-accent',
            ].join(' ')}
            aria-label={isCopied ? 'Copied' : 'Copy card'}
            title={isCopied ? 'Copied' : 'Copy card'}
          >
            {isCopied ? <Check size={15} aria-hidden="true" /> : <Copy size={15} aria-hidden="true" />}
          </button>
        </div>
      </div>

      {hasSource && isSourceOpen ? (
        <div
          className="mt-3 rounded-md border-l-2 border-primary-signal bg-background/35 p-4"
          onClick={(event) => event.stopPropagation()}
        >
          <div className="mb-2 flex items-center gap-2 text-primary-signal">
            <SourceIcon size={13} strokeWidth={1.8} aria-hidden="true" />
            <span className="text-caption-bold">원본 데이터</span>
          </div>

          {sourceImageUrl ? (
            <div className="mb-3 overflow-hidden rounded-md border border-text-secondary/12 bg-surface-low p-2 shadow-neon-sm">
              <div className="relative overflow-hidden rounded-md bg-background">
                <img
                  src={sourceImageUrl}
                  alt={source?.title || card.title}
                  className="max-h-44 w-full object-contain opacity-90"
                />
              </div>
            </div>
          ) : null}

          {sourceMeta ? (
            <p className="line-clamp-1 cursor-text select-text text-caption text-text-secondary/75">
              {sourceMeta}
            </p>
          ) : null}
          {sourceContent && !sourceImageUrl ? (
            <p className="mt-2 max-h-16 overflow-y-auto whitespace-pre-wrap text-body-sm italic leading-5 text-text-primary/85 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {sourceContent}
            </p>
          ) : null}
        </div>
      ) : null}

      {card.summary ? (
        <KnowledgeSummary
          summary={card.summary}
          className="mt-2.5 max-h-[4rem] cursor-text select-text overflow-hidden text-[13px] leading-5 text-text-secondary/85"
          paragraphClassName="mb-0.5 last:mb-0"
          listClassName="space-y-0.5"
          itemClassName="flex items-start gap-2"
        />
      ) : null}

      <div className="mt-3 flex flex-wrap items-center gap-1.5">
        {card.category ? (
          <span className="inline-flex items-center rounded-full border border-primary-signal/25 bg-primary-signal/8 px-2.5 py-1 text-[11px] font-bold uppercase text-primary-signal">
            {card.category.categoryName}
          </span>
        ) : null}
        {card.tags.slice(0, 3).map((tag) => (
          <span
            key={tag.tagId}
            className="inline-flex items-center rounded-full border border-primary-signal/25 bg-primary-signal/8 px-2.5 py-1 text-[11px] font-bold uppercase text-primary-signal"
          >
            {tag.tagName}
          </span>
        ))}
        <span className="ml-auto text-caption text-text-secondary/55">
          {formatDate(card.createdAt)}
        </span>
      </div>
    </article>
  );
}

export function RecentKnowledgeList({
  cards,
  isLoading,
  error,
  action,
  isScrollable = true,
  title = RECENT_TITLE,
  loadingMessage = LOADING_MESSAGE,
  loadingDescription = LOADING_DESCRIPTION,
  errorMessage = ERROR_MESSAGE,
  errorDescription = ERROR_DESCRIPTION,
  emptyTitle = EMPTY_TITLE,
  emptyDescription = EMPTY_DESCRIPTION,
  sourceByCardId,
  serverSourceByCardId,
  useServerSources = false,
  onOpenCard,
}: RecentKnowledgeListProps) {
  const [copiedCardId, setCopiedCardId] = useState<string | null>(null);

  const handleCopy = (card: KnowledgeCardResponse) => {
    void copyCard(card)
      .then(() => {
        setCopiedCardId(card.cardId);
        window.setTimeout(() => {
          setCopiedCardId((currentCardId) => (currentCardId === card.cardId ? null : currentCardId));
        }, 1200);
      })
      .catch((error) => {
        console.error('[SAN:sidepanel] failed to copy card', error);
      });
  };

  return (
    <section className={['flex flex-col', isScrollable ? 'min-h-0 flex-1' : 'shrink-0'].join(' ')}>
      <div className="mb-3 flex h-11 shrink-0 items-center justify-between gap-3">
        <div className="shrink-0 text-body-sm font-medium text-text-secondary/85">
          {title}
        </div>
        {action ? (
          <div className="flex min-w-0 flex-1 justify-end pr-3">
            {action}
          </div>
        ) : null}
      </div>

      <div className={[
        '[scrollbar-width:none] [&::-webkit-scrollbar]:hidden',
        isScrollable ? 'min-h-0 flex-1 overflow-y-auto pr-1' : ''
      ].join(' ')}>
        {isLoading ? (
          <div className="flex min-h-[220px] flex-col items-center justify-center gap-3 pt-6 text-center">
            <div className="extension-primary-drop-glow text-primary-signal/80">
              <Loader2 size={44} strokeWidth={1.6} className="animate-spin" aria-hidden="true" />
            </div>
            <div className="space-y-1">
              <p className="text-body-sm font-medium text-text-secondary">
                {loadingMessage}
              </p>
              <p className="text-caption text-text-secondary/60">
                {loadingDescription}
              </p>
            </div>
          </div>
        ) : null}

        {error ? (
          <div className="flex min-h-[220px] flex-col items-center justify-center gap-3 pt-6 text-center">
            <div className="extension-primary-drop-glow text-primary-signal/80">
              <PackageOpen size={44} strokeWidth={1.6} aria-hidden="true" />
            </div>
            <div className="space-y-1">
              <p className="text-body-sm font-medium text-text-secondary">
                {errorMessage}
              </p>
              <p className="text-caption text-text-secondary/60">
                {errorDescription}
              </p>
            </div>
          </div>
        ) : null}

        {!isLoading && !error && cards.length === 0 ? (
          <div className="flex min-h-[220px] flex-col items-center justify-center gap-3 pt-6 text-center">
            <div className="extension-primary-drop-glow text-primary-signal/80">
              <PackageOpen size={44} strokeWidth={1.6} aria-hidden="true" />
            </div>
            <div className="space-y-1">
              <p className="text-body-sm font-medium text-text-secondary">
                {emptyTitle}
              </p>
              <p className="text-caption text-text-secondary/60">
                {emptyDescription}
              </p>
            </div>
          </div>
        ) : null}

        {!isLoading && !error && cards.length > 0 ? (
          <div className="space-y-4 pb-4">
            {cards.map((card) => (
              <KnowledgeCardArticle
                key={card.cardId}
                card={card}
                source={sourceByCardId?.[card.cardId]}
                isCopied={copiedCardId === card.cardId}
                onCopy={handleCopy}
                serverSource={serverSourceByCardId?.[card.cardId]}
                useServerSources={useServerSources}
                onOpenCard={onOpenCard}
              />
            ))}
          </div>
        ) : null}
      </div>
    </section>
  );
}
