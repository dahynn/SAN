import { useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { FolderOpen, Loader2 } from 'lucide-react';
import { useArchiveCategories, useArchiveCategoryCards, useArchiveCardTagRelations } from '@san/shared';
import { graphFixtureCategories, graphFixtureLeavesByCategory } from './fixtures';

/* ── types ── */

type Pos = { x: number; y: number };
type CatNode = { id: string; name: string; count: number; idlePos: Pos; dockPos: Pos };
type CardNode = { id: string; title: string; tags: string[]; pos: Pos; layer: number };
type TagLineIndices = { aIdx: number; bIdx: number; weight: number };
type CrossCard = { id: string; title: string; pos: Pos; catId: string };

/* ── layout ── */

const MAX_CARDS = 13;
const SELECTED_CAT_DOCK_POS: Pos = { x: 50, y: 90 };

const CAT_IDLE_PRESETS: Record<number, Pos[]> = {
  1: [{ x: 50, y: 48 }],
  2: [{ x: 34, y: 42 }, { x: 66, y: 55 }],
  3: [{ x: 20, y: 38 }, { x: 55, y: 52 }, { x: 82, y: 36 }],
  4: [{ x: 16, y: 34 }, { x: 56, y: 26 }, { x: 82, y: 46 }, { x: 36, y: 62 }],
  5: [{ x: 14, y: 30 }, { x: 46, y: 22 }, { x: 82, y: 32 }, { x: 26, y: 60 }, { x: 68, y: 58 }],
};

const CAT_DOCK_PRESETS: Record<number, Pos[]> = {
  1: [{ x: 50, y: 90 }],
  2: [{ x: 36, y: 90 }, { x: 64, y: 90 }],
  3: [{ x: 18, y: 91 }, { x: 50, y: 89 }, { x: 82, y: 91 }],
  4: [{ x: 12, y: 91 }, { x: 37, y: 90 }, { x: 63, y: 90 }, { x: 88, y: 91 }],
  5: [{ x: 8, y: 92 }, { x: 28, y: 90 }, { x: 50, y: 89 }, { x: 72, y: 90 }, { x: 92, y: 92 }],
};

function clamp(v: number, lo: number, hi: number) {
  return Math.min(hi, Math.max(lo, v));
}

function getCategoryIdlePositions(total: number): Pos[] {
  const preset = CAT_IDLE_PRESETS[total];
  if (preset) return preset;

  const columns = Math.ceil(Math.sqrt(total * 1.35));
  const rows = Math.ceil(total / columns);
  const xMin = 12;
  const xMax = 88;
  const yMin = 24;
  const yMax = 66;
  const rowGap = rows > 1 ? (yMax - yMin) / (rows - 1) : 0;
  const columnGap = columns > 1 ? (xMax - xMin) / (columns - 1) : 0;

  return Array.from({ length: total }, (_, i) => {
    const row = Math.floor(i / columns);
    const col = i % columns;
    const rowCount = Math.min(columns, total - row * columns);
    const xBase = rowCount === 1 ? 50 : xMin + (col / (rowCount - 1)) * (xMax - xMin);
    const stagger = row % 2 === 1 ? columnGap * 0.28 : 0;

    return {
      x: clamp(xBase + stagger, 8, 92),
      y: clamp(rows === 1 ? 44 : yMin + row * rowGap, 18, 72),
    };
  });
}

function getCategoryDockPositions(total: number): Pos[] {
  const preset = CAT_DOCK_PRESETS[total];
  if (preset) return preset;

  const rows = total <= 6 ? 1 : 2;
  const perRow = Math.ceil(total / rows);
  const yPositions = rows === 1 ? [90] : [88, 95];

  return Array.from({ length: total }, (_, i) => {
    const row = Math.floor(i / perRow);
    const col = i % perRow;
    const rowCount = Math.min(perRow, total - row * perRow);
    const x = rowCount === 1 ? 50 : 8 + (col / (rowCount - 1)) * 84;

    return {
      x: clamp(x, 6, 94),
      y: yPositions[row] ?? 94,
    };
  });
}

function layoutCards(origin: Pos, total: number): { pos: Pos; layer: number }[] {
  if (total === 0) return [];

  if (total === 1) return [{ pos: { x: origin.x, y: origin.y - 14 }, layer: 0 }];
  if (total === 2) return [
    { pos: { x: origin.x - 13, y: origin.y - 16 }, layer: 0 },
    { pos: { x: origin.x + 13, y: origin.y - 16 }, layer: 0 },
  ];
  if (total === 3) return [
    { pos: { x: origin.x - 14, y: origin.y - 15 }, layer: 0 },
    { pos: { x: origin.x + 14, y: origin.y - 15 }, layer: 0 },
    { pos: { x: origin.x, y: origin.y - 33 }, layer: 1 },
  ];
  if (total <= 5) return [
    ...Array.from({ length: 2 }, (_, i) => ({
      pos: { x: origin.x + (i === 0 ? -14 : 14), y: origin.y - 15 }, layer: 0,
    })),
    ...Array.from({ length: total - 2 }, (_, i) => {
      const spread = total === 4 ? 16 : 20;
      const cnt = total - 2;
      return {
        pos: { x: clamp(cnt === 1 ? origin.x : origin.x - spread + (i / (cnt - 1)) * spread * 2, 3, 97), y: origin.y - 34 },
        layer: 1,
      };
    }),
  ];

  const rows: number[] = [];
  let remaining = total;
  let rowIdx = 0;
  const estimatedLayers = Math.max(1, Math.ceil(total / 2.5));

  while (remaining > 0) {
    if (remaining <= 2) { rows.push(remaining); break; }
    const progress = rowIdx / Math.max(estimatedLayers - 1, 1);
    const width = Math.round(2 + Math.pow(progress, 0.8) * 2);
    const count = Math.min(remaining, width);
    if (remaining - count === 1) { rows.push(count + 1); remaining = 0; }
    else { rows.push(count); remaining -= count; }
    rowIdx++;
  }

  const rowHeight = clamp(92 / rows.length, 8, 13);
  const out: { pos: Pos; layer: number }[] = [];

  for (let layer = 0; layer < rows.length; layer++) {
    const count = rows[layer];
    const y = clamp(origin.y - 15 - layer * rowHeight, 3, 88);
    const spread = 4 + count * 5;
    const nudge = (layer % 2) * 2.5 - 1.25;

    for (let i = 0; i < count; i++) {
      const x = count === 1
        ? origin.x + nudge
        : origin.x + nudge - spread + (i / (count - 1)) * spread * 2;
      out.push({ pos: { x: clamp(x, 3, 97), y }, layer });
    }
  }
  return out;
}

function curvePath(a: Pos, b: Pos, i: number): string {
  const mx = (a.x + b.x) / 2;
  const my = (a.y + b.y) / 2;
  const curve = clamp(Math.hypot(a.x - b.x, a.y - b.y) * 0.14, 2, 8);
  return `M${a.x} ${a.y} Q${mx + (i % 2 ? curve : -curve)} ${my - curve},${b.x} ${b.y}`;
}

function normalizeTag(tag: string): string {
  if (!tag) return '';
  // 앞의 #만 제거하고, 의미 있는 특수문자(+, #, ., -)는 보존
  return tag
    .trim()
    .toLowerCase()
    .replace(/^#/, '')
    .replace(/[^\w\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F+#.-]/g, '');
}

function buildTagLineIndices(cards: CardNode[]): TagLineIndices[] {
  const out: TagLineIndices[] = [];
  const cardTagSets = cards.map(c => {
    const rawTags = c.tags || [];
    const normalized = rawTags
      .map(t => normalizeTag(String(t)))
      .filter(Boolean);
    return new Set(normalized);
  });

  for (let i = 0; i < cards.length; i++) {
    const setA = cardTagSets[i];
    if (setA.size === 0) continue;
    for (let j = i + 1; j < cards.length; j++) {
      const setB = cardTagSets[j];
      if (setB.size === 0) continue;
      const commonTags = Array.from(setA).filter(tag => setB.has(tag));
      if (commonTags.length > 0) {
        out.push({ aIdx: i, bIdx: j, weight: commonTags.length });
      }
    }
  }
  return out;
}

/* ── damped spring ── */

type SV = { x: number; y: number; vx: number; vy: number };

function sv(x: number, y: number): SV { return { x, y, vx: 0, vy: 0 }; }

function svStep(s: SV, tx: number, ty: number, dt: number, k: number, c: number): SV {
  const ax = -k * (s.x - tx) - c * s.vx;
  const ay = -k * (s.y - ty) - c * s.vy;
  return { x: s.x + s.vx * dt, y: s.y + s.vy * dt, vx: s.vx + ax * dt, vy: s.vy + ay * dt };
}

function svDone(s: SV, tx: number, ty: number): boolean {
  return Math.abs(s.x - tx) < 0.04 && Math.abs(s.y - ty) < 0.04 && Math.abs(s.vx) < 0.04 && Math.abs(s.vy) < 0.04;
}

const STEM_DELAY = 100;
const STEM_GAP = 38;
const STEM_MS = 400;
const SUB_STEPS = 4;
const ORIGIN_K = 1000;
const ORIGIN_C = 55;

/* ================================================================ */
/*  Main                                                            */
/* ================================================================ */

export function KnowledgePlanetPrototype({ showMarkers = true }: { showMarkers?: boolean }) {
  const navigate = useNavigate();
  const [selectedCatId, setSelectedCatId] = useState<string | null>(null);
  const [hoveredCardId, setHoveredCardId] = useState<string | null>(null);
  const [probeCardId, setProbeCardId] = useState<string | null>(null);

  const catQuery = useArchiveCategories({ enabled: showMarkers });
  const cardQuery = useArchiveCategoryCards(selectedCatId);
  const hoverSimilarQuery = useArchiveCardTagRelations(hoveredCardId);
  const rootSimilarQuery = useArchiveCardTagRelations(probeCardId);
  const useFixtures = import.meta.env.DEV && import.meta.env.VITE_USE_GRAPH_FIXTURES !== 'false';

  /* ── categories ── */
  const categories = useMemo<CatNode[]>(() => {
    if (!showMarkers) return [];
    const apiCats = catQuery.data?.categories;
    if (!apiCats?.length && useFixtures) {
      const idle = getCategoryIdlePositions(graphFixtureCategories.length);
      const dock = getCategoryDockPositions(graphFixtureCategories.length);
      return graphFixtureCategories.map((c, i) => ({ id: c.id, name: c.name, count: 0, idlePos: idle[i], dockPos: dock[i] }));
    }
    if (!apiCats?.length) return [];
    const idle = getCategoryIdlePositions(apiCats.length);
    const dock = getCategoryDockPositions(apiCats.length);
    return apiCats.map((c, i) => ({ id: c.categoryId, name: c.categoryName, count: c.cardCount, idlePos: idle[i], dockPos: dock[i] }));
  }, [catQuery.data?.categories, showMarkers, useFixtures]);

  const selectedCat = categories.find(c => c.id === selectedCatId) ?? null;

  /* ── cards ── */
  const cards = useMemo<CardNode[]>(() => {
    if (!selectedCat) return [];
    const apiCards = cardQuery.data?.cards;
    let raw: { id: string; title: string; tags: string[]; time: number }[] = [];

    if (!apiCards?.length && useFixtures) {
      const leaves = graphFixtureLeavesByCategory[selectedCatId ?? 'nature'] ?? graphFixtureLeavesByCategory.nature ?? [];
      raw = leaves.map(l => ({ id: l.id, title: l.title, tags: l.tags, time: l.collectedAt ? new Date(l.collectedAt).getTime() : 0 }));
    } else if (apiCards?.length) {
      raw = apiCards.map(c => ({ id: c.cardId, title: c.title, tags: c.tags.map(t => t.tagName), time: new Date(c.createdAt).getTime() }));
    }

    raw.sort((a, b) => a.time - b.time);
    const capped = raw.slice(0, MAX_CARDS);
    const slots = layoutCards(SELECTED_CAT_DOCK_POS, capped.length);

    return capped.map((c, i) => ({
      id: c.id, title: c.title, tags: c.tags,
      pos: slots[i].pos, layer: slots[i].layer,
    }));
  }, [selectedCat, cardQuery.data?.cards, selectedCatId, useFixtures]);

  const tagLineIndices = useMemo(() => buildTagLineIndices(cards), [cards]);

  /* ── root links ── */
  useEffect(() => {
    if (!cards.length) { setProbeCardId(null); return; }
    const best = cards.reduce((a, b) => b.tags.length > a.tags.length ? b : a, cards[0]);
    setProbeCardId(best.id);
  }, [cards]);

  const rootLinks = useMemo(() => {
    if (!selectedCat || rootSimilarQuery.isError || !rootSimilarQuery.data?.relatedCards?.length) return [] as { catId: string; to: Pos }[];
    const seen = new Set<string>();
    const out: { catId: string; to: Pos }[] = [];
    try {
      for (const sc of rootSimilarQuery.data.relatedCards) {
        if (!sc) continue;
        const catId = sc.categoryId;
        if (!catId || catId === selectedCatId || seen.has(catId)) continue;
        const cat = categories.find(c => c.id === catId);
        if (!cat) continue;
        seen.add(catId);
        out.push({ catId, to: cat.idlePos });
      }
    } catch {
      return [];
    }
    return out;
  }, [selectedCat, rootSimilarQuery.data, rootSimilarQuery.isError, selectedCatId, categories]);

  const linkedCatIds = useMemo(() => new Set(rootLinks.map(r => r.catId)), [rootLinks]);

  /* ── hover state ── */
  const hoveredCard = hoveredCardId ? cards.find(c => c.id === hoveredCardId) ?? null : null;

  const hovRelatedIds = useMemo(() => {
    if (!hoveredCard) return new Set<string>();
    const hTags = hoveredCard.tags.map(normalizeTag).filter(Boolean);
    return new Set(
      cards
        .filter(c => {
          if (c.id === hoveredCard.id) return false;
          const cTags = c.tags.map(normalizeTag).filter(Boolean);
          return cTags.some(t => hTags.includes(t));
        })
        .map(c => c.id),
    );
  }, [hoveredCard, cards]);

  const crossCards = useMemo<CrossCard[]>(() => {
    if (!hoveredCard || hoverSimilarQuery.isError || !hoverSimilarQuery.data?.relatedCards?.length) return [];
    const out: { id: string; title: string; pos: Pos; catId: string }[] = [];
    try {
      const apiRelated = hoverSimilarQuery.data.relatedCards;
      for (const sc of apiRelated) {
        if (!sc) continue;
        const catId = sc.categoryId;
        if (!catId || catId === selectedCatId) continue;
        const cat = categories.find(c => c.id === catId);
        if (!cat) continue;
        const idx = out.length;
        out.push({
          id: sc.cardId, title: sc.title, catId,
          pos: { x: clamp(cat.idlePos.x + ((idx % 3) - 1) * 9, 4, 96), y: clamp(cat.idlePos.y - 16 - Math.floor(idx / 3) * 10, 4, 74) },
        });
        if (out.length >= 5) break;
      }
    } catch {
      return [];
    }
    return out;
  }, [hoveredCard, hoverSimilarQuery.data, hoverSimilarQuery.isError, selectedCatId, categories]);

  const crossCatIds = useMemo(() => new Set(crossCards.map(c => c.catId)), [crossCards]);

  /* ── category spring positions ── */
  const catSpringsRef = useRef<SV[]>([]);
  const catLoopRef = useRef(0);
  const catLastTRef = useRef(performance.now());
  const [catPositions, setCatPositions] = useState<Pos[]>(() => categories.map(c => c.idlePos));

  useEffect(() => {
    while (catSpringsRef.current.length < categories.length) {
      const c = categories[catSpringsRef.current.length];
      catSpringsRef.current.push(sv(c.idlePos.x, c.idlePos.y));
    }
  }, [categories]);

  useEffect(() => {
    catLastTRef.current = performance.now();
    const tick = (now: number) => {
      const dt = Math.min((now - catLastTRef.current) / 1000, 0.033);
      catLastTRef.current = now;
      const sub = dt / SUB_STEPS;
      let moving = false;

      categories.forEach((cat, i) => {
        const s = catSpringsRef.current[i];
        if (!s) return;
        const isSelected = selectedCatId === cat.id;
        const t = isSelected ? SELECTED_CAT_DOCK_POS : cat.idlePos;
        for (let step = 0; step < SUB_STEPS; step++) {
          catSpringsRef.current[i] = svStep(s, t.x, t.y, sub, ORIGIN_K, ORIGIN_C);
        }
        if (!svDone(catSpringsRef.current[i], t.x, t.y)) moving = true;
        else catSpringsRef.current[i] = sv(t.x, t.y);
      });

      setCatPositions(catSpringsRef.current.map(s => ({ x: s.x, y: s.y })));
      if (moving) catLoopRef.current = requestAnimationFrame(tick);
    };
    catLoopRef.current = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(catLoopRef.current);
  }, [selectedCatId, categories]);

  const selIdx = categories.findIndex(c => c.id === selectedCatId);
  const selCatLivePos = selIdx >= 0 ? catPositions[selIdx] : null;

  return (
    <section className="relative font-sans text-text-primary" style={{ minHeight: 'calc(100vh - var(--dashboard-nav-offset, 88px))' }}>
      <BG />
      {showMarkers && catQuery.isPending ? (
        <Msg><Loader2 size={24} className="animate-spin text-action-accent" /><span className="text-sm text-text-secondary">지식 숲을 불러오는 중...</span></Msg>
      ) : showMarkers && !catQuery.isPending && !categories.length ? (
        <Msg><FolderOpen size={28} className="text-text-secondary/40" /><p className="text-sm text-text-secondary">표시할 카테고리가 없습니다.</p></Msg>
      ) : (
        <div className="relative h-full w-full" style={{ minHeight: 'calc(100vh - var(--dashboard-nav-offset, 88px))' }} onClick={() => selectedCatId && setSelectedCatId(null)} role="presentation">
          {categories.map((cat, i) => {
            const pos = catPositions[i] ?? cat.idlePos;
            const isSelected = selectedCatId === cat.id;
            const isLinked = linkedCatIds.has(cat.id) || crossCatIds.has(cat.id);
            const hasSelection = selectedCatId !== null;
            return (
              <button key={cat.id} type="button"
                onClick={e => { e.stopPropagation(); setHoveredCardId(null); setSelectedCatId(prev => prev === cat.id ? null : cat.id); }}
                className="absolute transition-[opacity,filter] duration-300"
                style={{
                  left: `${pos.x}%`, top: `${pos.y}%`,
                  zIndex: isSelected ? 30 : hasSelection ? 1 : 5,
                  opacity: hasSelection && !isSelected ? (isLinked ? 0.7 : 0.2) : 1,
                  transform: `translate(-50%,-50%) scale(${isSelected ? 1.1 : 1})`,
                }}>
                <div className={`flex min-w-[7.5rem] flex-col gap-1.5 rounded-tl-[20px] rounded-br-[20px] rounded-tr-lg rounded-bl-lg border px-4 py-3 glass-card !shadow-none transition-colors duration-300 ${isSelected ? 'border-action-accent/60 bg-action-accent/10' : isLinked ? 'border-action-accent/40 bg-action-accent/5' : 'border-action-accent/25 bg-surface-container/80 hover:border-action-accent/50 hover:bg-surface-container'}`}>
                  <span className="text-xs font-bold uppercase tracking-wider text-text-primary">{cat.name}</span>
                  <span className="text-[11px] text-text-secondary">{cat.count} cards</span>
                </div>
              </button>
            );
          })}
          {rootLinks.length > 0 && selectedCat && (
            <RootLines key={`rl-${selectedCatId}`} from={catPositions[selIdx] ?? SELECTED_CAT_DOCK_POS} links={rootLinks} />
          )}
          {selectedCatId && cards.length > 0 && selectedCat && selCatLivePos && (
            <CardGraph key={selectedCatId}
              catPos={selCatLivePos} dockPos={SELECTED_CAT_DOCK_POS}
              cards={cards} tagLineIndices={tagLineIndices}
              hoveredCardId={hoveredCardId} hovRelatedIds={hovRelatedIds}
              onHover={setHoveredCardId} onCardClick={id => navigate(`/cards/${id}`)} />
          )}
          {hoveredCard && crossCards.length > 0 && (
            <CrossBridge key={hoveredCardId} from={hoveredCard.pos} cards={crossCards} />
          )}
          {selectedCatId && cardQuery.isPending && (
            <div className="absolute inset-0 grid place-items-center" style={{ zIndex: 10 }}>
              <Loader2 size={20} className="animate-spin text-action-accent/40" />
            </div>
          )}
        </div>
      )}
    </section>
  );
}

/* ================================================================ */
/*  CardGraph                                                       */
/* ================================================================ */

function CardGraph({ catPos, dockPos, cards, tagLineIndices, hoveredCardId, hovRelatedIds, onHover, onCardClick }: {
  catPos: Pos; dockPos: Pos; cards: CardNode[]; tagLineIndices: TagLineIndices[];
  hoveredCardId: string | null; hovRelatedIds: Set<string>;
  onHover: (id: string | null) => void; onCardClick: (id: string) => void;
}) {
  const cardSpringsRef = useRef<SV[]>([]);
  if (cardSpringsRef.current.length !== cards.length) {
    cardSpringsRef.current = cards.map(() => sv(catPos.x, catPos.y));
  }
  const stemStartRef = useRef(performance.now());
  const lastTRef = useRef(performance.now());
  const frameRef = useRef(0);
  const catPosRef = useRef(catPos);
  catPosRef.current = catPos;
  const [, bump] = useState(0);
  const [settled, setSettled] = useState(false);
  const isHovering = hoveredCardId !== null;

  useEffect(() => {
    cardSpringsRef.current = cards.map(() => sv(catPosRef.current.x, catPosRef.current.y));
    stemStartRef.current = performance.now();
    lastTRef.current = performance.now();
    setSettled(false);
    const totalDur = STEM_DELAY + cards.length * STEM_GAP + STEM_MS + 400;
    const loop = (now: number) => {
      const dt = Math.min((now - lastTRef.current) / 1000, 0.033);
      lastTRef.current = now;
      const sub = dt / SUB_STEPS;
      const cp = catPosRef.current;
      const catDx = cp.x - dockPos.x;
      const catDy = cp.y - dockPos.y;
      cardSpringsRef.current.forEach((cs, i) => {
        const dist = Math.hypot(cards[i].pos.x - dockPos.x, cards[i].pos.y - dockPos.y);
        const pull = clamp(1 - dist / 60, 0.15, 0.85);
        const tx = cards[i].pos.x + catDx * pull;
        const ty = cards[i].pos.y + catDy * pull;
        const k = clamp(200 - dist * 2.5, 60, 200);
        const c = clamp(20 - dist * 0.25, 8, 20);
        for (let s = 0; s < SUB_STEPS; s++) {
          cardSpringsRef.current[i] = svStep(cardSpringsRef.current[i], tx, ty, sub, k, c);
        }
      });
      const stemElapsed = now - stemStartRef.current;
      const catSettled = Math.abs(catDx) < 0.1 && Math.abs(catDy) < 0.1;
      const cDone = cardSpringsRef.current.every((cs, i) => svDone(cs, cards[i].pos.x, cards[i].pos.y));
      const allDone = catSettled && cDone && stemElapsed > totalDur;
      if (allDone) {
        cardSpringsRef.current = cards.map(cd => sv(cd.pos.x, cd.pos.y));
        setSettled(true);
      }
      bump(t => t + 1);
      if (!allDone) frameRef.current = requestAnimationFrame(loop);
    };
    frameRef.current = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(frameRef.current);
  }, [cards, dockPos.x, dockPos.y]);

  const stemElapsed = performance.now() - stemStartRef.current;
  const showConn = stemElapsed > STEM_DELAY + cards.length * STEM_GAP + STEM_MS;

  return (
    <>
      <svg aria-hidden className="pointer-events-none absolute inset-0 h-full w-full" style={{ zIndex: 12 }} viewBox="0 0 100 100" preserveAspectRatio="none">
        {cards.map((card, i) => {
          const cs = cardSpringsRef.current[i];
          const cp = settled ? card.pos : { x: cs?.x ?? card.pos.x, y: cs?.y ?? card.pos.y };
          const tStart = STEM_DELAY + i * STEM_GAP;
          const progress = clamp((stemElapsed - tStart) / STEM_MS, 0, 1);
          const dashOff = 1 - progress * progress * (3 - 2 * progress);
          const d = stemPath(catPos, cp, i);
          const active = !isHovering || hoveredCardId === card.id || hovRelatedIds.has(card.id);
          const baseOp = active ? 0.25 : 0.05;
          return (
            <g key={`stem-${card.id}`}>
              <path d={d} pathLength={1} fill="none"
                stroke={`rgba(74,222,128,${baseOp * 0.3})`} strokeWidth="1.2"
                strokeDasharray="1" strokeDashoffset={dashOff}
                style={{ transition: settled ? 'stroke 200ms ease' : 'none' }} />
              <path d={d} pathLength={1} fill="none"
                stroke={`rgba(74,222,128,${baseOp})`} strokeWidth="0.2"
                strokeDasharray="1" strokeDashoffset={dashOff}
                style={{ transition: settled ? 'stroke 200ms ease' : 'none' }} />
            </g>
          );
        })}
        {tagLineIndices.map(({ aIdx, bIdx, weight }, idx) => {
          const aId = cards[aIdx].id;
          const bId = cards[bIdx].id;
          const isRelevant = isHovering && (aId === hoveredCardId || bId === hoveredCardId || (hovRelatedIds.has(aId) && hovRelatedIds.has(bId)));
          const op = isHovering
            ? (isRelevant ? clamp(0.4 + weight * 0.1, 0.4, 0.8) : 0.05)
            : clamp(0.15 + weight * 0.08, 0.15, 0.45);
          const aS = cardSpringsRef.current[aIdx];
          const bS = cardSpringsRef.current[bIdx];
          const af = settled ? cards[aIdx].pos : { x: aS?.x ?? cards[aIdx].pos.x, y: aS?.y ?? cards[aIdx].pos.y };
          const bf = settled ? cards[bIdx].pos : { x: bS?.x ?? cards[bIdx].pos.x, y: bS?.y ?? cards[bIdx].pos.y };
          return (
            <path key={idx} d={curvePath(af, bf, idx)} fill="none"
              stroke={`rgba(74,222,128,${op})`}
              strokeWidth={isRelevant ? '0.35' : '0.22'} strokeDasharray="1 0.6"
              style={{ opacity: showConn ? 1 : 0, transition: 'opacity 500ms ease, stroke 200ms ease, stroke-width 200ms ease' }} />
          );
        })}
      </svg>
      {cards.map((card, i) => {
        const cs = cardSpringsRef.current[i];
        const cp = settled ? card.pos : { x: cs?.x ?? card.pos.x, y: cs?.y ?? card.pos.y };
        const tStart = STEM_DELAY + i * STEM_GAP;
        const stemT = clamp((stemElapsed - tStart) / STEM_MS, 0, 1);
        const bloom = clamp((stemT - 0.7) / 0.3, 0, 1);
        const isThis = hoveredCardId === card.id;
        const isRelated = hovRelatedIds.has(card.id);
        const isDimmed = isHovering && !isThis && !isRelated;
        return (
          <button key={card.id} type="button"
            onMouseEnter={() => onHover(card.id)} onMouseLeave={() => onHover(null)}
            onClick={e => { e.stopPropagation(); onCardClick(card.id); }}
            className="absolute"
            style={{
              left: `${cp.x}%`, top: `${cp.y}%`, zIndex: isThis ? 25 : 20,
              opacity: settled ? (isDimmed ? 0.18 : 1) : bloom,
              transform: `translate(-50%,-50%) scale(${settled ? (isThis ? 1.06 : 1) : 0.4 + bloom * 0.6})`,
              transition: settled ? 'opacity 300ms ease-out, transform 300ms ease-out' : 'none',
            }}>
            <div className={`flex w-[8.5rem] flex-col gap-1 rounded-tl-[14px] rounded-br-[14px] rounded-tr-md rounded-bl-md border px-3 py-2.5 glass-card !shadow-none transition-colors duration-200 ${isThis ? 'border-action-accent/60 bg-action-accent/8' : isRelated ? 'border-action-accent/35 bg-surface-container/90' : 'border-action-accent/18 bg-surface-container/80 hover:border-action-accent/34 hover:bg-surface-container'}`}>
              <span className={`line-clamp-2 text-[11px] font-semibold leading-[1.4] ${isThis ? 'text-text-primary' : 'text-text-secondary'}`}>
                {card.title}
              </span>
              {card.tags.length > 0 && (
                <span className="truncate text-[10px] text-action-accent/60">
                  {card.tags.slice(0, 2).map(t => `#${t}`).join(' ')}
                </span>
              )}
            </div>
          </button>
        );
      })}
    </>
  );
}

/* ================================================================ */
/*  RootLines                                                       */
/* ================================================================ */

function RootLines({ from, links }: { from: Pos; links: { catId: string; to: Pos }[] }) {
  const [show, setShow] = useState(false);
  useEffect(() => { const t = window.setTimeout(() => setShow(true), 300); return () => window.clearTimeout(t); }, []);
  return (
    <svg aria-hidden className="pointer-events-none absolute inset-0 h-full w-full" style={{ zIndex: 2 }} viewBox="0 0 100 100" preserveAspectRatio="none">
      {links.map((lk, i) => {
        const sag = 5 + Math.abs(from.x - lk.to.x) * 0.05;
        const d = `M${from.x} ${from.y} C${from.x} ${from.y + sag},${lk.to.x} ${lk.to.y + sag},${lk.to.x} ${lk.to.y}`;
        return (
          <g key={lk.catId}>
            <path d={d} pathLength={1} fill="none" stroke="rgba(74,222,128,0.06)" strokeWidth="1.4"
              strokeDasharray="1" strokeDashoffset={show ? 0 : 1}
              style={{ transition: `stroke-dashoffset 700ms ease-out ${i * 100}ms` }} />
            <path d={d} pathLength={1} fill="none" stroke="rgba(74,222,128,0.16)" strokeWidth="0.22" strokeDasharray="1.5 1"
              strokeDashoffset={show ? 0 : 1}
              style={{ transition: `stroke-dashoffset 700ms ease-out ${i * 100}ms` }} />
          </g>
        );
      })}
    </svg>
  );
}

/* ================================================================ */
/*  CrossBridge                                                     */
/* ================================================================ */

function CrossBridge({ from, cards }: { from: Pos; cards: CrossCard[] }) {
  const [show, setShow] = useState(false);
  useEffect(() => { const t = window.setTimeout(() => setShow(true), 50); return () => window.clearTimeout(t); }, []);
  return (
    <>
      <svg aria-hidden className="pointer-events-none absolute inset-0 h-full w-full" style={{ zIndex: 18 }} viewBox="0 0 100 100" preserveAspectRatio="none">
        {cards.map((card, i) => {
          const midY = (from.y + card.pos.y) / 2;
          const d = `M${from.x} ${from.y} C${from.x} ${midY},${card.pos.x} ${midY},${card.pos.x} ${card.pos.y}`;
          return (
            <path key={card.id} d={d} pathLength={1} fill="none"
              stroke="rgba(74,222,128,0.3)" strokeWidth="0.2" strokeDasharray="1.2 0.6"
              strokeDashoffset={show ? 0 : 1}
              style={{ transition: `stroke-dashoffset 450ms ease-out ${i * 60}ms` }} />
          );
        })}
      </svg>
      {cards.map((card, i) => (
        <div key={card.id} className="pointer-events-none absolute" style={{
          left: `${card.pos.x}%`, top: `${card.pos.y}%`, zIndex: 22,
          opacity: show ? 1 : 0,
          transform: show ? 'translate(-50%,-50%) scale(1)' : 'translate(-50%,10%) scale(0.85)',
          transition: `opacity 250ms ease-out ${i * 60 + 200}ms, transform 300ms ease-out ${i * 60 + 200}ms`,
        }}>
          <div className="flex w-[7.5rem] flex-col gap-0.5 rounded-tl-[12px] rounded-br-[12px] rounded-tr-md rounded-bl-md border border-action-accent/30 bg-action-accent/5 px-2.5 py-2 glass-card !shadow-none">
            <span className="line-clamp-1 text-[10px] font-semibold text-action-accent">{card.title}</span>
          </div>
        </div>
      ))}
    </>
  );
}

/* ── utilities ── */

function stemPath(from: Pos, to: Pos, i: number): string {
  const dx = to.x - from.x;
  const dy = to.y - from.y;
  const bend = ((i % 5) - 2) * 1.2;
  return `M${from.x} ${from.y} C${from.x + bend} ${from.y + dy * 0.35},${from.x + dx * 0.75} ${to.y - dy * 0.06},${to.x} ${to.y}`;
}

function Msg({ children }: { children: ReactNode }) {
  return <div className="absolute inset-0 z-10 grid place-items-center"><div className="flex flex-col items-center gap-3 text-center">{children}</div></div>;
}

function BG() {
  return (
    <div className="pointer-events-none absolute -inset-48 overflow-visible">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_62%,var(--color-action-accent,#4ade80)_0%,transparent_45%)] opacity-[0.05]" />
      <div className="absolute left-1/2 top-[60%] h-[45rem] w-[45rem] -translate-x-1/2 -translate-y-1/2 rounded-full bg-action-accent/[0.06] blur-[90px]" />
    </div>
  );
}
