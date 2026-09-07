import { useEffect, useMemo, useRef, useState } from 'react';
import { useInfiniteQuery } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, Calendar, CalendarDays, ChevronLeft, ChevronRight, Hash, Search, X } from 'lucide-react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useArchiveCategoryCards, type SearchCardResult, type SearchParams } from '@san/shared';
import { searchApi } from '../../api/client';
import { ContentEmptyState } from '../../components/shared/empty/ContentEmptyState';
import { ArchiveSummary } from '../components/archive/ArchiveSummary';

interface Filters {
  tag: string;
  fromDate: string;
  toDate: string;
}

export function ArchiveCategoryPage() {
  const navigate = useNavigate();
  const { categoryId } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('query')?.trim() ?? '';
  const [filters, setFilters] = useState<Filters>({ tag: '', fromDate: '', toDate: '' });
  const archiveQuery = useArchiveCategoryCards(categoryId);
  const categoryName = archiveQuery.data?.categoryName ?? '';
  const size = 12;

  const searchQuery = useInfiniteQuery({
    queryKey: ['archive-category-search', categoryId, categoryName, keyword, filters.tag, filters.fromDate, filters.toDate, size],
    queryFn: ({ pageParam }) => searchApi.search(toSearchParams(keyword, categoryName, filters, pageParam, size)),
    enabled: Boolean((keyword || filters.tag || filters.fromDate || filters.toDate) && categoryName),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => (lastPage.hasNext ? allPages.length : undefined),
  });

  const searchResults = useMemo(
    () => dedupeByCardId(searchQuery.data?.pages.flatMap((page) => page.results) ?? []),
    [searchQuery.data],
  );

  const archiveCards = archiveQuery.data?.cards ?? [];
  const isSearching = Boolean(keyword || filters.tag || filters.fromDate || filters.toDate);
  const hasActiveFilters = isSearching;

  return (
    <section className="flex w-full min-w-0 flex-col gap-8 py-12 text-text-primary">
      <header className="flex flex-col gap-4">
        <button
          type="button"
          onClick={() => navigate('/archive')}
          className="flex w-fit items-center gap-2 text-sm font-bold text-text-primary/45 transition hover:text-text-primary"
        >
          <ArrowLeft size={16} />
          전체 archive
        </button>
        <div>
          <h1 className="text-4xl font-extrabold tracking-tight">{categoryName || 'Archive'}</h1>
          <p className="mt-3 text-text-primary/50">이 폴더 안의 지식카드를 찾고, 좁히고, 다시 꺼내볼 수 있습니다.</p>
        </div>
      </header>

      <FilterPanel
        keyword={keyword}
        filters={filters}
        hasActiveFilters={hasActiveFilters}
        onKeywordChange={(value) => setSearchParams(value ? { query: value } : {})}
        onFilterChange={setFilters}
        onReset={() => {
          setFilters({ tag: '', fromDate: '', toDate: '' });
          setSearchParams({});
        }}
      />

      {!isSearching ? (
        archiveQuery.isPending ? (
          <p className="py-16 text-center text-sm text-text-primary/40">지식카드를 불러오는 중입니다...</p>
        ) : archiveQuery.isError ? (
          <p className="py-16 text-center text-sm text-red-400">지식카드를 불러오지 못했습니다.</p>
        ) : archiveCards.length === 0 ? (
          <ContentEmptyState title="이 폴더는 아직 비어 있습니다" description="카드가 쌓이면 이곳에 차곡차곡 모입니다." />
        ) : (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
            {archiveCards.map((card) => (
              <ArchiveCard
                key={card.cardId}
                title={card.title}
                summary={null}
                tags={card.tags.map((tag) => tag.tagName)}
                categoryName={categoryName}
                createdAt={card.createdAt}
                onClick={() => navigate(`/cards/${card.cardId}`)}
              />
            ))}
          </div>
        )
      ) : searchQuery.isPending && searchResults.length === 0 ? (
        <p className="py-16 text-center text-sm text-text-primary/40">검색 중입니다...</p>
      ) : searchQuery.isError ? (
        <p className="py-16 text-center text-sm text-red-400">검색 결과를 불러오지 못했습니다.</p>
      ) : searchResults.length === 0 ? (
        <ContentEmptyState title="검색 결과가 없습니다" description="검색어 또는 조건을 조금 느슨하게 바꿔보세요." />
      ) : (
        <>
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
            {searchResults.map((card) => (
              <ArchiveCard
                key={card.cardId}
                title={card.title}
                summary={card.summary}
                tags={[]}
                categoryName={categoryName}
                onClick={() => navigate(`/cards/${card.cardId}`)}
              />
            ))}
          </div>
          {searchQuery.hasNextPage ? (
            <div className="flex justify-center pt-8">
              <button
                type="button"
                onClick={() => void searchQuery.fetchNextPage()}
                className="rounded-2xl border border-text-secondary/10 bg-text-primary/5 px-6 py-3 text-sm font-bold text-text-primary/75 transition hover:border-action-accent/40 hover:text-text-primary"
              >
                더 보기
              </button>
            </div>
          ) : null}
        </>
      )}
    </section>
  );
}

function FilterPanel({
  keyword,
  filters,
  hasActiveFilters,
  onKeywordChange,
  onFilterChange,
  onReset,
}: {
  keyword: string;
  filters: Filters;
  hasActiveFilters: boolean;
  onKeywordChange: (value: string) => void;
  onFilterChange: (filters: Filters) => void;
  onReset: () => void;
}) {
  const [inputValue, setInputValue] = useState(keyword);

  useEffect(() => {
    const timer = window.setTimeout(() => setInputValue(keyword), 0);
    return () => window.clearTimeout(timer);
  }, [keyword]);

  useEffect(() => {
    if (inputValue.trim() === keyword) return;
    const timer = window.setTimeout(() => onKeywordChange(inputValue.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [inputValue, keyword, onKeywordChange]);

  return (
    <div className="relative z-10 flex flex-col gap-7 rounded-[32px] border border-text-secondary/10 bg-surface-container/70 p-7">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex flex-col gap-4 lg:flex-row lg:flex-wrap lg:items-center">
          <div className="flex min-h-12 min-w-0 flex-col gap-3 rounded-2xl border border-text-secondary/5 bg-text-primary/[0.03] px-5 py-4 sm:flex-row sm:items-center sm:gap-4 sm:py-0">
            <span className="shrink-0 text-xs font-bold text-text-primary/35">날짜 범위</span>
            <div className="flex min-w-0 flex-col gap-2 sm:flex-row sm:items-center sm:gap-4">
              <CategoryDatePicker
                value={filters.fromDate}
                onChange={(value) => onFilterChange({ ...filters, fromDate: value })}
                placeholder="연도. 월. 일."
              />
              <span className="hidden text-text-primary/18 sm:inline">-</span>
              <CategoryDatePicker
                value={filters.toDate}
                onChange={(value) => onFilterChange({ ...filters, toDate: value })}
                placeholder="연도. 월. 일."
              />
            </div>
          </div>

          <label className="flex h-12 min-w-0 items-center gap-3 rounded-2xl border border-text-secondary/5 bg-text-primary/[0.03] px-4 sm:min-w-[18rem]">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-action-accent/15 text-action-accent">
              <Hash size={16} aria-hidden="true" />
            </span>
            <input
              value={filters.tag}
              onChange={(event) => onFilterChange({ ...filters, tag: event.target.value })}
              placeholder="태그 검색"
              className="h-full min-w-0 flex-1 bg-transparent text-sm font-bold text-text-primary outline-none placeholder:text-text-primary/25"
            />
          </label>
        </div>

        <button
          type="button"
          onClick={() => {
            setInputValue('');
            onReset();
          }}
          disabled={!hasActiveFilters}
          className="h-10 w-fit rounded-md border border-text-secondary/10 px-4 text-xs font-bold text-text-primary/45 transition hover:border-action-accent/30 hover:bg-action-accent/10 hover:text-action-accent disabled:cursor-not-allowed disabled:opacity-35 disabled:hover:border-text-secondary/10 disabled:hover:bg-transparent disabled:hover:text-text-primary/45"
        >
          초기화
        </button>
      </div>

      <div className="flex h-12 w-full items-center gap-3 rounded-2xl border border-text-secondary/5 bg-text-primary/[0.03] px-4 transition focus-within:ring-1 focus-within:ring-action-accent/30">
        <Search size={18} aria-hidden="true" className="shrink-0 text-text-secondary/80" />
        <input
          type="search"
          value={inputValue}
          onChange={(event) => setInputValue(event.target.value)}
          placeholder="이 폴더 안에서 검색"
          className="h-full min-w-0 flex-1 bg-transparent px-0 text-sm tracking-wide text-text-primary outline-none placeholder:text-text-secondary/50 [&::-webkit-search-cancel-button]:hidden [&::-webkit-search-decoration]:hidden"
        />
        {inputValue ? (
          <button
            type="button"
            onClick={() => setInputValue('')}
            className="flex shrink-0 items-center justify-center text-text-secondary transition hover:text-text-primary"
            aria-label="Clear search"
          >
            <X size={16} />
          </button>
        ) : null}
      </div>
    </div>
  );
}

function CategoryDatePicker({
  value,
  onChange,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [viewDate, setViewDate] = useState(value ? parseLocalDate(value) : new Date());
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleDateClick = (day: number) => {
    const selected = new Date(viewDate.getFullYear(), viewDate.getMonth(), day);
    if (selected > getTodayStart()) return;
    onChange(formatDate(selected));
    setIsOpen(false);
  };

  const totalDays = new Date(viewDate.getFullYear(), viewDate.getMonth() + 1, 0).getDate();
  const firstDay = new Date(viewDate.getFullYear(), viewDate.getMonth(), 1).getDay();
  const days = Array.from({ length: totalDays }, (_, index) => index + 1);

  return (
    <div ref={containerRef} className="relative min-w-0">
      <button
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        className="inline-flex h-9 items-center gap-2 text-sm font-black text-text-primary outline-none transition hover:text-action-accent"
      >
        <span className={value ? 'text-text-primary' : 'text-text-primary/85'}>
          {value || placeholder}
        </span>
        <Calendar size={15} className="shrink-0 text-action-accent" aria-hidden="true" />
      </button>

      {isOpen ? (
        <div className="absolute left-0 top-full z-40 mt-3 w-64 overflow-hidden rounded-tl-[28px] rounded-br-[28px] rounded-tr-lg rounded-bl-lg border border-text-secondary/10 bg-surface-container p-5 shadow-2xl">
          <div className="mb-4 flex items-center justify-between">
            <button
              type="button"
              onClick={() => setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() - 1, 1))}
              className="text-text-primary/40 transition hover:text-text-primary"
            >
              <ChevronLeft size={18} />
            </button>
            <span className="text-sm font-black tracking-wider text-text-primary">
              {viewDate.toLocaleString('ko-KR', { year: 'numeric', month: 'long' })}
            </span>
            <button
              type="button"
              onClick={() => setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() + 1, 1))}
              className="text-text-primary/40 transition hover:text-text-primary"
            >
              <ChevronRight size={18} />
            </button>
          </div>

          <div className="mb-2 grid grid-cols-7 gap-1 text-center">
            {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((day) => (
              <span key={day} className="text-[10px] font-black text-text-primary/20">
                {day}
              </span>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-1">
            {Array.from({ length: firstDay }, (_, index) => (
              <div key={`empty-${index}`} className="h-8 w-8" />
            ))}
            {days.map((day) => {
              const date = new Date(viewDate.getFullYear(), viewDate.getMonth(), day);
              const isFuture = date > getTodayStart();
              const isSelected = value === formatDate(date);

              return (
                <button
                  key={day}
                  type="button"
                  disabled={isFuture}
                  onClick={() => handleDateClick(day)}
                  className={`h-8 w-8 rounded-lg text-xs font-bold transition-all disabled:pointer-events-none disabled:text-text-primary/15 ${
                    isSelected
                      ? 'bg-action-accent text-text-on-accent shadow-[0_0_10px_rgba(74,222,128,0.5)]'
                      : isFuture
                        ? 'text-text-primary/15'
                        : 'text-text-primary/60 hover:bg-action-accent/20 hover:text-action-accent'
                  }`}
                >
                  {day}
                </button>
              );
            })}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function getTodayStart() {
  const today = new Date();
  return new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

function parseLocalDate(value: string) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function formatDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

function ArchiveCard({
  title,
  summary,
  tags,
  categoryName,
  createdAt,
  onClick,
}: {
  title: string;
  summary: string | null;
  tags: string[];
  categoryName?: string;
  createdAt?: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="group relative flex h-[300px] min-w-0 cursor-pointer flex-col overflow-hidden rounded-[30px] border border-text-secondary/5 bg-surface-low p-7 text-left transition-all duration-300 hover:-translate-y-0.5 hover:border-action-accent/18 hover:bg-surface-container/90"
    >
      <div className="absolute -right-16 -top-16 h-48 w-48 rounded-full bg-action-accent/[0.035] blur-[60px]" />
      <div className="relative flex items-start justify-between gap-4">
        <span className="inline-flex min-w-0 items-center gap-2 rounded-full border border-action-accent/15 bg-action-accent/8 px-3 py-1.5 text-[11px] font-black uppercase tracking-wider text-action-accent">
          <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-action-accent" />
          <span className="truncate">{categoryName || 'Archive'}</span>
        </span>
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-text-secondary/5 bg-text-primary/[0.03] text-text-primary/28 transition group-hover:border-action-accent/20 group-hover:text-action-accent">
          <ArrowRight size={16} aria-hidden="true" />
        </span>
      </div>

      <div className="relative mt-8 min-w-0">
        <h2 className="line-clamp-3 text-[22px] font-extrabold leading-tight text-text-primary transition group-hover:text-action-accent">
          {title}
        </h2>
        {summary ? (
          <ArchiveSummary
            summary={summary}
            className="mt-5 max-h-[4.5rem] space-y-1 overflow-hidden text-[13px] leading-5 text-text-primary/50"
          />
        ) : null}
      </div>

      <div className="relative mt-auto flex flex-col gap-4 border-t border-text-secondary/5 pt-5">
        <div className="flex items-center gap-2 text-[11px] font-bold text-text-primary/35">
          <CalendarDays size={13} className="text-action-accent/70" aria-hidden="true" />
          <span>{createdAt ? formatArchiveDate(createdAt) : '날짜 정보 없음'}</span>
        </div>
        {tags.length > 0 ? (
          <div className="flex flex-wrap gap-1.5">
            {tags.slice(0, 4).map((tag) => (
              <span
                key={tag}
                className="inline-flex items-center gap-1 rounded-full border border-text-secondary/5 bg-text-primary/[0.03] px-2.5 py-1 text-[11px] font-bold leading-none text-text-primary/42 transition group-hover:border-action-accent/20 group-hover:text-action-accent"
              >
                <Hash size={10} aria-hidden="true" />
                {tag}
              </span>
            ))}
          </div>
        ) : null}
      </div>
    </button>
  );
}

function formatArchiveDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(date);
}

function dedupeByCardId(cards: SearchCardResult[]) {
  return Array.from(new Map(cards.map((card) => [card.cardId, card])).values());
}

function toSearchParams(keyword: string, categoryName: string, filters: Filters, page: number, size: number): SearchParams {
  return {
    keyword,
    category: categoryName,
    page,
    size,
    ...(filters.tag.trim() ? { tag: filters.tag.trim().replace(/^#/, '') } : {}),
    ...(filters.fromDate ? { fromDate: filters.fromDate } : {}),
    ...(filters.toDate ? { toDate: filters.toDate } : {}),
  };
}
