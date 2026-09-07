import { Archive, FileText, Inbox, Lightbulb, Plus, Search, Star } from 'lucide-react';

export type EmptyStateType = 'recall' | 'archive' | 'search' | 'til' | 'scrap' | 'custom';
export type EmptyStateVariant = 'full' | 'inline';

interface ActionButton {
  label: string;
  onClick: () => void;
}

export interface EmptyStateProps {
  type?: EmptyStateType;
  variant?: EmptyStateVariant;
  title?: string;
  description?: string;
  primaryAction?: ActionButton;
  secondaryAction?: ActionButton;
}

const EMPTY_PRESET: Record<EmptyStateType, { title: string; description: string }> = {
  recall: {
    title: '아직 떠올릴 지식이 없어요',
    description: '저장한 자료가 쌓이면 다시 살펴볼 수 있어요.',
  },
  archive: {
    title: '저장된 카드가 없어요',
    description: '스크랩을 저장하면 아카이브에서 다시 확인할 수 있어요.',
  },
  search: {
    title: '검색 결과가 없어요',
    description: '다른 키워드로 검색하거나 조건을 줄여보세요.',
  },
  til: {
    title: '작성된 TIL이 없어요',
    description: '오늘의 학습 내용을 정리하면 이곳에 표시돼요.',
  },
  scrap: {
    title: '저장된 스크랩이 없어요',
    description: '웹에서 중요한 자료를 저장하면 카드로 정리돼요.',
  },
  custom: {
    title: '표시할 내용이 없어요',
    description: '조건을 바꾸거나 새 항목을 추가해보세요.',
  },
};

function EmptyIcon({ type, size }: { type: EmptyStateType; size: number }) {
  const iconProps = {
    size,
    strokeWidth: 1.7,
    'aria-hidden': true,
  };

  if (type === 'recall') {
    return <Lightbulb {...iconProps} />;
  }

  if (type === 'archive') {
    return <Archive {...iconProps} />;
  }

  if (type === 'search') {
    return <Search {...iconProps} />;
  }

  if (type === 'til') {
    return <FileText {...iconProps} />;
  }

  if (type === 'scrap') {
    return <Star {...iconProps} />;
  }

  return <Inbox {...iconProps} />;
}

export function EmptyState({
  type = 'archive',
  variant = 'inline',
  title,
  description,
  primaryAction,
  secondaryAction,
}: EmptyStateProps) {
  const preset = EMPTY_PRESET[type];
  const displayTitle = title ?? preset.title;
  const displayDescription = description ?? preset.description;
  const isFull = variant === 'full';

  return (
    <section
      className={[
        'flex flex-col items-center justify-center text-center',
        'rounded-tl-[32px] rounded-br-[32px] rounded-tr-lg rounded-bl-lg',
        'border border-text-secondary/10 bg-surface-low',
        isFull ? 'min-h-[400px] w-full gap-5 px-8 py-16' : 'min-h-[280px] w-full gap-4 px-6 py-10',
      ].join(' ')}
    >
      <div
        className={[
          'relative flex items-center justify-center',
          'rounded-tl-[32px] rounded-br-[32px] rounded-tr-lg rounded-bl-lg',
          'border border-primary-signal/20 bg-misty-teal/30 text-primary-signal',
          isFull ? 'h-20 w-20' : 'h-14 w-14',
        ].join(' ')}
      >
        <EmptyIcon type={type} size={isFull ? 34 : 24} />
        {primaryAction ? (
          <span className="absolute -bottom-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary-signal text-background">
            <Plus size={12} strokeWidth={2.4} aria-hidden="true" />
          </span>
        ) : null}
      </div>

      <div className="flex max-w-md flex-col items-center gap-2">
        <h2 className={isFull ? 'text-h2-bold text-text-primary' : 'text-body-main-bold text-text-primary'}>
          {displayTitle}
        </h2>
        <p className={isFull ? 'whitespace-pre-line text-body-main text-text-secondary' : 'whitespace-pre-line text-body-sm text-text-secondary'}>
          {displayDescription}
        </p>
      </div>

      {(primaryAction || secondaryAction) ? (
        <div className="flex flex-wrap items-center justify-center gap-3">
          {primaryAction ? (
            <button
              type="button"
              onClick={primaryAction.onClick}
              className={[
                'inline-flex items-center justify-center rounded-full',
                'bg-primary-signal font-semibold text-background transition hover:bg-primary-signal-hover',
                isFull ? 'min-h-11 px-6 text-body-sm-bold' : 'min-h-9 px-4 text-caption-bold',
              ].join(' ')}
            >
              {primaryAction.label}
            </button>
          ) : null}

          {secondaryAction ? (
            <button
              type="button"
              onClick={secondaryAction.onClick}
              className={[
                'inline-flex items-center justify-center rounded-full',
                'border border-text-secondary/10 glass-panel bg-surface-lowest/70 font-semibold text-text-secondary transition hover:bg-surface-container/90 bg-surface-container/80 hover:text-text-primary',
                isFull ? 'min-h-11 px-6 text-body-sm-bold' : 'min-h-9 px-4 text-caption-bold',
              ].join(' ')}
            >
              {secondaryAction.label}
            </button>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
