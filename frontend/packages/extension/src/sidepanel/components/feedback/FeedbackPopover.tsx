import { type FormEvent, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Bug, CheckCircle2, Lightbulb, Loader2, MessageSquare, SmilePlus, X } from 'lucide-react';
import { getApiErrorMessage, type FeedbackType } from '@san/shared';
import { feedbackApi } from '@extension/api/client';

interface FeedbackPopoverProps {
  onClose: () => void;
}

const feedbackTypes: Array<{
  type: FeedbackType;
  label: string;
  icon: typeof Bug;
  activeClassName: string;
  iconClassName: string;
}> = [
  {
    type: 'BUG',
    label: '\uBC84\uADF8',
    icon: Bug,
    activeClassName: 'border-red-300/35 bg-red-400/10 text-red-200 shadow-[0_0_18px_rgba(252,165,165,0.12)]',
    iconClassName: 'bg-red-400/10 text-red-200',
  },
  {
    type: 'INCONVENIENCE',
    label: '\uBD88\uD3B8\uD568',
    icon: SmilePlus,
    activeClassName: 'border-red-300/35 bg-red-400/10 text-red-200 shadow-[0_0_18px_rgba(252,165,165,0.12)]',
    iconClassName: 'bg-red-400/10 text-red-200',
  },
  {
    type: 'FEATURE_REQUEST',
    label: '\uC81C\uC548',
    icon: Lightbulb,
    activeClassName: 'extension-primary-box-glow border-primary-signal/45 bg-primary-signal/10 text-primary-signal',
    iconClassName: 'bg-primary-signal/10 text-primary-signal',
  },
  {
    type: 'ETC',
    label: '\uAE30\uD0C0',
    icon: MessageSquare,
    activeClassName: 'border-sky-300/35 bg-sky-300/10 text-sky-100 shadow-[0_0_18px_rgba(125,211,252,0.12)]',
    iconClassName: 'bg-sky-300/10 text-sky-100',
  },
];

const text = {
  title: '\uC758\uACAC \uBCF4\uB0B4\uAE30',
  success: '\uD53C\uB4DC\uBC31\uC774 \uC804\uC1A1\uB418\uC5C8\uC2B5\uB2C8\uB2E4.',
  close: '\uB2EB\uAE30',
  placeholder: '\uD3B8\uD558\uAC8C \uC758\uACAC\uC744 \uB0A8\uACA8\uC8FC\uC138\uC694.',
  contactLabel: '\uC5F0\uB77D\uCC98 \uB610\uB294 \uC774\uBA54\uC77C (\uC120\uD0DD)',
  contactPlaceholder: '\uC791\uC740 \uC120\uBB3C\uC744 \uC804\uD560 \uC218 \uC788\uC5B4\uC694.',
  errorFallback: '\uD53C\uB4DC\uBC31 \uC804\uC1A1\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.',
  cancel: '\uCDE8\uC18C',
  send: '\uBCF4\uB0B4\uAE30',
};

async function getActiveTabUrl() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  return tab?.url ?? window.location.href;
}

export function FeedbackPopover({ onClose }: FeedbackPopoverProps) {
  const [type, setType] = useState<FeedbackType>('INCONVENIENCE');
  const [content, setContent] = useState('');
  const [contact, setContact] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !isSubmitting) {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isSubmitting, onClose]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedContent = content.trim();
    const trimmedContact = contact.trim();
    if (!trimmedContent || isSubmitting) return;

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      await feedbackApi.create({
        type,
        content: trimmedContent,
        contact: trimmedContact || null,
        pageUrl: await getActiveTabUrl(),
      });
      setIsSuccess(true);
      setContent('');
      setContact('');
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, text.errorFallback));
    } finally {
      setIsSubmitting(false);
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-40 bg-scrim/20 backdrop-blur-md"
      role="presentation"
      onMouseDown={() => {
        if (!isSubmitting) onClose();
      }}
    >
      <div
        className="absolute left-3 right-3 top-[72px] max-h-[calc(100vh-88px)] overflow-y-auto rounded-xl border border-text-secondary/[0.16] glass-popover bg-surface-lowest/62 p-3 shadow-[0_22px_60px_rgba(0,0,0,0.5),inset_0_1px_0_rgba(255,255,255,0.12)] backdrop-blur-3xl"
        role="dialog"
        aria-modal="true"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-2">
          <div>
            <p className="text-[9px] font-bold uppercase tracking-[0.2em] text-action-accent">Feedback</p>
            <h2 className="mt-0.5 text-sm font-bold text-text-primary">{text.title}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-md text-text-secondary transition hover:bg-text-primary/10 hover:text-text-primary"
            aria-label="Close feedback"
          >
            <X size={15} />
          </button>
        </div>

        {isSuccess ? (
          <div className="mt-3 rounded-lg border border-action-accent/20 bg-action-accent/10 p-3 text-center">
            <CheckCircle2 className="mx-auto text-action-accent" size={22} />
            <p className="mt-2 text-xs font-semibold text-text-primary">{text.success}</p>
            <button
              type="button"
              onClick={onClose}
              className="mt-3 h-8 rounded-md bg-action-accent px-3 text-xs font-bold text-text-on-accent transition hover:bg-action-accent/90"
            >
              {text.close}
            </button>
          </div>
        ) : (
          <form className="mt-3 flex flex-col gap-3" onSubmit={handleSubmit}>
            <div className="grid gap-1.5">
              <span className="text-[10px] font-bold text-text-secondary">카테고리 선택</span>
              <div className="grid grid-cols-4 gap-1.5">
                {feedbackTypes.map((item) => {
                  const Icon = item.icon;
                  const isSelected = item.type === type;
                  return (
                    <button
                      key={item.type}
                      type="button"
                      onClick={() => setType(item.type)}
                      className={[
                        'flex h-12 flex-col items-center justify-center gap-1 rounded-lg border text-[10px] font-bold backdrop-blur-xl transition',
                        isSelected
                          ? item.activeClassName
                          : 'border-text-secondary/10 glass-popover bg-surface-lowest/75 text-text-secondary shadow-[inset_0_1px_0_rgba(255,255,255,0.05)] hover:border-text-secondary/20 hover:bg-surface-container/90 bg-surface-container/80 hover:text-text-primary',
                      ].join(' ')}
                    >
                      <span className={['flex h-5 w-5 items-center justify-center rounded-md', isSelected ? item.iconClassName : 'glass-card bg-surface-container/80 text-text-secondary'].join(' ')}>
                        <Icon size={13} strokeWidth={1.8} />
                      </span>
                      {item.label}
                    </button>
                  );
                })}
              </div>
            </div>

            <label className="grid gap-1.5">
              <span className="text-[10px] font-bold text-text-secondary">내용</span>
              <textarea
                value={content}
                onChange={(event) => setContent(event.target.value)}
                maxLength={5000}
                rows={4}
                placeholder={text.placeholder}
                className="min-h-[104px] resize-none rounded-lg border border-text-secondary/10 glass-card bg-surface-container/60 p-3 text-xs leading-relaxed text-text-primary shadow-[inset_0_1px_0_rgba(255,255,255,0.04)] outline-none transition placeholder:text-text-secondary/55 focus:border-primary-signal/45 focus:bg-surface-container/90 bg-surface-container/70"
              />
            </label>

            <label className="grid gap-1.5">
              <span className="text-[10px] font-bold text-text-secondary">{text.contactLabel}</span>
              <input
                value={contact}
                onChange={(event) => setContact(event.target.value)}
                maxLength={255}
                placeholder={text.contactPlaceholder}
                className="h-9 rounded-lg border border-text-secondary/10 glass-card bg-surface-container/60 px-3 text-xs text-text-primary shadow-[inset_0_1px_0_rgba(255,255,255,0.04)] outline-none transition placeholder:text-text-secondary/50 focus:border-primary-signal/45 focus:bg-surface-container/90 bg-surface-container/70"
              />
            </label>

            {errorMessage && (
              <p className="rounded-md border border-red-400/20 bg-red-500/10 px-2 py-1.5 text-[11px] text-red-200">
                {errorMessage}
              </p>
            )}

            <div className="flex items-center justify-end gap-1.5 border-t border-text-secondary/5 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="h-8 rounded-md border border-text-secondary/10 px-3 text-[11px] font-bold text-text-secondary transition hover:bg-surface-container/90 bg-surface-container/80 hover:text-text-primary"
              >
                {text.cancel}
              </button>
              <button
                type="submit"
                disabled={!content.trim() || isSubmitting}
                className="flex h-8 min-w-16 items-center justify-center rounded-md bg-action-accent px-3 text-[11px] font-bold text-text-on-accent transition hover:bg-action-accent/90 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isSubmitting ? <Loader2 size={13} className="animate-spin" /> : text.send}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>,
    document.body,
  );
}
