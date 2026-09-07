export type TILMode = 'drafts' | 'edit' | 'preview';

interface TILModeTabsProps {
  activeTab: TILMode;
  onChange?: (tab: TILMode) => void;
}

const tabs: Array<{ key: TILMode; label: string }> = [
  { key: 'drafts', label: 'Drafts' },
  { key: 'edit', label: 'Edit' },
  { key: 'preview', label: 'Preview' },
];

export function TILModeTabs({ activeTab, onChange }: TILModeTabsProps) {
  return (
    <nav className="grid h-10 min-w-0 flex-1 grid-cols-3 items-center gap-1 rounded-md border border-text-secondary/10 bg-text-primary/[0.035] p-1 md:min-w-[252px] md:flex-none">
      {tabs.map((tab) => {
        const active = tab.key === activeTab;

        return (
          <button
            key={tab.key}
            type="button"
            onClick={() => onChange?.(tab.key)}
            className={[
              'flex h-[30px] w-full min-w-0 items-center justify-center rounded px-3 text-sm font-semibold transition-colors sm:min-w-[78px]',
              active
                ? 'bg-action-accent text-text-on-accent'
                : 'text-text-secondary hover:bg-text-primary/[0.06] hover:text-text-primary',
            ].join(' ')}
          >
            {tab.label}
          </button>
        );
      })}
    </nav>
  );
}
