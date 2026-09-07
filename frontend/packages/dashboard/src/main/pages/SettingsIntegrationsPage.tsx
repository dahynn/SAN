import { ArrowRight, ExternalLink, FolderGit2, GitBranch, RefreshCw, Loader2, Search, Link2, TerminalSquare, AlertTriangle, Star } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getApiErrorMessage, type GithubRepository, type TilGithubContributionResponse } from '@san/shared';
import { githubApi, tilApi } from '../../api/client';
import githubSvg from '@ui/assets/icons/github.svg';
import { InlineActionToast } from '../../components/shared/toast/InlineActionToast';
import { GithubContributionGraph } from '../components/github/GithubContributionGraph';
import { rememberGithubLinkAuthFlow } from '../../auth/lib/githubAuthFlow';

const GITHUB_LINK_ERROR_MESSAGE: Record<string, string> = {
  A201: 'GitHub 연동 인증에 실패했습니다. 다시 시도해주세요.',
  A202: 'GitHub 계정이 연동되어 있지 않습니다.',
  A204: '이미 다른 계정에 연결된 GitHub 계정입니다.',
  A205: 'GitHub 로그인 계정은 연동을 해제할 수 없습니다.',
  A206: '현재 연동된 GitHub 계정이 존재합니다.',
};

// Debounce hook
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

function RepositorySkeleton() {
  return (
    <ul className="flex flex-col gap-3">
      {[1, 2, 3, 4].map((key) => (
        <li key={key} className="flex items-center justify-between gap-3 rounded-lg border border-text-secondary/10 bg-surface-low/80 p-4">
          <div className="min-w-0 flex-1 animate-pulse">
            <div className="h-4 w-2/3 rounded bg-text-primary/10"></div>
            <div className="mt-2 h-3 w-1/3 rounded bg-text-primary/5"></div>
          </div>
          <div className="h-8 w-16 shrink-0 rounded-md bg-text-primary/5 animate-pulse"></div>
        </li>
      ))}
    </ul>
  );
}

function formatDateParam(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getRecentSixMonthRange() {
  const to = new Date();
  const from = new Date(to);
  from.setMonth(from.getMonth() - 6);

  return {
    from: formatDateParam(from),
    to: formatDateParam(to),
  };
}

export function SettingsIntegrationsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const githubLinked = searchParams.get('githubLinked') === 'true';
  const githubError = searchParams.get('githubError');
  
  const [isGithubLinked, setIsGithubLinked] = useState(githubLinked);
  const [connectedRepositories, setConnectedRepositories] = useState<GithubRepository[]>([]);
  const [availableRepositories, setAvailableRepositories] = useState<GithubRepository[]>([]);
  
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<{ target: string; message: string } | null>(null);
  
  const [isLoadingConnected, setIsLoadingConnected] = useState(true);
  const [isLoadingAvailable, setIsLoadingAvailable] = useState(false);
  const [isLinking, setIsLinking] = useState(false);
  const [isUnlinking, setIsUnlinking] = useState(false);
  const [connectingRepoId, setConnectingRepoId] = useState<number | null>(null);
  const [githubContribution, setGithubContribution] = useState<TilGithubContributionResponse | null>(null);
  const [isLoadingContribution, setIsLoadingContribution] = useState(false);
  const [contributionError, setContributionError] = useState<string | null>(null);

  const [searchQuery, setSearchQuery] = useState('');
  const debouncedSearchQuery = useDebounce(searchQuery, 300);


  const loadConnectedRepositories = useCallback(async () => {
    setIsLoadingConnected(true);
    setErrorMessage(null);
    setActionError(null);

    try {
      const status = await githubApi.getLinkStatus();
      setConnectedRepositories(status.connectedRepository ? [status.connectedRepository] : []);
      setIsGithubLinked(status.linked);
      if (!status.linked) {
        setAvailableRepositories([]);
      }
    } catch {
      setConnectedRepositories([]);
      setIsGithubLinked(false);
      setAvailableRepositories([]);
    } finally {
      setIsLoadingConnected(false);
    }
  }, []);

  const loadAvailableRepositories = useCallback(async () => {
    setIsLoadingAvailable(true);
    try {
      const items = await githubApi.getRepositories();
      setAvailableRepositories(items);
    } catch {
      setAvailableRepositories([]);
    } finally {
      setIsLoadingAvailable(false);
    }
  }, []);

  useEffect(() => {
    if (githubLinked) {
      navigate('/settings/integrations', { replace: true });
    }

    if (githubError) {
      setErrorMessage(GITHUB_LINK_ERROR_MESSAGE[githubError] ?? `GitHub 연동에 실패했습니다. (${githubError})`);
      setActionError({ target: 'link', message: 'GitHub 연결에 실패했어요.' });
      navigate('/settings/integrations', { replace: true });
    }

    let ignore = false;
    githubApi.getLinkStatus()
      .then((status) => {
        if (ignore) return;
        setConnectedRepositories(status.connectedRepository ? [status.connectedRepository] : []);
        setIsGithubLinked(status.linked);
        if (status.linked) {
          loadAvailableRepositories();
        } else {
          setAvailableRepositories([]);
        }
      })
      .catch(() => {
        if (ignore) return;
        setConnectedRepositories([]);
        setIsGithubLinked(false);
        setAvailableRepositories([]);
      })
      .finally(() => {
        if (ignore) return;
        setIsLoadingConnected(false);
      });

    return () => {
      ignore = true;
    };
  }, [githubError, githubLinked, loadAvailableRepositories, navigate]);

  const handleLinkGithub = async () => {
    if (isLinking || isGithubLinked) return;

    setErrorMessage(null);
    setIsLinking(true);

    try {
      const authorizeUrl = await githubApi.getLinkAuthorizeUrl();
      rememberGithubLinkAuthFlow();
      window.location.href = authorizeUrl;
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'GitHub 연동을 시작하지 못했습니다.', GITHUB_LINK_ERROR_MESSAGE));
      setActionError({ target: 'link', message: 'GitHub 연결에 실패했어요.' });
      setIsLinking(false);
    }
  };

  const handleUnlinkGithub = async () => {
    if (isUnlinking) return;

    const confirmed = window.confirm('GitHub 연동을 해제할까요? 연결된 레포지토리도 더 이상 SAN에서 사용할 수 없습니다.');
    if (!confirmed) return;

    setErrorMessage(null);
    setIsUnlinking(true);

    try {
      // DELETE /api/github/link
      await githubApi.unlinkAccount();
      setConnectedRepositories([]);
      setAvailableRepositories([]);
      setIsGithubLinked(false);
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'GitHub 연동 해제에 실패했습니다.', GITHUB_LINK_ERROR_MESSAGE));
    } finally {
      setIsUnlinking(false);
    }
  };

  const handleConnectRepository = async (repo: GithubRepository) => {
    // 단일 연결 정책이므로 만약 이미 연결된 게 있다면 해제 후 연결하거나 막을 수 있음
    if (connectedRepositories.length > 0) {
      const confirmed = window.confirm('이미 연결된 레포지토리가 있습니다. 기존 연결을 해제하고 새 레포지토리를 연결할까요?');
      if (!confirmed) return;
      
      try {
        await githubApi.disconnectRepository(connectedRepositories[0].githubRepositoryId);
      } catch (error) {
        setErrorMessage(getApiErrorMessage(error, '기존 레포지토리 연결 해제에 실패했습니다.'));
        return;
      }
    }

    setConnectingRepoId(repo.githubRepositoryId);
    setErrorMessage(null);

    try {
      await githubApi.connectRepository({
        githubRepositoryId: repo.githubRepositoryId,
      });
      await loadConnectedRepositories();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, '레포지토리 연결에 실패했습니다.'));
    } finally {
      setConnectingRepoId(null);
    }
  };

  const handleDisconnectRepository = async (repositoryId: number) => {
    setErrorMessage(null);

    try {
      await githubApi.disconnectRepository(repositoryId);
      await loadConnectedRepositories();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, '레포지토리 연결 해제에 실패했습니다.'));
    }
  };

  const loadGithubContributions = useCallback(async () => {
    if (!isGithubLinked) {
      setGithubContribution(null);
      setContributionError(null);
      return;
    }

    setIsLoadingContribution(true);
    setContributionError(null);

    try {
      const range = getRecentSixMonthRange();
      const contribution = await tilApi.getGithubContributions(range);
      setGithubContribution(contribution);
    } catch {
      setGithubContribution(null);
      setContributionError('TIL 커밋 기록을 불러오지 못했습니다.');
    } finally {
      setIsLoadingContribution(false);
    }
  }, [isGithubLinked]);

  useEffect(() => {
    void loadGithubContributions();
  }, [loadGithubContributions]);

  const filteredAvailableRepos = useMemo(() => {
    const connectedIds = new Set(connectedRepositories.map(r => r.githubRepositoryId));
    const notConnected = availableRepositories.filter(r => !connectedIds.has(r.githubRepositoryId));
    
    if (!debouncedSearchQuery) return notConnected;
    return notConnected.filter(repo => repo.fullName.toLowerCase().includes(debouncedSearchQuery.toLowerCase()));
  }, [availableRepositories, connectedRepositories, debouncedSearchQuery]);
  const hasConnectedRepository = connectedRepositories.length > 0;

  return (
    <section className="mx-auto w-full max-w-[1200px] space-y-8 py-12 text-text-primary">
      {/* ── 상단: GitHub 연동 상태 카드 ── */}
      <div className="rounded-xl border border-text-secondary/10 bg-surface-low/80 p-8">
        <div className="flex flex-col items-start justify-between gap-lg sm:flex-row sm:items-center">
          <div className="min-w-0">
            <p className="text-[11px] font-bold uppercase tracking-widest text-action-accent">
              통합 연동 <span className="mx-2 text-text-primary/20">•</span> <span className="text-text-primary/40">외부 동기화</span>
            </p>
            <div className="mt-3 flex items-center gap-3">
              <h1 className="text-4xl font-bold text-text-primary tracking-tight">GitHub</h1>
              <span
                className={`inline-flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider ${
                  isGithubLinked
                    ? 'text-action-accent'
                    : 'text-text-primary/40'
                }`}
              >
                <span className="relative flex h-2 w-2" aria-hidden="true">
                  {isGithubLinked && <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-action-accent opacity-60" />}
                  <span className={`relative inline-flex h-2 w-2 rounded-full ${isGithubLinked ? 'bg-action-accent' : 'bg-text-primary/25'}`} />
                </span>
                {isGithubLinked ? '연동됨' : '미연동'}
              </span>
            </div>
            <p className="mt-4 max-w-lg text-sm text-text-primary/50 leading-relaxed">
              GitHub 계정을 연동하여 Repository를 동기화하세요.
              <br />
              SAN에서 쌓은 TIL 기록을 꾸준한 커밋으로 남기고, GitHub 잔디를 심어보세요.
            </p>
          </div>

          <div className="relative flex shrink-0 items-center gap-3">
            {isGithubLinked && (
              <button
                type="button"
                onClick={handleUnlinkGithub}
                disabled={isUnlinking}
                className="rounded-md border border-text-secondary/10 px-3 py-2 text-sm font-medium text-text-primary/45 transition hover:border-red-400/30 hover:bg-red-500/10 hover:text-red-300 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isUnlinking ? '해제 중...' : '연동 해제'}
              </button>
            )}
            <InlineActionToast message={actionError?.target === 'link' ? actionError.message : null} />
            {!isGithubLinked && (
              <button
                type="button"
                onClick={handleLinkGithub}
                disabled={isLinking}
                className="inline-flex h-11 items-center gap-2 rounded-full bg-action-accent px-6 text-sm font-bold text-text-on-accent transition-all hover:bg-action-accent/90 active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <img
                  src={githubSvg}
                  alt="GitHub"
                  className="h-5 w-5 brightness-0"
                />
                {isLinking ? '연동 중...' : 'GitHub 계정 연동'}
              </button>
            )}
          </div>
        </div>
      </div>

      <div className={`rounded-xl border p-6 ${
        isGithubLinked
          ? 'border-text-secondary/10 bg-surface-low/80'
          : 'border-text-secondary/5 bg-surface-low/45 opacity-75'
      }`}>
        <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex min-w-0 items-center gap-4">
            <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full border ${
              isGithubLinked
                ? 'border-action-accent/25 bg-action-accent/12 text-action-accent'
                : 'border-text-secondary/10 bg-text-primary/5 text-text-primary/35'
            }`}>
              <Star size={20} fill="currentColor" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <h2 className={`text-base font-bold ${isGithubLinked ? 'text-text-primary' : 'text-text-primary/60'}`}>
                GitHub Star로 지식카드 수집하기
              </h2>
              <p className="mt-1 text-sm leading-6 text-text-primary/50">
                {isGithubLinked
                  ? 'Star한 Repository를 바탕으로 추천 스크랩을 만들고, 지식카드로 바로 수집할 수 있습니다.'
                  : 'GitHub 계정을 연동하면 Star 기반 추천 스크랩을 지식카드로 수집할 수 있습니다.'}
              </p>
            </div>
          </div>
          {isGithubLinked ? (
            <button
              type="button"
              onClick={() => navigate('/profile/stars')}
              className="inline-flex h-10 shrink-0 items-center justify-center gap-1.5 rounded-md bg-action-accent px-4 text-sm font-bold text-text-on-accent transition hover:bg-action-accent/90 active:scale-95"
            >
              수집하러 가기
              <ArrowRight size={14} strokeWidth={1.8} aria-hidden="true" />
            </button>
          ) : (
            <span className="inline-flex h-9 shrink-0 items-center rounded-md border border-text-secondary/10 px-3 text-xs font-bold text-text-primary/35">
              GitHub 연동 후 사용 가능
            </span>
          )}
        </div>
      </div>

      {/* 에러 메시지 */}
      {errorMessage && (
        <div className="flex items-center gap-4 rounded-xl border border-red-500/20 bg-red-500/5 p-4 mt-6">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-500/10 text-red-500">
            <AlertTriangle size={20} />
          </div>
          <div className="flex-1">
            <h3 className="text-sm font-bold text-red-500">연결 오류</h3>
            <p className="text-xs text-red-500/70">{errorMessage}</p>
          </div>
          {!isGithubLinked && (
            <button onClick={handleLinkGithub} className="text-xs font-medium text-red-500 hover:text-red-400 underline decoration-red-500/30 underline-offset-4">
              재인증하기
            </button>
          )}
        </div>
      )}

      {/* ── 하단: 2단 레포지토리 관리 영역 ── */}
      <div className="grid gap-6 lg:grid-cols-2 lg:items-stretch mt-10">
        
        {/* 왼쪽: Available Repositories */}
        <div className="flex h-full flex-col gap-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-text-primary">연결 가능한 Repository</h2>
              {isGithubLinked && hasConnectedRepository && (
                <p className="mt-1 text-xs text-text-primary/40">하나의 Repository만 선택 가능합니다.</p>
              )}
            </div>
            <span className="text-xs text-text-primary/40">
              {!isGithubLinked ? 'GitHub 연동 필요' : hasConnectedRepository ? '선택 완료' : 'Repository 선택 가능'}
            </span>
          </div>

          <div className="flex h-[460px] flex-col overflow-hidden rounded-xl border border-text-secondary/10 bg-surface-low/80">
            {/* 검색바 */}
            <div className="border-b border-text-secondary/5 p-4">
              <div className="relative">
                <Search size={16} className="pointer-events-none absolute left-3.5 top-1/2 z-10 -translate-y-1/2 text-text-primary/45" />
                <input
                  type="text"
                  placeholder="Repository 검색..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  disabled={!isGithubLinked || isLoadingAvailable}
                  className="w-full rounded-lg border border-text-secondary/10 bg-surface-container/90 py-2.5 pl-10 pr-4 text-sm text-text-primary placeholder:text-text-secondary/30 outline-none transition focus:border-text-secondary/20 disabled:opacity-50"
                />
              </div>
            </div>

            {/* 목록 또는 Empty State */}
            <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-4 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
              {!isGithubLinked ? (
                <div className="flex h-full flex-col items-center justify-center text-center">
                  <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-full border border-text-secondary/10 border-dashed bg-text-primary/5 text-text-primary/30">
                    <Link2 size={24} />
                  </div>
                  <h3 className="text-base font-bold text-text-primary">계정이 연동되지 않았습니다</h3>
                  <p className="mt-2 max-w-[260px] text-xs text-text-primary/40 leading-relaxed">
                    GitHub 계정을 연동하면 SAN과 동기화할 Repository를 조회하고 선택할 수 있습니다.
                  </p>
                  <button
                    onClick={handleLinkGithub}
                    className="mt-6 rounded-lg bg-text-primary/5 px-4 py-2 text-xs font-medium text-text-primary hover:bg-text-primary/10 transition"
                  >
                    GitHub 계정 연동
                  </button>
                </div>
              ) : isLoadingAvailable ? (
                <RepositorySkeleton />
              ) : filteredAvailableRepos.length > 0 ? (
                <ul className="flex flex-col gap-3">
                  {filteredAvailableRepos.map((repo) => (
                    <li key={repo.githubRepositoryId} className="flex items-center justify-between gap-3 rounded-lg border border-text-secondary/10 bg-surface-low/80 p-4 transition hover:bg-text-primary/[0.04]">
                      <div className="flex min-w-0 items-center gap-4">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-text-primary/5 text-text-primary/40">
                          <FolderGit2 size={20} />
                        </div>
                        <div className="min-w-0">
                          <p className="truncate text-sm font-bold text-text-primary">{repo.fullName}</p>
                          <p className="text-xs text-text-primary/40 mt-1">{repo.privateRepository ? '비공개(Private)' : '공개(Public)'} / {repo.defaultBranch}</p>
                        </div>
                      </div>
                      {hasConnectedRepository ? (
                        <span className="shrink-0 rounded-md border border-text-secondary/10 px-3 py-1.5 text-xs font-medium text-text-primary/35">
                          1개만 가능
                        </span>
                      ) : (
                        <button
                          onClick={() => handleConnectRepository(repo)}
                          disabled={connectingRepoId === repo.githubRepositoryId}
                          className="flex h-8 shrink-0 items-center justify-center rounded-md bg-action-accent/20 px-4 text-xs font-bold text-action-accent transition hover:bg-action-accent/30 disabled:opacity-50"
                        >
                          {connectingRepoId === repo.githubRepositoryId ? <Loader2 size={14} className="animate-spin" /> : '연결하기'}
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="flex h-full flex-col items-center justify-center text-center text-text-primary/40">
                  <p className="text-sm font-semibold text-text-primary/55">
                    {hasConnectedRepository ? '연결 가능한 Repository가 없습니다.' : '검색 결과가 없습니다.'}
                  </p>
                  <p className="mt-2 max-w-[260px] text-xs leading-5 text-text-primary/35">
                    {hasConnectedRepository
                      ? '현재 하나의 Repository만 선택할 수 있습니다.'
                      : '검색어를 조금 다르게 입력해보세요.'}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 오른쪽: Connected Repositories */}
        <div className="flex h-full flex-col gap-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-text-primary">연결된 Repository</h2>
                  <span className="flex h-5 w-5 items-center justify-center rounded-full bg-text-primary/10 text-[10px] font-bold text-text-primary/60">
                    {connectedRepositories.length}
                  </span>
                </div>
                <p className="mt-1 text-xs text-text-primary/40">현재 SAN과 동기화 중인 Repository입니다.</p>
              </div>
            </div>
            <button
              onClick={loadConnectedRepositories}
              disabled={isLoadingConnected || !isGithubLinked}
              className="text-text-primary/40 hover:text-text-primary transition disabled:opacity-50"
            >
              <RefreshCw size={16} className={isLoadingConnected ? 'animate-spin' : ''} />
            </button>
          </div>

          <div className="flex h-[460px] flex-col gap-4">
            <div className={`flex shrink-0 flex-col overflow-hidden rounded-xl bg-transparent ${
              isGithubLinked && connectedRepositories.length > 0
                ? 'border border-transparent p-0'
                : 'min-h-[220px] border border-text-secondary/10 bg-surface-low/80 p-4'
            }`}>
              {!isGithubLinked || (!isLoadingConnected && connectedRepositories.length === 0) ? (
                <div className="flex h-full flex-col items-center justify-center text-center">
                  <div className="relative mb-4 flex h-16 w-16 items-center justify-center rounded-xl border border-text-secondary/10 bg-text-primary/5 text-text-primary/30">
                    <TerminalSquare size={28} />
                    <div className="absolute -bottom-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-surface-lowest">
                      <div className="flex h-4 w-4 items-center justify-center rounded-full bg-action-accent text-text-on-accent">
                        <span className="text-[10px] font-bold leading-none">+</span>
                      </div>
                    </div>
                  </div>
                  <h3 className="text-base font-bold text-text-primary">
                    {isGithubLinked ? '연결된 Repository가 없습니다' : 'GitHub 연동이 필요합니다'}
                  </h3>
                  <p className="mt-2 max-w-[280px] text-xs text-text-primary/40 leading-relaxed">
                    {isGithubLinked 
                      ? '좌측 목록에서 SAN과 동기화할 Repository를 선택해주세요.'
                      : '계정 연동 후, SAN 워크스페이스에서 관리할 Repository를 선택할 수 있습니다.'}
                  </p>
                </div>
              ) : isLoadingConnected ? (
                <RepositorySkeleton />
              ) : (
                <div className="flex min-h-0 flex-1 flex-col">
                  <ul className="flex min-h-0 flex-col gap-3 overflow-y-auto [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
                    {connectedRepositories.map((repo) => (
                      <li key={repo.githubRepositoryId} className="rounded-lg border border-text-secondary/10 bg-surface-low/80 p-4 ring-1 ring-action-accent/15">
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex min-w-0 items-center gap-4">
                            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-text-primary/5">
                              <img src={githubSvg} alt="" aria-hidden="true" className="h-5 w-5 brightness-0 invert opacity-80" />
                            </div>
                            <div className="min-w-0">
                              <a href={repo.htmlUrl} target="_blank" rel="noreferrer" className="block truncate text-sm font-bold text-text-primary transition hover:text-action-accent">
                                {repo.fullName}
                              </a>
                              <div className="mt-1 flex items-center gap-2 text-xs text-text-primary/40">
                                <span>{repo.privateRepository ? '비공개(Private)' : '공개(Public)'}</span>
                                <span className="h-1 w-1 rounded-full bg-text-primary/20" aria-hidden="true" />
                                <span className="inline-flex items-center gap-1 text-action-accent">
                                  <span className="h-1.5 w-1.5 rounded-full bg-action-accent" aria-hidden="true" />
                                  Active
                                </span>
                              </div>
                            </div>
                          </div>
                          <button
                            onClick={() => handleDisconnectRepository(repo.githubRepositoryId)}
                            className="inline-flex shrink-0 items-center rounded-md border border-text-secondary/10 px-2.5 py-1.5 text-xs font-bold text-text-primary/35 transition hover:border-red-400/30 hover:bg-red-500/10 hover:text-red-300"
                            title="레포 끊기"
                          >
                            레포 끊기
                          </button>
                        </div>

                        <div className="mt-4 flex items-center justify-between gap-3 border-t border-text-secondary/5 pt-3">
                          <span className="inline-flex min-w-0 items-center gap-1.5 rounded-md border border-text-secondary/10 bg-surface-low/70 px-2.5 py-1 text-xs font-medium text-text-primary/55">
                            <GitBranch size={13} />
                            <span className="truncate">{repo.defaultBranch}</span>
                          </span>
                          <a
                            href={repo.htmlUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex shrink-0 items-center gap-1.5 rounded-md bg-text-primary/5 px-2.5 py-1 text-xs font-bold text-action-accent transition hover:bg-text-primary/10"
                          >
                            GitHub에서 보기
                            <ExternalLink size={13} />
                          </a>
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>

            {isGithubLinked && (
              <GithubContributionGraph
                contribution={githubContribution}
                isLoading={isLoadingContribution}
                error={contributionError}
                onRetry={loadGithubContributions}
                className="min-h-0 flex-1"
              />
            )}
          </div>
        </div>

      </div>
    </section>
  );
}
