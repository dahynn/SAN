import { useCallback, useEffect, useMemo, useState } from 'react';
import { getS3ImageFileValidationError } from '@san/shared';
import type {
  KnowledgeCardDetailResponse,
  KnowledgeCardResponse,
  KnowledgeCardView,
  SearchCardResult,
} from '@san/shared';
import { authApi, authTokenStorage, cardsApi, searchApi } from '@extension/api/client';
import type { ExtensionMessage, PendingScrap, SavedInsight } from '@extension/types';
import { DropZone } from './components/capture/DropZone';
import GlowBackground from './components/feedback/GlowBackground';
import { KnowledgeSearchBar } from './components/knowledge/KnowledgeSearchBar';
import { RecentKnowledgeList } from './components/knowledge/RecentKnowledgeList';
import { SimilarKnowledgeList } from './components/knowledge/SimilarKnowledgeList';
import { ArchiveList } from './components/archive/ArchiveList';
import {
  loadPendingScrap,
  loadSavedInsights,
  savePendingScrap,
  useSaveScrap,
} from './hooks/useSaveScrap';
import SidePanelNavbar from './components/layout/SidePanelNavbar';
import { CreatedKnowledgeCard } from './components/knowledge/CreatedKnowledgeCard';
import { KnowledgeLoadingCard } from './components/knowledge/KnowledgeLoadingCard';
import { ExtensionAuthCard } from './components/auth/ExtensionAuthCard';
import { createLinkScrap, isHttpUrl } from '@extension/utils/scrap';
import { OnboardingTour } from './components/onboarding/OnboardingTour';

const DEBUG_PREFIX = '[SAN:sidepanel]';
const ACCESS_TOKEN_KEY = 'san_access_token';
const PENDING_STORAGE_KEY = 'san:pending-scrap';
const IMAGE_DB_NAME = 'san-extension-images';
const IMAGE_STORE_NAME = 'pending-images';
const isDebug = import.meta.env.DEV;
const dashboardBaseUrl = getDashboardBaseUrl();
const dashboardLoginUrl = new URL('/login', dashboardBaseUrl);
dashboardLoginUrl.searchParams.set('clientType', 'EXTENSION');
const dashboardGithubLoginUrl = new URL('/login', dashboardBaseUrl);
dashboardGithubLoginUrl.searchParams.set('clientType', 'EXTENSION');
dashboardGithubLoginUrl.searchParams.set('autoGithub', 'true');
const SEARCH_RESULT_TITLE = '\uAC80\uC0C9 \uACB0\uACFC';
const RECENT_TAB_LABEL = '\uCD5C\uADFC \uC9C0\uC2DD';
const SIMILAR_TAB_LABEL = '\uC720\uC0AC \uC9C0\uC2DD';

type KnowledgeTab = 'recent' | 'similar';

function getDashboardBaseUrl() {
  if (import.meta.env.VITE_DASHBOARD_BASE_URL) {
    return import.meta.env.VITE_DASHBOARD_BASE_URL.replace(/\/$/, '');
  }
  if (!import.meta.env.PROD) return 'http://localhost:5173';
  throw new Error('Missing VITE_DASHBOARD_BASE_URL for production extension build');
}

function isUnauthorizedError(error: unknown) {
  return (
    typeof error === 'object'
    && error !== null
    && 'response' in error
    && typeof (error as { response?: { status?: unknown } }).response?.status === 'number'
    && (error as { response?: { status?: number } }).response?.status === 401
  );
}

function debugLog(message: string, data?: unknown) {
  if (!isDebug) return;
  if (data === undefined) {
    console.debug(DEBUG_PREFIX, message);
    return;
  }
  console.debug(DEBUG_PREFIX, message, data);
}

function isPendingScrap(value: unknown): value is PendingScrap {
  if (!value || typeof value !== 'object') return false;
  const maybe = value as Partial<PendingScrap>;
  return typeof maybe.source_type === 'string' && typeof maybe.title === 'string';
}

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

function dataUrlToFile(dataUrl: string, fileName: string): File {
  const [header, base64Data = ''] = dataUrl.split(',');
  const mimeType = header.match(/^data:(.*?);base64$/)?.[1] ?? 'image/png';
  const binary = atob(base64Data);
  const bytes = new Uint8Array(binary.length);

  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }

  return new File([bytes], fileName, { type: mimeType });
}

function openImageDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(IMAGE_DB_NAME, 1);

    request.onupgradeneeded = () => {
      request.result.createObjectStore(IMAGE_STORE_NAME);
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function savePendingImageFile(file: File): Promise<string> {
  const id = typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const db = await openImageDb();

  await new Promise<void>((resolve, reject) => {
    const transaction = db.transaction(IMAGE_STORE_NAME, 'readwrite');
    transaction.objectStore(IMAGE_STORE_NAME).put(file, id);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });

  db.close();
  return id;
}

async function loadPendingImageFile(
  id: string,
  fallbackName = 'pending-image',
  fallbackType = 'application/octet-stream'
): Promise<File | null> {
  const db = await openImageDb();
  const value = await new Promise<Blob | File | undefined>((resolve, reject) => {
    const transaction = db.transaction(IMAGE_STORE_NAME, 'readonly');
    const request = transaction.objectStore(IMAGE_STORE_NAME).get(id);
    request.onsuccess = () => resolve(request.result as Blob | File | undefined);
    request.onerror = () => reject(request.error);
  });

  db.close();

  if (!value) return null;
  if (value instanceof File) return value;

  return new File([value], fallbackName, { type: value.type || fallbackType });
}

async function deletePendingImageFile(id: string | null | undefined) {
  if (!id) return;

  const db = await openImageDb();
  await new Promise<void>((resolve, reject) => {
    const transaction = db.transaction(IMAGE_STORE_NAME, 'readwrite');
    transaction.objectStore(IMAGE_STORE_NAME).delete(id);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });
  db.close();
}

function toKnowledgeCardSearchResponse(card: SearchCardResult): KnowledgeCardResponse {
  return {
    cardId: card.cardId,
    title: card.title,
    summary: card.summary,
    category: null,
    tags: [],
    createdAt: '',
  };
}

interface KnowledgeListTabsProps {
  activeTab: KnowledgeTab;
  canOpenSimilarTab: boolean;
  onChange: (tab: KnowledgeTab) => void;
}

function KnowledgeListTabs({ activeTab, canOpenSimilarTab, onChange }: KnowledgeListTabsProps) {
  const tabs: Array<{ id: KnowledgeTab; label: string; disabled?: boolean }> = [
    { id: 'recent', label: RECENT_TAB_LABEL },
    { id: 'similar', label: SIMILAR_TAB_LABEL, disabled: !canOpenSimilarTab },
  ];

  return (
    <div className="flex shrink-0 items-center gap-5">
      {tabs.map((tab) => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            type="button"
            disabled={tab.disabled}
            onClick={() => onChange(tab.id)}
            data-tour-id={`knowledge-${tab.id}-tab`}
            className={[
              'whitespace-nowrap text-xs font-semibold transition',
              isActive
                ? 'text-action-accent'
                : 'text-text-secondary/45 hover:text-text-primary',
              tab.disabled ? 'cursor-not-allowed opacity-30 hover:text-text-secondary/55' : '',
            ].join(' ')}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}

async function requestActiveTabMetadata(): Promise<PendingScrap | null> {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab?.id) return null;

  try {
    return await chrome.tabs.sendMessage<ExtensionMessage, PendingScrap>(tab.id, { type: 'REQUEST_METADATA' });
  } catch (error) {
    debugLog('failed to read metadata from active tab, using fallback metadata', error);
    const url = tab.url ?? null;
    const domain = url ? new URL(url).hostname : '';
    return {
      source_type: 'TEXT',
      source_url: url,
      raw_content: null,
      image_url: null,
      title: tab.title ?? 'Dragged text',
      domain,
      favicon: domain ? `https://www.google.com/s2/favicons?domain=${domain}&sz=32` : null,
    };
  }
}

export default function SidePanel() {
  const [pendingScrap, setPendingScrap] = useState<PendingScrap | null>(null);
  const [pendingImageFile, setPendingImageFile] = useState<File | null>(null);
  const [cards, setCards] = useState<SavedInsight[]>([]);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [relatedCards, setRelatedCards] = useState<KnowledgeCardResponse[]>([]);
  const [isLoadingRelated, setIsLoadingRelated] = useState(false);
  const [relatedError, setRelatedError] = useState<string | null>(null);
  const [hasRelatedResult, setHasRelatedResult] = useState(false);
  const [isRestoringPendingImage, setIsRestoringPendingImage] = useState(false);
  const [createdCard, setCreatedCard] = useState<KnowledgeCardView | null>(null);
  const [recentCards, setRecentCards] = useState<KnowledgeCardResponse[]>([]);
  const [isLoadingRecent, setIsLoadingRecent] = useState(false);
  const [recentError, setRecentError] = useState<string | null>(null);
  const [knowledgeSearchQuery, setKnowledgeSearchQuery] = useState('');
  const [knowledgeSearchCards, setKnowledgeSearchCards] = useState<KnowledgeCardResponse[]>([]);
  const [isSearchingKnowledge, setIsSearchingKnowledge] = useState(false);
  const [knowledgeSearchError, setKnowledgeSearchError] = useState<string | null>(null);
  const [hasKnowledgeSearchResult, setHasKnowledgeSearchResult] = useState(false);
  const [activeKnowledgeTab, setActiveKnowledgeTab] = useState<KnowledgeTab>('recent');
  const [isLogoutConfirmOpen, setIsLogoutConfirmOpen] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [isAuthCardOpen, setIsAuthCardOpen] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const [serverSourceByCardId, setServerSourceByCardId] = useState<Record<string, KnowledgeCardDetailResponse | undefined>>({});

  const sourceByCardId = useMemo(() => {
    const entries = cards
      .filter((card): card is SavedInsight & { card_id: string } => Boolean(card.card_id))
      .map((card) => [card.card_id, card] as const);

    return Object.fromEntries(entries);
  }, [cards]);

  const refreshRecentCards = useCallback(async () => {
    const requestToken = await authTokenStorage.getToken();
    if (!requestToken) {
      setIsAuthenticated(false);
      setRecentCards([]);
      setCreatedCard(null);
      setRelatedCards([]);
      setServerSourceByCardId({});
      setActiveKnowledgeTab('recent');
      return;
    }

    setIsLoadingRecent(true);
    setRecentError(null);
    try {
      const response = await cardsApi.getAll();
      setRecentCards(response.cards.slice(0, 3));
    } catch (error) {
      console.error(DEBUG_PREFIX, 'failed to load recent cards', error);
      if (isUnauthorizedError(error)) {
        const currentToken = await authTokenStorage.getToken();
        if (currentToken && currentToken !== requestToken) {
          debugLog('ignored stale unauthorized response after token rotation');
          return;
        }

        await authTokenStorage.clearToken();
        setIsAuthenticated(false);
        setRecentCards([]);
        setCreatedCard(null);
        setRelatedCards([]);
        setServerSourceByCardId({});
        setKnowledgeSearchCards([]);
        setHasKnowledgeSearchResult(false);
        setKnowledgeSearchError(null);
        setActiveKnowledgeTab('recent');
        setRecentError(null);
        void chrome.runtime.sendMessage({ type: 'SAN_AUTH_STATE_CHANGED', isAuthenticated: false });
        return;
      }
      setRecentError('Recent cards could not be loaded.');
    } finally {
      setIsLoadingRecent(false);
    }
  }, []);

  const hydrateServerSources = useCallback(async (nextCards: KnowledgeCardResponse[]) => {
    const missingCardIds = nextCards
      .map((card) => card.cardId)
      .filter((cardId) => !serverSourceByCardId[cardId]);

    if (missingCardIds.length === 0) return;

    const results = await Promise.allSettled(
      missingCardIds.map(async (cardId) => [cardId, await cardsApi.getDetail(cardId)] as const),
    );

    const fulfilledEntries = results
      .filter((result): result is PromiseFulfilledResult<readonly [string, KnowledgeCardDetailResponse]> => result.status === 'fulfilled')
      .map((result) => result.value);

    if (fulfilledEntries.length === 0) return;

    setServerSourceByCardId((current) => ({
      ...current,
      ...Object.fromEntries(fulfilledEntries),
    }));
  }, [serverSourceByCardId]);

  const {
    isSaving,
    savingLabel,
    saveError,
    saveNotice,
    handleSave,
    clearSaveFeedback,
    setSaveError,
  } = useSaveScrap({
    cards,
    setCards,
    pendingScrap,
    setPendingScrap,
    pendingImageFile,
    setPendingImageFile,
    isAuthenticated,
    setRelatedCards,
    setRelatedError,
    setIsLoadingRelated,
    setHasRelatedResult,
    isRestoringPendingImage,
    deletePendingImageFile,
    setCreatedCard,
    refreshRecentCards,
  });

  const clearResultState = useCallback(() => {
    clearSaveFeedback();
    setRelatedError(null);
    setRelatedCards([]);
    setHasRelatedResult(false);
    setCreatedCard(null);
    setIsLoadingRelated(false);
    setActiveKnowledgeTab('recent');
  }, [clearSaveFeedback]);

  const applyPendingScrap = useCallback(async (nextPendingScrap: PendingScrap) => {
    clearResultState();
    setPendingScrap(nextPendingScrap);

    if (
      nextPendingScrap.source_type === 'IMAGE'
      && nextPendingScrap.image_preview_url?.startsWith('data:')
      && !nextPendingScrap.image_blob_id
    ) {
      try {
        const file = dataUrlToFile(
          nextPendingScrap.image_preview_url,
          `${nextPendingScrap.title || 'captured-image'}.png`
        );
        const imageBlobId = await savePendingImageFile(file);
        const nextPendingImageScrap: PendingScrap = {
          ...nextPendingScrap,
          image_file_name: file.name,
          image_mime_type: file.type,
          image_blob_id: imageBlobId,
        };

        setPendingImageFile(file);
        setPendingScrap(nextPendingImageScrap);
        await savePendingScrap(nextPendingImageScrap);
      } catch (error) {
        console.error(DEBUG_PREFIX, 'failed to prepare captured image file', error);
        setPendingImageFile(null);
        setSaveError('Captured image could not be prepared. Please try capture again.');
      }
      return;
    }

    if (nextPendingScrap.source_type === 'IMAGE' && nextPendingScrap.image_blob_id) {
      setIsRestoringPendingImage(true);
      try {
        const file = await loadPendingImageFile(
          nextPendingScrap.image_blob_id,
          nextPendingScrap.image_file_name ?? nextPendingScrap.title,
          nextPendingScrap.image_mime_type ?? undefined
        );

        if (file) {
          setPendingImageFile(file);
          return;
        }

        setPendingImageFile(null);
        setSaveError('Pending image could not be restored. Please capture the image again.');
      } catch (error) {
        console.error(DEBUG_PREFIX, 'failed to restore pending image file', error);
        setPendingImageFile(null);
        setSaveError('Pending image could not be restored. Please capture the image again.');
      } finally {
        setIsRestoringPendingImage(false);
      }
      return;
    }

    setPendingImageFile(null);
  }, [clearResultState, setSaveError]);

  useEffect(() => {
    debugLog('side panel mounted');

    Promise.all([loadSavedInsights(), loadPendingScrap()])
      .then(([savedCards, storedPendingScrap]) => {
        debugLog('saved insights loaded', { count: savedCards.length });
        setCards(savedCards);
        if (!storedPendingScrap) return;

        debugLog('pending scrap restored', storedPendingScrap);
        setPendingScrap(storedPendingScrap);

        if (storedPendingScrap.source_type !== 'IMAGE' || !storedPendingScrap.image_blob_id) {
          return;
        }

        setIsRestoringPendingImage(true);
        loadPendingImageFile(
          storedPendingScrap.image_blob_id,
          storedPendingScrap.image_file_name ?? storedPendingScrap.title,
          storedPendingScrap.image_mime_type ?? undefined
        )
          .then((file) => {
            if (file) {
              setPendingImageFile(file);
              return;
            }
            setSaveError('Pending image could not be restored. Please drop the image again.');
          })
          .catch((error) => {
            console.error(DEBUG_PREFIX, 'failed to restore pending image file', error);
            setSaveError('Pending image could not be restored. Please drop the image again.');
          })
          .finally(() => {
            setIsRestoringPendingImage(false);
          });
      })
      .catch((error) => {
        console.error(DEBUG_PREFIX, 'failed to load side panel state', error);
      });

    const handleMessage = (msg: ExtensionMessage) => {
      debugLog('runtime message received', msg);
      if (msg.type === 'PUSH_TO_SIDEPANEL' && isPendingScrap(msg.payload)) {
        void applyPendingScrap(msg.payload);
      }
    };

    chrome.runtime.onMessage.addListener(handleMessage);
    return () => chrome.runtime.onMessage.removeListener(handleMessage);
  }, [applyPendingScrap, setSaveError]);

  useEffect(() => {
    const handlePendingScrapChange = (
      changes: Record<string, chrome.storage.StorageChange>,
      areaName: string
    ) => {
      if (areaName !== 'local') return;

      const pendingScrapChange = changes[PENDING_STORAGE_KEY];
      if (!pendingScrapChange) return;

      const nextPendingScrap = pendingScrapChange.newValue;
      if (isPendingScrap(nextPendingScrap)) {
        void applyPendingScrap(nextPendingScrap);
        return;
      }

      if (pendingScrapChange.newValue === undefined) {
        setPendingScrap(null);
        setPendingImageFile(null);
      }
    };

    chrome.storage.onChanged.addListener(handlePendingScrapChange);
    return () => chrome.storage.onChanged.removeListener(handlePendingScrapChange);
  }, [applyPendingScrap]);

  useEffect(() => {
    let ignore = false;

    const refreshAuthState = async () => {
      const token = await authTokenStorage.getToken();
      if (ignore) {
        return;
      }

      const hasToken = Boolean(token);
      setIsAuthenticated(hasToken);
      if (hasToken) {
        void refreshRecentCards();
      } else {
        setRecentCards([]);
        setCreatedCard(null);
        setRelatedCards([]);
        setServerSourceByCardId({});
        setActiveKnowledgeTab('recent');
      }
    };

    void refreshAuthState();

    const handleStorageChange = (
      changes: Record<string, chrome.storage.StorageChange>,
      areaName: string
    ) => {
      if (areaName === 'local' && changes[ACCESS_TOKEN_KEY]) {
        void refreshAuthState();
      }
    };

    const handleAuthMessage = (msg: ExtensionMessage) => {
      if (msg.type === 'SAN_AUTH_STATE_CHANGED') {
        void refreshAuthState();
      }
    };

    const handlePanelFocus = () => {
      void refreshAuthState();
    };

    const handleVisibilityChange = () => {
      if (!document.hidden) {
        void refreshAuthState();
      }
    };

    chrome.storage.onChanged.addListener(handleStorageChange);
    chrome.runtime.onMessage.addListener(handleAuthMessage);
    window.addEventListener('focus', handlePanelFocus);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      ignore = true;
      chrome.storage.onChanged.removeListener(handleStorageChange);
      chrome.runtime.onMessage.removeListener(handleAuthMessage);
      window.removeEventListener('focus', handlePanelFocus);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [refreshRecentCards]);

  useEffect(() => {
    if (!isAuthenticated) return;
    void hydrateServerSources(recentCards);
  }, [hydrateServerSources, isAuthenticated, recentCards]);

  useEffect(() => {
    if (!isAuthenticated) return;
    void hydrateServerSources(relatedCards);
  }, [hydrateServerSources, isAuthenticated, relatedCards]);

  useEffect(() => {
    if (!isAuthenticated) return;
    void hydrateServerSources(knowledgeSearchCards);
  }, [hydrateServerSources, isAuthenticated, knowledgeSearchCards]);

  const handleTextDrop = useCallback(async (text: string) => {
    await deletePendingImageFile(pendingScrap?.image_blob_id);
    const trimmedText = text.trim();
    const nextPending: PendingScrap = isHttpUrl(trimmedText)
      ? createLinkScrap(trimmedText)
      : {
          ...((await requestActiveTabMetadata()) ?? {
            source_type: 'TEXT',
            source_url: null,
            raw_content: null,
            image_url: null,
            title: 'Dragged text',
            domain: '',
            favicon: null,
          }),
          source_type: 'TEXT',
          raw_content: text,
          image_blob_id: null,
        };

    clearResultState();
    debugLog('drop zone text received', {
      length: trimmedText.length,
      source_url: nextPending.source_url,
      title: nextPending.title,
    });
    setPendingScrap(nextPending);
    setPendingImageFile(null);
    await savePendingScrap(nextPending);
  }, [clearResultState, pendingScrap?.image_blob_id]);

  const handleImageDrop = useCallback(async (file: File) => {
    const validationError = getS3ImageFileValidationError(file);
    if (validationError) {
      setSaveError(validationError);
      return;
    }

    const metadata = await requestActiveTabMetadata();
    const previewUrl = await fileToDataUrl(file);
    const imageBlobId = await savePendingImageFile(file);
    await deletePendingImageFile(pendingScrap?.image_blob_id);
    const nextPending: PendingScrap = {
      ...(metadata ?? {
        source_type: 'IMAGE',
        source_url: null,
        raw_content: null,
        image_url: null,
        title: file.name || 'Dropped image',
        domain: '',
        favicon: null,
      }),
      source_type: 'IMAGE',
      raw_content: file.name || 'Dropped image',
      image_url: null,
      image_preview_url: previewUrl,
      image_file_name: file.name,
      image_mime_type: file.type,
      image_blob_id: imageBlobId,
      title: file.name || metadata?.title || 'Dropped image',
    };

    clearResultState();
    setPendingScrap(nextPending);
    setPendingImageFile(file);
    await savePendingScrap(nextPending);
  }, [clearResultState, pendingScrap?.image_blob_id]);

  const handleClearPending = useCallback(() => {
    void deletePendingImageFile(pendingScrap?.image_blob_id);
    setPendingScrap(null);
    setPendingImageFile(null);
    clearResultState();
    void savePendingScrap(null);
  }, [clearResultState, pendingScrap?.image_blob_id]);

  const openDashboardLogin = useCallback(() => {
    chrome.tabs.create({ url: dashboardLoginUrl.toString() });
  }, []);

  const openDashboardGithubLogin = useCallback(() => {
    chrome.tabs.create({ url: dashboardGithubLoginUrl.toString() });
  }, []);

  const openDashboard = useCallback(async () => {
    if (!isAuthenticated) {
      await chrome.tabs.create({ url: dashboardLoginUrl.toString() });
      return;
    }

    try {
      const { ticket } = await authApi.createDashboardBridgeTicket();
      const dashboardBridgeUrl = new URL('/auth/bridge/dashboard', dashboardBaseUrl);
      dashboardBridgeUrl.searchParams.set('ticket', ticket);
      dashboardBridgeUrl.searchParams.set('redirect', '/');
      await chrome.tabs.create({ url: dashboardBridgeUrl.toString() });
    } catch (error) {
      console.error(DEBUG_PREFIX, 'failed to open dashboard with bridge login', error);
      await chrome.tabs.create({ url: dashboardLoginUrl.toString() });
    }
  }, [isAuthenticated]);

  const openDashboardCardDetail = useCallback((cardId: string) => {
    if (!isAuthenticated) {
      chrome.tabs.create({ url: dashboardLoginUrl.toString() });
      return;
    }

    void (async () => {
      try {
        const { ticket } = await authApi.createDashboardBridgeTicket();
        const dashboardBridgeUrl = new URL('/auth/bridge/dashboard', dashboardBaseUrl);
        dashboardBridgeUrl.searchParams.set('ticket', ticket);
        dashboardBridgeUrl.searchParams.set('redirect', `/cards/${cardId}`);
        await chrome.tabs.create({ url: dashboardBridgeUrl.toString() });
      } catch (error) {
        console.error(DEBUG_PREFIX, 'failed to open dashboard card detail with bridge login', error);
        await chrome.tabs.create({ url: dashboardLoginUrl.toString() });
      }
    })();
  }, [isAuthenticated]);

  const openExtensionLogin = useCallback(() => {
    setIsAuthCardOpen(true);
    setIsProfileMenuOpen(false);
  }, []);

  const handleProfileButtonClick = useCallback(() => {
    if (!isAuthenticated) {
      openExtensionLogin();
      return;
    }

    setIsProfileMenuOpen((current) => !current);
  }, [isAuthenticated, openExtensionLogin]);

  const handleHomeClick = useCallback(() => {
    setIsAuthCardOpen(false);
    setIsProfileMenuOpen(false);
    setIsLogoutConfirmOpen(false);
  }, []);

  const handleExtensionAuthComplete = useCallback(() => {
    setIsAuthCardOpen(false);
    setIsAuthenticated(true);
    void refreshRecentCards();
  }, [refreshRecentCards]);

  const handleCancelLogout = useCallback(() => {
    if (isLoggingOut) return;
    setIsLogoutConfirmOpen(false);
  }, [isLoggingOut]);

  const handleLogoutClick = useCallback(() => {
    setIsProfileMenuOpen(false);
    setIsLogoutConfirmOpen(true);
  }, []);

  const handleConfirmLogout = useCallback(async () => {
    setIsLoggingOut(true);

    try {
      await authApi.logout();
    } catch (error) {
      console.error(DEBUG_PREFIX, 'failed to logout on server', error);
    } finally {
      await chrome.runtime
        .sendMessage({ type: 'SAN_AUTH_CLEAR' })
        .catch(async (error) => {
          console.error(DEBUG_PREFIX, 'failed to clear auth through background', error);
          await authTokenStorage.clearToken();
        });
      setIsAuthenticated(false);
      setRecentCards([]);
      setCreatedCard(null);
      setRelatedCards([]);
      setServerSourceByCardId({});
      setKnowledgeSearchCards([]);
      setHasKnowledgeSearchResult(false);
      setKnowledgeSearchError(null);
      setActiveKnowledgeTab('recent');
      setIsLogoutConfirmOpen(false);
      setIsLoggingOut(false);
      void chrome.runtime.sendMessage({ type: 'SAN_AUTH_STATE_CHANGED', isAuthenticated: false });
    }
  }, []);

  useEffect(() => {
    if (isLoadingRelated) {
      setActiveKnowledgeTab('recent');
      return;
    }

    if (hasRelatedResult || relatedError) {
      setActiveKnowledgeTab('similar');
    }
  }, [hasRelatedResult, isLoadingRelated, relatedError]);

  const handleKnowledgeSearch = useCallback(async () => {
    const search = knowledgeSearchQuery.trim();

    if (!search) {
      setHasKnowledgeSearchResult(false);
      setKnowledgeSearchCards([]);
      setKnowledgeSearchError(null);
      return;
    }

    if (!isAuthenticated) {
      setHasKnowledgeSearchResult(true);
      setKnowledgeSearchCards([]);
      setKnowledgeSearchError('Login is required to search your knowledge archive.');
      return;
    }

    setHasKnowledgeSearchResult(true);
    setIsSearchingKnowledge(true);
    setKnowledgeSearchError(null);

    try {
      const response = await searchApi.search({ keyword: search, page: 0, size: 10 });
      setKnowledgeSearchCards(response.results.map(toKnowledgeCardSearchResponse));
    } catch (error) {
      console.error(DEBUG_PREFIX, 'failed to search knowledge cards', error);
      setKnowledgeSearchCards([]);
      setKnowledgeSearchError('Knowledge archive search failed.');
    } finally {
      setIsSearchingKnowledge(false);
    }
  }, [isAuthenticated, knowledgeSearchQuery]);

  const canOpenSimilarTab = hasRelatedResult || relatedCards.length > 0 || Boolean(relatedError);
  const knowledgeSearchAction = (
    <KnowledgeSearchBar
      value={knowledgeSearchQuery}
      disabled={isSearchingKnowledge}
      onChange={(value) => {
        setKnowledgeSearchQuery(value);
        if (!value.trim()) {
          setHasKnowledgeSearchResult(false);
          setKnowledgeSearchCards([]);
          setKnowledgeSearchError(null);
        }
      }}
      onSubmit={handleKnowledgeSearch}
    />
  );
  const knowledgeTabs = (
    <KnowledgeListTabs
      activeTab={activeKnowledgeTab}
      canOpenSimilarTab={canOpenSimilarTab}
      onChange={setActiveKnowledgeTab}
    />
  );

  return (
    <div className="flex h-full w-full flex-col overflow-hidden bg-background text-text-primary">
      <div className="relative flex min-h-0 flex-1 flex-col overflow-hidden">
        <SidePanelNavbar
          isAuthenticated={isAuthenticated}
          isProfileMenuOpen={isProfileMenuOpen}
          onHomeClick={handleHomeClick}
          onOpenDashboard={openDashboard}
          onProfileButtonClick={handleProfileButtonClick}
          onLogoutClick={handleLogoutClick}
        />
        <OnboardingTour
          isAuthenticated={isAuthenticated}
          hasPendingScrap={Boolean(pendingScrap)}
          canOpenSimilarTab={canOpenSimilarTab}
        />
        {isLogoutConfirmOpen && (
          <div className="fixed inset-0 z-40 flex items-center justify-center bg-scrim px-4 backdrop-blur-sm">
            <div
              role="dialog"
              aria-modal="true"
              aria-labelledby="logout-confirm-title"
              className="w-full max-w-[280px] rounded-lg border border-text-secondary/10 bg-surface-container p-4 text-text-primary shadow-2xl"
            >
              <h2 id="logout-confirm-title" className="text-sm font-semibold">
                로그아웃 하시겠습니까?
              </h2>
              <div className="mt-4 grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={handleCancelLogout}
                  disabled={isLoggingOut}
                  className="h-9 rounded-md border border-text-secondary/10 bg-surface-highest text-sm font-medium text-text-secondary transition hover:bg-surface-container/90 bg-surface-container/70 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={handleConfirmLogout}
                  disabled={isLoggingOut}
                  className="h-9 rounded-md border border-action-accent/35 bg-action-accent/15 text-sm font-semibold text-action-accent transition hover:bg-action-accent/20 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {isLoggingOut ? '처리 중' : '확인'}
                </button>
              </div>
            </div>
          </div>
        )}
        <GlowBackground />
        <div className="relative z-10 flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto px-2 pb-2 pt-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {/* 1. Top Workspace Area (Fixed 190px): Switch between DropZone and Loading */}
          {isAuthCardOpen && !isAuthenticated ? (
            <ExtensionAuthCard
              onAuthenticated={handleExtensionAuthComplete}
              onGithubLogin={openDashboardGithubLogin}
            />
          ) : (
            <>
              <div className="shrink-0">
                {isSaving || (isLoadingRelated && !createdCard) ? (
                  <KnowledgeLoadingCard />
                ) : (
                  <DropZone
                    pendingScrap={pendingScrap}
                    onTextDrop={handleTextDrop}
                    onImageDrop={handleImageDrop}
                    onSave={handleSave}
                    onClear={handleClearPending}
                    isSaving={isSaving}
                    savingLabel={savingLabel}
                    saveLabel="Save"
                    saveError={saveError}
                    saveNotice={saveNotice}
                    canSave={true}
                    authNotice={!isAuthenticated && pendingScrap ? '로그인하여 지식을 보관하고 AI가 생성한 카드를 확인해 보세요.' : null}
                    onLogin={!isAuthenticated ? () => setIsAuthCardOpen(true) : undefined}
                  />
                )}
              </div>

          {/* 2. Creation Result Area (Fixed 120px): Appears only after successful creation */}
              {isAuthenticated && createdCard && !hasKnowledgeSearchResult && (
                <div className="shrink-0">
                  <CreatedKnowledgeCard card={createdCard} onOpenCard={openDashboardCardDetail} />
                </div>
              )}

          {/* 3. Content Area: Search Bar + (Related Cards OR Recent List) */}
              <div className="flex shrink-0 flex-col">
                {!isAuthenticated ? (
                  <ArchiveList cards={cards} />
                ) : (
                  <>
                    {hasKnowledgeSearchResult ? (
                      <RecentKnowledgeList
                        cards={knowledgeSearchCards}
                        isLoading={isSearchingKnowledge}
                        error={knowledgeSearchError}
                        isScrollable={false}
                        title={SEARCH_RESULT_TITLE}
                        action={knowledgeSearchAction}
                        sourceByCardId={sourceByCardId}
                        serverSourceByCardId={serverSourceByCardId}
                        useServerSources
                        onOpenCard={openDashboardCardDetail}
                      />
                    ) : activeKnowledgeTab === 'similar' && canOpenSimilarTab ? (
                      <SimilarKnowledgeList
                        cards={relatedCards}
                        isLoading={false}
                        error={relatedError}
                        isScrollable={false}
                        title={knowledgeTabs}
                        action={knowledgeSearchAction}
                        sourceByCardId={sourceByCardId}
                        serverSourceByCardId={serverSourceByCardId}
                        useServerSources
                        onOpenCard={openDashboardCardDetail}
                      />
                    ) : (
                      <RecentKnowledgeList
                        cards={recentCards}
                        isLoading={isLoadingRecent && recentCards.length === 0}
                        error={recentError}
                        isScrollable={false}
                        title={knowledgeTabs}
                        action={knowledgeSearchAction}
                        sourceByCardId={sourceByCardId}
                        serverSourceByCardId={serverSourceByCardId}
                        useServerSources
                        onOpenCard={openDashboardCardDetail}
                      />
                    )}
                  </>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
