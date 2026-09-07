import { AlertTriangle, RotateCcw, SearchX, UserLock, WifiOff } from 'lucide-react';

export type ErrorFallbackType = 'default' | 'network' | 'notFound' | 'server' | 'auth';
export type ErrorFallbackVariant = 'full' | 'inline';

export interface ErrorFallbackProps {
  type?: ErrorFallbackType;
  variant?: ErrorFallbackVariant;
  message?: string;
  description?: string;
  actionLabel?: string;
  onRetry?: () => void;
}

const ERROR_PRESET: Record<ErrorFallbackType, { message: string; description: string; actionLabel: string }> = {
  default: {
    message: '문제가 발생했어요',
    description: '잠시 후 다시 시도해주세요.',
    actionLabel: '다시 시도',
  },
  network: {
    message: '인터넷 연결을 확인해주세요',
    description: '네트워크 상태를 확인하고 다시 시도해주세요.',
    actionLabel: '다시 시도',
  },
  notFound: {
    message: '페이지를 찾을 수 없어요',
    description: '주소가 올바른지 확인하거나 홈으로 돌아가주세요.',
    actionLabel: '돌아가기',
  },
  server: {
    message: '서버 오류가 발생했어요',
    description: '잠시 후 다시 시도해주세요.',
    actionLabel: '다시 시도',
  },
  auth: {
    message: '로그인이 필요해요',
    description: '해당 페이지는 로그인 후 이용할 수 있어요.',
    actionLabel: '로그인하기',
  },
};

function ErrorIcon({ type, size }: { type: ErrorFallbackType; size: number }) {
  const iconProps = {
    size,
    strokeWidth: 1.8,
    'aria-hidden': true,
  };

  if (type === 'network') {
    return <WifiOff {...iconProps} />;
  }

  if (type === 'notFound') {
    return <SearchX {...iconProps} />;
  }

  if (type === 'auth') {
    return <UserLock {...iconProps} />;
  }

  return <AlertTriangle {...iconProps} />;
}

export function ErrorFallback({
  type = 'default',
  variant = 'inline',
  message,
  description,
  actionLabel,
  onRetry,
}: ErrorFallbackProps) {
  const preset = ERROR_PRESET[type];
  const displayMessage = message ?? preset.message;
  const displayDescription = description ?? preset.description;
  const displayActionLabel = actionLabel ?? preset.actionLabel;
  const isFull = variant === 'full';

  return (
    <section
      role="alert"
      className={[
        'relative mx-auto flex w-full max-w-[440px] flex-col items-center justify-center overflow-hidden text-center',
        'rounded-tl-[48px] rounded-br-[48px] rounded-tr-lg rounded-bl-lg',
        'border border-text-secondary/15 glass-popover bg-surface-lowest/68 backdrop-blur-2xl',
        '!shadow-none',
        isFull ? 'min-h-[400px] gap-4 px-10 py-16' : 'min-h-[280px] gap-4 px-6 py-10',
      ].join(' ')}
    >
      <div className="pointer-events-none absolute inset-x-10 top-0 h-px bg-gradient-to-r from-transparent via-action-accent/35 to-transparent" />
      <div className="pointer-events-none absolute -left-20 top-10 h-44 w-44 rounded-full bg-action-accent/10 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-24 right-8 h-52 w-52 rounded-full bg-action-accent/8 blur-3xl" />

      <div
        className={[
          'relative z-10 flex items-center justify-center text-action-accent',
          'rounded-tl-[48px] rounded-br-[48px] rounded-tr-lg rounded-bl-lg',
          'border border-action-accent/22 glass-panel bg-action-accent/8 backdrop-blur-xl',
          '!shadow-none',
          isFull ? 'h-20 w-20' : 'h-14 w-14',
        ].join(' ')}
      >
        <ErrorIcon type={type} size={isFull ? 34 : 24} />
      </div>

      <div className="relative z-10 flex max-w-[22rem] flex-col items-center gap-2">
        <h2 className={isFull ? 'text-h2-bold text-text-primary' : 'text-body-main-bold text-text-primary'}>
          {displayMessage}
        </h2>
        <p className={isFull ? 'text-body-main text-text-ghost' : 'text-body-sm text-text-ghost'}>
          {displayDescription}
        </p>
      </div>

      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className={[
            'inline-flex items-center justify-center gap-2 rounded-full',
            'border border-action-accent/22 glass-panel bg-action-accent/10 font-semibold text-action-accent backdrop-blur-xl transition hover:border-action-accent/36 hover:bg-action-accent/15',
            isFull ? 'min-h-11 px-8 text-body-sm-bold' : 'min-h-9 px-4 text-caption-bold',
          ].join(' ')}
        >
          <RotateCcw size={isFull ? 16 : 13} aria-hidden="true" />
          {displayActionLabel}
        </button>
      ) : null}
    </section>
  );
}
