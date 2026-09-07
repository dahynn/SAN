import { useCallback, useEffect, useMemo, useState } from 'react';

const PUBLIC_STORAGE_KEY = 'san:onboarding-tour-public-completed';
const AUTHENTICATED_STORAGE_KEY = 'san:onboarding-tour-authenticated-completed';
const SPOTLIGHT_PADDING = 8;
const TOOLTIP_WIDTH = 304;
const TOOLTIP_ESTIMATED_HEIGHT = 176;
const TOOLTIP_GAP = 18;
const VIEWPORT_MARGIN = 12;
const TEXT_TAB_MIN_WIDTH = 92;
const TEXT_TAB_MIN_HEIGHT = 40;

interface OnboardingTourProps {
  isAuthenticated: boolean;
  hasPendingScrap: boolean;
  canOpenSimilarTab: boolean;
}

interface TourStep {
  id: string;
  targetId: string;
  title: string;
  body: string;
}

interface TargetRect {
  top: number;
  left: number;
  width: number;
  height: number;
  centerX: number;
  borderRadius: string;
}

function getStorageValue(key: string): Promise<boolean> {
  return new Promise((resolve) => {
    chrome.storage.local.get(key, (items) => {
      resolve(Boolean(items[key]));
    });
  });
}

function setStorageValue(key: string, value: boolean): Promise<void> {
  return new Promise((resolve) => {
    chrome.storage.local.set({ [key]: value }, () => resolve());
  });
}

function getTargetElement(targetId: string) {
  return document.querySelector<HTMLElement>(`[data-tour-id="${targetId}"]`);
}

function isKnowledgeTabTarget(targetId: string) {
  return targetId === 'knowledge-recent-tab' || targetId === 'knowledge-similar-tab';
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

export function OnboardingTour({
  isAuthenticated,
  hasPendingScrap,
  canOpenSimilarTab,
}: OnboardingTourProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [targetRect, setTargetRect] = useState<TargetRect | null>(null);

  const storageKey = isAuthenticated ? AUTHENTICATED_STORAGE_KEY : PUBLIC_STORAGE_KEY;

  const steps = useMemo<TourStep[]>(() => {
    if (isAuthenticated) {
      const authenticatedSteps: TourStep[] = [
        {
          id: 'recent',
          targetId: 'knowledge-recent-tab',
          title: '최근 지식 모아보기',
          body: '로그인하면 저장한 지식 카드가 최신순으로 이곳에 쌓여요.',
        },
      ];

      if (canOpenSimilarTab) {
        authenticatedSteps.push({
          id: 'similar',
          targetId: 'knowledge-similar-tab',
          title: '유사 지식 확인하기',
          body: '새 지식을 저장한 뒤에는 비슷한 카드들을 여기에서 비교해 볼 수 있어요.',
        });
      }

      authenticatedSteps.push({
        id: 'search',
        targetId: 'knowledge-search',
        title: '저장한 지식 검색하기',
        body: '나중에 떠오른 키워드를 입력하면 저장해 둔 카드들을 바로 찾아볼 수 있어요.',
      });

      return authenticatedSteps;
    }

    const publicSteps: TourStep[] = [
      {
        id: 'capture',
        targetId: 'capture-drop-zone',
        title: '먼저 여기에 모아보세요',
        body: '페이지에서 텍스트, 이미지, 링크를 끌어오거나 박스를 눌러 직접 입력할 수 있어요.',
      },
    ];

    if (hasPendingScrap) {
      publicSteps.push({
        id: 'save',
        targetId: 'capture-save-button',
        title: 'Save로 지식 카드 만들기',
        body: '캡처한 내용이 준비되면 이 버튼으로 저장해요. 로그인하면 내 지식과 연관된 정보까지 이어서 볼 수 있어요.',
      });
    }

    publicSteps.push(
      {
        id: 'dashboard',
        targetId: 'dashboard-button',
        title: '대시보드로 넓게 보기',
        body: '모아 둔 지식과 카드 흐름을 더 큰 화면에서 보고 싶을 때 여기를 눌러요.',
      },
      {
        id: 'profile',
        targetId: 'profile-button',
        title: '계정 관리는 여기서',
        body: '회원가입, 로그인, 로그아웃은 여기서 할 수 있어요.',
      },
      {
        id: 'theme',
        targetId: 'theme-toggle',
        title: '눈에 편한 테마로 바꾸기',
        body: '밝은 모드와 어두운 모드를 오가며 사이드패널을 편하게 맞출 수 있어요.',
      },
    );

    return publicSteps;
  }, [canOpenSimilarTab, hasPendingScrap, isAuthenticated]);

  const currentStep = steps[currentIndex];

  const completeTour = useCallback(async () => {
    setIsOpen(false);
    await setStorageValue(storageKey, true);
  }, [storageKey]);

  const moveToVisibleStep = useCallback((startIndex: number) => {
    if (steps.length === 0) return;

    for (let index = startIndex; index < steps.length; index += 1) {
      const element = getTargetElement(steps[index].targetId);
      if (element) {
        if (index !== currentIndex) {
          setCurrentIndex(index);
        }
        return;
      }
    }

    void completeTour();
  }, [completeTour, currentIndex, steps]);

  const updateTargetRect = useCallback(() => {
    if (!currentStep) return;

    const element = getTargetElement(currentStep.targetId);
    if (!element) {
      moveToVisibleStep(currentIndex + 1);
      return;
    }

    const rect = element.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const isKnowledgeTab = isKnowledgeTabTarget(currentStep.targetId);

    if (isKnowledgeTab) {
      const width = Math.max(rect.width + SPOTLIGHT_PADDING * 2, TEXT_TAB_MIN_WIDTH);
      const height = Math.max(rect.height + SPOTLIGHT_PADDING * 2, TEXT_TAB_MIN_HEIGHT);

      setTargetRect({
        top: centerY - height / 2,
        left: centerX - width / 2,
        width,
        height,
        centerX,
        borderRadius: '9999px',
      });
      return;
    }

    const isCompactTarget =
      rect.width <= 72
      && rect.height <= 72
      && Math.abs(rect.width - rect.height) <= 18;

    if (isCompactTarget) {
      const size = Math.max(rect.width, rect.height) + SPOTLIGHT_PADDING * 2;

      setTargetRect({
        top: centerY - size / 2,
        left: centerX - size / 2,
        width: size,
        height: size,
        centerX,
        borderRadius: '9999px',
      });
      return;
    }

    const paddedLeft = rect.left - SPOTLIGHT_PADDING;
    const paddedRight = rect.right + SPOTLIGHT_PADDING;
    const paddedTop = rect.top - SPOTLIGHT_PADDING;
    const paddedBottom = rect.bottom + SPOTLIGHT_PADDING;
    const left = Math.max(paddedLeft, VIEWPORT_MARGIN);
    const right = Math.min(paddedRight, window.innerWidth - VIEWPORT_MARGIN);
    const top = Math.max(paddedTop, VIEWPORT_MARGIN);
    const bottom = Math.min(paddedBottom, window.innerHeight - VIEWPORT_MARGIN);

    setTargetRect({
      top,
      left,
      width: Math.max(right - left, 0),
      height: Math.max(bottom - top, 0),
      centerX,
      borderRadius: '22px',
    });
  }, [currentIndex, currentStep, moveToVisibleStep]);

  useEffect(() => {
    let ignore = false;

    getStorageValue(storageKey)
      .then((hasCompleted) => {
        if (!ignore && !hasCompleted) {
          setCurrentIndex(0);
          setTargetRect(null);
          setIsOpen(true);
          return;
        }

        if (!ignore) {
          setIsOpen(false);
        }
      })
      .catch(() => undefined);

    return () => {
      ignore = true;
    };
  }, [storageKey]);

  useEffect(() => {
    if (!isOpen || currentIndex < steps.length) return;
    void completeTour();
  }, [completeTour, currentIndex, isOpen, steps.length]);

  useEffect(() => {
    if (!isOpen || !currentStep) return;

    setTargetRect(null);
    const element = getTargetElement(currentStep.targetId);
    element?.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'auto' });

    const frameId = window.requestAnimationFrame(updateTargetRect);
    const secondFrameId = window.requestAnimationFrame(updateTargetRect);
    const timeoutId = window.setTimeout(updateTargetRect, 80);
    const observer = element ? new ResizeObserver(updateTargetRect) : null;
    const handleUpdate = () => updateTargetRect();

    if (element && observer) {
      observer.observe(element);
    }
    window.addEventListener('resize', handleUpdate);
    window.addEventListener('scroll', handleUpdate, true);

    return () => {
      window.cancelAnimationFrame(frameId);
      window.cancelAnimationFrame(secondFrameId);
      window.clearTimeout(timeoutId);
      observer?.disconnect();
      window.removeEventListener('resize', handleUpdate);
      window.removeEventListener('scroll', handleUpdate, true);
    };
  }, [currentStep, isOpen, updateTargetRect]);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        void completeTour();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [completeTour, isOpen]);

  if (!isOpen || !currentStep || !targetRect) {
    return null;
  }

  const isLastStep = currentIndex >= steps.length - 1;
  const tooltipTop = targetRect.top + targetRect.height + TOOLTIP_GAP;
  const shouldPlaceAbove = tooltipTop + TOOLTIP_ESTIMATED_HEIGHT > window.innerHeight;
  const top = shouldPlaceAbove
    ? Math.max(VIEWPORT_MARGIN, targetRect.top - TOOLTIP_ESTIMATED_HEIGHT - TOOLTIP_GAP)
    : tooltipTop;
  const tooltipWidth = Math.min(TOOLTIP_WIDTH, window.innerWidth - VIEWPORT_MARGIN * 2);
  const maxTooltipLeft = Math.max(VIEWPORT_MARGIN, window.innerWidth - tooltipWidth - VIEWPORT_MARGIN);
  const left = clamp(
    targetRect.centerX - tooltipWidth / 2,
    VIEWPORT_MARGIN,
    maxTooltipLeft,
  );
  const arrowCenter = clamp(
    targetRect.centerX - left,
    22,
    tooltipWidth - 22,
  );

  return (
    <div className="pointer-events-none fixed inset-0 z-50">
      <div
        className="pointer-events-none absolute rounded-[22px] border border-action-accent/80 bg-action-accent/[0.07] shadow-[0_0_0_9999px_rgba(7,10,8,0.56),0_0_0_4px_rgba(134,226,151,0.12),0_18px_54px_rgba(87,212,121,0.2),inset_0_1px_0_rgba(255,255,255,0.28)] ring-1 ring-white/25 transition-all duration-200"
        style={{
          top: targetRect.top,
          left: targetRect.left,
          width: targetRect.width,
          height: targetRect.height,
          borderRadius: targetRect.borderRadius,
        }}
      />

      <section
        role="dialog"
        aria-live="polite"
        aria-label="SAN quick tour"
        className="pointer-events-auto absolute rounded-2xl border border-white/20 glass-popover bg-surface-lowest/92 p-4 text-text-primary shadow-[0_20px_54px_rgba(0,0,0,0.36),inset_0_1px_0_rgba(255,255,255,0.18)] backdrop-blur-2xl"
        style={{ top, left, width: tooltipWidth }}
      >
        <span className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-action-accent/70 to-transparent" />
        <span
          className={[
            'absolute h-3 w-3 bg-surface-lowest/92',
            shouldPlaceAbove
              ? '-bottom-1.5 border-b border-r border-white/20'
              : '-top-1.5 border-l border-t border-white/20',
          ].join(' ')}
          style={{ left: arrowCenter, transform: 'translateX(-50%) rotate(45deg)' }}
        />

        <div className="mb-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-1.5">
            {steps.map((step, index) => (
              <span
                key={step.id}
                className={[
                  'h-1.5 rounded-full transition-all',
                  index === currentIndex
                    ? 'w-7 bg-action-accent shadow-[0_0_12px_rgba(126,220,145,0.45)]'
                    : 'w-1.5 bg-text-secondary/25',
                ].join(' ')}
              />
            ))}
          </div>
          <span className="rounded-full border border-text-secondary/10 bg-surface-container/55 px-2 py-0.5 text-[10px] font-semibold text-text-secondary">
            {currentIndex + 1} / {steps.length}
          </span>
        </div>

        <h2 className="text-[15px] font-bold leading-6 text-text-primary">{currentStep.title}</h2>
        <p className="mt-2 text-xs leading-5 text-text-secondary/90">{currentStep.body}</p>

        <div className="mt-4 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={() => void completeTour()}
            className="h-8 rounded-full px-3 text-xs font-semibold text-text-secondary transition hover:bg-surface-container/70 hover:text-text-primary"
          >
            건너뛰기
          </button>

          <div className="flex items-center gap-2">
            {currentIndex > 0 && (
              <button
                type="button"
                onClick={() => setCurrentIndex((index) => Math.max(index - 1, 0))}
                className="h-8 rounded-full border border-text-secondary/15 bg-surface-container/35 px-3 text-xs font-semibold text-text-secondary transition hover:bg-surface-container/70 hover:text-text-primary"
              >
                이전
              </button>
            )}
            <button
              type="button"
              onClick={() => {
                if (isLastStep) {
                  void completeTour();
                  return;
                }
                moveToVisibleStep(currentIndex + 1);
              }}
              className="h-8 rounded-full border border-action-accent/35 bg-action-accent px-3.5 text-xs font-bold text-text-on-accent shadow-[0_8px_20px_rgba(87,212,121,0.2)] transition hover:bg-action-accent/90"
            >
              {isLastStep ? '완료' : '다음'}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
