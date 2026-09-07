import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Download, Menu, User, X } from 'lucide-react';
import { authTokenStorage } from '@dashboard/api/client';
import { SearchBar } from '../search/SearchBar';
import { ThemeToggle } from '@san/ui';
import { getExtensionInstallUrl } from '../../utils/extensionInstallUrl';
import githubSvg from '@ui/assets/icons/github.svg';
import sanLogoSvg from '@ui/assets/brand/SAN_LOGO.svg';
import sanTypoSvg from '@ui/assets/brand/SAN_TYPO.svg';

interface TopNavBarProps {
  activeMenu?: string;
  searchPlaceholder?: string;
  userAvatarUrl?: string;
  onSettingsClick?: () => void;
}

export function TopNavBar({
  searchPlaceholder = '키워드로 검색...',
  userAvatarUrl,
  activeMenu,
  onSettingsClick,
}: TopNavBarProps) {
  const navigate = useNavigate();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const extensionInstallUrl = getExtensionInstallUrl();

  useEffect(() => {
    let ignore = false;

    const refreshAuthState = async () => {
      const token = await authTokenStorage.getToken();
      if (!ignore) {
        setIsAuthenticated(Boolean(token));
      }
    };

    void refreshAuthState();

    const handleStorageChange = () => {
      void refreshAuthState();
    };

    window.addEventListener('storage', handleStorageChange);
    return () => {
      ignore = true;
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  const handleSearch = (keyword: string) => {
    const params = new URLSearchParams();
    if (keyword) {
      params.set('query', keyword);
    }
    navigate(params.toString() ? `/archive?${params.toString()}` : '/archive');
    setIsMenuOpen(false);
  };

  const handleUserClick = async () => {
    const token = await authTokenStorage.getToken();
    navigate(token ? '/profile' : '/login');
    setIsMenuOpen(false);
  };

  const handleGithubClick = () => {
    onSettingsClick?.();
    navigate('/settings/integrations');
    setIsMenuOpen(false);
  };

  const actionButtons = (
    <>
      <ThemeToggle iconSize={22} strokeWidth={1.5} />

      {isAuthenticated && extensionInstallUrl ? (
        <a
          href={extensionInstallUrl}
          target="_blank"
          rel="noreferrer"
          aria-label="SAN extension install"
          title="SAN 익스텐션 설치"
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-text-primary/5 hover:text-action-accent"
        >
          <Download size={21} strokeWidth={1.6} />
        </a>
      ) : null}

      <button
        onClick={handleGithubClick}
        aria-label="GitHub settings"
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-text-primary/5 hover:text-text-primary"
      >
        <img
          src={githubSvg}
          alt=""
          aria-hidden="true"
          className="gnb-github-icon h-[22px] w-[22px] opacity-70 transition hover:opacity-100"
        />
      </button>

      <button
        onClick={handleUserClick}
        aria-label="User profile"
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-text-secondary transition hover:bg-text-primary/5 hover:text-text-primary"
      >
        {userAvatarUrl && isAuthenticated ? (
          <img src={userAvatarUrl} alt="" className="h-full w-full rounded-full object-cover" />
        ) : (
          <User size={22} strokeWidth={1.5} />
        )}
      </button>
    </>
  );

  const navActions = (
    <>
      <SearchBar placeholder={searchPlaceholder} onSearch={handleSearch} />
      {actionButtons}
    </>
  );

  return (
    <header className="flex w-full items-center justify-between border-b border-action-accent/10 glass-popover bg-surface-lowest/82 px-4 py-4 backdrop-blur-2xl md:px-8">
      {/* Left side */}
      <div className="flex items-center gap-10">
        <button
          type="button"
          onClick={() => navigate('/')}
          className="flex items-center gap-2.5 transition hover:opacity-80"
        >
          <img src={sanLogoSvg} alt="SAN Logo" className="h-[26px] w-[26px]" />
          <img src={sanTypoSvg} alt="SAN" className="h-[15px]" />
        </button>

        <div className="hidden items-center gap-8 lg:flex">
          <button
            onClick={() => navigate('/')}
            className={`text-[17px] font-medium tracking-wide transition hover:text-text-primary ${activeMenu === 'Dashboard' ? 'text-action-accent' : 'text-text-secondary'}`}
          >
            Home
          </button>
          <button
            onClick={() => navigate('/til')}
            className={`text-[17px] font-medium tracking-wide transition hover:text-text-primary ${activeMenu === 'TIL' ? 'text-action-accent' : 'text-text-secondary'}`}
          >
            TIL
          </button>
          <button
            onClick={() => navigate('/archive')}
            className={`text-[17px] font-medium tracking-wide transition hover:text-text-primary ${activeMenu === 'Search' ? 'text-action-accent' : 'text-text-secondary'}`}
          >
            Archive
          </button>
        </div>
      </div>

      {/* Right side Desktop */}
      <div className="hidden items-center gap-5 lg:flex">
        {navActions}
      </div>

      {/* Mobile Menu Toggle */}
      <div className="flex lg:hidden">
        <button
          type="button"
          onClick={() => setIsMenuOpen((current) => !current)}
          className="p-2 text-text-secondary transition hover:text-text-primary"
        >
          {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* Mobile Menu Content */}
      {isMenuOpen && (
        <div className="absolute left-0 top-full flex w-full flex-col items-end gap-4 border-b border-action-accent/10 bg-surface-lowest p-4 lg:hidden">
          <button onClick={() => { navigate('/'); setIsMenuOpen(false); }} className={`text-right text-lg font-medium ${activeMenu === 'Dashboard' ? 'text-action-accent' : 'text-text-secondary'}`}>Home</button>
          <button onClick={() => { navigate('/til'); setIsMenuOpen(false); }} className={`text-right text-lg font-medium ${activeMenu === 'TIL' ? 'text-action-accent' : 'text-text-secondary'}`}>TIL</button>
          <button onClick={() => { navigate('/archive'); setIsMenuOpen(false); }} className={`text-right text-lg font-medium ${activeMenu === 'Search' ? 'text-action-accent' : 'text-text-secondary'}`}>Archive</button>
          <div className="mt-4 flex w-full min-w-0 flex-col gap-3 border-t border-text-secondary/5 pt-4">
            <SearchBar
              placeholder={searchPlaceholder}
              onSearch={handleSearch}
              className="w-full min-w-0"
            />
            <div className="flex items-center justify-end gap-2">
              {actionButtons}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
