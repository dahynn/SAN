import { useCallback, useRef, useState } from 'react';
import {
  getApiErrorMessage,
  toKnowledgeCardView,
  type CreateScrapRequest,
  type CreateScrapResponse,
  type KnowledgeCardResponse,
  type KnowledgeCardView,
} from '@san/shared';
import { asyncJobsApi, cardsApi, scrapsApi, s3Api } from '@extension/api/client';
import type { PendingScrap, SavedInsight } from '@extension/types';
import { normalizeHttpUrl, normalizeSavedInsight } from '@extension/utils/scrap';

const STORAGE_KEY = 'san:saved-insights';
const PENDING_STORAGE_KEY = 'san:pending-scrap';
const JOB_POLL_INTERVAL_MS = 1500;
const JOB_POLL_MAX_ATTEMPTS = 40;
const REANALYZE_EXISTING_SCRAP_NOTICE = '\uAE30\uC874 \uC218\uC9D1 \uB370\uC774\uD130\uB97C \uB2E4\uC2DC \uBD84\uC11D\uD558\uACE0 \uC788\uC5B4\uC694.';
const EXISTING_CARD_NOTICE = '\uC774\uBBF8 \uAC19\uC740 \uB370\uC774\uD130\uB85C \uB9CC\uB4E0 \uCE74\uB4DC\uAC00 \uC788\uC5B4\uC694.';
const ANALYZING_SCRAP_NOTICE = '\uC218\uC9D1 \uB370\uC774\uD130\uB97C \uBD84\uC11D\uD558\uACE0 \uC788\uC5B4\uC694.';

function isPendingScrap(value: unknown): value is PendingScrap {
  if (!value || typeof value !== 'object') return false;
  const maybe = value as Partial<PendingScrap>;
  return typeof maybe.source_type === 'string' && typeof maybe.title === 'string';
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForCardAnalysis(jobId: string) {
  for (let attempt = 0; attempt < JOB_POLL_MAX_ATTEMPTS; attempt += 1) {
    const job = await asyncJobsApi.getStatus(jobId);
    if (job.status === 'COMPLETED') return;
    if (job.status === 'FAILED') {
      throw new Error(job.errorMessage ?? 'Knowledge card creation failed.');
    }
    await delay(JOB_POLL_INTERVAL_MS);
  }

  throw new Error('Knowledge card creation timed out.');
}

async function resolveCreatedCard(response: CreateScrapResponse) {
  if (response.cardCreationStatus === 'ANALYSIS_IN_PROGRESS') {
    const analysisJobId = response.analysisJobId;
    if (!analysisJobId) {
      throw new Error('Knowledge card analysis job was not provided.');
    }
    await waitForCardAnalysis(analysisJobId);
  }

  const cardResponse = response.cardId
    ? { cardId: response.cardId }
    : await cardsApi.getByScrapId(response.scrapId);
  const cardsResponse = await cardsApi.getAll();
  const card = cardsResponse.cards.find((item) => item.cardId === cardResponse.cardId) ?? null;

  return {
    cardId: cardResponse.cardId,
    card,
  };
}

function getCardCreationNotice(response: CreateScrapResponse) {
  if (response.originStatus === 'EXISTING' && response.cardCreationStatus === 'ANALYSIS_IN_PROGRESS') {
    return REANALYZE_EXISTING_SCRAP_NOTICE;
  }

  if (response.originStatus === 'EXISTING' && response.cardCreationStatus === 'CARD_READY') {
    return EXISTING_CARD_NOTICE;
  }

  if (response.originStatus === 'CREATED' && response.cardCreationStatus === 'ANALYSIS_IN_PROGRESS') {
    return ANALYZING_SCRAP_NOTICE;
  }

  return null;
}

function toSavedInsight(scrap: PendingScrap): SavedInsight {
  const id = typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

  return {
    ...scrap,
    id,
    created_at: new Date().toISOString(),
  };
}

function toCreateScrapRequest(scrap: PendingScrap, imageObjectKey?: string | null): CreateScrapRequest {
  const normalizedSourceUrl = normalizeHttpUrl(scrap.source_url);
  const normalizedRawContent = scrap.raw_content?.trim() ?? '';
  const inferredUrl = normalizedSourceUrl ?? normalizeHttpUrl(normalizedRawContent);

  if (scrap.source_type === 'LINK') {
    return {
      sourceUrl: inferredUrl,
      rawContent: inferredUrl ?? scrap.raw_content ?? scrap.title,
    };
  }

  if (scrap.source_type === 'IMAGE') {
    return {
      sourceUrl: normalizedSourceUrl,
      rawContent: scrap.image_url ?? scrap.raw_content ?? scrap.image_file_name ?? scrap.title,
      imageObjectKey,
    };
  }

  return {
    sourceUrl: inferredUrl ?? normalizedSourceUrl,
    rawContent: scrap.raw_content ?? normalizedSourceUrl ?? scrap.image_file_name ?? scrap.title,
  };
}

export async function loadSavedInsights(): Promise<SavedInsight[]> {
  const stored = await chrome.storage.local.get(STORAGE_KEY);
  const value = stored[STORAGE_KEY];
  if (!Array.isArray(value)) return [];
  const normalized = value
    .filter((item): item is SavedInsight => Boolean(item) && typeof item === 'object')
    .map((item) => normalizeSavedInsight(item));
  if (JSON.stringify(value) !== JSON.stringify(normalized)) {
    await chrome.storage.local.set({ [STORAGE_KEY]: normalized });
  }
  return normalized;
}

export async function saveInsights(cards: SavedInsight[]) {
  await chrome.storage.local.set({ [STORAGE_KEY]: cards.map((card) => normalizeSavedInsight(card)) });
}

export async function loadPendingScrap(): Promise<PendingScrap | null> {
  const stored = await chrome.storage.local.get(PENDING_STORAGE_KEY);
  const value = stored[PENDING_STORAGE_KEY];
  return isPendingScrap(value) ? value : null;
}

export async function savePendingScrap(scrap: PendingScrap | null) {
  if (scrap) {
    await chrome.storage.local.set({ [PENDING_STORAGE_KEY]: scrap });
    return;
  }
  await chrome.storage.local.remove(PENDING_STORAGE_KEY);
}

interface UseSaveScrapParams {
  cards: SavedInsight[];
  setCards: (next: SavedInsight[]) => void;
  pendingScrap: PendingScrap | null;
  setPendingScrap: (next: PendingScrap | null) => void;
  pendingImageFile: File | null;
  setPendingImageFile: (next: File | null) => void;
  isAuthenticated: boolean;
  setRelatedCards: (next: KnowledgeCardResponse[]) => void;
  setRelatedError: (next: string | null) => void;
  setIsLoadingRelated: (next: boolean) => void;
  setHasRelatedResult: (next: boolean) => void;
  isRestoringPendingImage: boolean;
  deletePendingImageFile: (id: string | null | undefined) => void | Promise<void>;
  setCreatedCard: (next: KnowledgeCardView | null) => void;
  refreshRecentCards: () => void | Promise<void>;
}

export function useSaveScrap({
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
}: UseSaveScrapParams) {
  const [isSaving, setIsSaving] = useState(false);
  const [savingLabel, setSavingLabel] = useState('Saving...');
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);
  const isSavingRef = useRef(false);

  const clearSaveFeedback = useCallback(() => {
    setSaveError(null);
    setSaveNotice(null);
  }, []);

  const handleSave = useCallback(async () => {
    if (!pendingScrap) return;
    if (isSavingRef.current) return;

    isSavingRef.current = true;
    setIsSaving(true);
    setSavingLabel(isAuthenticated ? 'Saving scrap...' : 'Saving locally...');
    setSaveError(null);
    setSaveNotice(null);
    setRelatedError(null);
    setRelatedCards([]);
    setHasRelatedResult(false);
    setCreatedCard(null);
    setIsLoadingRelated(false);

    try {
      if (isRestoringPendingImage) {
        throw new Error('Image is still being restored. Please try again in a moment.');
      }

      if (!isAuthenticated) {
        const saved = toSavedInsight(pendingScrap);
        const nextCards = [saved, ...cards];
        setCards(nextCards);
        setPendingScrap(null);
        setPendingImageFile(null);
        await savePendingScrap(null);
        await saveInsights(nextCards);
        await deletePendingImageFile(pendingScrap.image_blob_id);
        setSaveNotice('수집한 데이터가 성공적으로 저장되었습니다.');
        return;
      }

      let imageObjectKey: string | null = null;
      if (pendingScrap.source_type === 'IMAGE' && pendingImageFile) {
        setSavingLabel('Uploading image...');
        const uploadResult = await s3Api.uploadImage(pendingImageFile);
        imageObjectKey = uploadResult.objectKey;
      }

      const request = toCreateScrapRequest(pendingScrap, imageObjectKey);
      setSavingLabel('Saving scrap...');
      const response = await scrapsApi.create(request);
      setSaveNotice(getCardCreationNotice(response));
      setSavingLabel(response.cardCreationStatus === 'ANALYSIS_IN_PROGRESS' ? 'Creating card...' : 'Loading card...');
      setIsLoadingRelated(true);
      const createdCard = await resolveCreatedCard(response);
      const saved = {
        ...toSavedInsight(pendingScrap),
        id: response.scrapId,
        card_id: createdCard.cardId,
        created_at: response.createdAt,
      };
      const nextCards = [
        saved,
        ...cards.filter((item) => item.id !== response.scrapId && item.card_id !== createdCard.cardId),
      ];

      setCreatedCard(createdCard.card ? toKnowledgeCardView(createdCard.card) : null);
      setCards(nextCards);
      setPendingScrap(null);
      setPendingImageFile(null);
      await savePendingScrap(null);
      await saveInsights(nextCards);
      await deletePendingImageFile(pendingScrap.image_blob_id);
      setSavingLabel('Finding related cards...');
      const similarCards = await cardsApi.getSimilarByCardId(createdCard.cardId);
      setRelatedCards(similarCards.similarCards.slice(0, 3));
      setHasRelatedResult(true);
      void refreshRecentCards();
    } catch (error) {
      setSaveError(getApiErrorMessage(error, 'Failed to save scrap.'));
      setRelatedError(getApiErrorMessage(error, 'Failed to load related cards.'));
    } finally {
      isSavingRef.current = false;
      setIsSaving(false);
      setIsLoadingRelated(false);
    }
  }, [
    cards,
    deletePendingImageFile,
    isAuthenticated,
    isRestoringPendingImage,
    pendingImageFile,
    pendingScrap,
    refreshRecentCards,
    setCards,
    setCreatedCard,
    setHasRelatedResult,
    setIsLoadingRelated,
    setPendingImageFile,
    setPendingScrap,
    setRelatedCards,
    setRelatedError,
  ]);

  return {
    isSaving,
    savingLabel,
    saveError,
    saveNotice,
    handleSave,
    clearSaveFeedback,
    setSaveError,
    setSaveNotice,
  };
}
