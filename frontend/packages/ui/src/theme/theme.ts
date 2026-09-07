export type ThemeMode = 'dark' | 'light';

const THEME_STORAGE_KEY = 'san-theme';

function getWindow(): Window | undefined {
  return typeof window === 'undefined' ? undefined : window;
}

function getPreferredTheme(win: Window): ThemeMode {
  return win.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
}

export function getStoredTheme(): ThemeMode | null {
  const win = getWindow();
  if (!win) return null;

  const value = win.localStorage.getItem(THEME_STORAGE_KEY);
  return value === 'light' || value === 'dark' ? value : null;
}

export function getResolvedTheme(): ThemeMode {
  const win = getWindow();
  if (!win) return 'dark';

  return getStoredTheme() ?? getPreferredTheme(win);
}

export function applyTheme(theme: ThemeMode) {
  const win = getWindow();
  if (!win) return;

  win.document.documentElement.dataset.theme = theme;
  win.document.documentElement.style.colorScheme = theme;
}

export function initTheme() {
  applyTheme(getResolvedTheme());
}

export function setTheme(theme: ThemeMode) {
  const win = getWindow();
  if (!win) return;

  win.localStorage.setItem(THEME_STORAGE_KEY, theme);
  applyTheme(theme);
}

export function toggleTheme() {
  const current = getResolvedTheme();
  const next: ThemeMode = current === 'dark' ? 'light' : 'dark';
  setTheme(next);
  return next;
}
