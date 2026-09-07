import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowUpRight, BrainCircuit, PackageOpen } from 'lucide-react';
import type { KnowledgeCardResponse, TilResponse } from '@san/shared';
import type { TilRecallCardsQuery } from '../types';

const EMPTY_RECALL_CARDS: KnowledgeCardResponse[] = [];

interface RecallHistoryProps {
    recallCardsQuery: TilRecallCardsQuery;
    selectedTil: TilResponse | null;
    variant?: 'page' | 'panel';
}

export function RecallHistory({ recallCardsQuery, selectedTil, variant = 'page' }: RecallHistoryProps) {
    const navigate = useNavigate();
    const itemRefs = useRef<Array<HTMLElement | null>>([]);
    const [activeIndex, setActiveIndex] = useState(0);
    const cards = recallCardsQuery.data?.recallCards ?? EMPTY_RECALL_CARDS;
    const visibleCards = useMemo(() => cards.slice(0, 12), [cards]);
    const isPanel = variant === 'panel';
    const sectionClassName = isPanel
        ? 'no-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto bg-transparent px-5 py-5'
        : 'bg-transparent px-7 py-7 md:px-[60px]';

    useEffect(() => {
        itemRefs.current = itemRefs.current.slice(0, visibleCards.length);
        if (visibleCards.length === 0) {
            return;
        }

        const observer = new IntersectionObserver(
            (entries) => {
                const focusedEntry = entries
                    .filter((entry) => entry.isIntersecting)
                    .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];

                const index = Number(focusedEntry?.target.getAttribute('data-recall-index'));
                if (Number.isFinite(index)) {
                    setActiveIndex(index);
                }
            },
            {
                threshold: [0.35, 0.6, 0.85],
                rootMargin: '-25% 0px -35% 0px',
            },
        );

        itemRefs.current.forEach((item) => {
            if (item) observer.observe(item);
        });

        return () => observer.disconnect();
    }, [visibleCards.length]);

    if (!selectedTil) {
        return (
            <section className={sectionClassName}>
                <RecallEmptyState />
            </section>
        );
    }

    if (recallCardsQuery.isPending) {
        return (
            <section className={sectionClassName}>
                {!isPanel ? <RecallHeader count={0} /> : null}
                <div className={`${isPanel ? 'mt-1' : 'mt-7'} space-y-4`}>
                    {[0, 1].map((item) => (
                        <div key={item} className="ml-8 h-20 animate-pulse rounded-lg bg-text-primary/[0.035]" />
                    ))}
                </div>
            </section>
        );
    }

    if (recallCardsQuery.isError) {
        return (
            <section className={sectionClassName}>
                <PanelStatus message={"\uB9AC\uCF5C \uCE74\uB4DC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC5B4\uC694."} tone="error" />
            </section>
        );
    }

    if (visibleCards.length === 0) {
        return (
            <section className={sectionClassName}>
                {!isPanel ? <RecallHeader count={0} /> : null}
                <PanelStatus
                    message={"\uC544\uC9C1 \uD568\uAED8 \uBCFC \uB9CC\uD55C \uC9C0\uC2DD \uCE74\uB4DC\uAC00 \uC5C6\uC5B4\uC694."}
                    compact={isPanel}
                />
            </section>
        );
    }

    return (
        <section className={sectionClassName}>
            {!isPanel ? <RecallHeader count={cards.length} /> : null}

            <div className={`relative ${isPanel ? 'mt-1' : 'mt-6'}`}>
                <span className="pointer-events-none absolute bottom-0 left-[14px] top-0 w-px bg-action-accent/26" />
                <div className="space-y-8">
                    {visibleCards.map((card, index) => {
                        const isActive = index === activeIndex;
                        const isFirst = index === 0;

                        return (
                            <article
                                key={card.cardId}
                                onMouseEnter={() => setActiveIndex(index)}
                                onFocus={() => setActiveIndex(index)}
                                ref={(node) => {
                                    itemRefs.current[index] = node;
                                }}
                                data-recall-index={index}
                                className={`group relative grid grid-cols-[28px_minmax(0,1fr)] ${isPanel ? 'gap-3' : 'gap-5'}`}
                            >
                                <div className="relative flex justify-center">
                                    <span
                                        className={`relative z-10 mt-1.5 block h-2.5 w-2.5 rounded-full border transition-all duration-300 ${
                                            isActive
                                                ? 'border-action-accent/80 bg-action-accent'
                                                : 'border-action-accent/12 bg-surface-highest'
                                        }`}
                                    />
                                </div>

                                <button
                                    type="button"
                                    onClick={() => navigate(`/cards/${card.cardId}`)}
                                    className="relative min-w-0 text-left transition-opacity hover:opacity-90"
                                >
                                    <div>
                                        <p className={`text-sm font-extrabold ${isActive ? 'text-action-accent' : 'text-text-secondary/55'}`}>
                                            {isFirst ? '\u0031\uC77C \uC804' : formatTimeLabel(card.createdAt)}
                                        </p>

                                        <h3 className={`${isPanel ? 'text-sm' : 'text-base'} mt-2 line-clamp-2 font-extrabold leading-snug text-text-primary`}>
                                            {"AI\uAC00 \uC78A\uC744 \uB54C\uCBE4 \uAC00\uC838\uB2E4\uC900 \uC9C0\uC2DD: "}
                                            {card.title}
                                        </h3>

                                        <p className={`${isPanel ? 'text-xs' : 'text-sm'} mt-3 line-clamp-2 leading-relaxed text-text-secondary/85`}>
                                            {card.summary ?? '\uC774 TIL\uACFC \uD568\uAED8 \uB2E4\uC2DC \uD655\uC778\uD558\uBA74 \uC88B\uC740 \uC9C0\uC2DD \uCE74\uB4DC\uC785\uB2C8\uB2E4.'}
                                        </p>
                                    </div>

                                    <footer className="mt-3 flex flex-wrap items-center gap-2">
                                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-action-accent">
                                            {'\uC6D0\uBB38 \uBCF4\uAE30'}
                                            <ArrowUpRight size={11} />
                                        </span>
                                        {card.category?.categoryName ? (
                                            <span className="text-[11px] font-bold uppercase tracking-wider text-text-secondary/55">
                                                {card.category.categoryName}
                                            </span>
                                        ) : null}
                                        {card.tags.slice(0, 3).map((tag) => (
                                            <span key={tag.tagId} className="rounded-full bg-text-primary/5 px-2 py-1 text-[10px] font-semibold text-text-secondary/60">
                                                #{tag.tagName}
                                            </span>
                                        ))}
                                    </footer>
                                </button>
                            </article>
                        );
                    })}
                </div>
            </div>
        </section>
    );
}

function RecallEmptyState() {
    return (
        <div className="flex min-h-[220px] flex-col items-center justify-center gap-3 pt-6 text-center">
            <div className="til-light-teal-accent text-action-accent/80 drop-shadow-[0_0_18px_rgba(74,222,128,0.22)]">
                <PackageOpen size={44} strokeWidth={1.6} aria-hidden="true" />
            </div>
            <div className="space-y-1">
                <p className="text-body-sm font-medium text-text-secondary">
                    {'\uC544\uC9C1 \uCE74\uB4DC\uAC00 \uC5C6\uC5B4\uC694'}
                </p>
                <p className="text-caption text-text-secondary/60">
                    {'TIL\uC744 \uC0DD\uC131\uD558\uBA74 \uBCF5\uC2B5 \uCE74\uB4DC\uAC00 \uC774\uACF3\uC5D0 \uD45C\uC2DC\uB3FC\uC694.'}
                </p>
            </div>
        </div>
    );
}

function RecallHeader({ count }: { count: number }) {
    return (
        <header className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
                <span className="flex h-7 w-7 items-center justify-center rounded-full bg-action-accent/10 text-action-accent">
                    <BrainCircuit size={16} />
                </span>
                <h2 className="text-xl font-extrabold tracking-tight text-text-primary">Recall History</h2>
            </div>
            {count > 0 ? (
                <span className="text-xs font-bold text-text-secondary/45">
                    {count} cards
                </span>
            ) : null}
        </header>
    );
}

function PanelStatus({
    compact = true,
    message,
    tone = 'default',
}: {
    compact?: boolean;
    message: string;
    tone?: 'default' | 'error';
}) {
    return (
        <div className={`${compact ? 'py-4' : 'mt-8 py-8'} text-sm`}>
            <p className={tone === 'error' ? 'text-red-300' : 'text-text-secondary/75'}>
                {message}
            </p>
        </div>
    );
}

function formatTimeLabel(value: string) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'REMEMBERED';

    return date.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
    });
}
