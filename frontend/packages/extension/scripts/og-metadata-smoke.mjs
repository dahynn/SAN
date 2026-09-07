import { readFile, rm, mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { transformWithEsbuild } from 'vite';

const PREFIX = '[SAN:og-test]';
const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outFile = resolve(repoRoot, 'node_modules/.tmp/og-metadata-smoke/metadata.mjs');

class FakeDocument {
  constructor(html, title) {
    this.title = title;
    this.metas = parseMetaTags(html);
  }

  querySelector(selector) {
    const match = selector.match(/meta\[(?:property|name)='([^']+)'\]/);
    if (!match) return null;

    const property = match[1];
    const found = this.metas.find((meta) => meta.property === property || meta.name === property);
    return found ? { content: found.content } : null;
  }
}

function parseMetaTags(html) {
  const metas = [];
  const metaTagPattern = /<meta\s+[^>]*>/gi;
  const attrPattern = /([:\w-]+)\s*=\s*["']([^"']*)["']/gi;

  for (const [tag] of html.matchAll(metaTagPattern)) {
    const attrs = {};
    for (const [, key, value] of tag.matchAll(attrPattern)) {
      attrs[key.toLowerCase()] = value;
    }
    metas.push(attrs);
  }

  return metas;
}

function logStep(message, data) {
  if (data === undefined) {
    console.log(PREFIX, message);
    return;
  }
  console.log(PREFIX, message, JSON.stringify(data, null, 2));
}

function assertEqual(caseName, field, actual, expected) {
  if (actual !== expected) {
    console.error(PREFIX, `FAIL ${caseName} :: ${field}`);
    console.error(PREFIX, 'expected:', expected);
    console.error(PREFIX, 'actual  :', actual);
    throw new Error(`${caseName} failed at ${field}`);
  }
  console.log(PREFIX, `PASS ${caseName} :: ${field}`);
}

async function loadExtractor() {
  await rm(resolve(repoRoot, 'node_modules/.tmp/og-metadata-smoke'), { recursive: true, force: true });
  await mkdir(dirname(outFile), { recursive: true });

  const source = await readFile(resolve(repoRoot, 'src/content/metadata.ts'), 'utf8');
  const transformed = await transformWithEsbuild(source, 'metadata.ts', {
    loader: 'ts',
    format: 'esm',
    target: 'node20',
  });
  await writeFile(outFile, transformed.code, 'utf8');

  return import(pathToFileURL(outFile).href);
}

const cases = [
  {
    name: 'og tags are preferred',
    url: 'https://example.com/articles/123?from=test',
    title: 'Fallback title',
    html: `
      <meta property="og:title" content="OG Title">
      <meta property="og:description" content="OG Description">
      <meta property="og:image" content="https://cdn.example.com/og.png">
      <meta name="description" content="Plain description">
    `,
    expected: {
      title: 'OG Title',
      raw_content: 'OG Description',
      image_url: 'https://cdn.example.com/og.png',
      source_url: 'https://example.com/articles/123?from=test',
      domain: 'example.com',
      favicon: 'https://www.google.com/s2/favicons?domain=example.com&sz=32',
    },
  },
  {
    name: 'fallback tags are used',
    url: 'https://news.example.net/post',
    title: 'Document Title',
    html: '<meta name="description" content="Fallback Description">',
    expected: {
      title: 'Document Title',
      raw_content: 'Fallback Description',
      image_url: null,
      source_url: 'https://news.example.net/post',
      domain: 'news.example.net',
      favicon: 'https://www.google.com/s2/favicons?domain=news.example.net&sz=32',
    },
  },
];

try {
  const { extractMetadataFromDocument } = await loadExtractor();
  logStep(`running ${cases.length} OG metadata cases`);

  for (const testCase of cases) {
    logStep(`CASE ${testCase.name}`);
    const actual = extractMetadataFromDocument(
      new FakeDocument(testCase.html, testCase.title),
      { href: testCase.url },
      (message, data) => logStep(`${testCase.name} :: ${message}`, data),
    );

    for (const [field, expected] of Object.entries(testCase.expected)) {
      assertEqual(testCase.name, field, actual[field], expected);
    }
  }

  logStep('all OG metadata checks passed');
} catch (error) {
  console.error(PREFIX, 'OG metadata checks failed');
  console.error(error);
  process.exitCode = 1;
}
