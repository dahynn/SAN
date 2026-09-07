// 로그인 성공 후 토큰 처리 분기 파일
// - dashboard localStorage 구현을 직접 알아야 함
// - extension 진입 로그인은 syncExtensionAuth로 extension에 토큰을 저장함
// - dashboard 로그인은 bridge ticket으로 extension 세션을 별도 발급함
// - extension 진입 로그인일 때 dashboard 저장소를 비우는 화면별 정책을 담고 있음
import type { AuthTokens, ClientType } from '@san/shared';
import { authApi } from '@dashboard/api/client';
import { syncExtensionAuth, syncExtensionBridgeTicket } from '@dashboard/api/extensionAuth';
import { authTokenStorage } from '@dashboard/api/tokenStorage';

export async function completeAuth(tokens: AuthTokens, clientType: ClientType, username?: string) {
  const scopedTokens = { ...tokens, clientType };

  if (clientType === 'EXTENSION') {
    await syncExtensionAuth(scopedTokens);
    return;
  }

  await authTokenStorage.setTokens(scopedTokens);
  if (username) {
    await authTokenStorage.setUsername(username);
  }

  void syncDashboardBridgeAuth();
}

async function syncDashboardBridgeAuth() {
  try {
    const { ticket } = await authApi.createBridgeTicket();
    await syncExtensionBridgeTicket(ticket);
  } catch (error) {
    console.info('[SAN:extension-auth] dashboard login extension bridge skipped', error);
  }
}
