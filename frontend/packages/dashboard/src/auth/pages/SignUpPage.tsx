import { type FormEvent, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { getApiErrorMessage } from '@san/shared';
import { authApi } from '../../api/client';
import { getAuthClientType, withAuthClientType } from '../lib/clientType';
import { completeAuth } from '../lib/completeAuth';
import { useToast } from '../../components/shared/toast/toastContext';

type UsernameCheckStatus = 'idle' | 'checking' | 'available' | 'unavailable';

export function Signup() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [searchParams] = useSearchParams();
  const clientType = getAuthClientType(searchParams);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [checkedUsername, setCheckedUsername] = useState('');
  const [usernameCheckStatus, setUsernameCheckStatus] = useState<UsernameCheckStatus>('idle');
  const [usernameMessage, setUsernameMessage] = useState<string | null>(null);
  const [confirmPasswordError, setConfirmPasswordError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validateId = (id: string) => {
    const regex = /^[a-z0-9]{4,20}$/;
    return regex.test(id);
  };

  const validatePassword = (pw: string) => {
    // 영문, 숫자, 특수문자 포함 8~20자
    const regex = /^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*()_+={}[\]:;"'<>,.?/\\|~`-]).{8,20}$/;
    return regex.test(pw);
  };

  const handleUsernameChange = (value: string) => {
    setUsername(value);
    setCheckedUsername('');
    setUsernameCheckStatus('idle');
    setUsernameMessage(null);
  };

  const handleCheckUsername = async () => {
    const trimmed = username.trim();
    setUsernameMessage(null);
    setConfirmPasswordError(null);
    if (!trimmed) {
      setUsernameCheckStatus('unavailable');
      setUsernameMessage('아이디를 입력해주세요.');
      return;
    }
    setUsernameCheckStatus('checking');
    try {
      await authApi.checkUsername(trimmed);
      setCheckedUsername(trimmed);
      setUsernameCheckStatus('available');
      setUsernameMessage('사용 가능한 아이디입니다.');
    } catch (error) {
      setCheckedUsername('');
      setUsernameCheckStatus('unavailable');
      setUsernameMessage(getApiErrorMessage(error, '사용할 수 없는 아이디입니다.'));
    }
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setConfirmPasswordError(null);

    let hasError = false;
    const trimmed = username.trim();

    if (!validateId(trimmed)) {
      setUsernameCheckStatus('unavailable');
      setUsernameMessage('아이디 형식이 올바르지 않습니다.');
      hasError = true;
    } else if (checkedUsername !== trimmed || usernameCheckStatus !== 'available') {
      setUsernameCheckStatus('unavailable');
      setUsernameMessage('아이디 중복 확인을 해주세요.');
      hasError = true;
    }

    if (!validatePassword(password)) {
      showToast({
        type: 'error',
        title: '비밀번호 형식이 올바르지 않아요',
        description: '영문·숫자·특수문자 포함 8~20자로 입력해 주세요.',
      });
      hasError = true;
    } else if (password !== confirmPassword) {
      setConfirmPasswordError('비밀번호가 일치하지 않습니다.');
      hasError = true;
    }

    if (hasError) return;

    setIsSubmitting(true);
    try {
      await authApi.signup({ username: trimmed, password });
      const tokens = await authApi.login({ username: trimmed, password, clientType });
      await completeAuth(tokens, clientType, trimmed);
      navigate('/');
    } catch (error) {
      let msg = getApiErrorMessage(error, '회원가입에 실패했습니다.');
      msg = msg.replace(/^(Password|아이디|비밀번호):\s*/i, '');
      showToast({
        type: 'error',
        title: '회원가입에 실패했어요',
        description: msg,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputClass =
    'block h-12 w-full rounded-xl border border-text-secondary/10 glass-card bg-surface-container/70 px-4 text-sm text-text-primary outline-none placeholder:text-text-secondary/35 transition focus:border-action-accent/60 focus:ring-1 focus:ring-action-accent/20';

  return (
    <div className="relative flex min-h-screen w-full items-center justify-center overflow-hidden bg-background">

      {/* 왼쪽 브랜딩 — lg 이상에서만 표시 (로그인과 완전 동일) */}
      <div className="relative z-10 hidden w-1/2 flex-col justify-end p-16 lg:flex">
        <h1 className="text-6xl font-bold text-text-primary">SAN</h1>
        <p className="mt-3 max-w-[280px] text-sm leading-relaxed text-text-secondary">
          지식 아카이브를 정밀하고 고요하게 키워나가세요.
        </p>
        <p className="mt-8 text-[11px] tracking-widest text-text-secondary/30">
          © 2026 SAN DIGITAL ARTIFACTS
        </p>
      </div>

      {/* 오른쪽 카드 */}
      <div className="relative z-10 flex w-full items-center justify-center px-4 py-10 sm:px-8 lg:w-1/2 lg:px-16">
        <div
          className="flex w-full max-w-[400px] flex-col justify-center rounded-3xl border border-text-secondary/15 glass-card bg-surface-container/80 shadow-[0_8px_32px_0_rgba(0,0,0,0.3)] backdrop-blur-3xl"
          style={{ padding: '32px', height: '640px' }}
        >
          {/* 헤더 */}
          <h2 className="text-2xl font-bold text-text-primary sm:text-[32px]">회원가입</h2>
          <p className="mb-5 mt-2 text-sm leading-relaxed text-text-secondary">
            나만의 지식 아카이브 공간을 만들어보세요.
          </p>

          {/* 폼 */}
          <form className="space-y-3" onSubmit={handleSubmit}>

            {/* 아이디 */}
            <div className="shrink-0">
              <label className="mb-1.5 block text-xs font-bold text-text-secondary">
                아이디
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="아이디 입력"
                  value={username}
                  autoComplete="username"
                  onChange={(e) => handleUsernameChange(e.target.value)}
                  className={`${inputClass} flex-1`}
                />
                <button
                  type="button"
                  onClick={handleCheckUsername}
                  disabled={usernameCheckStatus === 'checking'}
                  className="h-12 shrink-0 rounded-xl border border-action-accent/40 px-4 text-[12px] font-bold text-action-accent transition hover:bg-action-accent/10 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {usernameCheckStatus === 'checking' ? '확인 중' : '중복 확인'}
                </button>
              </div>
              <p className="mt-2 text-[12px] text-text-secondary/50">영문 소문자·숫자 4~20자</p>
              <p
                className={[
                  'mt-1 min-h-[16px] text-[12px] font-medium',
                  usernameCheckStatus === 'available' ? 'text-action-accent' : 'text-red-400',
                ].join(' ')}
              >
                {usernameMessage || ''}
              </p>
            </div>

            {/* 비밀번호 */}
            <div className="shrink-0">
              <label className="mb-1.5 block text-xs font-bold text-text-secondary">
                비밀번호
              </label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="비밀번호 입력"
                  value={password}
                  autoComplete="new-password"
                  onChange={(e) => setPassword(e.target.value)}
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
              <p className="mt-2 text-[12px] text-text-secondary/50">영문·숫자·특수문자 포함 8~20자</p>
            </div>

            {/* 비밀번호 확인 */}
            <div className="shrink-0">
              <label className="mb-1.5 block text-xs font-bold text-text-secondary">
                비밀번호 확인
              </label>
              <div className="relative">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  placeholder="비밀번호 확인"
                  value={confirmPassword}
                  autoComplete="new-password"
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className={`${inputClass} pr-12`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword((v) => !v)}
                  aria-label={showConfirmPassword ? '비밀번호 숨기기' : '비밀번호 보기'}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-text-secondary/45 transition hover:text-text-secondary"
                >
                  {showConfirmPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
              <p className="mt-1.5 min-h-[16px] text-[12px] text-red-400">
                {confirmPasswordError || ''}
              </p>
            </div>

            <div className="pt-2 shrink-0">
              {/* 에러 메시지 (API 호출 실패, 비밀번호 불일치, 약관 미동의 등) */}
              {/* 가입 버튼 */}
              <button
                type="submit"
                disabled={isSubmitting}
                className="h-12 w-full rounded-xl bg-action-accent text-sm font-bold text-background outline-none transition hover:bg-action-accent-hover focus:outline-none active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
                style={{ boxShadow: '0 0 24px 0 rgba(74,222,128,0.28)' }}
              >
                {isSubmitting ? '가입 중...' : '회원가입'}
              </button>
            </div>
          </form>

          {/* 로그인 링크 */}
          <div className="mt-6">
            <p className="text-center text-sm text-text-secondary">
              이미 계정이 있으신가요?{' '}
              <Link to={withAuthClientType('/login', clientType)} className="font-bold text-action-accent hover:underline">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </div>

      {/* 하단 바 — 모바일에선 숨김 */}
      <div className="pointer-events-none absolute bottom-0 left-0 right-0 z-10 hidden items-center justify-between px-8 py-4 lg:flex">
        <span className="text-[10px] tracking-widest text-text-secondary/30">© 2024 SAN DIGITAL ARTIFACTS</span>
        <div className="flex gap-5">
          <span className="text-[10px] tracking-widest text-text-secondary/30">개인정보 처리방침</span>
          <span className="text-[10px] tracking-widest text-text-secondary/30">이용약관</span>
        </div>
      </div>
    </div>
  );
}
