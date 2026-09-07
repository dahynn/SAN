import { type ReactNode, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ErrorFallback } from '@san/ui';
import { authTokenStorage } from '../../api/client';

interface AuthGateProps {
  children: ReactNode;
}

export function AuthGate({ children }: AuthGateProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const [isChecking, setIsChecking] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    let ignore = false;

    authTokenStorage.getToken()
      .then((token) => {
        if (ignore) return;
        setIsAuthenticated(Boolean(token));
      })
      .finally(() => {
        if (ignore) return;
        setIsChecking(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  if (isChecking) {
    return (
      <div className="grid min-h-[400px] place-items-center">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary-signal/20 border-t-primary-signal" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="grid min-h-[calc(100vh-12rem)] place-items-center py-dashboard-gap">
        <ErrorFallback
          type="auth"
          variant="full"
          onRetry={() => navigate('/login', { state: { from: location.pathname } })}
        />
      </div>
    );
  }

  return <>{children}</>;
}
