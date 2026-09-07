import { Outlet, useLocation } from 'react-router-dom';
import { TopNavBar } from '@dashboard/main/components/layout/GNB';
import { FloatingFeedbackButton } from '@dashboard/main/components/feedback/FloatingFeedbackButton';

export function MainLayout() {
  const location = useLocation();
  const activeMenu = getActiveMenu(location.pathname);

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden bg-background">
      <header className="fixed left-0 top-0 z-[2000] w-full">
        <TopNavBar activeMenu={activeMenu} />
      </header>

      <main className="dashboard-shell pt-[var(--dashboard-nav-offset)] pb-16">
        <Outlet />
      </main>
      <FloatingFeedbackButton />
    </div>
  );
}

function getActiveMenu(pathname: string) {
  if (pathname.startsWith('/til')) return 'TIL';
  if (pathname.startsWith('/archive') || pathname.startsWith('/result')) return 'Search';
  if (pathname.startsWith('/settings')) return 'GitHub';
  if (pathname.startsWith('/profile')) return 'Profile';
  return 'Dashboard';
}

