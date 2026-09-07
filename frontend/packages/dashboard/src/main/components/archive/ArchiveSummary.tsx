interface ArchiveSummaryProps {
  summary: string | null | undefined;
  className?: string;
}

const BULLET_LINE_PATTERN = /^(?:[-*\u2022]\s+|\d+[.)]\s+)(.+)$/;

function toSummaryItems(summary: string | null | undefined) {
  const lines = summary
    ?.split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean) ?? [];

  return lines.map((line) => line.match(BULLET_LINE_PATTERN)?.[1]?.trim() ?? line);
}

export function ArchiveSummary({ summary, className = '' }: ArchiveSummaryProps) {
  const items = toSummaryItems(summary);

  if (items.length === 0) {
    return (
      <p className={className}>
        요약 내용이 아직 생성되지 않았습니다.
      </p>
    );
  }

  if (items.length === 1) {
    return (
      <p className={className}>
        {items[0]}
      </p>
    );
  }

  return (
    <ul className={className}>
      {items.map((item, index) => (
        <li key={`${item}-${index}`} className="flex items-start gap-2">
          <span
            className="mt-[0.6em] h-1.5 w-1.5 shrink-0 rounded-full bg-archive-card-accent shadow-[0_0_8px_rgba(111,255,190,0.36)]"
            aria-hidden="true"
          />
          <span className="min-w-0 flex-1">{item}</span>
        </li>
      ))}
    </ul>
  );
}
