import { type FormEvent, type ReactNode, useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Bell, ChevronDown, ExternalLink, Keyboard, Loader2, LogOut, RefreshCw, Shield, Trash2, X } from 'lucide-react';
import { getApiErrorMessage, type AuthSession } from '@san/shared';
import { authApi, authTokenStorage, githubApi, statisticsApi } from '../../api/client';
import {
  getExtensionShortcuts,
  getExtensionTilRecallSettings,
  openExtensionShortcutSettings as requestOpenExtensionShortcutSettings,
  setExtensionTilRecallSettings,
  type ExtensionShortcuts,
  type TilRecallSettings,
} from '../../api/extensionAuth';
import { useToast } from '../../components/shared/toast/toastContext';

const sessionLabel: Record<AuthSession['clientType'], string> = {
  DASHBOARD: '대시보드',
  EXTENSION: '익스텐션',
};

function formatExpiresIn(seconds: number) {
  const days = Math.floor(seconds / 86400);
  if (days > 0) return `${days}일 남음`;

  const hours = Math.floor(seconds / 3600);
  if (hours > 0) return `${hours}시간 남음`;

  const minutes = Math.max(1, Math.floor(seconds / 60));
  return `${minutes}분 남음`;
}

function maskSessionId(sessionId: string) {
  if (sessionId.length <= 13) return sessionId;
  return `${sessionId.slice(0, 8)}...${sessionId.slice(-5)}`;
}

const DEFAULT_TIL_RECALL_SETTINGS: TilRecallSettings = {
  enabled: true,
  time: '07:00',
};

const RECALL_PERIOD_OPTIONS = [
  { value: 'AM', label: '오전' },
  { value: 'PM', label: '오후' },
] as const;

const RECALL_MINUTE_STEP = 5;
const RECALL_HOUR_OPTIONS = Array.from({ length: 12 }, (_, index) => index + 1);
const RECALL_MINUTE_OPTIONS = Array.from({ length: 12 }, (_, index) => index * RECALL_MINUTE_STEP);

type RecallPeriod = (typeof RECALL_PERIOD_OPTIONS)[number]['value'];
type RecallTimeField = 'period' | 'hour' | 'minute';

interface RecallTimeParts {
  period: RecallPeriod;
  hour: number;
  minute: number;
}

function formatRecallTimeLabel(time: string) {
  const [hourText, minuteText] = time.split(':');
  const hour = Number(hourText);
  const period = hour >= 12 ? '오후' : '오전';
  const displayHour = hour % 12 || 12;
  return `${period} ${displayHour}:${minuteText}`;
}

function parseRecallTimeParts(time: string): RecallTimeParts {
  const [hourText, minuteText] = time.split(':');
  const hour = Number(hourText);
  const minute = Number(minuteText);

  return {
    period: hour >= 12 ? 'PM' : 'AM',
    hour: hour % 12 || 12,
    minute: Number.isFinite(minute) ? minute : 0,
  };
}

function createRecallTimeValue(parts: RecallTimeParts) {
  const hour24 = parts.period === 'PM'
    ? (parts.hour % 12) + 12
    : parts.hour % 12;

  return `${String(hour24).padStart(2, '0')}:${String(parts.minute).padStart(2, '0')}`;
}

export function ProfilePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [sessionError, setSessionError] = useState<string | null>(null);
  const [revokeSessionId, setRevokeSessionId] = useState<string | null>(null);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);
  const [withdrawPassword, setWithdrawPassword] = useState('');
  const [withdrawError, setWithdrawError] = useState<string | null>(null);
  const [isWithdrawing, setIsWithdrawing] = useState(false);
  const [recallSettings, setRecallSettings] = useState<TilRecallSettings>(DEFAULT_TIL_RECALL_SETTINGS);
  const [isRecallSettingsLoading, setIsRecallSettingsLoading] = useState(true);
  const [isRecallSettingsSaving, setIsRecallSettingsSaving] = useState(false);
  const [recallSettingsError, setRecallSettingsError] = useState<string | null>(null);
  const [shortcutSettingsMessage, setShortcutSettingsMessage] = useState<string | null>(null);

  const sessionsQuery = useQuery({
    queryKey: ['auth', 'sessions'],
    queryFn: () => authApi.getSessions(),
    staleTime: 0,
  });
  const sessions = sessionsQuery.data?.sessions ?? [];
  const sessionsErrorMessage = sessionsQuery.error
    ? getApiErrorMessage(sessionsQuery.error, '세션 목록을 불러오지 못했습니다.')
    : null;

  const profileQuery = useQuery({
    queryKey: ['auth', 'profile-label'],
    queryFn: async () => {
      const [username, githubStatus] = await Promise.allSettled([
        authTokenStorage.getUsername(),
        githubApi.getLinkStatus(),
      ]);

      if (
        githubStatus.status === 'fulfilled'
        && githubStatus.value.linked
        && githubStatus.value.githubUsername
      ) {
        return { name: githubStatus.value.githubUsername, caption: `github.com/${githubStatus.value.githubUsername}` };
      }

      const localUsername = username.status === 'fulfilled' ? username.value : null;
      return { name: localUsername ?? 'SAN 사용자', caption: localUsername ? '아이디 계정' : '현재 로그인된 계정' };
    },
    staleTime: 1000 * 60,
  });
  const profileLabel = profileQuery.data ?? { name: 'SAN 사용자', caption: '현재 로그인된 계정' };

  const usernameQuery = useQuery({
    queryKey: ['auth', 'current-username'],
    queryFn: () => authTokenStorage.getUsername(),
    staleTime: 0,
  });

  const statisticsQuery = useQuery({
    queryKey: ['statistics', 'overview', usernameQuery.data ?? 'unknown'],
    queryFn: () => statisticsApi.getOverview(),
    enabled: usernameQuery.isSuccess,
    staleTime: 1000 * 60,
  });
  const statistics = statisticsQuery.data;
  const shortcutQuery = useQuery({
    queryKey: ['extension', 'shortcuts'],
    queryFn: getExtensionShortcuts,
    retry: false,
    staleTime: 0,
  });
  const shortcuts = shortcutQuery.data;
  const refetchShortcuts = shortcutQuery.refetch;
  const statisticsItems = [
    {
      label: '지식 카드',
      value: statistics?.totalKnowledgeCardCount,
    },
    {
      label: 'TIL 기록',
      value: statistics?.totalTilCount,
    },
    {
      label: '오늘 생성',
      value: statistics?.todayKnowledgeCardCount,
      accent: true,
    },
  ];

  const clearLocalAuthAndMoveLogin = useCallback(async () => {
    await authTokenStorage.clearToken();
    queryClient.clear();
    navigate('/login', { replace: true });
  }, [navigate, queryClient]);

  useEffect(() => {
    let ignore = false;

    getExtensionTilRecallSettings()
      .then((settings) => {
        if (ignore) return;
        setRecallSettings(settings);
        setRecallSettingsError(null);
      })
      .catch(() => {
        if (ignore) return;
        setRecallSettingsError('익스텐션을 연결하면 알림 설정을 불러올 수 있어요.');
      })
      .finally(() => {
        if (ignore) return;
        setIsRecallSettingsLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    const handleFocus = () => {
      void refetchShortcuts();
    };

    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [refetchShortcuts]);

  const saveRecallSettings = useCallback(async (nextSettings: TilRecallSettings) => {
    const previousSettings = recallSettings;
    setRecallSettings(nextSettings);
    setRecallSettingsError(null);
    setIsRecallSettingsSaving(true);

    try {
      const savedSettings = await setExtensionTilRecallSettings(nextSettings);
      setRecallSettings(savedSettings);
      showToast({
        type: 'success',
        title: '변경됐습니다',
        description: formatRecallTimeLabel(savedSettings.time),
        duration: 2200,
      });
    } catch (error) {
      console.warn('[SAN:recall-settings] failed to save recall settings', error);
      setRecallSettings(previousSettings);
      const message = '알림 설정을 저장하지 못했어요. 익스텐션 연결을 확인해 주세요.';
      setRecallSettingsError(message);
      showToast({
        type: 'error',
        title: '저장 실패',
        description: '익스텐션 연결을 확인해 주세요.',
        duration: 2600,
      });
    } finally {
      setIsRecallSettingsSaving(false);
    }
  }, [recallSettings, showToast]);

  const openShortcutSettings = useCallback(async () => {
    setShortcutSettingsMessage(null);

    try {
      await requestOpenExtensionShortcutSettings();
      void refetchShortcuts();
    } catch (error) {
      console.warn('[SAN:shortcut-settings] failed to open shortcut settings', error);
      await navigator.clipboard?.writeText('chrome://extensions/shortcuts').catch(() => undefined);
      setShortcutSettingsMessage('익스텐션 연결이 안 되어 단축키 설정 주소를 복사했어요.');
    }
  }, [refetchShortcuts]);

  const recallTimeDisabled = !recallSettings.enabled || isRecallSettingsLoading || isRecallSettingsSaving;
  const handleLogout = async () => {
    setIsLoggingOut(true);

    try {
      await authApi.logout();
    } catch (error) {
      console.warn('[SAN:auth] logout request failed', error);
    } finally {
      await clearLocalAuthAndMoveLogin();
      setIsLoggingOut(false);
    }
  };

  const handleRevokeSession = async (session: AuthSession) => {
    setRevokeSessionId(session.sessionId);
    setSessionError(null);

    try {
      await authApi.revokeSession(session.sessionId, session.clientType);
      await sessionsQuery.refetch();
    } catch (error) {
      setSessionError(getApiErrorMessage(error, '세션을 폐기하지 못했습니다.'));
    } finally {
      setRevokeSessionId(null);
    }
  };

  const handleWithdraw = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setWithdrawError(null);

    if (!withdrawPassword) {
      setWithdrawError('비밀번호를 입력해 주세요.');
      return;
    }

    setIsWithdrawing(true);
    try {
      await authApi.withdraw({ password: withdrawPassword });
      await clearLocalAuthAndMoveLogin();
    } catch (error) {
      setWithdrawError(getApiErrorMessage(error, '회원탈퇴에 실패했습니다.'));
    } finally {
      setIsWithdrawing(false);
    }
  };

  return (
    <section className="mx-auto w-full max-w-[720px] py-12 text-text-primary">
      <header className="mb-7 border-b border-text-secondary/8 pb-6">
        <h1 className="bg-gradient-to-r from-text-primary to-text-secondary bg-clip-text text-h1-bold text-transparent">
          마이 프로필
        </h1>
        <p className="mt-2 text-sm text-text-primary/45">계정 통계와 로그인 세션을 관리하세요.</p>
      </header>

      <div className="space-y-6">
        <section className="rounded-lg border border-text-secondary/[0.07] glass-card bg-surface-container/70 p-6 shadow-[0_18px_48px_rgba(0,0,0,0.2)]">
          <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex min-w-0 items-center gap-4">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-text-primary/10 text-lg font-black text-primary-signal">
                {profileLabel.name.slice(0, 1).toUpperCase()}
              </div>
              <div className="min-w-0">
                <h2 className="truncate text-lg font-bold tracking-tight text-text-primary">{profileLabel.name}</h2>
                <p className="mt-1 truncate text-sm text-text-secondary">{profileLabel.caption}</p>
              </div>

            </div>

            <button
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
              className="flex h-9 shrink-0 items-center justify-center gap-2 rounded-md glass-card bg-surface-container/60 px-4 text-xs font-bold text-text-primary/80 transition hover:bg-text-primary/10 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLoggingOut ? <Loader2 size={15} className="animate-spin" /> : <LogOut size={15} />}
              {isLoggingOut ? '로그아웃 중' : '로그아웃'}
            </button>
          </div>

          <div className="mt-6 border-t border-text-secondary/[0.06] pt-5">
            <div className="grid grid-cols-3 gap-4">
              {statisticsItems.map((item) => (
                <div key={item.label} className="min-w-0">
                  <p className="truncate text-[11px] font-bold uppercase tracking-wide text-text-secondary/75">
                    {item.label}
                  </p>
                  <p className={`mt-2 text-xl font-black tabular-nums ${item.accent ? 'text-primary-signal' : 'text-text-primary'}`}>
                    {statisticsQuery.isLoading || statisticsQuery.isError || item.value == null
                      ? '-'
                      : item.value.toLocaleString()}
                  </p>
                </div>
              ))}
            </div>
          </div>

          {statisticsQuery.error && (
            <p className="mt-4 rounded-lg border border-red-500/20 bg-red-500/10 px-4 py-3 text-xs text-red-200">
              {getApiErrorMessage(statisticsQuery.error, '통계를 불러오지 못했습니다.')}
            </p>
          )}
        </section>

        <ProfileSettingsCards
          recallSettings={recallSettings}
          recallTimeDisabled={recallTimeDisabled}
          isRecallSettingsLoading={isRecallSettingsLoading}
          isRecallSettingsSaving={isRecallSettingsSaving}
          recallSettingsError={recallSettingsError}
          saveRecallSettings={saveRecallSettings}
          openShortcutSettings={openShortcutSettings}
          shortcutSettingsMessage={shortcutSettingsMessage}
          shortcuts={shortcuts}
          isShortcutLoading={shortcutQuery.isPending}
          shortcutError={shortcutQuery.isError ? '익스텐션 연결이 필요해요.' : null}
        />

        <section className="border-t border-text-secondary/[0.06] pt-6">
          <div className="mb-4 flex items-center justify-between gap-4">
            <div>
              <h3 className="flex items-center gap-2 text-base font-bold text-text-primary">
                <Shield size={17} className="text-primary-signal" />
                로그인 세션
              </h3>
              <p className="mt-1 text-xs text-text-primary/40">대시보드와 확장 프로그램 세션을 관리합니다.</p>
            </div>
            <button
              type="button"
              onClick={() => void sessionsQuery.refetch()}
              disabled={sessionsQuery.isFetching}
              className="flex h-8 w-8 shrink-0 items-center justify-center text-text-primary/45 transition hover:text-text-primary disabled:cursor-not-allowed disabled:opacity-50"
              aria-label="세션 새로고침"
              title="세션 새로고침"
            >
              <RefreshCw size={14} className={sessionsQuery.isFetching ? 'animate-spin' : undefined} />
            </button>
          </div>

          {(sessionError || sessionsErrorMessage) && (
            <p className="mb-4 rounded-lg border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-200">
              {sessionError || sessionsErrorMessage}
            </p>
          )}

          <div className="no-scrollbar max-h-[360px] space-y-3 overflow-y-auto pr-1">
            {sessionsQuery.isLoading ? (
              <div className="flex h-24 items-center justify-center text-sm text-text-primary/35">
                <Loader2 size={18} className="mr-2 animate-spin" />
                세션을 불러오는 중
              </div>
            ) : sessions.length === 0 ? (
              <div className="rounded-lg border border-text-secondary/5 bg-text-primary/[0.03] p-5 text-sm text-text-primary/40">
                활성 세션이 없습니다.
              </div>
            ) : (
              sessions.map((session) => (
                <div
                  key={`${session.clientType}-${session.sessionId}`}
                  className="flex flex-col gap-4 rounded-lg border border-text-secondary/[0.07] glass-card bg-surface-container/70 px-5 py-4 !shadow-none transition-colors hover:border-text-secondary/[0.12] sm:flex-row sm:items-center sm:justify-between"
                >
                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-text-primary">{sessionLabel[session.clientType]}</span>
                      {session.current && (
                        <span className="rounded-md bg-primary-signal/12 px-2 py-0.5 text-[10px] font-black text-primary-signal">
                          현재
                        </span>
                      )}
                    </div>
                    <p className="mt-1 font-mono text-xs text-text-secondary">{maskSessionId(session.sessionId)}</p>
                    <p className="mt-2 text-xs font-medium text-text-primary/38">만료까지 {formatExpiresIn(session.expiresInSeconds)}</p>
                  </div>

                  {session.current ? (
                    <button
                      type="button"
                      onClick={handleLogout}
                      disabled={isLoggingOut}
                      className="h-8 shrink-0 rounded-md border border-red-500/20 px-3 text-xs font-bold text-red-300 transition hover:bg-red-500/10 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      현재 세션 로그아웃
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => void handleRevokeSession(session)}
                      disabled={revokeSessionId === session.sessionId}
                      className="h-8 shrink-0 rounded-md glass-card bg-surface-container/60 px-3 text-xs font-bold text-text-primary/70 !shadow-none transition hover:bg-text-primary/10 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                      {revokeSessionId === session.sessionId ? '폐기 중' : '폐기'}
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </section>

        <section className="rounded-lg border border-text-secondary/[0.06] bg-transparent px-3 py-2">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="flex items-center gap-1.5 text-xs font-bold text-red-200/80">
                <AlertTriangle size={13} />
                회원탈퇴
              </h3>
              <p className="mt-0.5 text-[11px] leading-4 text-text-primary/32">
                탈퇴하면 모든 로그인 세션이 만료되고 계정이 비활성화됩니다.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setIsWithdrawOpen(true)}
              className="flex h-7 shrink-0 items-center justify-center gap-1 rounded-md border border-red-400/18 bg-transparent px-2.5 text-[11px] font-bold text-red-200/75 transition hover:border-red-400/60 hover:bg-red-500/15 hover:text-red-200 active:scale-[0.98]"
            >
              <Trash2 size={12} />
              회원탈퇴
            </button>
          </div>
        </section>
      </div>

      {isWithdrawOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-scrim/70 px-4 backdrop-blur-sm">
          <form
            onSubmit={handleWithdraw}
            className="w-full max-w-[360px] rounded-3xl border border-text-secondary/10 glass-card bg-surface-container/80 p-6 shadow-2xl"
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-text-primary">회원탈퇴 확인</h2>
                <p className="mt-2 text-sm leading-relaxed text-text-primary/45">비밀번호를 입력하면 계정 탈퇴가 진행됩니다.</p>
              </div>
              <button
                type="button"
                onClick={() => {
                  setIsWithdrawOpen(false);
                  setWithdrawPassword('');
                  setWithdrawError(null);
                }}
                disabled={isWithdrawing}
                className="rounded-full p-1 text-text-primary/35 transition hover:bg-text-primary/5 hover:text-text-primary"
                aria-label="닫기"
              >
                <X size={18} />
              </button>
            </div>

            <input
              type="password"
              value={withdrawPassword}
              autoComplete="current-password"
              placeholder="비밀번호 입력"
              onChange={(event) => setWithdrawPassword(event.target.value)}
              className="mt-6 h-12 w-full rounded-xl border border-text-secondary/10 glass-card bg-surface-container/70 px-4 text-sm text-text-primary outline-none placeholder:text-text-secondary/25 focus:border-red-300/50"
            />
            <p className="mt-2 min-h-[18px] text-xs text-red-300">{withdrawError || ''}</p>

            <button
              type="submit"
              disabled={isWithdrawing}
              className="mt-4 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-red-400 text-sm font-black text-text-on-accent transition hover:brightness-110 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isWithdrawing && <Loader2 size={16} className="animate-spin" />}
              {isWithdrawing ? '탈퇴 처리 중' : '탈퇴하기'}
            </button>
          </form>
        </div>
      )}
    </section>
  );
}

function ProfileSettingsCards({
  recallSettings,
  recallTimeDisabled,
  isRecallSettingsLoading,
  isRecallSettingsSaving,
  recallSettingsError,
  saveRecallSettings,
  openShortcutSettings,
  shortcutSettingsMessage,
  shortcuts,
  isShortcutLoading,
  shortcutError,
}: {
  recallSettings: TilRecallSettings;
  recallTimeDisabled: boolean;
  isRecallSettingsLoading: boolean;
  isRecallSettingsSaving: boolean;
  recallSettingsError: string | null;
  saveRecallSettings: (settings: TilRecallSettings) => Promise<void>;
  openShortcutSettings: () => Promise<void>;
  shortcutSettingsMessage: string | null;
  shortcuts?: ExtensionShortcuts;
  isShortcutLoading: boolean;
  shortcutError: string | null;
}) {
  const recallTimeParts = parseRecallTimeParts(recallSettings.time);
  const updateRecallTimePart = (nextParts: Partial<RecallTimeParts>) => {
    void saveRecallSettings({
      ...recallSettings,
      time: createRecallTimeValue({ ...recallTimeParts, ...nextParts }),
    });
  };
  const [openRecallTimeField, setOpenRecallTimeField] = useState<RecallTimeField | null>(null);
  const selectedPeriodLabel = RECALL_PERIOD_OPTIONS.find((option) => option.value === recallTimeParts.period)?.label ?? '오전';
  const selectRecallTimePart = (nextParts: Partial<RecallTimeParts>) => {
    updateRecallTimePart(nextParts);
    setOpenRecallTimeField(null);
  };

  return (
    <section className="relative z-20 grid gap-4 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.15fr)]">
      <section className="rounded-lg border border-text-secondary/[0.07] glass-card bg-surface-container/70 p-5">
        <h3 className="flex items-center gap-2 text-sm font-bold text-text-primary">
          <Keyboard size={16} className="text-primary-signal" />
          단축키 설정
        </h3>

        <div className="mt-6 space-y-4">
          <ShortcutRow label="사이드패널 열기" shortcut={formatShortcutLabel(shortcuts?.openSidePanel, isShortcutLoading)} />
          <ShortcutRow label="화면 캡처" shortcut={formatShortcutLabel(shortcuts?.captureImage, isShortcutLoading)} />
        </div>
        {shortcutError ? (
          <p className="mt-4 rounded-md border border-primary-signal/15 bg-primary-signal/8 px-3 py-2 text-xs text-primary-signal/80">
            {shortcutError}
          </p>
        ) : null}

        <div className="mt-7 border-t border-text-secondary/[0.06] pt-6">
          <button
            type="button"
            onClick={() => void openShortcutSettings()}
            className="flex w-full items-center justify-center gap-1.5 text-[11px] font-semibold text-text-primary/45 transition hover:text-primary-signal"
          >
            Chrome에서 단축키 변경하기
            <ExternalLink size={12} />
          </button>
          {shortcutSettingsMessage && (
            <p className="mt-3 rounded-md border border-primary-signal/15 bg-primary-signal/8 px-3 py-2 text-xs text-primary-signal/80">
              {shortcutSettingsMessage}
            </p>
          )}
        </div>
      </section>

      <section className="rounded-lg border border-text-secondary/[0.07] glass-card bg-surface-container/70 p-5">
        <div className="flex items-start justify-between gap-4">
          <h3 className="flex items-center gap-2 text-sm font-bold text-text-primary">
            <Bell size={16} className="text-primary-signal" />
            알림 설정
          </h3>
          <button
            type="button"
            role="switch"
            aria-checked={recallSettings.enabled}
            disabled={isRecallSettingsLoading || isRecallSettingsSaving}
            onClick={() => void saveRecallSettings({ ...recallSettings, enabled: !recallSettings.enabled })}
            className={`relative h-6 w-11 rounded-full transition ${
              recallSettings.enabled ? 'bg-primary-signal' : 'bg-text-primary/15'
            } disabled:cursor-not-allowed disabled:opacity-60`}
          >
            <span
              className={`absolute top-1 h-4 w-4 rounded-full bg-white transition ${
                recallSettings.enabled ? 'left-6' : 'left-1'
              }`}
            />
          </button>
        </div>

        <div className="mt-6 space-y-6">
          <div className="flex flex-col gap-3">
            <div>
              <p className="text-sm font-bold text-text-primary">Recall 리마인더</p>
              <p className="mt-1 text-xs text-text-primary/42">저장한 노트 복습 알림을 받습니다.</p>
            </div>
            <RecallSaveStatus
              isSaving={isRecallSettingsSaving}
              error={recallSettingsError}
            />
          </div>

          <div className="flex flex-col gap-3">
            <div>
              <p className="text-sm font-bold text-text-primary">리마인더 발송 시간</p>
              <p className="mt-1 text-xs text-text-primary/42">리콜이 오면 리포트 수신 시각</p>
            </div>
            <div
              className="flex flex-nowrap items-center gap-2"
              onBlur={(event) => {
                if (!event.currentTarget.contains(event.relatedTarget)) {
                  setOpenRecallTimeField(null);
                }
              }}
            >
              <RecallTimeSelect
                label="오전/오후"
                value={selectedPeriodLabel}
                disabled={recallTimeDisabled}
                isOpen={openRecallTimeField === 'period'}
                onToggle={() => setOpenRecallTimeField((current) => (current === 'period' ? null : 'period'))}
                className="w-[88px]"
                menuClassName="w-full"
              >
                {RECALL_PERIOD_OPTIONS.map((option) => (
                  <RecallTimeOption
                    key={option.value}
                    selected={option.value === recallTimeParts.period}
                    onClick={() => selectRecallTimePart({ period: option.value })}
                  >
                    {option.label}
                  </RecallTimeOption>
                ))}
              </RecallTimeSelect>

              <RecallTimeSelect
                label="시"
                value={`${recallTimeParts.hour}시`}
                disabled={recallTimeDisabled}
                isOpen={openRecallTimeField === 'hour'}
                onToggle={() => setOpenRecallTimeField((current) => (current === 'hour' ? null : 'hour'))}
                className="w-[76px]"
                menuClassName="w-full"
              >
                {RECALL_HOUR_OPTIONS.map((hour) => (
                  <RecallTimeOption
                    key={hour}
                    selected={hour === recallTimeParts.hour}
                    onClick={() => selectRecallTimePart({ hour })}
                  >
                    {hour}
                  </RecallTimeOption>
                ))}
              </RecallTimeSelect>

              <RecallTimeSelect
                label="분"
                value={`${String(recallTimeParts.minute).padStart(2, '0')}분`}
                disabled={recallTimeDisabled}
                isOpen={openRecallTimeField === 'minute'}
                onToggle={() => setOpenRecallTimeField((current) => (current === 'minute' ? null : 'minute'))}
                className="w-[82px]"
                menuClassName="w-full"
              >
                {RECALL_MINUTE_OPTIONS.map((minute) => (
                  <RecallTimeOption
                    key={minute}
                    selected={minute === recallTimeParts.minute}
                    onClick={() => selectRecallTimePart({ minute })}
                  >
                    {String(minute).padStart(2, '0')}
                  </RecallTimeOption>
                ))}
              </RecallTimeSelect>
            </div>
          </div>
        </div>
      </section>
    </section>
  );
}

function RecallSaveStatus({
  isSaving,
  error,
}: {
  isSaving: boolean;
  error: string | null;
}) {
  if (isSaving) {
    return (
      <span className="flex items-center gap-1.5 text-[11px] font-semibold text-primary-signal">
        <Loader2 size={12} className="animate-spin" />
        저장 중
      </span>
    );
  }

  if (error) {
    return <span className="text-[11px] font-semibold text-red-300">저장 실패</span>;
  }

  return null;
}

function RecallTimeSelect({
  label,
  value,
  disabled,
  isOpen,
  onToggle,
  children,
  className,
  menuClassName,
}: {
  label: string;
  value: string;
  disabled: boolean;
  isOpen: boolean;
  onToggle: () => void;
  children: ReactNode;
  className: string;
  menuClassName: string;
}) {
  return (
    <div className={`relative ${className}`}>
      <button
        type="button"
        disabled={disabled}
        onClick={onToggle}
        className="flex h-9 w-full items-center justify-between gap-1 rounded-full border border-primary-signal/32 glass-card bg-surface-container/60 px-3 text-xs font-black text-primary-signal shadow-[0_0_0_1px_rgba(111,255,190,0.03)] outline-none transition hover:border-primary-signal/55 hover:bg-primary-signal/8 focus:border-primary-signal disabled:cursor-not-allowed disabled:border-text-secondary/[0.06] disabled:text-text-primary/35 disabled:opacity-60"
        aria-label={`${label} 선택`}
        aria-expanded={isOpen}
      >
        <span>{value}</span>
        <ChevronDown size={13} className={`shrink-0 transition ${isOpen ? 'rotate-180' : ''}`} aria-hidden="true" />
      </button>

      {isOpen && (
        <div className={`absolute left-0 top-10 z-20 overflow-hidden rounded-md border border-primary-signal/18 glass-card bg-surface-container py-1 shadow-[0_14px_28px_rgba(0,0,0,0.38)] ${menuClassName}`}>
          <div className="max-h-[188px] overflow-y-auto">
            {children}
          </div>
        </div>
      )}
    </div>
  );
}

function RecallTimeOption({
  selected,
  onClick,
  children,
}: {
  selected: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex h-8 w-full items-center px-3 text-left text-xs font-bold transition ${
        selected
          ? 'bg-primary-signal/10 text-primary-signal'
          : 'text-text-primary/58 hover:bg-text-primary/[0.05] hover:text-text-primary'
      }`}
    >
      {children}
    </button>
  );
}

function formatShortcutLabel(shortcut: string | undefined, isLoading: boolean) {
  if (isLoading) return 'Loading';
  if (!shortcut?.trim()) return 'Not set';
  return shortcut.replace(/\+/g, ' + ');
}

function ShortcutRow({ label, shortcut }: { label: string; shortcut: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <span className="min-w-0 text-xs font-semibold text-text-primary/62">{label}</span>
      <kbd className="shrink-0 rounded-full border border-text-secondary/[0.06] bg-text-primary/[0.06] px-2.5 py-1 text-[11px] font-black text-primary-signal">
        {shortcut}
      </kbd>
    </div>
  );
}
