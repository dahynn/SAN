import { MoonStar, SunMedium } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getResolvedTheme, toggleTheme, type ThemeMode } from '../../theme/theme';

interface ThemeToggleProps {
  className?: string;
  iconSize?: number;
  strokeWidth?: number;
}

export function ThemeToggle({ className, iconSize = 18, strokeWidth = 1.8 }: ThemeToggleProps) {
  const [theme, setTheme] = useState<ThemeMode>(() => getResolvedTheme());

  useEffect(() => {
    const handleStorage = () => {
      setTheme(getResolvedTheme());
    };

    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const handleToggle = () => {
    const next = toggleTheme();
    setTheme(next);
  };

  const isDark = theme === 'dark';

  return (
    <button
      type="button"
      onClick={handleToggle}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Light mode' : 'Dark mode'}
      className={[
        'flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-text-secondary transition',
        'hover:bg-text-primary/5 hover:text-text-primary',
        className ?? '',
      ].join(' ')}
    >
      {isDark ? (
        <SunMedium size={iconSize} strokeWidth={strokeWidth} />
      ) : (
        <MoonStar size={iconSize} strokeWidth={strokeWidth} />
      )}
    </button>
  );
}
