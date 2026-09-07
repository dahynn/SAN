const GITHUB_AUTH_FLOW_STORAGE_KEY = 'san:github-auth-flow';
const GITHUB_LINK_AUTH_FLOW = 'LINK';

export function rememberGithubLinkAuthFlow() {
  sessionStorage.setItem(GITHUB_AUTH_FLOW_STORAGE_KEY, GITHUB_LINK_AUTH_FLOW);
}

export function consumeRememberedGithubLinkAuthFlow() {
  const flow = sessionStorage.getItem(GITHUB_AUTH_FLOW_STORAGE_KEY);
  sessionStorage.removeItem(GITHUB_AUTH_FLOW_STORAGE_KEY);
  return flow === GITHUB_LINK_AUTH_FLOW;
}
