export type EmptyStateType = 'recall' | 'archive' | 'search' | 'til' | 'scrap' | 'custom';

interface ActionButton {
  label: string;
  onClick: () => void;
}

export interface EmptyStateProps {
  type?: EmptyStateType;
  title?: string;
  description?: string;
  primaryAction?: ActionButton;
  secondaryAction?: ActionButton;
  variant?: 'full' | 'inline';
}

const EMPTY_PRESET: Record<EmptyStateType, { title: string; description: string }> = {
  recall: {
    title: '아직 회상할 카드가 없어요',
    description: '저장한 지식이 쌓이면 다시 살펴볼 수 있어요.',
  },
  archive: {
    title: '저장된 카드가 없어요',
    description: '스크랩을 저장하면 이곳에서 지식 카드를 확인할 수 있어요.',
  },
  search: {
    title: '검색 결과가 없어요',
    description: '검색어를 바꾸거나 필터를 조정해 보세요.',
  },
  til: {
    title: '작성된 TIL이 없어요',
    description: '오늘 학습한 내용을 저장하면 TIL을 만들 수 있어요.',
  },
  scrap: {
    title: '저장된 스크랩이 없어요',
    description: '웹에서 텍스트, 이미지, 링크를 저장해 보세요.',
  },
  custom: {
    title: '표시할 내용이 없어요',
    description: '조건을 바꾸거나 새 항목을 추가해 보세요.',
  },
};

function EmptyIcon({ type, size = 40 }: { type: EmptyStateType; size?: number }) {
  const color = '#00ffc2';
  const muted = '#1e5056';

  if (type === 'recall') {
    return (
      <svg width={size} height={size} viewBox="0 0 40 40" fill="none" aria-hidden="true">
        <path
          d="M20 4C12 4 6 12 6 20c0 5 2.5 9 6 12v4h16v-4c3.5-3 6-7 6-12 0-8-6-16-14-16z"
          fill={muted}
          opacity="0.4"
        />
        <path d="M20 8C14 8 10 14 10 20c0 4 1.5 7 4 9.5" stroke={color} strokeLinecap="round" strokeWidth="1.5" />
        <circle cx="20" cy="20" r="3" fill={color} opacity="0.6" />
        <path d="M20 14v6l4 2" stroke={color} strokeLinecap="round" strokeWidth="1.5" />
      </svg>
    );
  }

  if (type === 'archive') {
    return (
      <svg width={size} height={size} viewBox="0 0 40 40" fill="none" aria-hidden="true">
        <rect x="6" y="10" width="28" height="22" rx="4" fill={muted} opacity="0.3" />
        <rect x="6" y="6" width="28" height="8" rx="2" stroke={color} strokeWidth="1.5" />
        <path d="M15 22h10M15 27h7" stroke={color} strokeLinecap="round" strokeWidth="1.5" opacity="0.6" />
        <circle cx="32" cy="32" r="6" fill={muted} opacity="0.4" />
        <path d="M30 32h4M32 30v4" stroke={color} strokeLinecap="round" strokeWidth="1.5" />
      </svg>
    );
  }

  if (type === 'search') {
    return (
      <svg width={size} height={size} viewBox="0 0 40 40" fill="none" aria-hidden="true">
        <circle cx="18" cy="18" r="11" fill={muted} opacity="0.3" />
        <circle cx="18" cy="18" r="11" stroke={color} strokeWidth="1.5" />
        <path d="M26 26l7 7" stroke={color} strokeLinecap="round" strokeWidth="2" />
        <path d="M14 18h8M18 14v8" stroke={color} strokeLinecap="round" strokeWidth="1.5" opacity="0.5" />
      </svg>
    );
  }

  if (type === 'til') {
    return (
      <svg width={size} height={size} viewBox="0 0 40 40" fill="none" aria-hidden="true">
        <rect x="8" y="6" width="24" height="30" rx="3" fill={muted} opacity="0.3" />
        <rect x="8" y="6" width="24" height="30" rx="3" stroke={color} strokeWidth="1.5" />
        <path d="M14 14h12M14 20h12M14 26h8" stroke={color} strokeLinecap="round" strokeWidth="1.5" opacity="0.6" />
        <circle cx="30" cy="30" r="7" fill="#101417" stroke={color} strokeWidth="1.5" />
        <path d="M27.5 30l2 2 3-3" stroke={color} strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" />
      </svg>
    );
  }

  if (type === 'scrap') {
    return (
      <svg width={size} height={size} viewBox="0 0 40 40" fill="none" aria-hidden="true">
        <path
          d="M20 6l3.5 7 7.5 1-5.5 5.5 1.5 7.5L20 24l-7 3 1.5-7.5L9 14l7.5-1z"
          fill={muted}
          opacity="0.3"
          stroke={color}
          strokeLinejoin="round"
          strokeWidth="1.5"
        />
        <path d="M20 26v8M16 34h8" stroke={color} strokeLinecap="round" strokeWidth="1.5" opacity="0.5" />
      </svg>
    );
  }

  return (
    <svg width={size} height={size} viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <path
        d="M20 6C12 6 6 13 8 22c2 8 10 12 16 10 4-1.5 8-6 8-12 0-8-5-14-12-14z"
        fill={muted}
        opacity="0.4"
        stroke={color}
        strokeWidth="1.5"
      />
      <path d="M20 34V20M14 26l6-6 6 6" stroke={color} strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" />
    </svg>
  );
}

function IconCard({ type }: { type: EmptyStateType }) {
  return (
    <div className="relative">
      <div className="flex h-48 w-48 items-center justify-center rounded-[32px] border border-[#1e5056]/30 bg-[#1e5056]/20 shadow-[0_0_60px_rgba(0,255,194,0.05)]">
        <EmptyIcon type={type} size={64} />
      </div>
      <div className="absolute right-6 top-4 h-1.5 w-1.5 rounded-full bg-[#00ffc2]/40" />
      <div className="absolute bottom-8 left-4 h-1 w-1 rounded-full bg-[#00ffc2]/30" />
      <div className="absolute left-2 top-12 h-1 w-1 rounded-full bg-[#1e5056]/60" />
      <div className="absolute bottom-4 right-2 h-1.5 w-1.5 rounded-full bg-[#1e5056]/40" />
    </div>
  );
}

export function EmptyState({
  type = 'archive',
  title,
  description,
  primaryAction,
  secondaryAction,
  variant = 'inline',
}: EmptyStateProps) {
  const preset = EMPTY_PRESET[type];
  const displayTitle = title ?? preset.title;
  const displayDescription = description ?? preset.description;

  if (variant === 'full') {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-8 px-8">
        <IconCard type={type} />

        <div className="flex max-w-sm flex-col items-center gap-3 text-center">
          <h3 className="text-xl font-black text-[#fbfffa]">{displayTitle}</h3>
          <p className="whitespace-pre-line text-sm leading-relaxed text-[#83958c]">{displayDescription}</p>
        </div>

        {(primaryAction || secondaryAction) ? (
          <div className="flex flex-col items-center gap-3">
            {primaryAction ? (
              <button
                type="button"
                onClick={primaryAction.onClick}
                className="rounded-full bg-[#00ffc2] px-8 py-3 text-sm font-bold text-[#101417] shadow-[0_0_20px_rgba(0,255,194,0.3)] transition-all duration-200 hover:bg-[#00ffc2]/90"
              >
                {primaryAction.label}
              </button>
            ) : null}
            {secondaryAction ? (
              <button
                type="button"
                onClick={secondaryAction.onClick}
                className="flex items-center gap-1 text-xs uppercase tracking-widest text-[#83958c] transition-colors duration-200 hover:text-[#b9cbc1]"
              >
                {secondaryAction.label}
              </button>
            ) : null}
          </div>
        ) : null}
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center gap-4 rounded-leaf border border-[#1e5056]/20 bg-[#181c1f]/40 px-6 py-10 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl border border-[#1e5056]/30 bg-[#1e5056]/20">
        <EmptyIcon type={type} size={28} />
      </div>

      <div className="flex flex-col gap-1.5">
        <p className="text-sm font-semibold text-[#fbfffa]">{displayTitle}</p>
        <p className="whitespace-pre-line text-xs leading-relaxed text-[#83958c]">{displayDescription}</p>
      </div>

      {primaryAction ? (
        <button
          type="button"
          onClick={primaryAction.onClick}
          className="rounded-full border border-[#00ffc2]/30 bg-[#00ffc2]/10 px-5 py-2 text-xs font-semibold text-[#00ffc2] transition-all duration-200 hover:bg-[#00ffc2]/20"
        >
          {primaryAction.label}
        </button>
      ) : null}
    </div>
  );
}
