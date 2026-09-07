interface KnowledgeSummaryProps {
  summary: string | null;
  className?: string;
  paragraphClassName?: string;
  listClassName?: string;
  itemClassName?: string;
}

type SummarySegment =
  | { type: 'paragraph'; text: string }
  | { type: 'list'; items: string[] };

const BULLET_LINE_PATTERN = /^(?:[-*\u2022]\s+|\d+[.)]\s+)(.+)$/;

function parseSummary(summary: string): SummarySegment[] {
  const lines = summary
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);

  if (lines.length === 0) return [];

  const normalizedLines = lines.map((line) => line.match(BULLET_LINE_PATTERN)?.[1]?.trim() ?? line);

  if (lines.length > 1 || lines.some((line) => BULLET_LINE_PATTERN.test(line))) {
    return [{ type: 'list', items: normalizedLines }];
  }

  return [{ type: 'paragraph', text: normalizedLines[0] }];
}

export function KnowledgeSummary({
  summary,
  className = '',
  paragraphClassName = '',
  listClassName = '',
  itemClassName = '',
}: KnowledgeSummaryProps) {
  if (!summary?.trim()) return null;

  const segments = parseSummary(summary);

  return (
    <div className={className}>
      {segments.map((segment, index) => {
        if (segment.type === 'list') {
          return (
            <ul key={`list-${index}`} className={listClassName}>
              {segment.items.map((item, itemIndex) => (
                <li key={`${index}-${itemIndex}`} className={itemClassName}>
                  <span
                    className="mt-[0.55em] h-1.5 w-1.5 shrink-0 rounded-full bg-action-accent shadow-[0_0_8px_rgba(111,255,190,0.45)]"
                    aria-hidden="true"
                  />
                  <span className="min-w-0 flex-1">{item}</span>
                </li>
              ))}
            </ul>
          );
        }

        return (
          <p key={`paragraph-${index}`} className={paragraphClassName}>
            {segment.text}
          </p>
        );
      })}
    </div>
  );
}
