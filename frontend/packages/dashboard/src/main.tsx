// packages/dashboard/src/main.tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ApiProvider } from '@san/shared';
import { initTheme } from '@san/ui';
import { scrapsApi, cardsApi, archiveApi } from './api/client';
import { router } from './router';
import { ToastProvider } from './components/shared/toast/ToastProvider';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30,
      retry: 1,
    },
  },
});

initTheme();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ApiProvider scrapsApi={scrapsApi} cardsApi={cardsApi} archiveApi={archiveApi}>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </ApiProvider>
    </QueryClientProvider>
  </StrictMode>
);
