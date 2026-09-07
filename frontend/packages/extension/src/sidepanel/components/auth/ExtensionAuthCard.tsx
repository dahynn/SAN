import { type FormEvent, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { getApiErrorMessage } from '@san/shared';
import { authApi, authTokenStorage } from '@extension/api/client';
import githubSvg from '@san/ui/assets/icons/github.svg';

type AuthMode = 'login' | 'signup';
type UsernameCheckStatus = 'idle' | 'checking' | 'available' | 'unavailable';

interface ExtensionAuthCardProps {
  onAuthenticated: () => void;
  onGithubLogin: () => void;
}

const inputClass =
  'block h-12 w-full rounded-xl border border-text-secondary/10 glass-card bg-surface-container/70 px-4 text-sm text-text-primary outline-none placeholder:text-text-secondary/35 transition focus:border-primary-signal/60 focus:ring-1 focus:ring-primary-signal/20';

export function ExtensionAuthCard({ onAuthenticated, onGithubLogin }: ExtensionAuthCardProps) {
  const [mode, setMode] = useState<AuthMode>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [checkedUsername, setCheckedUsername] = useState('');
  const [usernameCheckStatus, setUsernameCheckStatus] = useState<UsernameCheckStatus>('idle');
  const [usernameMessage, setUsernameMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isSignup = mode === 'signup';

  const validateId = (id: string) => {
    const regex = /^[a-z0-9]{4,20}$/;
    return regex.test(id);
  };

  const validatePassword = (pw: string) => {
    // 영문, 숫자, 특수문자 포함 8~20자
    const regex = /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+={}\[\]:;"'<>,.?\/\\|~`-]).{8,20}$/;
    return regex.test(pw);
  };

  const handleModeChange = (nextMode: AuthMode) => {
    setMode(nextMode);
    setPassword('');
    setConfirmPassword('');
    setShowPassword(false);
    setShowConfirmPassword(false);
    setCheckedUsername('');
    setUsernameCheckStatus('idle');
    setUsernameMessage(null);
    setErrorMessage(null);
  };

  const handleUsernameChange = (value: string) => {
    setUsername(value);
    setCheckedUsername('');
    setUsernameCheckStatus('idle');
    setUsernameMessage(null);
  };

  const handleCheckUsername = async () => {
    const trimmed = username.trim();
    setErrorMessage(null);
    setUsernameMessage(null);

    if (!trimmed) {
      setUsernameCheckStatus('unavailable');
      setUsernameMessage('아이디를 입력해 주세요.');
      return;
    }

    setUsernameCheckStatus('checking');
    try {
      await authApi.checkUsername(trimmed);
      setCheckedUsername(trimmed);
      setUsernameCheckStatus('available');
      setUsernameMessage('사용 가능한 아이디입니다.');
    } catch (error: any) {
      setCheckedUsername('');
      setUsernameCheckStatus('unavailable');
      if (error.response?.status === 409) {
        setUsernameMessage('이미 사용 중인 아이디입니다.');
      } else {
        setUsernameMessage(getApiErrorMessage(error, '사용할 수 없는 아이디입니다.'));
      }
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = username.trim();
    setErrorMessage(null);

    if (!trimmed || !password) {
      setErrorMessage('아이디와 비밀번호를 입력해 주세요.');
      return;
    }

    if (isSignup) {
      if (!validateId(trimmed)) {
        setErrorMessage('아이디 형식이 올바르지 않습니다.');
        return;
      }

      if (checkedUsername !== trimmed || usernameCheckStatus !== 'available') {
        setUsernameCheckStatus('unavailable');
        setUsernameMessage('아이디 중복 확인을 완료해 주세요.');
        return;
      }

      if (!validatePassword(password)) {
        setErrorMessage('비밀번호 형식이 올바르지 않습니다.');
        return;
      }

      if (password !== confirmPassword) {
        setErrorMessage('비밀번호가 일치하지 않습니다.');
        return;
      }
    }

    setIsSubmitting(true);
    try {
      if (isSignup) {
        await authApi.signup({ username: trimmed, password });
      }

      const tokens = await authApi.login({
        username: trimmed,
        password,
        clientType: 'EXTENSION',
      });
      await authTokenStorage.setTokens?.(tokens);
      void chrome.runtime
        .sendMessage({ type: 'SAN_AUTH_STATE_CHANGED', isAuthenticated: true })
        .catch(() => undefined);
      onAuthenticated();
    } catch (error) {
      const fallback = isSignup ? '회원가입에 실패했습니다.' : '로그인에 실패했습니다.';
      setErrorMessage(getApiErrorMessage(error, fallback).replace(/^(Password|username|아이디|비밀번호):\s*/i, ''));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="relative z-10 flex min-h-0 flex-1 items-center justify-center px-3 py-4 sm:px-8">
      <div
        className="extension-auth-card glass-card relative flex h-[clamp(520px,calc(100vh-112px),600px)] w-full max-w-[400px] flex-col justify-center overflow-hidden rounded-[28px] border border-text-secondary/20 bg-surface-container/90 p-5 shadow-[0_8px_32px_0_rgba(0,0,0,0.3)] backdrop-blur-3xl sm:rounded-3xl sm:p-8"
        style={{
          background: 'linear-gradient(135deg, color-mix(in oklab, var(--color-surface-container) 72%, white 28%) 0%, color-mix(in oklab, var(--color-surface-low) 86%, white 14%) 100%)',
        }}
      >
        <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-text-secondary/35 to-transparent" />

        <div className="relative mb-5">
          <h1 className="text-2xl font-bold text-text-primary sm:text-[32px]">
            {isSignup ? '회원가입' : '로그인'}
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-text-secondary">
            {isSignup ? '나만의 지식 아카이브 공간을 만들어보세요.' : '아카이브에 접속하고 탐험을 계속하세요.'}
          </p>
        </div>

        <form className="relative flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit}>
          {!isSignup && (
            <>
              <button
                type="button"
                onClick={onGithubLogin}
                className="relative flex h-14 w-full shrink-0 items-center rounded-xl border border-text-secondary/10 bg-surface-low text-sm font-bold text-text-primary outline-none transition hover:opacity-85 focus-visible:ring-1 focus-visible:ring-action-accent/35 active:scale-[0.98]"
              >
                <span className="absolute left-4 flex h-8 w-8 items-center justify-center">
                  <img src={githubSvg} alt="" aria-hidden="true" className="extension-login-github-icon h-6 w-6" />
                </span>
                <span className="pointer-events-none flex w-full justify-center px-14 text-center">
                  <span className="sm:hidden">GitHub 로그인</span>
                  <span className="hidden sm:inline">GitHub 계정으로 로그인</span>
                </span>
              </button>

              <div className="my-5 flex shrink-0 items-center gap-4">
                <div className="h-px flex-1 bg-text-primary/10" />
                <span className="text-[11px] text-text-secondary/50">Or</span>
                <div className="h-px flex-1 bg-text-primary/10" />
              </div>
            </>
          )}

          <div className={['flex min-h-0 flex-1 flex-col', isSignup ? 'justify-start gap-3' : 'justify-start gap-3'].join(' ')}>
            <div>
              <label className="mb-1.5 block text-xs font-bold text-text-secondary">아이디</label>
              <div className={isSignup ? 'flex gap-2' : undefined}>
                <input
                  type="text"
                  value={username}
                  autoComplete="username"
                  placeholder="영문 소문자·숫자 4~20자"
                  onChange={(event) => handleUsernameChange(event.target.value)}
                  className={`${inputClass} ${isSignup ? 'min-w-0 flex-1' : ''}`}
                />
                {isSignup && (
                  <button
                    type="button"
                    onClick={handleCheckUsername}
                    disabled={usernameCheckStatus === 'checking'}
                    className="h-12 shrink-0 rounded-xl border border-action-accent/40 px-4 text-[11px] font-bold text-action-accent outline-none transition hover:bg-action-accent/10 focus-visible:ring-1 focus-visible:ring-action-accent/35 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {usernameCheckStatus === 'checking' ? '확인 중' : '중복 확인'}
                  </button>
                )}
              </div>
              {isSignup && (
                <>
                  <p className="mt-2 pl-1 text-[11px] text-text-secondary/50">영문 소문자·숫자 4~20자</p>
                  <p
                    className={[
                      'mt-1 min-h-[16px] text-[11px] font-medium',
                      usernameCheckStatus === 'available' ? 'text-primary-signal' : 'text-red-400',
                    ].join(' ')}
                  >
                    {usernameMessage || ''}
                  </p>
                </>
              )}
            </div>

            <div>
              <label className="mb-1.5 block text-xs font-bold text-text-secondary">비밀번호</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  autoComplete={isSignup ? 'new-password' : 'current-password'}
                  placeholder="영문·숫자·특수문자 포함 8~20자"
                  onChange={(event) => setPassword(event.target.value)}
                  className={`${inputClass} pr-11`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-text-secondary/50 transition hover:text-text-secondary"
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
              {isSignup && <p className="mt-2 pl-1 text-[11px] text-text-secondary/50">영문·숫자·특수문자 포함 8~20자</p>}
            </div>

            {isSignup && (
              <div className="pt-0.5">
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    value={confirmPassword}
                    autoComplete="new-password"
                    placeholder="비밀번호 재입력"
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    className={`${inputClass} pr-11`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword((value) => !value)}
                    aria-label={showConfirmPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-text-secondary/50 transition hover:text-text-secondary"
                  >
                    {showConfirmPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                  </button>
                </div>
              </div>
            )}
          </div>

          <p className="mt-auto min-h-[16px] text-center text-[11px] text-red-400">{errorMessage || ''}</p>

          <button
            type="submit"
            disabled={isSubmitting}
            className="h-12 min-h-12 w-full shrink-0 rounded-xl bg-action-accent text-sm font-bold text-background shadow-neon outline-none transition hover:bg-action-accent-hover focus:outline-none active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? '처리 중...' : isSignup ? '회원가입' : '로그인'}
          </button>

          <p className="mt-6 text-center text-sm text-text-secondary">
            {isSignup ? '이미 계정이 있으신가요?' : '계정이 없으신가요?'}{' '}
            <button
              type="button"
              onClick={() => handleModeChange(isSignup ? 'login' : 'signup')}
              className="font-bold text-action-accent outline-none transition hover:text-action-accent/80 focus-visible:rounded focus-visible:ring-1 focus-visible:ring-action-accent/35"
            >
              {isSignup ? '로그인' : '회원가입'}
            </button>
          </p>
        </form>
      </div>
    </section>
  );
}
