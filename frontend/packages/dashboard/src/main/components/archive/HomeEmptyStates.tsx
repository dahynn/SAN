import { ArrowRight, GitBranch, Sprout } from 'lucide-react';

interface Action {
  label: string;
  onClick: () => void;
}

interface GraphEmptyStateProps {
  primaryAction?: Action;
  secondaryAction?: Action;
}

interface KnowledgeCardsEmptyStateProps {
  primaryAction?: Action;
}

export function HomeGraphEmptyState({ primaryAction, secondaryAction }: GraphEmptyStateProps) {
  return (
    <section className="relative grid min-h-[min(70vh,34rem)] w-full min-w-0 overflow-hidden rounded-leaf bg-surface-low px-6 py-12 text-center sm:px-8">
      <div className="pointer-events-none absolute inset-0 opacity-80">
        <div className="absolute left-1/2 top-1/2 h-60 w-60 -translate-x-1/2 -translate-y-1/2 rounded-full bg-home-archive-accent-10 blur-2xl" />
        <div className="absolute right-8 top-8 hidden h-48 w-48 rounded-leaf border border-text-secondary/10 bg-misty-teal/20 opacity-30 sm:block" />
      </div>

      <div className="relative z-10 flex min-w-0 flex-col items-center justify-center gap-6">
        <div className="relative flex h-24 w-24 items-center justify-center">
          <div className="absolute inset-0 rounded-full bg-home-archive-accent-15" />
          <div className="relative flex h-20 w-20 items-center justify-center rounded-leaf border border-home-archive-accent-25 bg-surface-highest text-home-archive-accent">
            <Sprout size={36} strokeWidth={1.7} aria-hidden="true" />
          </div>
        </div>

        <div className="flex max-w-xl flex-col items-center gap-4">
          <h2 className="text-h1-bold text-text-primary">고요한 지식의 숲</h2>
          <p className="text-body-main leading-7 text-text-secondary sm:text-body-lg">
            현재 아카이브가 비어 있습니다.
            <br />
            새로운 연구 노트를 작성하거나 기존 문서를 임포트하여
            <br className="hidden sm:block" />
            프로젝트 SAN의 네트워크를 확장해 보세요.
          </p>
        </div>

        {(primaryAction || secondaryAction) ? (
          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            {primaryAction ? (
              <button
                type="button"
                onClick={primaryAction.onClick}
                className="inline-flex min-h-11 items-center justify-center rounded-leaf bg-action-accent px-6 text-body-sm-bold font-bold text-background transition hover:bg-action-accent-hover"
              >
                {primaryAction.label}
              </button>
            ) : null}

            {secondaryAction ? (
              <button
                type="button"
                onClick={secondaryAction.onClick}
                className="inline-flex min-h-11 items-center justify-center rounded-leaf border border-text-secondary/10 glass-panel bg-surface-lowest/70 px-6 text-body-sm-bold font-bold text-text-primary transition hover:bg-surface-container/90 bg-surface-container/80"
              >
                {secondaryAction.label}
              </button>
            ) : null}
          </div>
        ) : null}
      </div>
    </section>
  );
}

export function HomeKnowledgeCardsEmptyState({ primaryAction }: KnowledgeCardsEmptyStateProps) {
  return (
    <article className="group relative flex h-[280px] w-[min(88vw,24rem)] min-w-0 shrink-0 snap-start flex-col justify-between rounded-tl-[32px] rounded-br-[32px] rounded-tr-2xl rounded-bl-2xl glass-card bg-surface-container/80 p-6 !shadow-none md:w-[calc((100%-24px)/2)] xl:w-[calc((100%-48px)/3)]">
      <div>
        <div className="mb-6 flex items-center justify-between">
          <span className="flex items-center justify-center rounded-tl-xl rounded-br-xl rounded-tr-sm rounded-bl-sm border border-action-accent/20 bg-action-accent/5 px-3 py-1.5 text-[11px] font-bold tracking-wide text-action-accent">
            Archive
          </span>
        </div>

        <div className="flex items-start gap-3">
          <div className="mt-1 flex h-10 w-10 shrink-0 items-center justify-center rounded-tl-[18px] rounded-br-[18px] rounded-tr-md rounded-bl-md glass-panel bg-surface-lowest/70 text-text-secondary !shadow-none">
            <GitBranch size={18} strokeWidth={1.7} aria-hidden="true" />
          </div>
          <div className="min-w-0">
            <h3 className="line-clamp-2 text-xl font-bold leading-snug text-text-primary">
              연결된 노드 없음
            </h3>
            <p className="mt-3 line-clamp-3 text-sm leading-relaxed text-text-secondary">
              데이터 간의 상관관계가 아직 정의되지 않았습니다. 두 개 이상의 지식 노드를 연결하여 맵을 생성하세요.
            </p>
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        {primaryAction ? (
          <button
            type="button"
            onClick={primaryAction.onClick}
            className="flex h-11 items-center justify-center gap-2 rounded-tl-[20px] rounded-br-[20px] rounded-tr-md rounded-bl-md glass-panel bg-surface-lowest/70 px-4 text-sm font-bold text-text-secondary !shadow-none transition-colors hover:bg-action-accent/10 hover:text-action-accent"
          >
            {primaryAction.label}
            <ArrowRight size={14} aria-hidden="true" />
          </button>
        ) : (
          <div className="flex h-11 w-11 items-center justify-center rounded-tl-[20px] rounded-br-[20px] rounded-tr-md rounded-bl-md glass-panel bg-surface-lowest/70 text-text-secondary !shadow-none">
            <ArrowRight size={18} aria-hidden="true" />
          </div>
        )}
      </div>
    </article>
  );
}
