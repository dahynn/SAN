import { type ReactNode, useCallback, useMemo, useState } from 'react';
import { AlertCircle, Check, Info, RotateCcw, X } from 'lucide-react';
import { ToastContext } from './toastContext';

export type ToastType = 'success' | 'error' | 'loading' | 'info';

export interface ToastInput {
  type?: ToastType;
  title: string;
  description?: string;
  duration?: number;
}

interface ToastItem extends Required<Omit<ToastInput, 'description'>> {
  id: number;
  description?: string;
}

const DEFAULT_DURATION = 3600;

let toastId = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismissToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback((toast: ToastInput) => {
    const id = ++toastId;
    const nextToast: ToastItem = {
      id,
      type: toast.type ?? 'info',
      title: toast.title,
      description: toast.description,
      duration: toast.duration ?? DEFAULT_DURATION,
    };

    setToasts((current) => [nextToast, ...current].slice(0, 4));

    if (nextToast.type !== 'loading' && nextToast.duration > 0) {
      window.setTimeout(() => dismissToast(id), nextToast.duration);
    }

    return id;
  }, [dismissToast]);

  const value = useMemo(() => ({ showToast, dismissToast }), [dismissToast, showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismissToast} />
    </ToastContext.Provider>
  );
}

function ToastViewport({ toasts, onDismiss }: { toasts: ToastItem[]; onDismiss: (id: number) => void }) {
  if (toasts.length === 0) return null;

  return (
    <div className="pointer-events-none fixed right-4 top-20 z-[60] flex w-[calc(100vw-32px)] max-w-[448px] flex-col gap-4 sm:right-6">
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} onDismiss={() => onDismiss(toast.id)} />
      ))}
    </div>
  );
}

function ToastCard({ toast, onDismiss }: { toast: ToastItem; onDismiss: () => void }) {
  if (toast.type === 'error') {
    return <ErrorToastCard toast={toast} onDismiss={onDismiss} />;
  }

  const tone = getToastTone(toast.type);

  return (
    <div
      role="status"
      className={[
        'pointer-events-auto flex w-full items-center justify-between gap-4',
        'rounded-tl-[48px] rounded-br-[48px] rounded-tr-lg rounded-bl-lg',
        'glass-popover border bg-surface-lowest/90 p-5 text-left backdrop-blur-2xl',
        'shadow-[0_0_20px_0_rgba(0,255,194,0.12)]',
        tone.border,
      ].join(' ')}
    >
      <div className="flex min-w-0 items-center gap-4">
        <ToastIcon type={toast.type} />
        <div className="min-w-0">
          <p className="truncate text-body-main text-text-primary">{toast.title}</p>
          {toast.description ? (
            <p className="mt-0.5 line-clamp-2 text-xs leading-5 text-text-secondary">{toast.description}</p>
          ) : null}
        </div>
      </div>

      <button
        type="button"
        onClick={onDismiss}
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-text-secondary/60 transition hover:bg-surface-lowest/80 bg-surface-lowest/70 hover:text-text-primary"
        aria-label="알림 닫기"
      >
        <X size={15} aria-hidden="true" />
      </button>
    </div>
  );
}

function ErrorToastCard({ toast, onDismiss }: { toast: ToastItem; onDismiss: () => void }) {
  const message = toast.description ?? toast.title;

  return (
    <button
      type="button"
      role="alert"
      onClick={onDismiss}
      className={[
        'pointer-events-auto flex h-14 w-full max-w-[320px] self-end items-center gap-5 px-4 text-left',
        'rounded-tl-[48px] rounded-br-lg rounded-tr-lg rounded-bl-[48px]',
        'border border-text-secondary/5 glass-panel bg-surface-container/90 backdrop-blur-xl',
        'transition hover:border-primary-signal/10 hover:bg-surface-container/75',
      ].join(' ')}
    >
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-surface-highest text-primary-signal">
        <AlertCircle size={20} strokeWidth={1.8} aria-hidden="true" />
      </span>

      <span className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="truncate text-xs font-bold text-text-primary">
          {message}
        </span>
        <span className="flex min-w-0 items-center gap-1 text-[10px] font-medium text-primary-signal">
          <RotateCcw size={10} strokeWidth={2} aria-hidden="true" />
          <span className="truncate">다시 확인하기</span>
        </span>
      </span>
    </button>
  );
}

function ToastIcon({ type }: { type: ToastType }) {
  if (type === 'loading') {
    return (
      <div className="flex shrink-0 items-center gap-1.5 px-1" aria-hidden="true">
        <span className="h-1.5 w-1.5 rounded-full bg-primary-signal" />
        <span className="h-1.5 w-1.5 rounded-full bg-primary-signal/40" />
        <span className="h-1.5 w-1.5 rounded-full bg-primary-signal/10" />
      </div>
    );
  }

  if (type === 'error') {
    return (
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-highest text-primary-signal">
        <AlertCircle size={17} strokeWidth={1.8} aria-hidden="true" />
      </div>
    );
  }

  if (type === 'success') {
    return (
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-leaf bg-primary-signal text-background">
        <Check size={16} strokeWidth={2.2} aria-hidden="true" />
      </div>
    );
  }

  return (
    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-leaf bg-primary-signal/10 text-primary-signal">
      <Info size={16} strokeWidth={1.8} aria-hidden="true" />
    </div>
  );
}

function getToastTone(type: ToastType) {
  if (type === 'error') {
    return { border: 'border-red-300/20' };
  }

  if (type === 'loading') {
    return { border: 'border-text-secondary/10' };
  }

  if (type === 'success') {
    return { border: 'border-primary-signal/30' };
  }

  return { border: 'border-primary-signal/10' };
}
