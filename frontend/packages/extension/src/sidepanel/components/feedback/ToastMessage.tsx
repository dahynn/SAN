import { AlertCircle, CheckCircle2 } from 'lucide-react';

type ToastMessageTone = 'success' | 'error';

interface ToastMessageProps {
  tone: ToastMessageTone;
  message: string;
}

export function ToastMessage({ tone, message }: ToastMessageProps) {
  const isError = tone === 'error';
  const Icon = isError ? AlertCircle : CheckCircle2;

  return (
    <div
      role={isError ? 'alert' : 'status'}
      className={[
        'flex min-h-14 items-center gap-3 rounded-tl-[48px] rounded-bl-[48px] rounded-tr-lg rounded-br-lg border px-4 py-3 text-caption',
        isError
          ? 'border-red-500/20 bg-red-500/10 text-red-300'
          : 'border-primary-signal/20 bg-primary-signal/10 text-primary-signal',
      ].join(' ')}
    >
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-highest">
        <Icon size={17} aria-hidden="true" />
      </span>
      <span className="min-w-0 leading-5">{message}</span>
    </div>
  );
}
