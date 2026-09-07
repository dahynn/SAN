// packages/shared/src/context/ApiContext.tsx
import { createContext, useContext, type ReactNode } from 'react';
import type { ScrapsApi } from '../api/scraps';
import type { CardsApi } from '../api/cards';
import type { ArchiveApi } from '../api/archive';

// ----------------------------
// Context 타입 정의
// ----------------------------
interface ApiContextValue {
  scrapsApi: ScrapsApi;
  cardsApi: CardsApi;
  archiveApi: ArchiveApi;
}

const ApiContext = createContext<ApiContextValue | null>(null);

// ----------------------------
// Provider — main.tsx 최상단에서 딱 한 번 감쌈
// dashboard: createScrapsApi(dashboardClient) 주입
// extension: createScrapsApi(extensionClient) 주입
// ----------------------------
interface ApiProviderProps {
  scrapsApi: ScrapsApi;
  cardsApi: CardsApi;
  archiveApi: ArchiveApi;
  children: ReactNode;
}

export function ApiProvider({ scrapsApi, cardsApi, archiveApi, children }: ApiProviderProps) {
  return (
    <ApiContext.Provider value={{ scrapsApi, cardsApi, archiveApi }}>
      {children}
    </ApiContext.Provider>
  );
}

// ----------------------------
// 내부 훅 — hooks에서만 사용, 컴포넌트에 직접 노출 X
// Provider 없이 쓰면 명확한 에러 메시지 출력
// ----------------------------
export function useApiContext(): ApiContextValue {
  const ctx = useContext(ApiContext);
  if (!ctx) {
    throw new Error(
      '[SAN] useApiContext: ApiProvider로 감싸지 않은 곳에서 호출되었습니다.\n' +
      'dashboard: src/main.tsx, extension: src/sidepanel/main.tsx 를 확인하세요.'
    );
  }
  return ctx;
}
