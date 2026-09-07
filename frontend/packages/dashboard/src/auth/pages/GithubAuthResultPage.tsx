import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { getApiErrorMessage } from '@san/shared';
import { githubAuthApi } from '../../api/client';
import { consumeRememberedAuthClientType, getAuthClientType, withAuthClientType } from '../lib/clientType';
import { completeAuth } from '../lib/completeAuth';
import { consumeRememberedGithubLinkAuthFlow } from '../lib/githubAuthFlow';

const GITHUB_AUTH_ERROR_MESSAGE: Record<string, string> = {
  A201: 'GitHub authentication failed. Please try again.',
  A204: '이미 다른 계정에 연결된 GitHub 계정입니다.',
  A206: '현재 연동된 GitHub 계정이 존재합니다.',
  C003: 'Authentication is required. Please log in again.',
};

const GITHUB_LINK_ERROR_CODES = new Set(['A202', 'A204', 'A205', 'A206']);

export function GithubAuthResultPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [exchangeErrorMessage, setExchangeErrorMessage] = useState<string | null>(null);
  const [rememberedClientType] = useState(consumeRememberedAuthClientType);
  const [isRememberedGithubLinkAuthFlow] = useState(consumeRememberedGithubLinkAuthFlow);
  const processedAuthKeyRef = useRef<string | null>(null);

  const ticket = searchParams.get('ticket');
  const code = searchParams.get('code');
  const error = searchParams.get('error');
  const githubLinked = searchParams.get('githubLinked');
  const clientType = getAuthClientType(searchParams, rememberedClientType ?? 'DASHBOARD');

  const message = exchangeErrorMessage
    ?? (githubLinked === 'true'
      ? 'GitHub account connected'
      : error
        ? (GITHUB_AUTH_ERROR_MESSAGE[error] ?? `GitHub authentication failed (${error})`)
        : !ticket && !code
          ? 'GitHub authentication ticket is missing'
          : 'Connecting GitHub account...');

  useEffect(() => {
    if (githubLinked === 'true') {
      navigate('/settings/integrations', { replace: true });
      return;
    }

    if (error) {
      if (GITHUB_LINK_ERROR_CODES.has(error) || (isRememberedGithubLinkAuthFlow && error === 'A201')) {
        navigate(`/settings/integrations?githubError=${encodeURIComponent(error)}`, { replace: true });
        return;
      }

      navigate(withAuthClientType('/login', clientType), {
        replace: true,
        state: { authError: GITHUB_AUTH_ERROR_MESSAGE[error] ?? `GitHub authentication failed (${error})` },
      });
      return;
    }

    if (!ticket && !code) {
      return;
    }

    const authKey = ticket ? `ticket:${ticket}` : `code:${code}`;
    if (processedAuthKeyRef.current === authKey) {
      return;
    }
    processedAuthKeyRef.current = authKey;

    let ignore = false;

    const tokenRequest = ticket
      ? githubAuthApi.exchangeGithubToken({ ticket })
      : githubAuthApi.loginWithGithubCode({ code: code as string, clientType });

    tokenRequest
      .then(async (tokens) => {
        if (ignore) return;
        await completeAuth(tokens, clientType);
        navigate(clientType === 'EXTENSION' ? '/' : '/settings/integrations', { replace: true });
      })
      .catch((exchangeError) => {
        if (ignore) return;
        setExchangeErrorMessage(getApiErrorMessage(exchangeError, 'GitHub authentication failed'));
      });

    return () => {
      ignore = true;
    };
  }, [clientType, code, error, githubLinked, isRememberedGithubLinkAuthFlow, navigate, ticket]);

  return (
    <main className="auth-shell grid min-h-screen w-full place-items-center overflow-x-hidden bg-background px-lg text-text-primary">
      <section className="grid w-full max-w-md gap-sm rounded-leaf border border-text-secondary/20 glass-card bg-surface-container/90 p-xl text-center shadow-neon-sm backdrop-blur-xl">
        <p className="text-caption-bold uppercase tracking-wide text-primary-signal">
          GitHub Auth
        </p>
        <h1 className="text-h2-bold">Authentication</h1>
        <p className="text-body-sm text-text-secondary">{message}</p>
        <Link
          to={withAuthClientType('/login', clientType)}
          className="mt-lg inline-flex min-h-11 items-center justify-center rounded-leaf bg-action-accent px-lg text-body-sm-bold text-background transition hover:bg-action-accent-hover"
        >
          Back to login
        </Link>
      </section>
    </main>
  );
}
