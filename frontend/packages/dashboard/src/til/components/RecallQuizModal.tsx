import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, HelpCircle, Loader2, RotateCcw, Sparkles, X } from 'lucide-react';
import { getResolvedTheme, type ThemeMode } from '@san/ui';
import type { RecallQuizResponse, RecallQuizType } from '@san/shared';
import { useRecallQuizSubmitMutation } from '../hooks/useTilMutations';
import { tilKeys } from '../hooks/useTilQueries';

const QUIZ_MODAL_TITLE = 'Recall 퀴즈';
const QUIZ_CLOSE_LABEL = '닫기';
const QUIZ_GENERATING_LABEL = 'AI가 퀴즈를 생성하고 있어요.';
const REVIEW_NO_QUIZ_LABEL = '복습할 퀴즈가 없습니다.';
const QUIZ_SELF_JUDGE_LABEL = '정답 확인';

interface RecallQuizModalProps {
  onClose: () => void;
  quizzes: RecallQuizResponse[];
  targetDate: string;
  tilTitle: string | null;
  quizType: RecallQuizType;
  onQuizTypeChange: (type: RecallQuizType) => void;
  isGenerating: boolean;
}

export function RecallQuizModal({
  onClose,
  quizzes,
  targetDate,
  tilTitle,
  quizType,
  onQuizTypeChange,
  isGenerating,
}: RecallQuizModalProps) {
  const solvedCount = useMemo(() => quizzes.filter((quiz) => quiz.solved).length, [quizzes]);
  const progress = quizzes.length > 0 ? (solvedCount / quizzes.length) * 100 : 0;
  const formattedDate = useMemo(() => formatDateLabel(targetDate), [targetDate]);
  const [theme, setTheme] = useState<ThemeMode>(() => getResolvedTheme());

  useEffect(() => {
    const root = document.documentElement;
    const syncTheme = () => {
      setTheme(root.dataset.theme === 'light' ? 'light' : 'dark');
    };

    syncTheme();

    const observer = new MutationObserver(syncTheme);
    observer.observe(root, { attributes: true, attributeFilter: ['data-theme'] });

    return () => observer.disconnect();
  }, []);

  const contrastTextColor = theme === 'light' ? '#ffffff' : '#000000';

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-scrim/80 px-4 backdrop-blur-md"
      onPointerDown={onClose}
    >
      <section
        role="dialog"
        aria-modal="true"
        className="flex h-[95vh] w-full max-w-[800px] flex-col overflow-hidden rounded-[32px] bg-surface-lowest/92 text-text-primary shadow-2xl transition-all"
        onPointerDown={(event) => event.stopPropagation()}
      >
        <div className="mx-auto flex h-full w-full max-w-[640px] min-w-0 flex-col px-6 py-6 md:px-7 md:py-7">
          <button
            type="button"
            onClick={onClose}
            aria-label={QUIZ_CLOSE_LABEL}
            className="ml-auto flex h-10 w-10 items-center justify-center rounded-full bg-text-primary/[0.04] text-text-secondary transition hover:bg-action-accent hover:text-forest-bg"
          >
            <X size={18} />
          </button>

          <header className="shrink-0 pt-2">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="text-[11px] font-bold uppercase tracking-[0.22em] text-text-secondary/55">
                  {QUIZ_MODAL_TITLE}
                </p>
                <h2 className="mt-1 text-2xl font-extrabold tracking-tight text-text-primary">
                  {tilTitle || QUIZ_MODAL_TITLE}
                </h2>
              </div>

              <div className="shrink-0 text-right">
                <p className="text-sm font-bold text-text-primary">{formattedDate}</p>
                <p className="mt-1 text-[11px] font-bold text-action-accent">
                  오늘 복습 {quizzes.length}개
                </p>
              </div>
            </div>

            <div className="mt-5">
              <div className="h-1.5 overflow-hidden rounded-full bg-text-primary/[0.04]">
                <div
                  className="h-full rounded-full bg-action-accent transition-all duration-500"
                  style={{ width: `${progress}%` }}
                />
              </div>
              <div className="mt-2 flex items-center justify-between text-[10px] font-bold uppercase tracking-[0.18em] text-text-secondary/40">
                <span>진행 상황</span>
                <span>{Math.round(progress)}% completed</span>
              </div>
            </div>

            <div className="mt-5 flex items-center">
              <div className="inline-flex rounded-lg border border-text-secondary/10 bg-text-primary/[0.035] p-1">
                <QuizTypeButton
                  active={quizType === 'OX'}
                  label="OX Quiz"
                  contrastTextColor={contrastTextColor}
                  onClick={() => onQuizTypeChange('OX')}
                />
                <QuizTypeButton
                  active={quizType === 'SHORT_ANSWER'}
                  label="Short Answer"
                  contrastTextColor={contrastTextColor}
                  onClick={() => onQuizTypeChange('SHORT_ANSWER')}
                />
              </div>
            </div>
          </header>

          <div className="no-scrollbar min-h-0 flex-1 overflow-y-auto py-6">
            {isGenerating ? (
              <div className="flex min-h-[320px] flex-col items-center justify-center gap-4 py-10 text-center">
                <div className="relative">
                  <Loader2 size={42} className="animate-spin text-action-accent/30" />
                  <Sparkles size={16} className="absolute -right-1 -top-1 text-action-accent" />
                </div>
                <p className="text-sm font-medium text-text-secondary/80">{QUIZ_GENERATING_LABEL}</p>
              </div>
            ) : quizzes.length > 0 ? (
              <div className="mx-auto flex w-full max-w-[580px] flex-col gap-4 pb-4">
                {quizzes.map((quiz, index) => (
                  <QuizCard
                    key={`${quiz.quizId}:${quiz.solved}:${quiz.submittedAnswer ?? ''}`}
                    quiz={quiz}
                    index={index}
                    targetDate={targetDate}
                    quizType={quizType}
                    contrastTextColor={contrastTextColor}
                  />
                ))}
              </div>
            ) : (
              <div className="flex min-h-[320px] flex-col items-center justify-center gap-3 py-10 text-center">
                <HelpCircle size={42} className="text-text-secondary/35" />
                <p className="text-sm font-medium text-text-secondary/70">{REVIEW_NO_QUIZ_LABEL}</p>
              </div>
            )}
          </div>

          <footer className="shrink-0 border-t border-text-secondary/5 pt-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-center gap-2 rounded-lg border border-text-secondary/10 bg-text-primary/[0.03] px-3 py-2 text-[11px] font-medium text-text-secondary/75">
                <CheckCircle2 size={14} className="text-action-accent" />
                <span>
                  {quizType === 'OX'
                    ? '답을 누른 뒤 다시 누르면 수정할 수 있어요.'
                    : '입력창을 다시 눌러 재도전할 수 있어요.'}
                </span>
              </div>

              <button
                type="button"
                onClick={onClose}
                className="inline-flex h-10 min-w-[112px] items-center justify-center rounded-leaf bg-action-accent px-5 text-[12px] font-extrabold transition hover:brightness-110 active:scale-95"
                style={{ color: contrastTextColor }}
              >
                {QUIZ_CLOSE_LABEL}
              </button>
            </div>
          </footer>
        </div>
      </section>
    </div>
  );
}

function QuizCard({
  quiz,
  index,
  targetDate,
  quizType,
  contrastTextColor,
}: {
  quiz: RecallQuizResponse;
  index: number;
  targetDate: string;
  quizType: RecallQuizType;
  contrastTextColor: string;
}) {
  const queryClient = useQueryClient();
  const [answer, setAnswer] = useState(quiz.submittedAnswer ?? '');
  const [isRevealed, setIsRevealed] = useState(quiz.solved);
  const isShortAnswer = quizType === 'SHORT_ANSWER';

  const submitMutation = useRecallQuizSubmitMutation({
    onSuccess: () => {
      setIsRevealed(true);
      void queryClient.invalidateQueries({
        queryKey: tilKeys.recallQuizzes(targetDate, quizType),
      });
    },
  });

  const handleSubmit = (submittedAnswer: string) => {
    if (submitMutation.isPending) return;

    setAnswer(submittedAnswer);
    submitMutation.mutate({
      quizId: quiz.quizId,
      answer: submittedAnswer,
    });
  };

  const handleRetry = () => {
    setIsRevealed(false);
    setAnswer('');
  };

  return (
    <article className="group relative overflow-hidden rounded-tl-[32px] rounded-br-[32px] rounded-tr-xl rounded-bl-xl border border-text-secondary/8 bg-surface-container/85 p-4 shadow-sm transition hover:border-text-secondary/12">
      <div className="absolute -right-10 -top-10 h-24 w-24 rounded-full bg-action-accent/5 blur-3xl transition group-hover:bg-action-accent/8" />

      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-action-accent">
            Question {index + 1}
          </p>
          <h3 className="mt-2 text-base font-extrabold leading-7 text-text-primary">{quiz.question}</h3>
        </div>

        {isRevealed ? (
          <button
            type="button"
            onClick={handleRetry}
            className="flex shrink-0 items-center gap-1.5 rounded-lg px-2 py-1 text-[11px] font-bold text-text-secondary/45 transition hover:bg-text-primary/[0.04] hover:text-action-accent"
          >
            <RotateCcw size={12} />
            다시 풀기
          </button>
        ) : null}
      </div>

      <div className="mt-4">
        {isShortAnswer ? (
          <div className="flex items-end gap-3 border-b border-text-secondary/10 pb-2 transition focus-within:border-action-accent/50">
            <input
              type="text"
              value={answer}
              onChange={(event) => setAnswer(event.target.value)}
              onFocus={() => isRevealed && handleRetry()}
              placeholder="정답을 입력하세요"
              className="w-full bg-transparent py-1.5 text-sm text-text-primary outline-none placeholder:text-text-secondary/25"
            />
            {!isRevealed ? (
              <button
                type="button"
                onClick={() => handleSubmit(answer)}
                disabled={!answer.trim() || submitMutation.isPending}
                className="inline-flex h-9 shrink-0 items-center justify-center rounded-leaf bg-action-accent px-4 text-[12px] font-extrabold transition hover:brightness-110 active:scale-95 disabled:cursor-not-allowed disabled:opacity-35"
                style={{ color: contrastTextColor }}
              >
                {submitMutation.isPending ? <Loader2 size={18} className="animate-spin" /> : QUIZ_SELF_JUDGE_LABEL}
              </button>
            ) : null}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-5 sm:gap-6">
            {(['O', 'X'] as const).map((option) => {
              const isMyAnswer = answer === option;
              const isCorrectChoice = isRevealed && isMyAnswer && quiz.correct === true;
              const isWrongChoice = isRevealed && isMyAnswer && quiz.correct === false;
              return (
                <button
                  key={option}
                  type="button"
                  onClick={() => {
                    if (isRevealed) {
                      if (isMyAnswer) handleRetry();
                      return;
                    }

                    handleSubmit(option);
                  }}
                  className={`flex h-11 items-center justify-center rounded-tl-[24px] rounded-br-[24px] rounded-tr-md rounded-bl-md border text-sm font-bold transition ${
                    isCorrectChoice
                      ? 'border-action-accent/50 bg-action-accent/12'
                      : isWrongChoice
                        ? 'border-red-400/40 bg-red-400/10'
                        : isMyAnswer && !isRevealed
                          ? 'border-text-primary/25 bg-text-primary/6'
                          : 'border-text-secondary/10 bg-surface-highest/50 hover:border-action-accent/35'
                  }`}
                >
                  {option}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {isRevealed && quiz.explanation ? (
        <div className="mt-4 rounded-xl border border-action-accent/10 bg-surface-lowest/70 p-4">
          <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-action-accent/85">해설</p>
          <p className="mt-1.5 text-sm leading-6 text-text-secondary/80">{quiz.explanation}</p>
        </div>
      ) : null}
    </article>
  );
}

function QuizTypeButton({
  active,
  label,
  contrastTextColor,
  onClick,
}: {
  active: boolean;
  label: string;
  contrastTextColor: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-md px-4 py-2 text-[11px] font-bold transition ${
        active ? 'bg-action-accent shadow-sm' : 'text-text-secondary/65 hover:text-text-primary'
      }`}
      style={active ? { color: contrastTextColor } : undefined}
    >
      {label}
    </button>
  );
}

function formatDateLabel(value: string) {
  const date = parseDateString(value);
  if (Number.isNaN(date.getTime())) return '';

  return date.toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function parseDateString(value: string) {
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}
