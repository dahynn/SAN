interface ArchiveItemProps {
  title: string;
  meta: string;
  sourceType: string;
  content?: string | null;
}

export default function ArchiveItem({ title, meta, sourceType, content }: ArchiveItemProps) {
  return (
    <article className="rounded-leaf border border-text-secondary/12 glass-card bg-surface-container/80 px-5 py-5 transition hover:border-primary-signal/25 hover:bg-surface-container">
      <h3 className="line-clamp-1 cursor-text select-text text-body-main-bold text-text-primary">
        {title}
      </h3>

      {content ? (
        <p className="mt-3 line-clamp-3 cursor-text select-text text-body-sm leading-6 text-text-secondary/85">
          {content}
        </p>
      ) : null}

      <div className="mt-4 flex flex-wrap items-center gap-2">
        <span className="inline-flex items-center rounded-full border border-primary-signal/25 bg-primary-signal/8 px-3 py-1.5 text-caption-bold uppercase text-primary-signal">
          {sourceType}
        </span>
        <span className="ml-auto text-caption text-text-secondary/55">
          {meta}
        </span>
      </div>
    </article>
  );
}
