import { useState } from 'react';
import { ExternalLink, Copy, Check, ScanText, Link2 } from 'lucide-react';

export interface CollectedDataItem {
    id: string;
    type: 'text' | 'image' | 'link';
    title: string;
    subtitle?: string;
    timeLabel?: string;
    excerpt?: string;
    tag?: string;
    imageUrl?: string;
    href?: string;
}

export function CollectedDataCard({ item }: { item: CollectedDataItem }) {
    const [copied, setCopied] = useState(false);

    const handleCopy = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();

        if (!item.excerpt) return;

        navigator.clipboard.writeText(item.excerpt);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const cardBaseClass =
        'group relative w-full flex-shrink-0 overflow-hidden rounded-tl-[30px] rounded-br-[30px] rounded-tr-[10px] rounded-bl-[10px] border border-text-secondary/[0.08] glass-popover bg-surface-lowest/75 backdrop-blur-md transition-colors hover:border-text-secondary/[0.13] hover:bg-surface-container/90 bg-surface-container/80';

    const cardContent = (
        <>
            {item.type === 'image' ? (
                <article className={cardBaseClass}>
                    <div className="relative h-40 w-full">
                        {item.imageUrl ? (
                            <img src={item.imageUrl} alt="" className="h-full w-full object-cover" />
                        ) : (
                            <div className="flex h-full w-full items-center justify-center bg-surface-lowest">
                                <div className="h-full w-full bg-gradient-to-br from-action-accent/20 to-scrim/40" />
                            </div>
                        )}

                        <div className="absolute inset-0 bg-gradient-to-t from-scrim/80 via-scrim/20 to-transparent" />

                        <div className="absolute bottom-0 left-0 right-0 p-4">
                            <h3 className="text-sm font-bold leading-tight text-text-primary">{item.title}</h3>
                            <p className="mt-1 text-xs font-medium text-text-secondary/80">
                                {item.subtitle || 'Captured from Source'}
                            </p>
                        </div>

                        <button
                            type="button"
                            onClick={handleCopy}
                            className="absolute right-3 top-3 rounded-lg p-1.5 text-text-primary/60 transition hover:bg-text-primary/8 hover:text-action-accent"
                        >
                            {copied ? <Check size={16} /> : <Copy size={16} />}
                        </button>
                    </div>
                </article>
            ) : item.type === 'link' ? (
                <article className={`flex flex-col gap-3 p-4 ${cardBaseClass}`}>
                    <header className="flex items-start justify-between gap-3">
                        <div className="flex min-w-0 items-start gap-3">
                            <SourceIcon type="link" />
                            <div className="min-w-0">
                                <h3 className="line-clamp-2 text-sm font-bold leading-snug text-text-primary">{item.title}</h3>
                                {item.timeLabel ? (
                                    <p className="mt-0.5 text-[11px] font-medium text-text-secondary/40">
                                        {item.timeLabel}
                                    </p>
                                ) : null}
                            </div>
                        </div>

                        <button
                            type="button"
                            onClick={handleCopy}
                            className="shrink-0 rounded-lg p-1.5 text-text-secondary/65 transition hover:bg-text-primary/8 hover:text-action-accent"
                        >
                            {copied ? <Check size={15} /> : <Copy size={15} />}
                        </button>
                    </header>

                    {item.excerpt ? (
                        <p className="line-clamp-2 text-sm leading-relaxed text-text-secondary/65">
                            {item.excerpt}
                        </p>
                    ) : null}
                </article>
            ) : (
                <article className={`flex flex-col gap-3 p-4 ${cardBaseClass}`}>
                    <header className="flex items-start justify-between gap-3">
                        <div className="flex min-w-0 flex-1 items-start gap-3">
                            <SourceIcon type="text" />
                            <div className="min-w-0 flex-1">
                                <h3 className="line-clamp-2 text-sm font-bold leading-snug text-text-primary">{item.title}</h3>
                                <p className="mt-0.5 text-xs font-medium text-text-secondary/40">
                                    {item.timeLabel}
                                </p>
                            </div>
                        </div>

                        <button
                            type="button"
                            onClick={handleCopy}
                            className="shrink-0 rounded-lg p-1.5 text-text-secondary/65 transition hover:bg-text-primary/8 hover:text-action-accent"
                        >
                            {copied ? <Check size={15} /> : <Copy size={15} />}
                        </button>
                    </header>

                    {item.excerpt ? (
                        <p className="line-clamp-3 select-text text-sm leading-relaxed text-text-secondary/70">
                            "{item.excerpt}"
                        </p>
                    ) : null}

                    <footer className="mt-1 flex items-center justify-between border-t border-text-secondary/[0.06] pt-3">
                        {item.tag ? (
                            <span className="max-w-[180px] truncate rounded-full border border-text-secondary/8 bg-text-primary/[0.03] px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider text-text-secondary/55">
                                {item.tag}
                            </span>
                        ) : (
                            <span />
                        )}

                        {item.href ? (
                            <div className="rounded-lg bg-text-primary/5 p-1.5 transition group-hover:bg-action-accent/10">
                                <ExternalLink
                                    size={15}
                                    className="text-text-secondary/80 group-hover:text-action-accent"
                                />
                            </div>
                        ) : null}
                    </footer>
                </article>
            )}
        </>
    );

    if (!item.href) return cardContent;

    return (
        <a href={item.href} target="_blank" rel="noreferrer" className="block">
            {cardContent}
        </a>
    );
}

function SourceIcon({ type }: { type: 'link' | 'text' }) {
    const Icon = type === 'link' ? Link2 : ScanText;

    return (
        <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center text-action-accent">
            <Icon size={18} strokeWidth={1.9} />
        </span>
    );
}
