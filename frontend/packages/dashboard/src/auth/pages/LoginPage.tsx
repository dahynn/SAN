import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import githubSvg from '@ui/assets/icons/github.svg';
import { getApiErrorMessage } from '@san/shared';
import { authApi, githubAuthApi } from '../../api/client';
import { getAuthClientType, rememberAuthClientType, withAuthClientType } from '../lib/clientType';
import { completeAuth } from '../lib/completeAuth';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const clientType = getAuthClientType(searchParams);
  const initialAuthError =
    typeof location.state === 'object' && location.state && 'authError' in location.state
      ? (location.state as { authError: string }).authError
      : null;

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [usernameError, setUsernameError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [generalError, setGeneralError] = useState<string | null>(initialAuthError);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const autoGithubStartedRef = useRef(false);

  const handleGithubLogin = useCallback(() => {
    rememberAuthClientType(clientType);
    window.location.replace(githubAuthApi.getGithubAuthorizeUrl(clientType));
  }, [clientType]);

  useEffect(() => {
    if (searchParams.get('autoGithub') !== 'true' || autoGithubStartedRef.current) {
      return;
    }

    autoGithubStartedRef.current = true;
    handleGithubLogin();
  }, [handleGithubLogin, searchParams]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    let hasError = false;
    if (!username.trim()) {
      setUsernameError('아이디를 입력해주세요.');
      hasError = true;
    } else {
      setUsernameError(null);
    }

    if (!password) {
      setPasswordError('비밀번호를 입력해주세요.');
      hasError = true;
    } else {
      setPasswordError(null);
    }

    setGeneralError(null);
    if (hasError) return;

    setIsSubmitting(true);
    try {
      const tokens = await authApi.login({ username, password, clientType });
      await completeAuth(tokens, clientType, username.trim());
      navigate('/');
    } catch (err) {
      let msg = getApiErrorMessage(err, '로그인에 실패했습니다.');
      msg = msg.replace(/^(Password|username|아이디|비밀번호):\s*/i, '');
      setGeneralError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputClass =
    'block h-12 w-full rounded-xl border border-text-secondary/10 glass-card bg-surface-container/70 px-4 text-sm text-text-primary outline-none placeholder:text-text-secondary/35 transition focus:border-action-accent/60 focus:ring-1 focus:ring-action-accent/20';

  return (
    <div className="relative flex min-h-screen w-full items-center justify-center overflow-hidden bg-background">
      <div className="relative z-10 hidden w-1/2 flex-col justify-end p-16 lg:flex">
        <h1 className="text-6xl font-bold text-text-primary">SAN</h1>
        <p className="mt-3 max-w-[280px] text-sm leading-relaxed text-text-secondary">
          지식 아카이브를 정밀하고 고요하게 키워나가세요.
        </p>
        <p className="mt-8 text-[11px] tracking-widest text-text-secondary/30">
          © 2026 SAN DIGITAL ARTIFACTS
        </p>
      </div>

      <div className="relative z-10 flex w-full items-center justify-center px-4 py-10 sm:px-8 lg:w-1/2 lg:px-16">
        <div
          className="flex w-full max-w-[400px] flex-col justify-center rounded-3xl border border-text-secondary/15 glass-card bg-surface-container/80 shadow-[0_8px_32px_0_rgba(0,0,0,0.3)] backdrop-blur-3xl"
          style={{
            padding: '32px',
            height: '600px',
          }}
        >
          <h2 className="text-2xl font-bold text-text-primary sm:text-[32px]">로그인</h2>
          <p className="mb-5 mt-2 text-sm leading-relaxed text-text-secondary">
            아카이브에 접속하고 탐험을 계속하세요.
          </p>

          <button
            type="button"
            onClick={handleGithubLogin}
            className="flex h-14 w-full shrink-0 items-center justify-center gap-2 rounded-xl text-sm font-bold text-text-primary transition hover:opacity-85 active:scale-[0.98]"
            style={{ background: 'var(--color-surface-lowest)', border: '1px solid color-mix(in oklab, var(--color-text-secondary) 16%, transparent)' }}
          >
            <span className="flex h-8 w-8 shrink-0 items-center justify-center">
              <img src={githubSvg} alt="GitHub" className="dashboard-login-github-icon h-6 w-6" />
            </span>
            <span className="text-center">GitHub 계정으로 로그인</span>
          </button>

          <div className="my-5 flex shrink-0 items-center gap-4">
            <div className="h-px flex-1 bg-text-secondary/10" />
            <span className="text-[11px] text-text-secondary/50">Or</span>
            <div className="h-px flex-1 bg-text-secondary/10" />
          </div>

          <form className="space-y-3" onSubmit={handleSubmit}>
            <div className="shrink-0">
              <label className="mb-1.5 block text-xs font-bold text-text-secondary">
                아이디
              </label>
              <input
                type="text"
                placeholder="영문 소문자·숫자 4~20자"
                value={username}
                autoComplete="username"
                onChange={(e) => {
                  setUsername(e.target.value);
                  setUsernameError(null);
                  setGeneralError(null);
                }}
                className={inputClass}
              />
              <p className="mt-1.5 min-h-[16px] text-[11px] text-red-400">
                {usernameError || ''}
              </p>
            </div>

            <div className="shrink-0">
              <div className="mb-1.5 flex items-center justify-between">
                <label className="text-xs font-bold text-text-secondary">
                  비밀번호
                </label>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="영문·숫자·특수문자 포함 8~20자"
                  value={password}
                  autoComplete="current-password"
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setPasswordError(null);
                    setGeneralError(null);
                  }}
                  className={`${inputClass} pr-12`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-text-secondary/45 transition hover:text-text-secondary"
                >
                  {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
              <p className="mt-1.5 min-h-[16px] text-[11px] text-red-400">
                {passwordError || ''}
              </p>
            </div>

            <div className="shrink-0 pt-1">
              <p className="mb-2 min-h-[16px] text-center text-[11px] text-red-400">
                {generalError || ''}
              </p>

              <button
                type="submit"
                disabled={isSubmitting}
                className="h-12 w-full rounded-xl bg-action-accent text-sm font-bold text-background outline-none transition hover:bg-action-accent-hover focus:outline-none active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
                style={{ boxShadow: '0 0 24px 0 rgba(74,222,128,0.25)' }}
              >
                {isSubmitting ? '접속 중...' : '로그인'}
              </button>
            </div>
          </form>

          <div className="mt-6">
            <p className="text-center text-sm text-text-secondary">
              계정이 없으신가요?{' '}
              <Link to={withAuthClientType('/signup', clientType)} className="font-bold text-action-accent hover:underline">
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </div>

      <div className="pointer-events-none absolute bottom-0 left-0 right-0 z-10 hidden items-center justify-between px-8 py-4 lg:flex">
        <span className="text-[10px] tracking-widest text-text-secondary/30">© 2026 SAN TEAM</span>
        <div className="flex gap-5">
          <span className="text-[10px] tracking-widest text-text-secondary/30">개인정보 처리방침</span>
          <span className="text-[10px] tracking-widest text-text-secondary/30">이용약관</span>
        </div>
      </div>
    </div>
  );
}
