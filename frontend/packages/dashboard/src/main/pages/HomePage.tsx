import { ArchiveSection } from '../components/archive/ArchiveSection';
import { GraphSection } from '../components/graph/GraphSection';
import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, CheckCircle2, ExternalLink, GitBranch, HelpCircle, Star, X } from 'lucide-react';
import { authTokenStorage, githubApi } from '../../api/client';
import { getExtensionInstallUrl } from '../utils/extensionInstallUrl';

const DASHBOARD_TUTORIAL_STORAGE_KEY_PREFIX = 'san-dashboard-tutorial-dismissed';

export function HomePage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [showTutorial, setShowTutorial] = useState(false);
  const [isGithubLinked, setIsGithubLinked] = useState(false);
  const notice = typeof location.state === 'object'
    && location.state
    && 'notice' in location.state
    && typeof location.state.notice === 'string'
    ? location.state.notice
    : null;
  const [tutorialStorageKey, setTutorialStorageKey] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    resolveDashboardTutorialStorageKey().then((storageKey) => {
      if (ignore) return;
      setTutorialStorageKey(storageKey);
      setShowTutorial(Boolean(storageKey) && window.localStorage.getItem(storageKey as string) !== 'true');

      if (storageKey) {
        githubApi.getLinkStatus()
          .then((status) => {
            if (!ignore) setIsGithubLinked(status.linked);
          })
          .catch(() => {
            if (!ignore) setIsGithubLinked(false);
          });
      }
    });

    return () => {
      ignore = true;
    };
  }, []);

  const dismissTutorial = () => {
    if (tutorialStorageKey) {
      window.localStorage.setItem(tutorialStorageKey, 'true');
    }
    setShowTutorial(false);
  };

  return (
    <div className="flex w-full min-w-0 flex-col">
      {notice ? (
        <p className="rounded-leaf border border-primary-signal/20 bg-primary-signal/10 px-md py-sm text-body-sm-bold text-primary-signal">
          {notice}
        </p>
      ) : null}

      {showTutorial ? (
        <DashboardTutorial
          isGithubLinked={isGithubLinked}
          onDismiss={dismissTutorial}
          onOpenGithub={() => {
            dismissTutorial();
            navigate(isGithubLinked ? '/profile/stars' : '/settings/integrations');
          }}
        />
      ) : null}

      <GraphSection />

      <div className="relative z-10 -mt-6 rounded-t-[40px] bg-background pt-10">
        <div className="px-6 pb-12">
          <ArchiveSection />
        </div>
      </div>
    </div>
  );
}

async function resolveDashboardTutorialStorageKey() {
  const [username, token] = await Promise.all([
    authTokenStorage.getUsername(),
    authTokenStorage.getToken(),
  ]);

  if (!token) return null;

  const normalizedUsername = username?.trim().toLowerCase();
  if (normalizedUsername) {
    return getDashboardTutorialStorageKey(`username:${normalizedUsername}`);
  }

  const tokenIdentity = getTokenIdentity(token);
  return getDashboardTutorialStorageKey(tokenIdentity ? `token:${tokenIdentity}` : `token-hash:${hashToken(token)}`);
}

function getDashboardTutorialStorageKey(identity: string) {
  return `${DASHBOARD_TUTORIAL_STORAGE_KEY_PREFIX}:${identity}`;
}

function getTokenIdentity(token: string) {
  const [, payload] = token.split('.');
  if (!payload) return null;

  try {
    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
    const paddedPayload = normalizedPayload.padEnd(Math.ceil(normalizedPayload.length / 4) * 4, '=');
    const decodedPayload = JSON.parse(window.atob(paddedPayload)) as Record<string, unknown>;
    const identity = decodedPayload.sub ?? decodedPayload.userId ?? decodedPayload.memberId ?? decodedPayload.email;
    return typeof identity === 'string' || typeof identity === 'number' ? String(identity) : null;
  } catch {
    return null;
  }
}

function hashToken(token: string) {
  let hash = 0;

  for (let index = 0; index < token.length; index += 1) {
    hash = (hash * 31 + token.charCodeAt(index)) >>> 0;
  }

  return hash.toString(36);
}

function DashboardTutorial({
  isGithubLinked,
  onDismiss,
  onOpenGithub,
}: {
  isGithubLinked: boolean;
  onDismiss: () => void;
  onOpenGithub: () => void;
}) {
  const [stepIndex, setStepIndex] = useState(0);
  const extensionInstallUrl = getExtensionInstallUrl();
  const steps = [
    {
      icon: CheckCircle2,
      title: '지식 수집',
      description: '익스텐션으로 바로 저장하고, 무엇을 모을지 막막할 땐 GitHub Star로 지식카드를 수집하세요.',
    },
    {
      icon: HelpCircle,
      title: 'TIL로 복습',
      description: '생성된 TIL을 확인하고, 퀴즈로 이해한 내용을 가볍게 점검해요.',
    },
    {
      icon: GitBranch,
      title: '간편 커밋',
      description: 'GitHub를 미리 연동해두면 생성된 TIL을 커밋 흐름까지 이어갈 수 있어요.',
    },
  ];
  const currentStep = steps[stepIndex];
  const CurrentIcon = currentStep.icon;
  const isLastStep = stepIndex === steps.length - 1;
  const isCollectionStep = stepIndex === 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-scrim/55 px-4 py-6 backdrop-blur-sm">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="dashboard-onboarding-title"
        className="relative w-full max-w-[640px] overflow-hidden rounded-2xl border border-text-secondary/10 bg-surface-container text-text-primary"
      >
        <div className="absolute right-4 top-4 z-10">
          <button
            type="button"
            onClick={onDismiss}
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-text-primary/30 transition hover:bg-text-primary/5 hover:text-text-primary/60"
            aria-label="온보딩 닫기"
          >
            <X size={16} aria-hidden="true" />
          </button>
        </div>

        <div className="px-5 pb-6 pt-7 sm:px-6 sm:pt-8">
          <h2 id="dashboard-onboarding-title" className="text-2xl font-extrabold tracking-tight text-text-primary">
            처음 오셨나요?
          </h2>
          <p className="mt-2 max-w-xl pr-8 text-sm leading-6 text-text-primary/52">
            SAN에서 지식을 모으고, TIL로 복습하고, 커밋까지 이어가는 흐름을 짧게 볼게요.
          </p>

          <div className="mt-6 rounded-xl border border-text-secondary/7 bg-text-primary/[0.025] p-5">
            <div className="flex items-start gap-4">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-text-primary/[0.04] text-text-primary/50">
                <CurrentIcon size={20} strokeWidth={1.8} aria-hidden="true" />
              </span>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-black uppercase tracking-wider text-action-accent/80">
                  {String(stepIndex + 1).padStart(2, '0')} / {String(steps.length).padStart(2, '0')}
                </p>
                <h3 className="mt-2 text-lg font-extrabold text-text-primary">{currentStep.title}</h3>
                <p className="mt-2 text-sm leading-6 text-text-primary/50">{currentStep.description}</p>

                {isCollectionStep ? (
                  <div className="mt-5 grid gap-2 sm:grid-cols-2">
                    {extensionInstallUrl ? (
                      <a
                        href={extensionInstallUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-text-secondary/7 bg-text-primary/[0.035] px-3 text-sm font-bold text-text-primary/58 transition hover:border-action-accent/20 hover:bg-action-accent/8 hover:text-action-accent"
                      >
                        <ExternalLink size={14} aria-hidden="true" />
                        Browser Extension
                      </a>
                    ) : null}
                    <button
                      type="button"
                      onClick={onOpenGithub}
                      className="inline-flex h-11 items-center justify-center gap-2 rounded-md border border-text-secondary/7 bg-text-primary/[0.035] px-3 text-sm font-bold text-text-primary/58 transition hover:border-action-accent/20 hover:bg-action-accent/8 hover:text-action-accent"
                    >
                      <Star size={14} aria-hidden="true" />
                      GitHub Stars
                    </button>
                  </div>
                ) : null}
              </div>
            </div>
          </div>

          <div className="mt-5 flex items-center gap-2">
            {steps.map((step, index) => (
              <span
                key={step.title}
                className={`h-1.5 rounded-full transition-all ${
                  index === stepIndex ? 'w-8 bg-action-accent' : 'w-1.5 bg-text-primary/14'
                }`}
                aria-hidden="true"
              />
            ))}
          </div>
        </div>

        <div className="flex flex-col-reverse gap-2 border-t border-text-secondary/5 bg-text-primary/[0.012] px-5 py-4 sm:flex-row sm:items-center sm:justify-end sm:px-6">
          {stepIndex > 0 ? (
            <button
              type="button"
              onClick={() => setStepIndex((index) => index - 1)}
              className="inline-flex h-10 items-center justify-center rounded-md px-3 text-sm font-bold text-text-primary/45 transition hover:bg-text-primary/5 hover:text-text-primary/70"
            >
              이전
            </button>
          ) : null}
          <button
            type="button"
            onClick={() => {
              if (isLastStep) {
                onDismiss();
                return;
              }
              setStepIndex((index) => index + 1);
            }}
            className="inline-flex h-10 items-center justify-center gap-1.5 rounded-md border border-action-accent/25 bg-action-accent/10 px-4 text-sm font-bold text-action-accent transition hover:bg-action-accent/15 active:scale-[0.98]"
          >
            {isLastStep ? '시작하기' : '다음'}
            <ArrowRight size={14} aria-hidden="true" />
          </button>
        </div>
      </section>
    </div>
  );
}
