import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ChevronLeft, ChevronRight, Pause, Play } from 'lucide-react';
import { ErrorFallback } from '@san/ui';
import { authTokenStorage } from '@dashboard/api/client';
import { useArchiveCards } from '@dashboard/main/hooks/useArchiveCards';
import { HomeKnowledgeCardsEmptyState } from './HomeEmptyStates';
import { HomeSectionTitle } from '../layout/HomeSectionTitle';
import { ArchiveSummary } from './ArchiveSummary';

export function ArchiveSection() {
  const navigate = useNavigate();
  const carouselRef = useRef<HTMLDivElement>(null);
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const { cards, isPending, isError } = useArchiveCards({ limit: 12 }, { enabled: isAuthenticated });
  const visibleCards = cards.slice(0, 12);
  const [isHovered, setIsHovered] = useState(false);
  const [isPlaying, setIsPlaying] = useState(true);
  const [activeIndex, setActiveIndex] = useState(0);

  const handleScroll = useCallback(() => {
    const carousel = carouselRef.current;
    if (!carousel || visibleCards.length === 0) return;
    setActiveIndex(Math.round(carousel.scrollLeft / carousel.clientWidth));
  }, [visibleCards.length]);

  const scrollCarousel = useCallback((direction: 'previous' | 'next') => {
    const carousel = carouselRef.current;
    if (!carousel) return;
    if (direction === 'next' && carousel.scrollLeft + carousel.clientWidth >= carousel.scrollWidth - 10) {
      carousel.scrollTo({ left: 0, behavior: 'smooth' });
      return;
    }
    if (direction === 'previous' && carousel.scrollLeft <= 0) return;
    carousel.scrollBy({ left: carousel.clientWidth * (direction === 'previous' ? -1 : 1), behavior: 'smooth' });
  }, []);

  useEffect(() => {
    if (!isAuthenticated || isPending || isError || visibleCards.length === 0 || isHovered || !isPlaying) return;
    const interval = setInterval(() => scrollCarousel('next'), 4000);
    return () => clearInterval(interval);
  }, [isAuthenticated, isError, isHovered, isPending, isPlaying, scrollCarousel, visibleCards.length]);

  useEffect(() => {
    let ignore = false;
    authTokenStorage.getToken()
      .then((token) => { if (!ignore) setIsAuthenticated(Boolean(token)); })
      .finally(() => { if (!ignore) setIsCheckingAuth(false); });
    return () => { ignore = true; };
  }, []);

  const totalPages = Math.max(1, Math.ceil(visibleCards.length / 3));
  const hasCards = isAuthenticated && !isPending && !isError && visibleCards.length > 0;

  return (
    <section className="w-full min-w-0 overflow-visible pb-xl">
      <div className="flex flex-col gap-dashboard-gap">
        <div className="flex flex-col gap-dashboard-gap sm:flex-row sm:items-center sm:justify-between">
          <HomeSectionTitle>나의 지식 아카이브</HomeSectionTitle>

          {isAuthenticated ? (
            <div className="flex shrink-0 items-center gap-3 rounded-full border border-text-secondary/10 glass-card bg-surface-container/80 px-3 py-1.5 !shadow-none">
              <button type="button" onClick={() => setIsPlaying(c => !c)} disabled={!hasCards}
                className="flex h-6 w-6 items-center justify-center rounded-full text-text-primary/40 transition hover:bg-surface-container/90 hover:text-text-primary disabled:opacity-20"
                aria-label={isPlaying ? '일시정지' : '재생'}>
                {isPlaying ? <Pause size={12} fill="currentColor" /> : <Play size={12} fill="currentColor" />}
              </button>
              <div className="h-3 w-px bg-text-secondary/10" />
              <div className="flex items-center gap-1">
                <button type="button" onClick={() => scrollCarousel('previous')} disabled={!hasCards || activeIndex === 0}
                  className="flex h-6 w-6 items-center justify-center rounded-full text-text-primary/40 transition hover:bg-surface-container/90 hover:text-text-primary disabled:opacity-20" aria-label="이전">
                  <ChevronLeft size={14} />
                </button>
                <div className="flex items-center gap-2 px-1">
                  {hasCards ? Array.from({ length: totalPages }).map((_, idx) => (
                    <button key={idx} type="button"
                      onClick={() => carouselRef.current?.scrollTo({ left: carouselRef.current.clientWidth * idx, behavior: 'smooth' })}
                      className={`h-2.5 rounded-full transition-all duration-300 ${idx === activeIndex ? 'w-5 bg-archive-card-accent' : 'w-2.5 bg-surface-highest/70 hover:bg-surface-container/90'}`}
                      aria-label={`${idx + 1}페이지`} />
                  )) : null}
                </div>
                <button type="button" onClick={() => scrollCarousel('next')} disabled={!hasCards || activeIndex === totalPages - 1}
                  className="flex h-6 w-6 items-center justify-center rounded-full text-text-primary/40 transition hover:bg-surface-container/90 hover:text-text-primary disabled:opacity-20" aria-label="다음">
                  <ChevronRight size={14} />
                </button>
              </div>
            </div>
          ) : null}
        </div>

        <div className="relative min-w-0" onMouseEnter={() => setIsHovered(true)} onMouseLeave={() => setIsHovered(false)}>
          <div className="pointer-events-none absolute inset-y-4 left-0 z-10 w-10 bg-gradient-to-r from-background to-transparent" />
          <div className="pointer-events-none absolute inset-y-4 right-0 z-10 w-14 bg-gradient-to-l from-background to-transparent" />
          <div ref={carouselRef} onScroll={handleScroll}
            className="flex w-full min-w-0 snap-x snap-mandatory gap-5 overflow-x-auto scroll-px-2 scroll-smooth py-4 pl-2 pr-8 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">

            {isCheckingAuth ? <StatusCard message="로그인 상태를 확인하는 중..." /> : null}
            {!isCheckingAuth && !isAuthenticated ? (
              <div className="flex min-h-[240px] w-full shrink-0 snap-start items-center justify-center">
                <ErrorFallback type="auth" variant="full" onRetry={() => navigate('/login', { state: { from: '/' } })} />
              </div>
            ) : null}
            {isAuthenticated && isPending ? <StatusCard message="아카이브 카드를 불러오는 중..." /> : null}
            {isAuthenticated && isError ? <StatusCard message="아카이브 카드를 불러올 수 없습니다." tone="error" /> : null}
            {isAuthenticated && !isPending && !isError && visibleCards.length === 0 ? (
              <HomeKnowledgeCardsEmptyState primaryAction={{ label: '분석 시작', onClick: () => navigate('/til') }} />
            ) : null}

            {hasCards ? visibleCards.map((card) => {
              const date = formatRelativeDate(card.created_at);
              const categoryName = card.category_name ?? card.tags[0]?.name ?? 'Uncategorized';
              return (
                <article
                  key={card.card_id}
                  onClick={() => navigate(`/cards/${card.card_id}`)}
                  className="group relative flex h-[260px] w-[min(88vw,23rem)] min-w-0 shrink-0 cursor-pointer snap-start flex-col justify-between rounded-leaf glass-card bg-surface-container/80 p-5 !shadow-none transition-all hover:bg-surface-container md:w-[calc((100%-24px)/2)] xl:w-[calc((100%-48px)/3)]"
                >
                  <div>
                    <div className="mb-3 flex items-center justify-between">
                      <span className="rounded-tl-lg rounded-br-lg rounded-tr-sm rounded-bl-sm border border-archive-card-accent-20 bg-archive-card-accent-5 px-2.5 py-1 text-[12px] font-bold text-archive-card-accent">
                        {categoryName}
                      </span>
                      <time className="text-[12px] font-medium text-text-secondary/70">{date}</time>
                    </div>
                    <h3 className="line-clamp-2 text-lg font-bold leading-snug text-text-primary">{card.title}</h3>
                    <ArchiveSummary
                      summary={card.summary}
                      className="mt-3 max-h-[4.5rem] space-y-1 overflow-hidden text-[13px] leading-5 text-text-secondary"
                    />
                  </div>
                  <div className="flex items-center justify-between gap-3">
                    {card.tags.length > 0 ? (
                      <div className="flex min-w-0 gap-1.5 overflow-hidden">
                        {card.tags.slice(0, 3).map(t => (
                          <span key={t.name} className="shrink-0 rounded-full border border-archive-card-accent-10 bg-archive-card-accent-5 px-2.5 py-1 text-[12px] font-medium leading-none text-archive-card-accent-70">#{t.name}</span>
                        ))}
                      </div>
                    ) : <div />}
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-tl-[16px] rounded-br-[16px] rounded-tr-md rounded-bl-md glass-panel bg-surface-lowest/70 text-text-secondary shadow-none transition-colors group-hover:bg-archive-card-accent-10 group-hover:text-archive-card-accent">
                      <ArrowRight size={15} />
                    </div>
                  </div>
                </article>
              );
            }) : null}
          </div>
        </div>
      </div>
    </section>
  );
}

function formatRelativeDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffH = Math.floor(diffMs / 3_600_000);
  if (diffH < 1) { const m = Math.floor(diffMs / 60_000); return m <= 0 ? '방금 전' : `${m}분 전`; }
  if (diffH < 24) return `${diffH}시간 전`;
  const d = Math.floor(diffH / 24);
  if (d < 7) return `${d}일 전`;
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' }).format(date);
}

function StatusCard({ message, tone = 'default' }: { message: string; tone?: 'default' | 'error' }) {
  return (
    <div className={`flex h-[240px] w-[min(84vw,22rem)] shrink-0 snap-start items-center justify-center rounded-tl-[28px] rounded-br-[28px] rounded-tr-xl rounded-bl-xl glass-card bg-surface-container/80 text-sm !shadow-none md:w-[calc((100%-20px)/2)] xl:w-[calc((100%-40px)/3)] ${tone === 'error' ? 'text-red-400' : 'text-text-secondary'}`}>
      {message}
    </div>
  );
}
