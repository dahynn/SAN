import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ErrorFallback } from '@san/ui';
import { authApi, authTokenStorage } from '@dashboard/api/client';

function getSafeRedirect(value: string | null) {
  if (!value || !value.startsWith('/') || value.startsWith('//')) {
    return '/';
  }

  return value;
}

export function DashboardBridgeLoginPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const exchangeStartedRef = useRef(false);
  const ticket = searchParams.get('ticket');
  const redirectTo = getSafeRedirect(searchParams.get('redirect'));
  const displayErrorMessage = errorMessage ?? (ticket ? null : 'Missing dashboard login ticket.');

  useEffect(() => {
    let ignore = false;

    if (!ticket) {
      return;
    }

    if (exchangeStartedRef.current) {
      return;
    }
    exchangeStartedRef.current = true;

    authApi
      .exchangeDashboardBridgeToken({ ticket })
      .then(async (tokens) => {
        if (ignore) return;
        await authTokenStorage.setTokens({ ...tokens, clientType: 'DASHBOARD' });
        navigate(redirectTo, { replace: true });
      })
      .catch((error: unknown) => {
        if (ignore) return;
        setErrorMessage(error instanceof Error ? error.message : 'Dashboard login failed.');
      });

    return () => {
      ignore = true;
    };
  }, [navigate, redirectTo, ticket]);

  if (displayErrorMessage) {
    return (
      <div className="grid min-h-screen place-items-center bg-background px-6 py-10">
        <ErrorFallback
          type="auth"
          variant="full"
          message="Dashboard login failed"
          description={displayErrorMessage}
          actionLabel="Go to login"
          onRetry={() => navigate('/login', { replace: true })}
        />
      </div>
    );
  }

  return (
    <div className="grid min-h-screen place-items-center bg-background">
      <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary-signal/20 border-t-primary-signal" />
    </div>
  );
}
