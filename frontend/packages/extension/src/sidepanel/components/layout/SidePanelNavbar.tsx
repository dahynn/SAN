import { LayoutDashboard, LogOut, MessageCircleHeart, User } from 'lucide-react';
import { useState } from 'react';
import { ThemeToggle } from '@san/ui';
import sanLogo from '@san/ui/assets/brand/SAN_LOGO.svg';
import sanTypo from '@san/ui/assets/brand/SAN_TYPO.svg';
import { FeedbackPopover } from '../feedback/FeedbackPopover';

interface SidePanelNavbarProps {
  isAuthenticated: boolean;
  isProfileMenuOpen: boolean;
  onHomeClick: () => void;
  onOpenDashboard: () => void;
  onProfileButtonClick: () => void;
  onLogoutClick: () => void;
}

export default function SidePanelNavbar({
  isAuthenticated,
  isProfileMenuOpen,
  onHomeClick,
  onOpenDashboard,
  onProfileButtonClick,
  onLogoutClick,
}: SidePanelNavbarProps) {
  const [isFeedbackOpen, setIsFeedbackOpen] = useState(false);

  return (
    <div className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-action-accent/15 glass-panel bg-background/95 px-4 backdrop-blur-md">
      <div className="flex min-w-0 flex-1 items-center">
        <button
          type="button"
          onClick={onHomeClick}
          data-tour-id="home-button"
          className="flex h-10 min-w-0 items-center gap-2.5 rounded-md transition hover:opacity-80 active:scale-[0.98]"
          aria-label="Go to extension home"
          title="Home"
        >
          <img
            src={sanLogo}
            alt=""
            className="h-7 w-7 shrink-0 object-contain"
            aria-hidden="true"
          />
          <img
            src={sanTypo}
            alt="SAN"
            className="h-5 w-[min(84px,calc(100vw-200px))] min-w-0 shrink object-contain object-left"
          />
        </button>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        <div data-tour-id="theme-toggle" className="flex h-8 w-8 items-center justify-center">
          <ThemeToggle
            iconSize={16}
            strokeWidth={1.8}
            className="extension-navbar-theme-toggle h-8 w-8 border border-action-accent/25 bg-action-accent/10 text-action-accent hover:border-action-accent/60 hover:bg-action-accent/15"
          />
        </div>

        {isAuthenticated && (
          <div className="relative flex h-8 w-8 items-center justify-center">
            <button
              type="button"
              onClick={() => setIsFeedbackOpen((current) => !current)}
              data-tour-id="feedback-button"
              className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-action-accent/25 bg-action-accent/10 text-action-accent transition hover:border-action-accent/60 hover:bg-action-accent/15 active:scale-95"
              aria-label="Feedback"
              aria-expanded={isFeedbackOpen}
              title="Feedback"
            >
              <MessageCircleHeart size={16} strokeWidth={1.8} aria-hidden="true" />
            </button>
            {isFeedbackOpen && <FeedbackPopover onClose={() => setIsFeedbackOpen(false)} />}
          </div>
        )}

        <div className="flex h-8 w-8 items-center justify-center">
          <button
            type="button"
            onClick={onOpenDashboard}
            data-tour-id="dashboard-button"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-action-accent/25 bg-action-accent/10 text-action-accent transition hover:border-action-accent/60 hover:bg-action-accent/15 active:scale-95"
            aria-label="Open dashboard"
            title="Open dashboard"
          >
            <LayoutDashboard size={16} strokeWidth={1.8} aria-hidden="true" />
          </button>
        </div>

        <div className="relative flex h-8 w-8 items-center justify-center">
          <button
            type="button"
            onClick={onProfileButtonClick}
            data-tour-id="profile-button"
            className={[
              'flex h-8 w-8 shrink-0 items-center justify-center rounded-full border transition active:scale-95',
              isAuthenticated
                ? 'border-action-accent/35 bg-action-accent/10 text-action-accent'
                : 'border-text-secondary/10 bg-surface-highest text-text-secondary hover:bg-surface-container/40',
            ].join(' ')}
            aria-label={isAuthenticated ? 'User profile' : 'Login'}
            aria-expanded={isAuthenticated ? isProfileMenuOpen : undefined}
            title={isAuthenticated ? 'User profile' : 'Login'}
          >
            <User size={16} strokeWidth={1.8} aria-hidden="true" />
          </button>

          {isAuthenticated && isProfileMenuOpen && (
            <div className="absolute right-0 top-10 w-32 overflow-hidden rounded-lg border border-text-secondary/[0.16] glass-popover bg-surface-lowest/80 p-1.5 shadow-[0_16px_36px_rgba(0,0,0,0.42),inset_0_1px_0_rgba(255,255,255,0.12)] backdrop-blur-2xl">
              <button
                type="button"
                onClick={onLogoutClick}
                className="flex h-9 w-full items-center gap-2 rounded-md px-2.5 text-left text-xs font-semibold text-text-secondary transition hover:bg-red-500/10 hover:text-red-300"
              >
                <LogOut size={14} aria-hidden="true" />
                Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
