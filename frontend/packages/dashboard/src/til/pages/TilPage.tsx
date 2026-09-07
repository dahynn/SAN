import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AlertTriangle, ChevronLeft, ChevronRight, GitCommitHorizontal, MoreHorizontal, MoreVertical, Trash2, PanelLeft, X } from 'lucide-react';
import { CollectedDataPanel } from '../components/CollectedDataPanel';
import { TILEditor } from '../components/TILEditor';
import { useTilPageLogic } from '../hooks/useTilPageLogic';
import { TILModeTabs, type TILMode } from '../components/TILModeTabs';
import type { TilResponse } from '@san/shared';

export function TilPage() {
    const [searchParams, setSearchParams] = useSearchParams();
    const {
        selectedDate,
        setSelectedDate,
        title,
        setTitle,
        draft,
        tilList,
        selectedSummaryId,
        setSelectedSummaryId,
        selectedTil,
        tilQuery,
        recallCardsQuery,
        sourcesQuery,
        generationStatusQuery,
        commitStatusQuery,
        generateMutation,
        updateMutation,
        deleteMutation,
        commitMutation,
        generationTone,
        commitTone,
        generationMessage,
        commitMessage,
    } = useTilPageLogic();

    const [activeTab, setActiveTab] = useState<TILMode>('drafts');
    const [isTilListOpen, setIsTilListOpen] = useState(false);
    const [openTilMenuId, setOpenTilMenuId] = useState<string | null>(null);
    const [isTitleMenuOpen, setIsTitleMenuOpen] = useState(false);
    const [isCalendarOpen, setIsCalendarOpen] = useState(false);
    const [calendarMonth, setCalendarMonth] = useState(() => startOfMonth(parseDateString(selectedDate)));
    const [pendingDeleteTilId, setPendingDeleteTilId] = useState<string | null>(null);
    const dateLabel = formatDateForDisplay(selectedDate);
    const dateParam = searchParams.get('date');

    useEffect(() => {
        if (!isValidDateParam(dateParam) || dateParam === selectedDate) return;
        setSelectedDate(dateParam);
    }, [dateParam, selectedDate, setSelectedDate]);

    useEffect(() => {
        if (!openTilMenuId && !isTitleMenuOpen && !isCalendarOpen) return;

        const handlePointerDown = () => {
            setOpenTilMenuId(null);
            setIsTitleMenuOpen(false);
            setIsCalendarOpen(false);
        };
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                setOpenTilMenuId(null);
                setIsTitleMenuOpen(false);
                setIsCalendarOpen(false);
            }
        };

        document.addEventListener('pointerdown', handlePointerDown);
        document.addEventListener('keydown', handleKeyDown);

        return () => {
            document.removeEventListener('pointerdown', handlePointerDown);
            document.removeEventListener('keydown', handleKeyDown);
        };
    }, [openTilMenuId, isTitleMenuOpen, isCalendarOpen]);

    const closeFloatingControls = () => {
        setOpenTilMenuId(null);
        setIsTitleMenuOpen(false);
        setIsCalendarOpen(false);
    };

    const changeSelectedDate = (date: string) => {
        setSelectedDate(date);
        setSearchParams({ date });
        setCalendarMonth(startOfMonth(parseDateString(date)));
        closeFloatingControls();
    };

    const goToPrevDate = () => {
        const date = parseDateString(selectedDate);
        date.setDate(date.getDate() - 1);
        changeSelectedDate(shiftDate(date));
    };

    const maxSelectableDate = getTodayDate();
    const isMaxSelectableDate = selectedDate >= maxSelectableDate;
    const maxSelectableMonth = startOfMonth(parseDateString(maxSelectableDate));
    const calendarDays = getCalendarDays(calendarMonth);

    const goToNextDate = () => {
        if (isMaxSelectableDate) return;
        const date = parseDateString(selectedDate);
        date.setDate(date.getDate() + 1);
        changeSelectedDate(shiftDate(date));
    };

    const isCommitting =
        commitMutation.isPending ||
        commitStatusQuery.data?.status === 'PENDING' ||
        commitStatusQuery.data?.status === 'PROCESSING';
    const displayedTitle = activeTab === 'drafts' ? selectedTil?.title ?? title : title;

    const requestDeleteTil = (summaryId = selectedTil?.summaryId) => {
        if (!summaryId || deleteMutation.isPending) return;

        setOpenTilMenuId(null);
        setIsTitleMenuOpen(false);
        setPendingDeleteTilId(summaryId);
    };

    const confirmDeleteTil = () => {
        if (!pendingDeleteTilId || deleteMutation.isPending) return;

        deleteMutation.mutate(pendingDeleteTilId, {
            onSettled: () => setPendingDeleteTilId(null),
        });
    };

    return (
        <section className="flex h-auto w-full flex-col overflow-hidden bg-background text-text-primary lg:h-[calc(100vh-104px)]">
            <div className="flex h-full w-full flex-col lg:min-h-0 lg:flex-row">
            <div className="no-scrollbar order-1 flex min-h-0 min-w-0 flex-1 flex-col overflow-y-auto px-0 pb-5 pt-12 md:px-1 lg:order-1 lg:pr-8">
                <header className="mb-4 flex flex-col gap-2">
                    <h1 className="flex items-baseline gap-1 text-2xl font-extrabold tracking-tight">
                        <span className="text-til-title-accent">T</span>
                        <span className="text-text-primary">oday</span>
                        <span className="ml-2 text-til-title-accent">I</span>
                        <span className="ml-2 text-til-title-accent">L</span>
                        <span className="text-text-primary">earned</span>
                    </h1>

                    <div className="flex flex-col justify-between gap-3 min-[1700px]:flex-row min-[1700px]:items-center">
                        <div className="flex min-w-0 items-center gap-2">
                            <div
                                className="relative flex min-w-0 items-center gap-1 px-1.5 py-1"
                                onPointerDown={(event) => event.stopPropagation()}
                            >
                                <button
                                    type="button"
                                    onClick={goToPrevDate}
                                    className="rounded p-0.5 text-text-secondary transition-colors hover:text-text-primary"
                                    aria-label="이전 날짜"
                                >
                                    <ChevronLeft size={14} />
                                </button>
                                <button
                                    type="button"
                                    onClick={() => {
                                        setIsCalendarOpen((current) => !current);
                                        setCalendarMonth(startOfMonth(parseDateString(selectedDate)));
                                        setOpenTilMenuId(null);
                                        setIsTitleMenuOpen(false);
                                    }}
                                    className="flex min-w-0 max-w-[12rem] shrink items-center justify-center truncate whitespace-nowrap px-2 py-0.5 text-center text-sm font-semibold tracking-wide text-text-primary sm:min-w-[148px]"
                                    aria-expanded={isCalendarOpen}
                                    title={dateLabel}
                                >
                                    {dateLabel}
                                </button>
                                <button
                                    type="button"
                                    onClick={goToNextDate}
                                    disabled={isMaxSelectableDate}
                                    className="rounded p-0.5 text-text-secondary transition-colors hover:text-text-primary disabled:cursor-not-allowed disabled:opacity-25"
                                    aria-label="다음 날짜"
                                >
                                    <ChevronRight size={14} />
                                </button>

                                {isCalendarOpen ? (
                                    <div className="absolute left-0 top-11 z-40 w-[260px] rounded-[24px] border border-text-secondary/12 glass-popover bg-surface-lowest/82 p-3 backdrop-blur-2xl">
                                        <div className="mb-3 flex items-center justify-between">
                                            <button
                                                type="button"
                                                onClick={() => setCalendarMonth((current) => shiftMonth(current, -1))}
                                                className="flex h-7 w-7 items-center justify-center rounded-lg text-text-secondary transition-colors hover:bg-text-primary/8 hover:text-text-primary"
                                                aria-label="이전 달"
                                            >
                                                <ChevronLeft size={15} />
                                            </button>
                                            <span className="text-sm font-bold text-text-primary">
                                                {formatCalendarMonth(calendarMonth)}
                                            </span>
                                            <button
                                                type="button"
                                                onClick={() => setCalendarMonth((current) => shiftMonth(current, 1))}
                                                disabled={calendarMonth >= maxSelectableMonth}
                                                className="flex h-7 w-7 items-center justify-center rounded-lg text-text-secondary transition-colors hover:bg-text-primary/8 hover:text-text-primary disabled:cursor-not-allowed disabled:opacity-30"
                                                aria-label="다음 달"
                                            >
                                                <ChevronRight size={15} />
                                            </button>
                                        </div>

                                        <div className="grid grid-cols-7 gap-0.5 text-center text-[10px] font-bold uppercase tracking-wider text-text-secondary/60">
                                            {['일', '월', '화', '수', '목', '금', '토'].map((day) => (
                                                <span key={day} className="py-1">{day}</span>
                                            ))}
                                        </div>

                                        <div className="mt-1 grid grid-cols-7 gap-0.5">
                                            {calendarDays.map((day) => {
                                                const dayValue = shiftDate(day);
                                                const isCurrentMonth = day.getMonth() === calendarMonth.getMonth();
                                                const isSelected = dayValue === selectedDate;
                                                const isDisabled = dayValue > maxSelectableDate;

                                                return (
                                                    <button
                                                        key={dayValue}
                                                        type="button"
                                                        onClick={() => {
                                                            if (isDisabled) return;
                                                            changeSelectedDate(dayValue);
                                                        }}
                                                        disabled={isDisabled}
                                                        className={`flex h-7 items-center justify-center rounded-lg text-xs font-semibold transition-colors ${
                                                            isSelected
                                                                ? 'bg-action-accent text-text-on-accent'
                                                                : isCurrentMonth
                                                                    ? 'text-text-primary hover:bg-text-primary/10'
                                                                    : 'text-text-secondary/35 hover:bg-text-primary/5'
                                                        } disabled:cursor-not-allowed disabled:text-text-secondary/20 disabled:hover:bg-transparent`}
                                                    >
                                                        {day.getDate()}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    </div>
                                ) : null}
                            </div>

                            <div className="hidden h-4 w-px bg-text-primary/10 min-[1700px]:block" />
                        </div>

                        <div className="flex w-full min-w-0 flex-row items-center gap-2 sm:justify-start min-[1700px]:w-auto min-[1700px]:justify-end">
                            <TILModeTabs activeTab={activeTab} onChange={setActiveTab} />

                            <button
                                type="button"
                                onClick={() => selectedTil && commitMutation.mutate(selectedTil.summaryId)}
                                disabled={isCommitting || !selectedTil}
                                className="flex h-10 w-10 min-w-10 shrink-0 items-center justify-center gap-1.5 rounded-bl-md rounded-br-[14px] rounded-tl-[14px] rounded-tr-md bg-action-accent px-0 text-sm font-medium text-text-on-accent transition-colors hover:bg-action-accent-hover disabled:cursor-not-allowed disabled:opacity-50 sm:w-[104px] sm:min-w-[104px] sm:px-3 md:w-[132px] md:min-w-[132px] md:gap-2 md:px-4"
                                title={isCommitting ? 'Committing...' : 'Commit'}
                            >
                                <GitCommitHorizontal size={15} />
                                <span className="hidden sm:inline">{isCommitting ? 'Committing...' : 'Commit'}</span>
                            </button>
                        </div>
                    </div>
                </header>

                <div className="flex min-h-0 flex-1 gap-2">
                    <div className="flex min-h-0 shrink-0 items-start gap-1.5">
                        <button
                            type="button"
                            onClick={() => {
                                setIsTilListOpen((current) => !current);
                                setOpenTilMenuId(null);
                            }}
                            className={`flex h-8 w-8 shrink-0 items-center justify-center text-text-secondary transition ${
                                isTilListOpen
                                    ? 'text-text-primary'
                                    : 'hover:text-action-accent'
                            }`}
                            aria-label="Toggle TIL list"
                            aria-expanded={isTilListOpen}
                        >
                            <PanelLeft size={19} />
                        </button>

                        <aside
                            className={`min-h-[500px] overflow-hidden transition-[width,border-color] duration-300 ease-out ${
                                isTilListOpen
                                    ? 'w-[260px] border-r border-text-secondary/10'
                                    : 'w-0 border-r-0 border-transparent'
                            }`}
                            aria-hidden={!isTilListOpen}
                        >
                            <div className="w-[260px]">
                                <div className={`no-scrollbar max-h-[calc(100vh-230px)] overflow-y-auto px-2 pb-3 transition-opacity duration-150 ${
                                    isTilListOpen ? 'opacity-100 delay-100' : 'pointer-events-none opacity-0'
                                }`}>
                                    {tilQuery.isPending ? (
                                        <div className="px-3 py-4 text-sm text-text-secondary">Loading...</div>
                                    ) : tilList.length === 0 ? (
                                        <div className="px-3 py-4 text-sm text-text-secondary">No TILs for this date.</div>
                                    ) : (
                                        tilList.map((til, index) => {
                                            const isSelected = til.summaryId === (selectedSummaryId ?? selectedTil?.summaryId);

                                            return (
                                                <div key={til.summaryId} className="group relative">
                                                    <button
                                                        type="button"
                                                        onClick={() => {
                                                            setSelectedSummaryId(til.summaryId);
                                                            setOpenTilMenuId(null);
                                                        }}
                                                        className={`flex w-full items-start gap-3 rounded-xl px-3 py-3 pr-9 text-left transition ${
                                                            isSelected
                                                                ? 'bg-text-primary/[0.075] text-text-primary'
                                                                : 'text-text-secondary hover:bg-text-primary/5 hover:text-text-primary'
                                                        }`}
                                                    >
                                                        <span className="min-w-0">
                                                            <span className="block truncate text-sm font-semibold">
                                                                {getTilTitle(til, index)}
                                                            </span>
                                                            <span className="mt-1 block text-xs text-text-secondary/70">
                                                                {formatTimeForDisplay(til.updatedAt)}
                                                            </span>
                                                        </span>
                                                    </button>

                                                    <button
                                                        type="button"
                                                        onClick={(event) => {
                                                            event.stopPropagation();
                                                            setOpenTilMenuId((current) => current === til.summaryId ? null : til.summaryId);
                                                        }}
                                                        aria-label="Open TIL actions"
                                                        aria-expanded={openTilMenuId === til.summaryId}
                                                        className={`absolute right-2 top-2 flex h-7 w-7 items-center justify-center rounded-full text-text-secondary transition ${
                                                            openTilMenuId === til.summaryId
                                                                ? 'text-text-secondary'
                                                                : 'opacity-0 hover:text-text-primary group-hover:opacity-100'
                                                        }`}
                                                    >
                                                        <MoreVertical size={15} strokeWidth={2.4} />
                                                    </button>

                                                    {openTilMenuId === til.summaryId ? (
                                                        <div
                                                            className="absolute right-2 top-10 z-30 w-32 overflow-hidden rounded-xl border border-text-secondary/12 glass-popover bg-surface-lowest/78 p-1 backdrop-blur-xl"
                                                            onPointerDown={(event) => event.stopPropagation()}
                                                        >
                                                            <button
                                                                type="button"
                                                                onClick={(event) => {
                                                                    event.stopPropagation();
                                                                    requestDeleteTil(til.summaryId);
                                                                }}
                                                                disabled={deleteMutation.isPending}
                                                                className="flex h-9 w-full items-center gap-2 rounded-lg px-2.5 text-sm font-medium text-text-secondary transition-colors hover:bg-red-500/12 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-40"
                                                            >
                                                                <Trash2 size={14} strokeWidth={2.2} />
                                                                삭제
                                                            </button>
                                                        </div>
                                                    ) : null}
                                                </div>
                                            );
                                        })
                                    )}
                                </div>
                            </div>
                        </aside>
                    </div>

                    <div className="flex min-w-0 flex-1 flex-col gap-3">
                        <div className="px-1">
                            <div className="relative flex items-start gap-3">
                                <textarea
                                    value={displayedTitle}
                                    onChange={(e) => setTitle(e.target.value)}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter') e.preventDefault();
                                    }}
                                    placeholder="제목을 입력하세요"
                                    readOnly={activeTab !== 'edit'}
                                    rows={1}
                                    className="min-w-0 flex-1 resize-none overflow-hidden bg-transparent text-xl font-extrabold leading-7 text-text-primary outline-none transition-all placeholder:text-text-secondary/20 focus:placeholder:text-text-secondary/10"
                                />

                                <div className="relative shrink-0">
                                    <button
                                        type="button"
                                        onClick={(event) => {
                                            event.stopPropagation();
                                            setIsTitleMenuOpen((current) => !current);
                                            setOpenTilMenuId(null);
                                        }}
                                        disabled={deleteMutation.isPending || !selectedTil}
                                        title="TIL actions"
                                        aria-label="TIL actions"
                                        aria-expanded={isTitleMenuOpen}
                                        className={`flex h-9 w-9 items-center justify-center rounded-xl transition-colors disabled:cursor-not-allowed disabled:opacity-35 ${
                                            isTitleMenuOpen
                                                ? 'text-text-secondary'
                                                : 'text-text-secondary hover:text-text-primary'
                                        }`}
                                    >
                                        <MoreHorizontal size={18} strokeWidth={2.4} />
                                    </button>

                                    {isTitleMenuOpen ? (
                                        <div
                                            className="absolute right-0 top-10 z-30 w-40 overflow-hidden rounded-xl border border-text-secondary/12 glass-popover bg-surface-lowest/82 p-1.5 backdrop-blur-xl"
                                            onPointerDown={(event) => event.stopPropagation()}
                                        >
                                        <button
                                            type="button"
                                            onClick={(event) => {
                                                event.stopPropagation();
                                                requestDeleteTil();
                                            }}
                                            disabled={deleteMutation.isPending || !selectedTil}
                                            className="flex h-9 w-full items-center gap-2.5 rounded-lg px-2.5 text-sm font-semibold text-text-secondary transition-colors hover:bg-red-500/12 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-40"
                                        >
                                            <Trash2 size={14} strokeWidth={2.2} />
                                            삭제
                                        </button>
                                        </div>
                                    ) : null}
                                </div>
                            </div>
                        </div>

                        <div className={`flex flex-col overflow-hidden rounded-2xl border border-text-secondary/5 bg-surface-lowest ${
                            activeTab === 'edit'
                                ? 'min-h-[760px] lg:min-h-[820px]'
                                : 'min-h-[500px]'
                        }`}>
                            <TILEditor
                                activeTab={activeTab}
                                selectedDate={selectedDate}
                                title={title}
                                setTitle={setTitle}
                                draft={draft}
                                selectedTil={selectedTil}
                                isTilLoading={tilQuery.isPending}
                                generateMutation={generateMutation}
                                updateMutation={updateMutation}
                                commitMutation={commitMutation}
                                generationStatusQuery={generationStatusQuery}
                                commitStatusQuery={commitStatusQuery}
                                generationTone={generationTone}
                                commitTone={commitTone}
                                generationMessage={generationMessage}
                                commitMessage={commitMessage}
                            />
                        </div>

                    </div>
                </div>
            </div>

            <aside className="order-2 flex min-h-0 w-full shrink-0 border-t border-text-secondary/10 bg-transparent lg:order-2 lg:w-[392px] lg:border-l lg:border-t-0 lg:pl-8 lg:pt-12">
                <CollectedDataPanel
                    sourcesQuery={sourcesQuery}
                    recallCardsQuery={recallCardsQuery}
                    selectedTil={selectedTil}
                />
            </aside>
            </div>

            {pendingDeleteTilId ? (
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center bg-scrim px-4 backdrop-blur-sm"
                    onPointerDown={() => {
                        if (!deleteMutation.isPending) setPendingDeleteTilId(null);
                    }}
                >
                    <div
                        className="w-full max-w-[360px] rounded-2xl border border-text-secondary/12 glass-popover bg-surface-lowest/82 p-5 backdrop-blur-2xl"
                        onPointerDown={(event) => event.stopPropagation()}
                    >
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex items-start gap-3">
                                <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-error/12 text-error">
                                    <AlertTriangle size={18} strokeWidth={2.2} />
                                </span>
                                <div>
                                    <h2 className="text-base font-bold text-text-primary">TIL을 삭제할까요?</h2>
                                    <p className="mt-2 text-sm leading-relaxed text-text-secondary">
                                        삭제한 TIL은 되돌릴 수 없습니다.
                                    </p>
                                </div>
                            </div>
                            <button
                                type="button"
                                onClick={() => setPendingDeleteTilId(null)}
                                disabled={deleteMutation.isPending}
                                className="rounded-full p-1 text-text-secondary transition-colors hover:bg-text-primary/8 hover:text-text-primary disabled:cursor-not-allowed disabled:opacity-40"
                                aria-label="닫기"
                            >
                                <X size={17} />
                            </button>
                        </div>

                        <div className="mt-6 flex justify-end gap-2">
                            <button
                                type="button"
                                onClick={() => setPendingDeleteTilId(null)}
                                disabled={deleteMutation.isPending}
                                className="h-9 rounded-lg px-4 text-sm font-semibold text-text-secondary transition-colors hover:bg-text-primary/8 hover:text-text-primary disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                취소
                            </button>
                            <button
                                type="button"
                                onClick={confirmDeleteTil}
                                disabled={deleteMutation.isPending}
                                className="flex h-9 items-center gap-2 rounded-lg bg-error px-4 text-sm font-bold text-text-primary transition-colors hover:bg-error/90 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                <Trash2 size={14} strokeWidth={2.2} />
                                {deleteMutation.isPending ? '삭제 중...' : '삭제'}
                            </button>
                        </div>
                    </div>
                </div>
            ) : null}
        </section>
    );
}

function formatDateForDisplay(dateString: string) {
    const date = parseDateString(dateString);

    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
    });
}

function formatCalendarMonth(date: Date) {
    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
    });
}

function formatTimeForDisplay(dateString: string) {
    const date = new Date(dateString);

    if (Number.isNaN(date.getTime())) return '';

    return date.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit',
    });
}
function getTilTitle(til: TilResponse, index: number) {
    return til.title?.trim() || `Untitled TIL ${index + 1}`;
}

function parseDateString(dateString: string) {
    const [year, month, day] = dateString.split('-').map(Number);
    return new Date(year, month - 1, day);
}

function startOfMonth(date: Date) {
    return new Date(date.getFullYear(), date.getMonth(), 1);
}

function shiftMonth(date: Date, months: number) {
    return new Date(date.getFullYear(), date.getMonth() + months, 1);
}

function getCalendarDays(monthDate: Date) {
    const firstDay = startOfMonth(monthDate);
    const start = new Date(firstDay);
    start.setDate(firstDay.getDate() - firstDay.getDay());

    return Array.from({ length: 42 }, (_, index) => {
        const date = new Date(start);
        date.setDate(start.getDate() + index);
        return date;
    });
}

function shiftDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function getTodayDate() {
    return shiftDate(new Date());
}

function isValidDateParam(value: string | null): value is string {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
    const date = parseDateString(value);
    return !Number.isNaN(date.getTime()) && shiftDate(date) === value;
}
