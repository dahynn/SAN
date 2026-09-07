import { type FormEvent, useState } from 'react';
import { Search, X } from 'lucide-react';

interface SearchBarProps {
  defaultValue?: string;
  placeholder?: string;
  className?: string;
  onSearch: (keyword: string) => void;
}

export function SearchBar({
  defaultValue = '',
  placeholder = 'Search knowledge cards...',
  className = 'w-72',
  onSearch,
}: SearchBarProps) {
  const [value, setValue] = useState(defaultValue);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSearch(value.trim());
  };

  return (
    <form
      onSubmit={handleSubmit}
      role="search"
      className={`inline-flex h-11 items-center gap-3 rounded-md bg-text-primary/5 px-4 transition focus-within:ring-1 focus-within:ring-action-accent/30 ${className}`}
    >
      <Search size={18} aria-hidden="true" className="shrink-0 text-text-secondary/80" />
      <input
        type="search"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder={placeholder}
        className="h-full flex-1 bg-transparent px-0 text-sm tracking-wide text-text-primary outline-none placeholder:text-text-secondary/50 [&::-webkit-search-cancel-button]:hidden [&::-webkit-search-decoration]:hidden"
      />
      {value ? (
        <button
          type="button"
          onClick={() => setValue('')}
          className="flex shrink-0 items-center justify-center text-text-secondary transition hover:text-text-primary"
          aria-label="Clear search"
        >
          <X size={16} />
        </button>
      ) : null}
    </form>
  );
}
