import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  CheckSquare,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
  Loader2,
  Square,
  Sparkles,
  Star,
} from 'lucide-react';
import {
  getApiErrorMessage,
  type AsyncJobStatusResponse,
  type GithubStarRecommendation,
  type GithubStarRecommendationGenerationResponse,
  type GithubStarRecommendationsResponse,
} from '@san/shared';
import { EmptyState } from '@san/ui';
import { asyncJobsApi, githubApi } from '../../api/client';

const STAR_RECOMMENDATIONS_QUERY_KEY = ['github', 'star-recommendations'] as const;

export function GithubStarImportPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const carouselRef = useRef<HTMLDivElement | null>(null);
  const resultsRef = useRef<HTMLDivElement | null>(null);
  const [generationJobId, setGenerationJobId] = useState<string | null>(null);
  const [activeIndex, setActiveIndex] = useState(0);
  const [itemsPerPage, setItemsPerPage] = useState(1);
  const [scanPulse, setScanPulse] = useState(0);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [collectingId, setCollectingId] = useState<string | null>(null);

  const githubLinkQuery = useQuery({
    queryKey: ['github', 'link-status'],
    queryFn: () => githubApi.getLinkStatus(),
    staleTime: 1000 * 30,
  });

  const linkedUsername = githubLinkQuery.data?.githubUsername ?? null;
  const isLinked = Boolean(githubLinkQuery.data?.linked);

  const starRecommendationsQuery = useQuery<GithubStarRecommendationsResponse>({
    queryKey: STAR_RECOMMENDATIONS_QUERY_KEY,
    queryFn: () => githubApi.getStarRecommendations(),
    enabled: isLinked,
    staleTime: 1000 * 20,
    refetchOnWindowFocus: false,
  });

  const requestRecommendationsMutation = useMutation({
    mutationFn: () => githubApi.requestStarRecommendations(),
    onSuccess: (response: GithubStarRecommendationGenerationResponse) => {
      setActionMessage(null);

      if ('jobId' in response) {
        setGenerationJobId(response.jobId);
        setScanPulse(0);
        return;
      }

      queryClient.setQueryData<GithubStarRecommendationsResponse>(STAR_RECOMMENDATIONS_QUERY_KEY, response);
      setGenerationJobId(null);
      setScanPulse(100);
      window.setTimeout(() => {
        resultsRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 0);
    },
    onError: (error) => {
      setActionMessage(getApiErrorMessage(error, 'GitHub Star 추천을 시작하지 못했습니다.'));
    },
  });

  const generationStatusQuery = useQuery<AsyncJobStatusResponse>({
    queryKey: ['async-job', generationJobId],
    queryFn: () => asyncJobsApi.getStatus(generationJobId ?? ''),
    enabled: Boolean(generationJobId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'PENDING' || status === 'PROCESSING' ? 1500 : false;
    },
  });

  const collectRecommendationMutation = useMutation({
    mutationFn: (recommendationId: string) => githubApi.collectStarRecommendation(recommendationId),
    onMutate: (recommendationId) => {
      setCollectingId(recommendationId);
      return recommendationId;
    },
    onSuccess: (response, recommendationId) => {
      queryClient.setQueryData<GithubStarRecommendationsResponse>(
        STAR_RECOMMENDATIONS_QUERY_KEY,
        (current) => {
          if (!current) return current;
          return {
            recommendations: current.recommendations.map((item) =>
              item.recommendationId === recommendationId
                ? { ...item, collected: response.collected }
                : item,
            ),
          };
        },
      );
      void queryClient.invalidateQueries({ queryKey: ['archive'] });
      void queryClient.invalidateQueries({ queryKey: ['cards'] });
      setActionMessage('선택한 추천 스크랩을 지식카드로 생성했습니다.');
    },
    onError: (error) => {
      setActionMessage(getApiErrorMessage(error, '지식카드 생성에 실패했습니다.'));
    },
    onSettled: () => {
      setCollectingId(null);
    },
  });

  const recommendations = starRecommendationsQuery.data?.recommendations ?? [];
  const hasRecommendations = recommendations.length > 0;
  const isGenerating = Boolean(generationJobId);
  const isRecommendationsLoading = starRecommendationsQuery.isLoading && !hasRecommendations;
  const canRequestRecommendations = isLinked && !isGenerating && !requestRecommendationsMutation.isPending;
  const showGenerationButton = isLinked && !isGenerating && !hasRecommendations;
  const totalPages = Math.max(1, Math.ceil(recommendations.length / itemsPerPage));

  useEffect(() => {
    if (!isLinked) {
      setGenerationJobId(null);
      setActionMessage(null);
      setCollectingId(null);
      setScanPulse(0);
      return;
    }

    void starRecommendationsQuery.refetch();
  }, [isLinked, starRecommendationsQuery.refetch]);

  useEffect(() => {
    const status = generationStatusQuery.data?.status;
    if (!status) return;

    if (status === 'COMPLETED') {
      setGenerationJobId(null);
      void starRecommendationsQuery.refetch();
      return;
    }

    if (status === 'FAILED') {
      setGenerationJobId(null);
      setActionMessage(
        generationStatusQuery.data?.errorMessage ?? '추천 후보를 생성하지 못했습니다.',
      );
    }
  }, [generationStatusQuery.data?.status, generationStatusQuery.data?.errorMessage, starRecommendationsQuery.refetch]);

  useEffect(() => {
    if (!generationJobId) {
      setScanPulse(starRecommendationsQuery.data?.recommendations.length ? 100 : 0);
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setScanPulse((current) => (current + 11) % 100);
    }, 140);

    return () => window.clearInterval(intervalId);
  }, [generationJobId, starRecommendationsQuery.data?.recommendations.length]);

  useEffect(() => {
    const carousel = carouselRef.current;
    const recommendations = starRecommendationsQuery.data?.recommendations ?? [];
    if (recommendations.length === 0) {
      setActiveIndex(0);
      return undefined;
    }

    if (!carousel) return undefined;

    const handleScroll = () => {
      const nextIndex = Math.round(carousel.scrollLeft / Math.max(1, carousel.clientWidth));
      const maxIndex = Math.max(0, Math.ceil(recommendations.length / itemsPerPage) - 1);
      setActiveIndex(Math.min(maxIndex, Math.max(0, nextIndex)));
    };

    handleScroll();
    carousel.addEventListener('scroll', handleScroll, { passive: true });
    return () => carousel.removeEventListener('scroll', handleScroll);
  }, [itemsPerPage, recommendations.length]);

  useEffect(() => {
    const carousel = carouselRef.current;
    if (!carousel || !hasRecommendations) return undefined;

    const updateItemsPerPage = () => {
      const firstItem = carousel.firstElementChild;
      if (!(firstItem instanceof HTMLElement)) return;

      const styles = window.getComputedStyle(carousel);
      const columnGap = Number.parseFloat(styles.columnGap || styles.gap || '0') || 0;
      const itemWidth = firstItem.getBoundingClientRect().width;
      if (itemWidth <= 0) return;

      const visibleItems = Math.max(1, Math.round((carousel.clientWidth + columnGap) / (itemWidth + columnGap)));
      setItemsPerPage(visibleItems);
    };

    updateItemsPerPage();

    const resizeObserver = new ResizeObserver(updateItemsPerPage);
    resizeObserver.observe(carousel);
    Array.from(carousel.children).forEach((child) => {
      if (child instanceof HTMLElement) resizeObserver.observe(child);
    });

    return () => resizeObserver.disconnect();
  }, [hasRecommendations, recommendations.length]);

  const stageLabel = useMemo(() => {
    if (!isLinked) return '연결 필요';
    if (isRecommendationsLoading) return '추천 후보 조회 중';
    if (isGenerating) return '추천 생성 중';
    if (hasRecommendations) return '추천 준비 완료';
    return '대기 중';
  }, [hasRecommendations, isGenerating, isLinked, isRecommendationsLoading]);

  const progressValue = hasRecommendations
    ? 100
    : isGenerating
      ? Math.max(8, scanPulse)
      : 0;

  const scrollRecommendations = (direction: 'prev' | 'next') => {
    const nextPage = direction === 'next' ? activeIndex + 1 : activeIndex - 1;
    scrollToRecommendationPage(nextPage);
  };

  const scrollToRecommendationPage = (pageIndex: number) => {
    const node = carouselRef.current;
    if (!node) return;

    const nextPage = Math.min(totalPages - 1, Math.max(0, pageIndex));
    const targetItem = node.children[nextPage * itemsPerPage];
    const left = targetItem instanceof HTMLElement ? targetItem.offsetLeft : node.clientWidth * nextPage;

    node.scrollTo({ left, behavior: 'smooth' });
  };

  const handleGenerateRecommendations = () => {
    if (!canRequestRecommendations) return;
    setActionMessage(null);
    requestRecommendationsMutation.mutate();
  };

  if (githubLinkQuery.isError) {
    return (
      <section className="mx-auto w-full max-w-[720px] py-10">
        <EmptyState
          type="custom"
          variant="full"
          title="GitHub 연결 상태를 확인할 수 없습니다."
          description={getApiErrorMessage(githubLinkQuery.error, '연결 정보를 불러오지 못했습니다.')}
          primaryAction={{
            label: '프로필로 돌아가기',
            onClick: () => navigate('/profile'),
          }}
        />
      </section>
    );
  }

  if (githubLinkQuery.isLoading) {
    return (
      <section className="mx-auto flex w-full max-w-[720px] flex-col items-center justify-center py-20 text-center">
        <Loader2 size={34} className="animate-spin text-primary-signal" />
        <p className="mt-4 text-sm font-medium text-text-secondary">GitHub 연동 상태를 확인하는 중입니다.</p>
      </section>
    );
  }

  if (!isLinked) {
    return (
      <section className="mx-auto w-full max-w-[720px] py-10">
        <EmptyState
          type="custom"
          variant="full"
          title="GitHub 계정 연결이 필요합니다."
          description="star 목록을 불러오려면 먼저 GitHub 계정을 연결해야 합니다."
          primaryAction={{
            label: '연동하러 가기',
            onClick: () => navigate('/settings/integrations'),
          }}
          secondaryAction={{
            label: '프로필로 돌아가기',
            onClick: () => navigate('/profile'),
          }}
        />
      </section>
    );
  }

  return (
    <section className="mx-auto w-full max-w-[720px] py-10 text-text-primary">
      <header className="flex flex-col gap-6 border-b border-text-secondary/8 pb-6">
        <div className="flex flex-col gap-3">
          <h1 className="text-h1-bold text-text-primary">
            {linkedUsername ? (
              <>
                <span>반갑습니다, </span>
                <span className="text-primary-signal">{linkedUsername}</span>
                <span> 님.</span>
                <br />
                <span>당신의 지식 세계를 분석합니다.</span>
              </>
            ) : (
              '당신의 지식 세계를 분석합니다.'
            )}
          </h1>
          <p className="max-w-2xl text-sm leading-6 text-text-secondary">
            GitHub star 목록을 읽고, AI가 관련 스크랩 주소 5개를 추천합니다.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {showGenerationButton ? (
            <button
              type="button"
              onClick={handleGenerateRecommendations}
              disabled={!canRequestRecommendations}
              className="inline-flex items-center gap-2 rounded-xl border border-text-secondary/10 bg-surface-lowest px-4 py-2 text-sm font-semibold text-text-primary transition hover:border-text-secondary/20 hover:bg-surface-low disabled:cursor-not-allowed disabled:opacity-50"
            >
              {requestRecommendationsMutation.isPending ? (
                <Loader2 size={16} className="animate-spin" />
              ) : (
                <Star size={16} />
              )}
              GitHub Star 기반 추천
            </button>
          ) : null}
          <button
            type="button"
            onClick={() => navigate('/profile')}
            className="inline-flex items-center gap-2 rounded-xl border border-text-secondary/10 bg-surface-lowest px-4 py-2 text-sm font-semibold text-text-secondary transition hover:border-text-secondary/20 hover:bg-surface-low hover:text-text-primary"
          >
            프로필로 돌아가기
          </button>
        </div>
      </header>

      <div className="mt-8 flex flex-col gap-8">
        <section className="rounded-leaf border border-text-secondary/10 glass-card bg-surface-container/80 p-6">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="min-w-0 max-w-2xl">
              <h2 className="mt-2 text-body-lg-bold text-text-primary">분석 준비 상태</h2>
              <p className="mt-2 text-sm leading-6 text-text-secondary">
                지금 화면은 보고서가 아니라 연동용 로딩 화면입니다. 아래 추천 결과가 이 흐름의 끝입니다.
              </p>
            </div>

            <div className="flex items-center gap-2 rounded-full bg-primary-signal/10 px-3 py-1 text-[11px] font-bold text-primary-signal">
              <Sparkles size={12} className={isGenerating ? 'animate-pulse' : ''} />
              {stageLabel}
            </div>
          </div>

          <div className="mt-6 grid gap-4 lg:grid-cols-3">
            {[
              { title: '리포지토리 스캔', description: '최근 star와 연결된 저장소를 먼저 읽습니다.' },
              { title: '패턴 추출', description: '기술 스택과 관심 흐름을 하나씩 묶습니다.' },
              { title: '추천 생성', description: '관련 스크랩 주소 5개를 뽑아 보여줍니다.' },
            ].map((step, index) => {
              const active =
                (index === 0 && (isRecommendationsLoading || isGenerating || hasRecommendations)) ||
                (index === 1 && (isGenerating || hasRecommendations)) ||
                (index === 2 && hasRecommendations);

              return (
                <div
                  key={step.title}
                  className={`rounded-leaf p-4 transition ${
                    active ? 'bg-primary-signal/8' : 'bg-surface-lowest/80'
                  } ${isGenerating && active ? 'animate-pulse' : ''}`}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="min-w-0">
                      <h3 className="mt-1 text-body-sm-bold text-text-primary">{step.title}</h3>
                      <p className="mt-2 text-sm leading-6 text-text-secondary">{step.description}</p>
                    </div>
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-signal/10 text-[11px] font-black text-primary-signal">
                      {String(index + 1).padStart(2, '0')}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="mt-6 rounded-leaf bg-surface-lowest/80 p-5">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="mt-1 text-sm font-bold text-text-primary">
                  {isRecommendationsLoading
                    ? '추천 후보를 불러오는 중입니다.'
                    : isGenerating
                      ? 'AI가 star와 스크랩의 연결을 계산하는 중입니다.'
                      : hasRecommendations
                        ? '추천 후보를 불러왔습니다.'
                        : '버튼을 누르면 추천이 시작됩니다.'}
                </p>
                {actionMessage ? (
                  <p className="mt-2 text-xs leading-5 text-text-secondary">{actionMessage}</p>
                ) : null}
              </div>
              {isGenerating ? (
                <span className="text-xs font-bold text-primary-signal tabular-nums">{scanPulse}%</span>
              ) : null}
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-text-primary/[0.05]">
              <div
                className="h-full rounded-full bg-primary-signal transition-all duration-300"
                style={{ width: `${progressValue}%` }}
              />
            </div>
          </div>
        </section>

        <section ref={resultsRef} className="rounded-leaf border border-text-secondary/10 glass-card bg-surface-container/80 p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="min-w-0">
              <h2 className="mt-2 text-body-lg-bold text-text-primary">추천 스크랩 5개</h2>
              <p className="mt-2 text-sm leading-6 text-text-secondary">
                카드 내부 체크박스 버튼을 누르면 해당 추천 스크랩 1개가 지식카드로 생성됩니다.
              </p>
            </div>
            {hasRecommendations ? (
              <div className="flex items-center gap-1.5 rounded-full border border-text-secondary/10 glass-card bg-surface-container/80 px-3 py-1.5 !shadow-none">
                <button
                  type="button"
                  onClick={() => scrollRecommendations('prev')}
                  disabled={activeIndex === 0}
                  className="flex h-6 w-6 items-center justify-center rounded-full text-text-primary/40 transition hover:bg-surface-container/90 hover:text-text-primary disabled:opacity-20"
                  aria-label="이전 추천 스크랩"
                >
                  <ChevronLeft size={14} />
                </button>
                <div className="flex items-center gap-2 px-1">
                  {Array.from({ length: totalPages }).map((_, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => scrollToRecommendationPage(idx)}
                      className={`h-2.5 rounded-full transition-all duration-300 ${
                        idx === activeIndex ? 'w-5 bg-primary-signal' : 'w-2.5 bg-surface-highest/70 hover:bg-surface-container/90'
                      }`}
                      aria-label={`${idx + 1}페이지로 이동`}
                    />
                  ))}
                </div>
                <button
                  type="button"
                  onClick={() => scrollRecommendations('next')}
                  disabled={activeIndex === totalPages - 1}
                  className="flex h-6 w-6 items-center justify-center rounded-full text-text-primary/40 transition hover:bg-surface-container/90 hover:text-text-primary disabled:opacity-20"
                  aria-label="다음 추천 스크랩"
                >
                  <ChevronRight size={14} />
                </button>
              </div>
            ) : null}
          </div>

          {isGenerating || isRecommendationsLoading ? (
            <div className="mt-6 flex gap-4 overflow-hidden">
              {Array.from({ length: 5 }).map((_, index) => (
                <div
                  key={index}
                  className="flex h-[288px] min-w-full flex-col rounded-2xl bg-surface-lowest/80 p-4 sm:min-w-[calc((100%-1rem)/2)] lg:min-w-[calc((100%-2rem)/3)]"
                >
                  <div className="flex flex-1 flex-col justify-between gap-3">
                    <div className="space-y-3">
                      <div className="h-3 w-10 rounded-full bg-text-secondary/10" />
                      <div className="h-4 w-4/5 rounded-full bg-text-secondary/10" />
                      <div className="h-3 w-full rounded-full bg-text-secondary/10" />
                      <div className="h-3 w-5/6 rounded-full bg-text-secondary/10" />
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-bold text-text-secondary/50">0{index + 1}</span>
                      <Sparkles
                        size={14}
                        className={isGenerating ? 'animate-pulse text-primary-signal' : 'text-text-secondary/30'}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : hasRecommendations ? (
            <div
              ref={carouselRef}
              className="mt-6 flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-smooth pb-2 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]"
            >
              {recommendations.map((item, index) => (
                <div
                  key={item.recommendationId}
                  className="min-w-full snap-start sm:min-w-[calc((100%-1rem)/2)] lg:min-w-[calc((100%-2rem)/3)]"
                >
                  <RecommendationCard
                    item={item}
                    index={index}
                    isCollecting={collectingId === item.recommendationId}
                    isDisabled={collectRecommendationMutation.isPending}
                    onCreate={() => {
                      setActionMessage(null);
                      collectRecommendationMutation.mutate(item.recommendationId);
                    }}
                  />
                </div>
              ))}
            </div>
          ) : (
            <div className="mt-6">
              <EmptyState
                type="custom"
                variant="inline"
                title="추천 후보가 없습니다."
                description="GitHub Star 기반 추천을 먼저 실행하면 관련 스크랩 주소 5개를 받아볼 수 있습니다."
                primaryAction={{
                  label: 'GitHub Star 기반 추천',
                  onClick: handleGenerateRecommendations,
                }}
              />
            </div>
          )}
        </section>

      </div>
    </section>
  );
}

function RecommendationCard({
  item,
  index,
  isCollecting,
  isDisabled,
  onCreate,
}: {
  item: GithubStarRecommendation;
  index: number;
  isCollecting: boolean;
  isDisabled: boolean;
  onCreate: () => void;
}) {
  return (
    <article
      className="group flex h-[288px] flex-col overflow-hidden rounded-2xl border border-text-secondary/6 bg-surface-lowest/80 p-4 transition hover:border-text-secondary/10 hover:bg-surface-low"
      style={{
        animationDelay: `${index * 60}ms`,
        animationName: 'san-fade-up',
        animationDuration: '280ms',
        animationFillMode: 'both',
      }}
    >
      <div className="flex h-full min-h-0 flex-col gap-3">
        <div className="flex items-start justify-between gap-3">
          <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-primary-signal/10 text-primary-signal">
            <span className="text-[10px] font-black">{String(index + 1).padStart(2, '0')}</span>
          </div>
          <h3 className="min-w-0 flex-1 break-words text-sm font-bold text-text-primary">{item.title}</h3>
        </div>

        <div className="flex min-h-0 flex-1 content-start flex-wrap gap-2 overflow-y-auto pr-1 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
          {item.tagList.map((tag) => (
            <span
              key={tag}
              className="inline-flex max-w-full items-center break-all rounded-full bg-text-primary/[0.04] px-2.5 py-1 text-[11px] font-medium text-text-secondary"
            >
              #{tag}
            </span>
          ))}
        </div>

        <div className="mt-auto flex shrink-0 flex-col gap-2">
          <a
            href={item.recommendationUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 text-[11px] font-bold text-primary-signal transition hover:opacity-80"
          >
            URL 열기
            <ExternalLink size={12} />
          </a>

          {item.collected ? (
            <div className="inline-flex items-center justify-center gap-1.5 rounded-xl border border-primary-signal/15 bg-primary-signal/10 px-3 py-2 text-[11px] font-bold text-primary-signal">
              <CheckSquare size={13} />
              생성 완료
            </div>
          ) : (
            <button
              type="button"
              onClick={onCreate}
              disabled={isDisabled}
              className="inline-flex min-h-9 items-center justify-center gap-1.5 rounded-xl border border-text-secondary/10 bg-surface-lowest px-3 py-2 text-[11px] font-bold text-text-primary transition hover:border-text-secondary/20 hover:bg-surface-low disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isCollecting ? <Loader2 size={12} className="animate-spin" /> : <Square size={12} />}
              지식카드로 생성
            </button>
          )}
        </div>
      </div>
    </article>
  );
}
