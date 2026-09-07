import { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useInfiniteQuery } from '@tanstack/react-query';
import { ChevronDown, Search, ExternalLink, Quote as QuoteIcon, MessageSquare, Clock, Globe, ArrowRight, Share2, Bookmark, Calendar, ChevronLeft, ChevronRight, AlertTriangle } from 'lucide-react';
import type { SearchCardResult, SearchParams } from '@san/shared';
import { searchApi } from '../../api/client';
import { ContentEmptyState } from '../../components/shared/empty/ContentEmptyState';
import { ArchiveFolderGrid } from '../../main/components/archive/ArchiveFolderGrid';

interface SearchFilters {
  tag: string;
  fromDate: string;
  toDate: string;
}

// Extend SearchCardResult for UI purposes (including mock data fields)
interface ExtendedSearchCardResult extends SearchCardResult {
  categoryName?: string;
  createdAt?: string;
}

interface SearchPageProps {
  keyword: string;
  totalCount: number;
  results: ExtendedSearchCardResult[];
  filters: SearchFilters;
  hasKeyword: boolean;
  isPending?: boolean;
  isError?: boolean;
  hasNext?: boolean;
  onFilterChange: (filters: SearchFilters) => void;
  onLoadMore: () => void;
  onSearchChange: (keyword: string) => void;
}

// --- Custom Date Picker Component ---
function CustomDatePicker({ 
  value, 
  onChange, 
  placeholder 
}: { 
  value: string; 
  onChange: (val: string) => void; 
  placeholder: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [viewDate, setViewDate] = useState(() => value ? parseDateString(value) : new Date());
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setViewDate(value ? parseDateString(value) : new Date());
  }, [value]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const daysInMonth = (year: number, month: number) => new Date(year, month + 1, 0).getDate();
  const firstDayOfMonth = (year: number, month: number) => new Date(year, month, 1).getDay();

  const handlePrevMonth = () => setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() - 1, 1));
  const handleNextMonth = () => setViewDate(new Date(viewDate.getFullYear(), viewDate.getMonth() + 1, 1));

  const handleDateClick = (day: number) => {
    const selected = new Date(viewDate.getFullYear(), viewDate.getMonth(), day);
    const formatted = `${selected.getFullYear()}-${String(selected.getMonth() + 1).padStart(2, '0')}-${String(selected.getDate()).padStart(2, '0')}`;
    onChange(formatted);
    setIsOpen(false);
  };

  const renderDays = () => {
    const totalDays = daysInMonth(viewDate.getFullYear(), viewDate.getMonth());
    const firstDay = firstDayOfMonth(viewDate.getFullYear(), viewDate.getMonth());
    const days = [];

    // Empty slots
    for (let i = 0; i < firstDay; i++) days.push(<div key={`empty-${i}`} className="h-8 w-8" />);

    // Days
    for (let d = 1; d <= totalDays; d++) {
      const date = new Date(viewDate.getFullYear(), viewDate.getMonth(), d);
      const isFuture = date > getTodayStart();
      const isSelected = value === formatDate(date);
      days.push(
        <button
          key={d}
          type="button"
          disabled={isFuture}
          onClick={() => handleDateClick(d)}
          className={`h-8 w-8 rounded-lg text-xs font-bold transition-all disabled:pointer-events-none disabled:text-text-primary/15 ${
            isSelected
              ? 'bg-action-accent text-text-on-accent shadow-[0_0_10px_rgba(74,222,128,0.5)]'
              : isFuture
                ? 'text-text-primary/15'
                : 'text-text-primary/60 hover:bg-action-accent/20 hover:text-action-accent'
          }`}
        >
          {d}
        </button>
      );
    }
    return days;
  };

  const displayValue = value ? value.replace(/-/g, '. ') : placeholder;

  return (
    <div className="relative" ref={containerRef}>
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-3 bg-transparent text-[15px] font-bold text-text-primary/90 outline-none transition-all hover:text-text-primary"
      >
        <span>{displayValue}</span>
        <Calendar size={16} className="text-action-accent" />
      </button>

      {isOpen && (
        <div className="absolute left-0 top-full z-[1000] mt-4 w-64 overflow-hidden rounded-tl-[32px] rounded-br-[32px] rounded-tr-lg rounded-bl-lg border border-text-secondary/10 bg-surface-container p-6 shadow-2xl animate-in fade-in zoom-in duration-200 origin-top-left">
          <div className="mb-4 flex items-center justify-between">
            <button onClick={handlePrevMonth} className="text-text-primary/40 hover:text-text-primary"><ChevronLeft size={18} /></button>
            <span className="text-sm font-black uppercase tracking-widest text-text-primary">
              {viewDate.toLocaleString('default', { month: 'long', year: 'numeric' })}
            </span>
            <button onClick={handleNextMonth} className="text-text-primary/40 hover:text-text-primary"><ChevronRight size={18} /></button>
          </div>
          <div className="grid grid-cols-7 gap-1 text-center mb-2">
            {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map(d => (
              <span key={d} className="text-[10px] font-black text-text-primary/20">{d}</span>
            ))}
          </div>
          <div className="grid grid-cols-7 gap-1">
            {renderDays()}
          </div>
        </div>
      )}
    </div>
  );
}

function parseDateString(value: string) {
  const [yearText, monthText, dayText] = value.split('-');
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  return new Date(year, month - 1, day);
}

function getTodayStart() {
  const today = new Date();
  return new Date(today.getFullYear(), today.getMonth(), today.getDate());
}

function formatDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function ResultPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useState<SearchFilters>({
    tag: '',
    fromDate: '',
    toDate: '',
  });

  const size = 12;
  const keyword = searchParams.get('query')?.trim() ?? '';
  const normalizedTag = normalizeTag(filters.tag);

  const searchQuery = useInfiniteQuery({
    queryKey: ['knowledge-search', keyword, normalizedTag, filters.fromDate, filters.toDate, size],
    queryFn: ({ pageParam }) => searchApi.search(toSearchParams(keyword, normalizedTag, filters, pageParam, size)),
    enabled: Boolean(keyword),
    initialPageParam: 0,
    getNextPageParam: (lastPage, allPages) => (lastPage.hasNext ? allPages.length : undefined),
  });

  const displayedResults = useMemo(
    () => dedupeByCardId(searchQuery.data?.pages.flatMap((page) => page.results) ?? []) as ExtendedSearchCardResult[],
    [searchQuery.data],
  );

  const handleSearchChange = useCallback((newKeyword: string) => {
    if (newKeyword.trim() === keyword) {
      return;
    }

    setSearchParams({ query: newKeyword });
  }, [keyword, setSearchParams]);

  return (
    <SearchPage
      key={keyword}
      keyword={keyword}
      totalCount={searchQuery.data?.pages[0]?.totalCount ?? displayedResults.length}
      results={displayedResults}
      filters={filters}
      hasKeyword={Boolean(keyword)}
      isPending={searchQuery.isPending && !displayedResults.length}
      isError={searchQuery.isError}
      hasNext={searchQuery.hasNextPage}
      onFilterChange={setFilters}
      onLoadMore={() => void searchQuery.fetchNextPage()}
      onSearchChange={handleSearchChange}
    />
  );
}

function SearchPage({
  keyword,
  totalCount,
  results,
  filters,
  hasKeyword,
  isPending = false,
  isError = false,
  hasNext = false,
  onFilterChange,
  onLoadMore,
  onSearchChange,
}: SearchPageProps) {
  const [inputValue, setInputValue] = useState(keyword);

  useEffect(() => {
    if (inputValue.trim() === keyword) {
      return;
    }

    const timer = window.setTimeout(() => {
      onSearchChange(inputValue);
    }, 300);

    return () => window.clearTimeout(timer);
  }, [inputValue, keyword, onSearchChange]);

  return (
    <section className="flex w-full min-w-0 flex-col gap-8 py-12 text-text-primary">
      <header className="flex flex-col gap-3">
        <div className="flex items-center gap-4">
        <h1 className="text-4xl font-extrabold tracking-tight text-text-primary">Archive</h1>
          {hasKeyword ? (
            <div className="rounded-full bg-action-accent/10 px-4 py-1 text-xs font-bold text-action-accent border border-action-accent/20">
              {totalCount} CARDS
            </div>
          ) : null}
        </div>
        <p className="text-lg text-text-primary/50">
          {hasKeyword
            ? '조건에 맞는 지식카드를 검색합니다.'
            : '카테고리별 폴더에서 지식카드를 찾아보세요.'}
        </p>
      </header>

      <div className="group relative z-[300] flex flex-col gap-8 rounded-[32px] md:rounded-[40px] glass-card bg-surface-container/80 p-6 md:p-10 shadow-3xl border border-text-secondary/5 transition-all hover:border-text-secondary/10">
        <div className="relative z-[400] flex flex-col lg:flex-row lg:items-center gap-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-6 rounded-tl-[24px] rounded-br-[24px] rounded-tr-lg rounded-bl-lg bg-text-primary/[0.03] px-6 py-4 border border-text-secondary/5 focus-within:border-action-accent/40 transition-all">
            <span className="text-[11px] font-black text-text-primary/30 uppercase tracking-widest">날짜 범위</span>
            <div className="flex items-center gap-4">
              <CustomDatePicker 
                value={filters.fromDate} 
                onChange={(val) => onFilterChange({ ...filters, fromDate: val })} 
                placeholder="연도. 월. 일." 
              />
              <span className="text-text-primary/10 font-bold">—</span>
              <CustomDatePicker 
                value={filters.toDate} 
                onChange={(val) => onFilterChange({ ...filters, toDate: val })} 
                placeholder="연도. 월. 일." 
              />
            </div>
          </div>

          <div className="flex items-center gap-3 rounded-2xl bg-text-primary/[0.03] px-5 py-4 border border-text-secondary/5">
            <div className="flex h-6 w-6 items-center justify-center rounded-lg bg-action-accent/20 text-action-accent">
              <span className="text-xs font-black">#</span>
            </div>
            <input 
              type="text" 
              placeholder="카테고리 검색"
              value={filters.tag}
              onChange={(e) => onFilterChange({ ...filters, tag: e.target.value })}
              className="bg-transparent text-sm font-medium outline-none placeholder:text-text-primary/20 w-full sm:w-32"
            />
          </div>
        </div>

        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
            <Search className="h-4 w-4 text-text-primary/20" />
          </div>
          <input
            type="text"
            className="block w-full pl-11 pr-4 py-4 rounded-2xl bg-text-primary/[0.03] border border-text-secondary/5 text-sm font-medium outline-none focus:border-action-accent/30 transition-all placeholder:text-text-primary/10"
            placeholder="찾는 내용을 검색해주세요."
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
          />
        </div>
      </div>

      {!hasKeyword ? (
        <ArchiveFolderGrid />
      ) : isPending ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4">
          <div className="h-10 w-10 border-2 border-action-accent/20 border-t-action-accent rounded-full animate-spin" />
          <p className="text-sm font-medium text-text-primary/40">검색 결과를 불러오는 중입니다...</p>
        </div>
      ) : isError ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4">
          <div className="h-12 w-12 rounded-full bg-red-500/10 flex items-center justify-center text-red-500">
             <AlertTriangle size={24} />
          </div>
          <p className="text-sm font-medium text-text-primary/40">검색 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.</p>
        </div>
      ) : results.length === 0 ? (
        <ContentEmptyState
          title="검색 결과가 없어요"
          description={'뿌리가 닿는 지식 카드를 찾지 못했습니다.\n다른 키워드로 다시 검색해보세요.'}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 md:gap-10">
          {results.map((card, idx) => (
            <KnowledgeCard key={card.cardId} card={card} index={idx} />
          ))}
        </div>
      )}

      {hasNext && !isPending && (
        <div className="flex justify-center pt-16">
          <button
            onClick={onLoadMore}
            className="group relative flex items-center gap-4 overflow-hidden rounded-2xl bg-text-primary/5 px-12 py-5 font-bold text-text-primary/80 border border-text-secondary/10 transition-all hover:border-action-accent/50 hover:bg-text-primary/[0.08]"
          >
            <div className="absolute inset-0 bg-gradient-to-r from-action-accent/0 via-action-accent/5 to-action-accent/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000" />
            결과 더보기
            <ChevronDown size={20} className="transition-transform group-hover:translate-y-1 text-action-accent" />
          </button>
        </div>
      )}
    </section>
  );
}

function KnowledgeCard({ card, index }: { card: ExtendedSearchCardResult; index: number }) {
  const navigate = useNavigate();
  const styleType = index % 4;

  if (styleType === 0) {
    return (
      <article 
        onClick={() => navigate(`/cards/${card.cardId}`)}
        className="group relative flex h-[380px] cursor-pointer flex-col overflow-hidden rounded-[40px] bg-surface-low p-10 border border-text-secondary/5 transition-all duration-500 hover:-translate-y-2 hover:bg-surface-container hover:border-action-accent/30 hover:shadow-[0_20px_50px_rgba(0,0,0,0.5),0_0_20px_rgba(74,222,128,0.05)]"
      >
        <div className="flex items-center justify-between mb-8">
          <div className="flex items-center gap-2">
            <span className="flex h-2 w-2 rounded-full bg-action-accent shadow-[0_0_8px_rgba(74,222,128,0.7)]" />
            <span className="text-[11px] font-black uppercase tracking-widest text-action-accent">{card.categoryName || 'GENERAL'}</span>
          </div>
          <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
            <button className="p-2 rounded-lg bg-text-primary/5 hover:bg-action-accent/20 text-text-primary/40 hover:text-action-accent transition-colors"><Bookmark size={14} /></button>
            <button className="p-2 rounded-lg bg-text-primary/5 hover:bg-action-accent/20 text-text-primary/40 hover:text-action-accent transition-colors"><Share2 size={14} /></button>
          </div>
        </div>
        <h3 className="text-2xl font-bold leading-tight mb-5 line-clamp-2 group-hover:text-action-accent transition-colors">{card.title}</h3>
        <p className="text-sm leading-relaxed text-text-primary/40 line-clamp-4 mb-auto group-hover:text-text-primary/60 transition-colors">{card.summary || '상세 정보가 아직 없습니다.'}</p>
        <div className="flex items-center justify-between pt-8 border-t border-text-secondary/5">
          <div className="flex items-center gap-3">
                <div className="h-9 w-9 rounded-xl bg-surface-lowest/80 flex items-center justify-center border border-text-secondary/5 shadow-inner">
               <Globe size={16} className="text-text-primary/40" />
            </div>
            <div className="flex flex-col">
              <span className="text-[11px] font-bold text-text-primary/60">Knowledge Archive</span>
              <span className="text-[10px] text-text-primary/20">{card.createdAt ? new Date(card.createdAt).toLocaleDateString() : 'No date'}</span>
            </div>
          </div>
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-text-primary/5 text-action-accent group-hover:bg-action-accent group-hover:text-text-on-accent transition-all">
            <ArrowRight size={18} />
          </div>
        </div>
      </article>
    );
  }

  if (styleType === 1) {
    return (
      <article 
        onClick={() => navigate(`/cards/${card.cardId}`)}
        className="group relative h-[380px] cursor-pointer overflow-hidden rounded-[40px] border border-text-secondary/5 shadow-2xl transition-all duration-500 hover:-translate-y-2 hover:border-action-accent/30"
      >
        <div className="absolute inset-0 bg-gradient-to-t from-surface-lowest via-surface-lowest/60 to-transparent z-10" />
        <img src={`https://images.unsplash.com/photo-1633356122544-f134324a6cee?auto=format&fit=crop&q=80&w=800`} className="absolute inset-0 h-full w-full object-cover transition-transform duration-1000 group-hover:scale-110 grayscale-[30%] group-hover:grayscale-0" alt="" />
        <div className="absolute inset-0 bg-scrim/20 group-hover:bg-transparent transition-colors z-0" />
        <div className="absolute inset-0 p-10 z-20 flex flex-col">
          <div className="flex justify-between items-start mb-auto">
            <span className="rounded-xl glass-panel bg-surface-lowest/60 backdrop-blur-xl px-4 py-1.5 text-[10px] font-black tracking-widest text-action-accent border border-text-secondary/10 uppercase">{card.categoryName || 'MEDIA'}</span>
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-text-primary/10 backdrop-blur-md text-text-primary border border-text-secondary/10 opacity-0 group-hover:opacity-100 transition-all translate-y-2 group-hover:translate-y-0">
               <ExternalLink size={18} />
            </div>
          </div>
          <div className="flex flex-col gap-4">
            <h3 className="text-2xl font-bold leading-tight">{card.title}</h3>
            <div className="flex items-center gap-4 text-[11px] font-bold text-text-primary/50">
               <span className="flex items-center gap-1.5"><Clock size={14} className="text-action-accent" /> 8 min read</span>
               <span className="flex items-center gap-1.5"><MessageSquare size={14} className="text-action-accent" /> 12 insights</span>
            </div>
          </div>
        </div>
      </article>
    );
  }

  if (styleType === 2) {
    return (
      <article 
        onClick={() => navigate(`/cards/${card.cardId}`)}
        className="group relative flex h-[380px] cursor-pointer flex-col items-center justify-center text-center rounded-[40px] bg-gradient-to-b from-surface-container to-surface-container p-12 border border-text-secondary/5 transition-all duration-500 hover:border-action-accent/40"
      >
        <QuoteIcon className="text-action-accent/20 mb-8" size={60} />
        <h3 className="text-2xl font-bold italic leading-relaxed text-text-primary/90 mb-10 line-clamp-4">"{card.summary || card.title}"</h3>
        <div className="flex flex-col items-center gap-3">
          <div className="h-12 w-12 rounded-full border-2 border-action-accent/20 p-0.5 shadow-lg group-hover:border-action-accent/50 transition-colors">
            <div className="h-full w-full rounded-full bg-gradient-to-tr from-action-accent to-emerald-600 flex items-center justify-center text-text-on-accent font-black text-sm uppercase">SJ</div>
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-black tracking-wide text-text-primary">Insight Curator</span>
            <span className="text-[10px] font-bold text-text-primary/20 uppercase tracking-widest">Thought Leadership</span>
          </div>
        </div>
      </article>
    );
  }

  return (
    <article 
      onClick={() => navigate(`/cards/${card.cardId}`)}
      className="group relative flex h-[380px] cursor-pointer flex-col rounded-[40px] bg-surface-lowest p-10 border border-text-secondary/5 overflow-hidden transition-all duration-500 hover:bg-surface-lowest hover:border-action-accent/30"
    >
      <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-action-accent/5 blur-[80px] transition-all group-hover:bg-action-accent/10" />
      <div className="mb-10 h-16 w-16 rounded-2xl bg-text-primary/[0.03] border border-text-secondary/5 flex items-center justify-center text-action-accent group-hover:scale-110 group-hover:bg-action-accent/10 transition-all">
        <Clock size={32} strokeWidth={1.5} />
      </div>
      <h3 className="text-2xl font-bold leading-tight mb-4 group-hover:text-action-accent transition-colors">{card.title}</h3>
      <p className="text-sm leading-relaxed text-text-primary/30 line-clamp-3 mb-auto group-hover:text-text-primary/50 transition-colors">{card.summary}</p>
      <div className="flex items-center gap-2 mt-8">
        {['React', 'Design', 'Next.js'].map(tag => (
          <span key={tag} className="px-3 py-1 rounded-full bg-text-primary/5 text-[10px] font-bold text-text-primary/40 border border-text-secondary/5 group-hover:border-action-accent/20 transition-all">#{tag}</span>
        ))}
      </div>
    </article>
  );
}

function normalizeTag(tag: string) {
  return tag.trim().replace(/^#/, '');
}

function dedupeByCardId(cards: SearchCardResult[]) {
  return Array.from(new Map(cards.map((card) => [card.cardId, card])).values());
}

function toSearchParams(
  keyword: string,
  normalizedTag: string,
  filters: SearchFilters,
  page: number,
  size: number,
): SearchParams {
  return {
    keyword,
    page,
    size,
    ...(normalizedTag ? { tag: normalizedTag } : {}),
    ...(filters.fromDate ? { fromDate: filters.fromDate } : {}),
    ...(filters.toDate ? { toDate: filters.toDate } : {}),
  };
}
