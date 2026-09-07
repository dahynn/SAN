import { AlertCircle, RotateCcw } from 'lucide-react';

interface InlineActionToastProps {
  message: string | null;
  actionLabel?: string;
  duration?: number;
}

export function InlineActionToast({ message, actionLabel = '다시 확인하기' }: InlineActionToastProps) {
  if (!message) return null;

  return (
    <div
      role="alert"
      className={[
        'pointer-events-none absolute bottom-full left-0 mb-3 flex h-14 w-full min-w-[260px] items-center gap-4 px-4',
        'rounded-tl-[48px] rounded-br-lg rounded-tr-lg rounded-bl-[48px]',
        'border border-red-400/20 glass-popover bg-surface-lowest/80 backdrop-blur-xl',
        'shadow-[0_8px_24px_rgba(0,0,0,0.22)]',
      ].join(' ')}
    >
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-400/10 text-red-300">
        <AlertCircle size={20} strokeWidth={1.8} aria-hidden="true" />
      </span>

      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="truncate text-xs font-bold text-text-primary">
          {message}
        </span>
        <span className="flex min-w-0 items-center gap-1 text-[10px] font-medium text-red-300">
          <RotateCcw size={10} strokeWidth={2} aria-hidden="true" />
          <span className="truncate">{actionLabel}</span>
        </span>
      </span>
    </div>
  );
}
