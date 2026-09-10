import { useCallback, useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Search, Loader2, RotateCcw } from 'lucide-react';
import type {
    AsyncJobStatusResponse,
    RecallQuizResponse,
    RecallQuizType,
    TilResponse,
    TilSourceContentResponse,
} from '@san/shared';
import type { TilRecallCardsQuery, TilRecallQuizzesQuery, TilSourcesQuery } from '../types';
import { CollectedDataCard, type CollectedDataItem } from './CollectedDataCard';
import { useRecallQuizGenerateMutation, useTilGenerationRetryMutation } from '../hooks/useTilMutations';
import { tilKeys, useTilAsyncJobStatus, useTilGenerationHistory, useTilRecallQuizzes } from '../hooks/useTilQueries';
import { RecallHistory } from './RecallHistory';
import { RecallQuizModal } from './RecallQuizModal';

const EMPTY_SOURCES: TilSourceContentResponse[] = [];
const REVIEW_STATUS_TITLE = '복습 현황';
const REVIEW_COMPLETE_DESCRIPTION = 'Recall 퀴즈 제출 기록이 있어요.';
const REVIEW_PENDING_DESCRIPTION = 'Recall 퀴즈를 풀면 복습 완료로 표시됩니다.';
const REVIEW_SOLVED_LABEL = '풀이 현황';
const REVIEW_CORRECT_LABEL = '정답 수';
const REVIEW_STATUS_LABEL = '상태';
const REVIEW_SUBMITTED_LABEL = '제출 완료';
const REVIEW_NO_QUIZ_LABEL = '복습할 퀴즈가 없습니다.';
const QUIZ_GENERATE_LABEL = '퀴즈 생성하기';
const QUIZ_GENERATING_LABEL = 'AI가 퀴즈를 생성하고 있어요...';

interface CollectedDataPanelProps {
    sourcesQuery: TilSourcesQuery;
    recallCardsQuery: TilRecallCardsQuery;
    selectedTil: TilResponse | null;
}

type PanelTab = 'sources' | 'recall';

export function CollectedDataPanel({ sourcesQuery, recallCardsQuery, selectedTil }: CollectedDataPanelProps) {
    const [activeTab, setActiveTab] = useState<PanelTab>('recall');
    const [quizType, setQuizType] = useState<RecallQuizType>('OX');
    const [searchQuery, setSearchQuery] = useState('');
    const debouncedSearch = useDebounce(searchQuery, 300);
    const sources = sourcesQuery.data?.sources ?? EMPTY_SOURCES;
    const recallCount = recallCardsQuery.data?.recallCards.length ?? 0;
    const recallQuizzesQuery = useTilRecallQuizzes(selectedTil?.targetDate, quizType, Boolean(selectedTil));
    const generationHistoryQuery = useTilGenerationHistory(selectedTil?.summaryId);
    const queryClient = useQueryClient();
    const retryGenerationMutation = useTilGenerationRetryMutation(() => {
        void queryClient.invalidateQueries({ queryKey: tilKeys.generationHistory(selectedTil?.summaryId) });
    });

    const items: CollectedDataItem[] = useMemo(() => {
        const baseItems = sources.map((source) => ({
            id: source.scrapId,
            type: toCollectedDataType(source.sourceType),
            title: source.title,
            timeLabel: new Date(source.createdAt).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
            }),
            excerpt: source.rawContent || source.sourceUrl || '',
            tag: [source.referenceId, source.category?.categoryName ? `# ${source.category.categoryName}` : '']
                .filter(Boolean)
                .join(' · '),
            imageUrl: source.imageUrl ?? undefined,
            href: source.sourceUrl ?? undefined,
        }));

        if (!debouncedSearch) return baseItems;

        const lowerSearch = debouncedSearch.toLowerCase();
        return baseItems.filter((item) =>
            item.title.toLowerCase().includes(lowerSearch) ||
            item.excerpt.toLowerCase().includes(lowerSearch) ||
            item.tag.toLowerCase().includes(lowerSearch)
        );
    }, [sources, debouncedSearch]);

    return (
        <aside className="flex h-full w-full flex-col overflow-hidden bg-transparent">
            <ReviewStatusSummary
                recallQuizzesQuery={recallQuizzesQuery}
                selectedTil={selectedTil}
                quizType={quizType}
                onQuizTypeChange={setQuizType}
            />

            <header className="flex shrink-0 items-center gap-2 border-b border-text-secondary/5 p-3">
                <PanelTabButton
                    active={activeTab === 'recall'}
                    badge={recallCount}
                    label="Recall"
                    onClick={() => setActiveTab('recall')}
                />
                <PanelTabButton
                    active={activeTab === 'sources'}
                    label="Source"
                    onClick={() => setActiveTab('sources')}
                />
            </header>

            {activeTab === 'sources' ? (
                <>
                    <div className="border-b border-text-secondary/5 p-4">
                        <label className="flex items-center gap-2 rounded-full bg-surface-highest px-4 py-2 transition focus-within:ring-1 focus-within:ring-action-accent/30">
                            <Search size={16} className="text-text-secondary" />
                            <input
                                type="search"
                                placeholder="키워드로 검색..."
                                value={searchQuery}
                                onChange={(event) => setSearchQuery(event.target.value)}
                                className="w-full bg-transparent text-sm text-text-primary outline-none placeholder:text-text-secondary/60"
                            />
                        </label>
                    </div>

                    <div className="no-scrollbar flex min-h-0 flex-1 flex-col gap-6 overflow-y-auto p-6">
                        {selectedTil && !sourcesQuery.isPending && sourcesQuery.data?.evidenceSnapshot === false ? (
                            <div className="rounded-xl border border-text-secondary/10 bg-text-primary/[0.03] px-4 py-3 text-sm leading-5 text-text-secondary">
                                이 TIL은 근거 스냅샷 저장 전 생성되어, 실제 사용한 원문을 확인할 수 없습니다.
                            </div>
                        ) : null}
                        {selectedTil && !sourcesQuery.isPending && sourcesQuery.data?.evidenceScope === 'TIL_INPUT_SNAPSHOT' ? (
                            <div className="rounded-xl border border-text-secondary/10 bg-text-primary/[0.03] px-4 py-3 text-sm leading-5 text-text-secondary">
                                `SRC-xx`는 이 TIL 생성에 입력된 카드 스냅샷입니다. 문장별 인용이나 사실 검증을 의미하지는 않습니다.
                            </div>
                        ) : null}
                        {sourcesQuery.data?.dataProtection ? (
                            <div className="rounded-xl border border-action-accent/20 bg-action-accent/[0.06] px-4 py-3 text-sm leading-5 text-text-secondary">
                                {sourcesQuery.data.dataProtection.maskedItemCount > 0
                                    ? `AI 입력 보호 기록: 개인정보 패턴이 감지된 카드 입력 ${sourcesQuery.data.dataProtection.maskedItemCount}건을 마스킹하고, 사용자 확인 후 AI 처리 요청을 등록했습니다.`
                                    : 'AI 입력 보호 기록: 개인정보 패턴은 감지되지 않았습니다.'}
                            </div>
                        ) : null}
                        {selectedTil && generationHistoryQuery.data?.length ? (
                            <TilGenerationHistory
                                jobs={generationHistoryQuery.data}
                                retryPending={retryGenerationMutation.isPending}
                                onRetry={(jobId) => {
                                    const confirmed = window.confirm('기존에 마스킹된 입력 스냅샷으로 TIL 생성을 다시 시도할까요?');
                                    if (confirmed) retryGenerationMutation.mutate(jobId);
                                }}
                            />
                        ) : null}
                        {sourcesQuery.isPending && selectedTil && !import.meta.env.DEV ? (
                            <div className="py-10 text-center text-sm italic text-text-secondary opacity-50">
                                수집 데이터를 불러오는 중...
                            </div>
                        ) : null}

                        {items.map((item) => (
                            <CollectedDataCard key={item.id} item={item} />
                        ))}

                        {selectedTil && !sourcesQuery.isPending && items.length === 0 ? (
                            <div className="py-10 text-center text-sm italic text-text-secondary opacity-50">
                                {debouncedSearch ? '검색 결과가 없습니다.' : '수집 데이터가 없습니다.'}
                            </div>
                        ) : null}
                    </div>
                </>
            ) : (
                <RecallHistory
                    recallCardsQuery={recallCardsQuery}
                    selectedTil={selectedTil}
                    variant="panel"
                />
            )}
        </aside>
    );
}

function TilGenerationHistory({
    jobs,
    retryPending,
    onRetry,
}: {
    jobs: AsyncJobStatusResponse[];
    retryPending: boolean;
    onRetry: (jobId: string) => void;
}) {
    return (
        <section className="rounded-xl border border-text-secondary/10 bg-text-primary/[0.03] px-4 py-3 text-sm text-text-secondary">
            <div className="mb-2 font-medium text-text-primary">AI 생성 실행 기록</div>
            <ul className="space-y-2">
                {jobs.slice(0, 5).map((job) => (
                    <li key={job.jobId} className="flex items-center justify-between gap-3">
                        <span>{formatJobStatus(job.status)} · {formatJobTime(job.completedAt ?? job.startedAt ?? job.createdAt)}</span>
                        {job.status === 'FAILED' ? (
                            <button
                                type="button"
                                disabled={retryPending}
                                onClick={() => onRetry(job.jobId)}
                                className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-xs font-medium text-action-accent transition hover:bg-action-accent/10 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                <RotateCcw size={12} /> 다시 시도
                            </button>
                        ) : null}
                    </li>
                ))}
            </ul>
        </section>
    );
}

function formatJobStatus(status: string) {
    if (status === 'COMPLETED') return '완료';
    if (status === 'FAILED') return '실패';
    if (status === 'PROCESSING') return '처리 중';
    return '대기 중';
}

function formatJobTime(value: string) {
    return new Date(value).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function ReviewStatusSummary({
    recallQuizzesQuery,
    selectedTil,
    quizType,
    onQuizTypeChange,
}: {
    recallQuizzesQuery: TilRecallQuizzesQuery;
    selectedTil: TilResponse | null;
    quizType: RecallQuizType;
    onQuizTypeChange: (type: RecallQuizType) => void;
}) {
    const queryClient = useQueryClient();
    const [isQuizModalOpen, setIsQuizModalOpen] = useState(false);
    const [quizJobId, setQuizJobId] = useState<string | null>(null);
    const [hasRequestedGeneration, setHasRequestedGeneration] = useState(false);
    const quizzes = recallQuizzesQuery.data?.quizzes ?? [];
    const solvedQuizCount = quizzes.filter((quiz: RecallQuizResponse) => quiz.solved).length;
    const correctQuizCount = quizzes.filter((quiz: RecallQuizResponse) => quiz.correct === true).length;
    const reviewed = solvedQuizCount > 0;
    const solvedLabel = quizzes.length > 0 ? `${solvedQuizCount}/${quizzes.length}` : '0/0';

    const generateMutation = useRecallQuizGenerateMutation({
        onSuccess: (response) => {
            setQuizJobId(response.quizJobId);
        },
        onError: (error) => {
            if (getHttpStatus(error) === 409) {
                setHasRequestedGeneration(true);
                void queryClient.invalidateQueries({
                    queryKey: tilKeys.recallQuizzes(selectedTil?.targetDate, quizType),
                });
            } else {
                setHasRequestedGeneration(false);
            }
        }
    });

    const quizJobStatusQuery = useTilAsyncJobStatus(quizJobId);

    useEffect(() => {
        if (quizJobStatusQuery.data?.status === 'COMPLETED' && selectedTil) {
            void queryClient.invalidateQueries({
                queryKey: tilKeys.recallQuizzes(selectedTil.targetDate, quizType),
            });
            const timer = window.setTimeout(() => setQuizJobId(null), 0);
            return () => window.clearTimeout(timer);
        }
        if (quizJobStatusQuery.data?.status === 'FAILED') {
            const timer = window.setTimeout(() => {
                setQuizJobId(null);
                setHasRequestedGeneration(false);
            }, 0);
            return () => window.clearTimeout(timer);
        }
        return undefined;
    }, [quizJobStatusQuery.data?.status, queryClient, selectedTil, quizType]);

    const isGenerating = generateMutation.isPending || quizJobStatusQuery.data?.status === 'PENDING' || quizJobStatusQuery.data?.status === 'PROCESSING';
    const canGenerate = !isGenerating && !hasRequestedGeneration;

    const handleGenerate = useCallback(() => {
        if (!selectedTil || !canGenerate) return;
        setHasRequestedGeneration(true);
        generateMutation.mutate({
            targetDate: selectedTil.targetDate,
            quizType,
        });
    }, [canGenerate, generateMutation, quizType, selectedTil]);

    useEffect(() => {
        const timer = window.setTimeout(() => {
            setHasRequestedGeneration(false);
            setQuizJobId(null);
        }, 0);
        return () => window.clearTimeout(timer);
    }, [selectedTil?.summaryId, quizType]);

    useEffect(() => {
        if (
            selectedTil &&
            recallQuizzesQuery.isSuccess &&
            quizzes.length === 0 &&
            canGenerate &&
            !quizJobId
        ) {
            const timer = window.setTimeout(handleGenerate, 0);
            return () => window.clearTimeout(timer);
        }
        return undefined;
    }, [
        selectedTil,
        recallQuizzesQuery.isSuccess,
        quizzes.length,
        canGenerate,
        quizJobId,
        handleGenerate,
    ]);

    useEffect(() => {
        if (generateMutation.isError && !isGenerating) {
            const timer = window.setTimeout(() => setHasRequestedGeneration(false), 0);
            return () => window.clearTimeout(timer);
        }
        return undefined;
    }, [generateMutation.isError, isGenerating]);

    return (
        <>
            <section className="shrink-0 px-5 pb-6 pt-2">
                <header className="flex items-center justify-between">
                    <div className="min-w-0">
                        <p className="text-sm font-extrabold text-text-primary">
                            {REVIEW_STATUS_TITLE}
                        </p>
                    </div>
                </header>
                {quizzes.length > 0 && (
                    <p className="mt-1 text-xs leading-relaxed text-text-secondary/75">
                        {reviewed ? REVIEW_COMPLETE_DESCRIPTION : REVIEW_PENDING_DESCRIPTION}
                    </p>
                )}

                {quizzes.length > 0 ? (
                    <button
                        type="button"
                        onClick={() => setIsQuizModalOpen(true)}
                        className="mt-4 block w-full text-left transition focus:outline-none"
                    >
                        <div className="rounded-xl bg-text-primary/[0.03] p-4 transition hover:bg-text-primary/[0.05]">
                            <dl className="space-y-3 text-xs">
                                <div className="flex items-center justify-between gap-3">
                                    <dt className="text-text-secondary/65">{REVIEW_SOLVED_LABEL}</dt>
                                    <dd className="font-bold text-text-primary">{solvedLabel}</dd>
                                </div>
                                <div className="flex items-center justify-between gap-3">
                                    <dt className="text-text-secondary/65">{REVIEW_CORRECT_LABEL}</dt>
                                    <dd className="font-bold text-text-primary">{correctQuizCount}</dd>
                                </div>
                                <div className="flex items-center justify-between gap-3">
                                    <dt className="text-text-secondary/65">{REVIEW_STATUS_LABEL}</dt>
                                    <dd className="font-bold text-text-primary">{reviewed ? REVIEW_SUBMITTED_LABEL : '-'}</dd>
                                </div>
                            </dl>
                        </div>
                    </button>
                ) : (
                    <div className="mt-4 flex flex-col items-center gap-3 py-6">
                        <p className="text-center text-sm italic text-text-secondary/50">
                            {REVIEW_NO_QUIZ_LABEL}
                        </p>
                        {selectedTil && (
                            <button
                                type="button"
                                onClick={handleGenerate}
                                disabled={!canGenerate}
                                className="flex items-center gap-2 rounded-lg bg-action-accent/10 px-4 py-2 text-xs font-bold text-action-accent transition hover:bg-action-accent/20 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                {isGenerating ? (
                                    <>
                                        <Loader2 size={14} className="animate-spin" />
                                        {QUIZ_GENERATING_LABEL}
                                    </>
                                ) : (
                                    QUIZ_GENERATE_LABEL
                                )}
                            </button>
                        )}
                    </div>
                )}
            </section>

            {selectedTil && isQuizModalOpen ? (
                <RecallQuizModal
                    onClose={() => setIsQuizModalOpen(false)}
                    tilTitle={selectedTil.title}
                    targetDate={selectedTil.targetDate}
                    quizzes={quizzes}
                    quizType={quizType}
                    onQuizTypeChange={onQuizTypeChange}
                    isGenerating={isGenerating}
                />
            ) : null}
        </>
    );
}

function PanelTabButton({
    active,
    badge,
    label,
    onClick,
}: {
    active: boolean;
    badge?: number;
    label: string;
    onClick: () => void;
}) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={`relative flex h-9 flex-1 items-center justify-center gap-2 text-xs font-bold uppercase tracking-widest transition-colors after:absolute after:bottom-0 after:left-1/2 after:h-px after:w-8 after:-translate-x-1/2 after:transition-all ${
                active
                    ? 'til-light-teal-accent text-action-accent after:bg-action-accent'
                    : 'text-text-secondary/65 after:bg-transparent hover:text-text-primary/90 hover:after:bg-text-primary/20'
            }`}
        >
            {label}
            {badge ? (
                <span className="til-light-teal-badge rounded-full bg-action-accent/15 px-1.5 py-0.5 text-[10px] text-action-accent">
                    {badge}
                </span>
            ) : null}
        </button>
    );
}

function useDebounce<T>(value: T, delay: number): T {
    const [debouncedValue, setDebouncedValue] = useState<T>(value);
    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedValue(value);
        }, delay);
        return () => clearTimeout(handler);
    }, [value, delay]);
    return debouncedValue;
}

function toCollectedDataType(sourceType: string): CollectedDataItem['type'] {
    if (sourceType === 'LINK') return 'link';
    if (sourceType === 'IMAGE') return 'image';
    return 'text';
}

function getHttpStatus(error: unknown) {
    return (error as { response?: { status?: number } })?.response?.status;
}
