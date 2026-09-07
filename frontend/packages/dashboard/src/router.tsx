import { createBrowserRouter, Navigate } from 'react-router-dom';
import { MainLayout } from './layouts/MainLayout';
import { LoginPage } from './auth/pages/LoginPage';
import { HomePage } from './main/pages/HomePage';
import { Signup } from './auth/pages/SignUpPage';
import { ArchiveCategoryPage } from './main/pages/ArchiveCategoryPage';
import { ResultPage } from './github/pages/ResultPage';
import { GithubAuthResultPage } from './auth/pages/GithubAuthResultPage';
import { DashboardBridgeLoginPage } from './auth/DashboardBridgeLoginPage';
import { SettingsIntegrationsPage } from './main/pages/SettingsIntegrationsPage';
import { TilPage } from './til/pages/TilPage';
import { ProfilePage } from './main/pages/ProfilePage';
import { GithubStarImportPage } from './main/pages/GithubStarImportPage';
import { NotFoundPage } from './main/pages/NotFoundPage';
import { AuthGate } from './auth/components/AuthGate';
import { KnowledgeCardDetailPage } from './cards/pages/KnowledgeCardDetailPage';
import { LegacyResultRedirect } from './LegacyResultRedirect';

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/signup',
    element: <Signup />,
  },
  {
    path: '/auth/github/callback',
    element: <GithubAuthResultPage />,
  },
  {
    path: '/auth/github/success',
    element: <GithubAuthResultPage />,
  },
  {
    path: '/auth/github/failure',
    element: <GithubAuthResultPage />,
  },
  {
    path: '/auth/bridge/dashboard',
    element: <DashboardBridgeLoginPage />,
  },
  {
    element: <MainLayout />,
    children: [
      {
        path: '/',
        element: <HomePage />,
      },
      {
        path: '/archive',
        element: (
          <AuthGate>
            <ResultPage />
          </AuthGate>
        ),
      },
      {
        path: '/archive/:categoryId',
        element: (
          <AuthGate>
            <ArchiveCategoryPage />
          </AuthGate>
        ),
      },
      {
        path: '/result',
        element: <LegacyResultRedirect />,
      },
      {
        path: '/til',
        element: (
          <AuthGate>
            <TilPage />
          </AuthGate>
        ),
      },
      {
        path: '/cards/:cardId',
        element: (
          <AuthGate>
            <KnowledgeCardDetailPage />
          </AuthGate>
        ),
      },
      {
        path: '/profile',
        element: (
          <AuthGate>
            <ProfilePage />
          </AuthGate>
        ),
      },
      {
        path: '/profile/stars',
        element: (
          <AuthGate>
            <GithubStarImportPage />
          </AuthGate>
        ),
      },
      {
        path: '/settings',
        element: (
          <AuthGate>
            <SettingsIntegrationsPage />
          </AuthGate>
        ),
      },
      {
        path: '/settings/integrations',
        element: (
          <AuthGate>
            <SettingsIntegrationsPage />
          </AuthGate>
        ),
      },
      {
        path: '/settings/repositories',
        element: <Navigate to="/settings/integrations" replace />,
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
