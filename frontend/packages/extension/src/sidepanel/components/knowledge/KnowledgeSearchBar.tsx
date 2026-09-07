import type { FormEvent } from 'react';
import { Search } from 'lucide-react';

const SEARCH_PLACEHOLDER = '\uC800\uC7A5\uD55C \uC9C0\uC2DD \uAC80\uC0C9';

interface KnowledgeSearchBarProps {
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
  onSubmit: () => void;
}

export function KnowledgeSearchBar({
  value,
  disabled = false,
  onChange,
  onSubmit,
}: KnowledgeSearchBarProps) {
  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSubmit();
  };

  return (
    <form
      data-tour-id="knowledge-search"
      className="relative flex h-10 w-[min(100%,220px)] shrink-0 self-stretch"
      onSubmit={handleSubmit}
    >
      <Search
        size={14}
        className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-primary-signal/75"
        aria-hidden="true"
      />
      <input
        type="search"
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        placeholder={SEARCH_PLACEHOLDER}
        className="h-10 w-full rounded-full border border-primary-signal/15 glass-panel bg-surface-container/45 pl-4 pr-10 text-left text-body-sm text-text-primary outline-none transition placeholder:text-text-secondary/50 focus:border-primary-signal/40 focus:bg-surface-container/75 disabled:cursor-not-allowed disabled:opacity-60 [&::-webkit-search-cancel-button]:hidden"
      />
    </form>
  );
}
