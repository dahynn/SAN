import type { PendingScrap, SavedInsight } from '@extension/types';

export function isHttpUrl(value: string | null | undefined): value is string {
  if (!value) return false;

  try {
    const parsed = new URL(value.trim());
    return parsed.protocol === 'http:' || parsed.protocol === 'https:';
  } catch {
    return false;
  }
}

export function normalizeHttpUrl(value: string | null | undefined): string | null {
  if (!isHttpUrl(value)) return null;
  return value.trim();
}

export function createLinkScrap(url: string): PendingScrap {
  const normalizedUrl = normalizeHttpUrl(url);

  if (!normalizedUrl) {
    throw new Error('Invalid URL');
  }

  const parsed = new URL(normalizedUrl);

  return {
    source_type: 'LINK',
    source_url: normalizedUrl,
    raw_content: normalizedUrl,
    image_url: null,
    title: normalizedUrl,
    domain: parsed.hostname,
    favicon: `https://www.google.com/s2/favicons?domain=${parsed.hostname}&sz=32`,
  };
}

export function normalizePendingScrap(scrap: PendingScrap): PendingScrap {
  const rawContentUrl = normalizeHttpUrl(scrap.raw_content);
  const sourceUrl = normalizeHttpUrl(scrap.source_url);

  if (scrap.source_type === 'IMAGE') {
    return {
      ...scrap,
      source_url: sourceUrl,
    };
  }

  if (scrap.source_type === 'LINK') {
    const normalizedUrl = sourceUrl ?? rawContentUrl;
    if (!normalizedUrl) return { ...scrap, source_url: sourceUrl };
    return {
      ...scrap,
      source_type: 'LINK',
      source_url: normalizedUrl,
      raw_content: normalizedUrl,
      title: normalizedUrl,
      domain: new URL(normalizedUrl).hostname,
      favicon: `https://www.google.com/s2/favicons?domain=${new URL(normalizedUrl).hostname}&sz=32`,
    };
  }

  if (rawContentUrl) {
    return createLinkScrap(rawContentUrl);
  }

  return {
    ...scrap,
    source_url: sourceUrl,
  };
}

export function normalizeSavedInsight(scrap: SavedInsight): SavedInsight {
  return {
    ...normalizePendingScrap(scrap),
    id: scrap.id,
    card_id: scrap.card_id,
    created_at: scrap.created_at,
  };
}
