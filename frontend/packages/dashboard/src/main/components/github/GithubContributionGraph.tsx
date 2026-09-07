import { RefreshCw } from 'lucide-react';
import { useMemo } from 'react';
import type { TilGithubContributionDayResponse, TilGithubContributionResponse } from '@san/shared';

type ContributionCell = TilGithubContributionDayResponse | null;

const CONTRIBUTION_LEVEL_CLASS = [
  'border-[var(--github-contribution-level-0-border)] bg-[var(--github-contribution-level-0-bg)]',
  'border-[var(--github-contribution-level-1-border)] bg-[var(--github-contribution-level-1-bg)]',
  'border-[var(--github-contribution-level-2-border)] bg-[var(--github-contribution-level-2-bg)]',
  'border-[var(--github-contribution-level-3-border)] bg-[var(--github-contribution-level-3-bg)]',
  'border-[var(--github-contribution-level-4-border)] bg-[var(--github-contribution-level-4-bg)]',
];

const MONTH_LABELS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
];

const CONTRIBUTION_COPY = {
  title: 'TIL \uCEE4\uBC0B \uC794\uB514',
  loading: 'GitHub\uC5D0 \uAE30\uB85D\uB41C TIL \uCEE4\uBC0B\uC744 \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4.',
  refresh: '\uC0C8\uB85C\uACE0\uCE68',
  monday: 'Mon',
  wednesday: 'Wed',
  friday: 'Fri',
  commitUnit: '\uAC1C \uCEE4\uBC0B',
};

const CELL_SIZE = 12;
const CELL_GAP = 4;
const WEEK_STEP = CELL_SIZE + CELL_GAP;

interface GithubContributionGraphProps {
  contribution: TilGithubContributionResponse | null;
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
  className?: string;
}

function parseLocalDate(date: string) {
  return new Date(`${date}T00:00:00`);
}

function buildContributionWeeks(days: TilGithubContributionDayResponse[]) {
  if (days.length === 0) return [];

  const cells: ContributionCell[] = Array.from({ length: parseLocalDate(days[0].date).getDay() }, () => null);
  cells.push(...days);

  while (cells.length % 7 !== 0) {
    cells.push(null);
  }

  const weeks: ContributionCell[][] = [];
  for (let index = 0; index < cells.length; index += 7) {
    weeks.push(cells.slice(index, index + 7));
  }
  return weeks;
}

function getContributionMonthLabels(weeks: ContributionCell[][]) {
  let previousMonth = -1;

  return weeks
    .map((week, weekIndex) => {
      const firstDay = week.find(Boolean);
      if (!firstDay) return null;

      const month = parseLocalDate(firstDay.date).getMonth();
      if (month === previousMonth && weekIndex !== 0) return null;

      previousMonth = month;
      return { weekIndex, label: MONTH_LABELS[month] };
    })
    .filter(Boolean) as { weekIndex: number; label: string }[];
}

function formatContributionDate(date: string) {
  const value = parseLocalDate(date);
  return `${value.getFullYear()}\uB144 ${value.getMonth() + 1}\uC6D4 ${value.getDate()}\uC77C`;
}

export function GithubContributionGraph({
  contribution,
  isLoading,
  error,
  onRetry,
  className = '',
}: GithubContributionGraphProps) {
  const weeks = useMemo(() => buildContributionWeeks(contribution?.days ?? []), [contribution?.days]);
  const monthLabels = useMemo(() => getContributionMonthLabels(weeks), [weeks]);
  const yearText = contribution ? `${formatContributionDate(contribution.from)} - ${formatContributionDate(contribution.to)}` : '';

  return (
    <div className={`rounded-lg border border-[var(--github-contribution-border)] bg-[var(--github-contribution-surface)] p-5 ${className}`}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-bold text-text-primary">
            {contribution
              ? `Total ${contribution.totalCommits} commits in SAN`
              : CONTRIBUTION_COPY.title}
          </h3>
          <p className="mt-1 text-[11px] text-text-primary/45">{yearText || CONTRIBUTION_COPY.loading}</p>
        </div>
        <button
          type="button"
          onClick={onRetry}
          disabled={isLoading}
          className="text-text-primary/45 transition hover:text-[var(--color-action-accent)] disabled:opacity-40"
          title={CONTRIBUTION_COPY.refresh}
        >
          <RefreshCw size={15} className={isLoading ? 'animate-spin' : ''} />
        </button>
      </div>

      {error ? (
        <div className="mt-4 rounded-md border border-red-500/15 bg-red-500/[0.06] px-3 py-2 text-xs font-medium text-red-200/80">
          {error}
        </div>
      ) : (
        <div className="mt-5 flex min-w-0 justify-center overflow-x-auto overflow-y-hidden pb-1 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
          <div className="min-w-0">
            <div className="relative ml-10 h-5">
              {monthLabels.map(({ weekIndex, label }) => (
                <span
                  key={`${label}-${weekIndex}`}
                  className="absolute top-0 text-xs font-semibold text-text-primary/55"
                  style={{ left: `${weekIndex * WEEK_STEP}px` }}
                >
                  {label}
                </span>
              ))}
            </div>

            <div className="flex gap-3">
              <div
                className="grid grid-rows-7 text-xs font-semibold leading-none text-text-primary/50"
                style={{ gap: `${CELL_GAP}px`, paddingTop: `${CELL_SIZE + CELL_GAP}px` }}
              >
                <span></span>
                <span>{CONTRIBUTION_COPY.monday}</span>
                <span></span>
                <span>{CONTRIBUTION_COPY.wednesday}</span>
                <span></span>
                <span>{CONTRIBUTION_COPY.friday}</span>
                <span></span>
              </div>

              <div className="flex" style={{ gap: `${CELL_GAP}px` }}>
                {isLoading && weeks.length === 0
                  ? Array.from({ length: 27 }, (_, weekIndex) => (
                      <div key={weekIndex} className="grid grid-rows-7" style={{ gap: `${CELL_GAP}px` }}>
                        {Array.from({ length: 7 }, (_, dayIndex) => (
                          <span
                            key={dayIndex}
                            className="animate-pulse rounded-[2px] border border-[var(--github-contribution-level-0-border)] bg-[var(--github-contribution-level-0-bg)]"
                            style={{ width: CELL_SIZE, height: CELL_SIZE }}
                          />
                        ))}
                      </div>
                    ))
                  : weeks.map((week, weekIndex) => (
                      <div key={weekIndex} className="grid grid-rows-7" style={{ gap: `${CELL_GAP}px` }}>
                        {week.map((day, dayIndex) => (
                          <span
                            key={day?.date ?? `${weekIndex}-${dayIndex}`}
                            className={`rounded-[2px] border transition ${day ? CONTRIBUTION_LEVEL_CLASS[Math.min(day.level, 4)] : 'border-transparent bg-transparent'} ${day ? 'hover:scale-125 hover:border-[var(--color-action-accent)]/70' : ''}`}
                            style={{ width: CELL_SIZE, height: CELL_SIZE }}
                            title={day ? `${formatContributionDate(day.date)}: ${day.count}${CONTRIBUTION_COPY.commitUnit}` : undefined}
                          />
                        ))}
                      </div>
                    ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
