import { useRef } from 'react';
import { Calendar, ChevronLeft, ChevronRight, Search } from 'lucide-react';
import { TILModeTabs, type TILMode } from './TILModeTabs';

interface TILWorkspaceHeaderProps {
  activeTab: TILMode;
  dateValue: string;
  dateLabel: string;
  onTabChange?: (tab: TILMode) => void;
  onDateChange?: (date: string) => void;
  onSearch?: (value: string) => void;
}

export function TILWorkspaceHeader({
  activeTab,
  dateValue,
  dateLabel,
  onTabChange,
  onDateChange,
  onSearch,
}: TILWorkspaceHeaderProps) {
  const dateInputRef = useRef<HTMLInputElement | null>(null);

  const openDatePicker = () => {
    const input = dateInputRef.current;
    if (!input) return;

    if (typeof input.showPicker === 'function') {
      input.showPicker();
      return;
    }

    input.focus();
  };

  const shiftDate = (days: number) => {
    const date = new Date(`${dateValue}T00:00:00`);
    if (Number.isNaN(date.getTime())) return;

    date.setDate(date.getDate() + days);
    onDateChange?.(formatDate(date));
  };

  return (
    <header className="glass-panel grid min-h-16 w-full min-w-0 grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-dashboard-gap bg-surface-lowest/90 backdrop-blur-xl">
      <div className="flex min-w-0 items-center gap-dashboard-gap">
        <h1 className="shrink-0 text-h2-bold leading-none text-text-primary">
          TIL Workspace
        </h1>

        <TILModeTabs activeTab={activeTab} onChange={onTabChange} />
      </div>

      <div className="glass-panel flex min-w-fit items-center justify-center overflow-hidden rounded-[8px] border border-text-primary/5 bg-surface-lowest/92 text-body-sm-bold text-text-primary backdrop-blur-xl focus-within:ring-1 focus-within:ring-action-accent/40">
        <button
          type="button"
          aria-label="Previous date"
          onClick={() => shiftDate(-1)}
          className="flex h-11 w-10 items-center justify-center text-text-secondary transition hover:bg-surface-container hover:text-action-accent"
        >
          <ChevronLeft size={18} />
        </button>

        <button
          type="button"
          onClick={openDatePicker}
          className="relative flex h-11 min-w-44 items-center justify-center gap-sm px-md text-text-primary transition hover:bg-surface-container"
        >
          <Calendar size={20} className="text-action-accent" />
          <span>{dateLabel}</span>
          <input
            ref={dateInputRef}
            type="date"
            value={dateValue}
            aria-label="Select TIL date"
            onChange={(event) => onDateChange?.(event.target.value)}
            className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
          />
        </button>

        <button
          type="button"
          aria-label="Next date"
          onClick={() => shiftDate(1)}
          className="flex h-11 w-10 items-center justify-center text-text-secondary transition hover:bg-surface-container hover:text-action-accent"
        >
          <ChevronRight size={18} />
        </button>
      </div>

      <label className="glass-panel ml-auto flex h-11 w-full max-w-80 min-w-0 items-center gap-md bg-surface-lowest/92 px-md focus-within:ring-1 focus-within:ring-action-accent/40">
        <Search
          size={20}
          className="shrink-0 text-text-secondary"
        />
        <input
          type="search"
          placeholder="키워드로 검색..."
          onChange={(event) => onSearch?.(event.target.value)}
          className="h-full min-w-0 flex-1 bg-transparent text-body-sm text-text-primary outline-none placeholder:text-text-ghost"
        />
      </label>
    </header>
  );
}

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
