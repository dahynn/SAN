import type { PendingScrap } from '@extension/types/index';

interface MetaElementLike {
  content?: string;
}

interface MetadataDocumentLike {
  title: string;
  querySelector<T extends MetaElementLike>(selector: string): T | null;
}

interface MetadataLocationLike {
  href: string;
}

type MetadataLog = (message: string, data?: unknown) => void;

export function getMeta(documentLike: MetadataDocumentLike, property: string): string | null {
  return documentLike.querySelector<HTMLMetaElement>(
    `meta[property='${property}'], meta[name='${property}']`
  )?.content ?? null;
}

export function extractMetadataFromDocument(
  documentLike: MetadataDocumentLike,
  locationLike: MetadataLocationLike,
  log?: MetadataLog,
): PendingScrap {
  const url = locationLike.href;
  const domain = new URL(url).hostname;

  const metadata: PendingScrap = {
    source_type: 'LINK',
    source_url: url,
    raw_content: getMeta(documentLike, 'og:description') ?? getMeta(documentLike, 'description'),
    image_url: getMeta(documentLike, 'og:image'),
    title: getMeta(documentLike, 'og:title') ?? documentLike.title,
    domain,
    favicon: `https://www.google.com/s2/favicons?domain=${domain}&sz=32`,
  };

  log?.('metadata extracted', metadata);
  return metadata;
}
