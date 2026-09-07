import {
  Bold,
  Italic,
  Link,
  List,
  RotateCcw,
  Play,
  GitBranch,
} from 'lucide-react';
import type { ReactNode } from 'react';
import { CurvedButton } from '@san/ui';

export function TILToolbar({
  onReset,
  onGenerate,
  onCommit,
  onFormat,
  isGenerating,
  isCommitting,
}: {
  onReset?: () => void;
  onGenerate?: () => void;
  onCommit?: () => void;
  onFormat?: (action: 'bold' | 'italic' | 'list' | 'link') => void;
  isGenerating?: boolean;
  isCommitting?: boolean;
}) {
  return (
    <div className="glass-panel flex h-12 items-center justify-between bg-surface-container/90 px-lg backdrop-blur-md">
      <div className="flex items-center gap-xs border-r border-text-secondary/20 pr-md">
        <ToolbarButton label="Bold" onClick={() => onFormat?.('bold')}>
          <Bold size={20} />
        </ToolbarButton>
        <ToolbarButton label="Italic" onClick={() => onFormat?.('italic')}>
          <Italic size={20} />
        </ToolbarButton>
        <ToolbarButton label="List" onClick={() => onFormat?.('list')}>
          <List size={20} />
        </ToolbarButton>
        <ToolbarButton label="Link" onClick={() => onFormat?.('link')}>
          <Link size={20} />
        </ToolbarButton>

        <span className="ml-sm text-caption text-text-secondary">UTF-8</span>
      </div>

      <div className="flex items-center gap-sm">
        <CurvedButton
          onClick={onGenerate}
          disabled={isGenerating}
          tone="subtle"
          size="sm"
          leadingIcon={<Play size={20} className="text-action-accent" />}
        >
          {isGenerating ? 'Generating...' : 'Generate'}
        </CurvedButton>

        <CurvedButton
          onClick={onCommit}
          disabled={isCommitting}
          tone="subtle"
          size="sm"
          leadingIcon={<GitBranch size={20} className="text-action-accent" />}
        >
          {isCommitting ? 'Committing...' : 'Commit'}
        </CurvedButton>

        <CurvedButton
          onClick={onReset}
          tone="ghost"
          size="sm"
          leadingIcon={<RotateCcw size={20} className="text-action-accent" />}
        >
          Reset
        </CurvedButton>
      </div>
    </div>
  );
}

function ToolbarButton({
  label,
  children,
  onClick,
}: {
  label: string;
  children: ReactNode;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className="flex h-8 w-8 items-center justify-center rounded-leaf text-text-secondary transition hover:bg-surface-container hover:text-action-accent"
    >
      {children}
    </button>
  );
}
