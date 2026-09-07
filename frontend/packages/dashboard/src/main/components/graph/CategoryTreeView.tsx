import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import bareTreeSource from '../../../assets/ph2_bare_tree_tp.png';
import type { GraphLeaf } from '../../types/graph';

type CategoryTreeViewProps = {
  leaves: GraphLeaf[];
  isPending: boolean;
  selectedCategoryId: string | null;
  enterToken: number;
};

type LeafSlot = {
  x: number;
  y: number;
  rotate: number;
  scale: number;
  opacity: number;
};

type ThreadStyle = {
  strokeWidth: number;
  opacity: number;
  glowOpacity: number;
};

type PositionedLeaf = {
  leaf: GraphLeaf;
  position: LeafSlot;
  index: number;
};

const leafPositions: LeafSlot[] = [
  { x: 34, y: 36, rotate: -4, scale: 1.02, opacity: 0.94 },
  { x: 44, y: 35, rotate: -2, scale: 0.98, opacity: 0.92 },
  { x: 53, y: 37, rotate: 1, scale: 1.06, opacity: 1 },
  { x: 63, y: 38, rotate: 4, scale: 0.98, opacity: 0.93 },
  { x: 72, y: 40, rotate: 6, scale: 0.92, opacity: 0.9 },
  { x: 38, y: 42, rotate: -1, scale: 0.95, opacity: 0.9 },
  { x: 49, y: 44, rotate: -5, scale: 0.96, opacity: 0.92 },
  { x: 22, y: 51, rotate: -30, scale: 0.88, opacity: 0.86 },
  { x: 29, y: 49, rotate: -24, scale: 0.9, opacity: 0.88 },
  { x: 37, y: 51, rotate: -18, scale: 0.94, opacity: 0.9 },
  { x: 46, y: 52, rotate: -10, scale: 0.98, opacity: 0.94 },
  { x: 34, y: 56, rotate: -22, scale: 0.88, opacity: 0.86 },
  { x: 58, y: 49, rotate: 22, scale: 0.92, opacity: 0.9 },
  { x: 65, y: 52, rotate: 28, scale: 0.94, opacity: 0.92 },
  { x: 72, y: 55, rotate: 32, scale: 0.88, opacity: 0.86 },
  { x: 78, y: 52, rotate: 26, scale: 0.84, opacity: 0.82 },
  { x: 61, y: 60, rotate: 20, scale: 0.93, opacity: 0.9 },
  { x: 18, y: 66, rotate: -36, scale: 0.86, opacity: 0.82 },
  { x: 26, y: 69, rotate: -31, scale: 0.9, opacity: 0.86 },
  { x: 33, y: 71, rotate: -27, scale: 0.88, opacity: 0.84 },
  { x: 57, y: 67, rotate: 28, scale: 0.9, opacity: 0.86 },
  { x: 66, y: 70, rotate: 34, scale: 0.88, opacity: 0.84 },
  { x: 74, y: 72, rotate: 38, scale: 0.84, opacity: 0.8 },
  { x: 47, y: 73, rotate: -6, scale: 0.92, opacity: 0.88 },
];

function normalizeTag(tag: string): string {
  if (!tag) return '';
  // 앞의 #만 제거하고, 의미 있는 특수문자(+, #, ., -)는 보존
  return tag
    .trim()
    .toLowerCase()
    .replace(/^#/, '')
    .replace(/[^\w\uAC00-\uD7AF\u1100-\u11FF\u3130-\u318F+#.-]/g, '');
}

export function CategoryTreeView({ leaves, isPending, selectedCategoryId, enterToken }: CategoryTreeViewProps) {
  const navigate = useNavigate();
  const [activeLeafId, setActiveLeafId] = useState<string | null>(null);
  const [treeVisible, setTreeVisible] = useState(false);
  const visibleLeaves = leaves.slice(0, 24);
  const hiddenLeafCount = Math.max(leaves.length - 24, 0);

  const positionedLeaves: PositionedLeaf[] = useMemo(
    () =>
      visibleLeaves.map((leaf, index) => ({
        leaf,
        position: leafPositions[index % leafPositions.length],
        index,
      })),
    [visibleLeaves],
  );

  const leafById = useMemo(() => new Map(positionedLeaves.map((entry) => [entry.leaf.id, entry])), [positionedLeaves]);
  const activeLeaf = activeLeafId ? leafById.get(activeLeafId)?.leaf ?? null : null;
  const activeLeafPosition = activeLeafId ? leafById.get(activeLeafId)?.position ?? null : null;

  useEffect(() => {
    setActiveLeafId(null);
    setTreeVisible(false);
    const timer = window.setTimeout(() => setTreeVisible(true), 24);
    return () => window.clearTimeout(timer);
  }, [enterToken, selectedCategoryId]);

  const relatedThreads = useMemo(() => {
    if (!activeLeaf || !activeLeafPosition) return [];

    const activeTags = activeLeaf.tags.map(normalizeTag).filter(Boolean);

    return positionedLeaves
      .filter(({ leaf }) => leaf.id !== activeLeaf.id)
      .map(({ leaf, position, index }) => {
        const leafTags = leaf.tags.map(normalizeTag).filter(Boolean);
        const sharedTags = leafTags.filter((tag) => activeTags.includes(tag));
        const overlapCount = sharedTags.length;
        if (overlapCount === 0) return null;

        return {
          id: `${activeLeaf.id}-${leaf.id}`,
          leaf,
          position,
          index,
          overlapCount,
          path: createCurvePath(activeLeafPosition, position, index),
        };
      })
      .filter((thread): thread is NonNullable<typeof thread> => thread !== null);
  }, [activeLeaf, activeLeafPosition, positionedLeaves]);

  const relatedLeafIds = useMemo(() => new Set(relatedThreads.map((thread) => thread.leaf.id)), [relatedThreads]);
  const hasActiveFocus = activeLeafId !== null;

  return (
    <div className="absolute inset-0 flex h-full w-full items-center justify-center">
      <div className="relative h-full w-full max-w-[72rem] overflow-visible" style={{ background: 'transparent', backgroundColor: 'transparent' }}>

        <div className="absolute inset-0 flex items-center justify-center">
          <div className="relative h-[min(88vh,52rem)] w-[min(96vw,72rem)] min-h-[42rem] overflow-visible" style={{ background: 'transparent', backgroundColor: 'transparent' }}>
            <div
              className="absolute inset-0"
              style={{
                background: 'transparent',
                backgroundColor: 'transparent',
                opacity: treeVisible ? 1 : 0,
                transform: treeVisible ? 'translateY(0) scale(1)' : 'translateY(26px) scale(0.975)',
                transformOrigin: 'center bottom',
                filter: treeVisible ? 'blur(0px)' : 'blur(2.4px)',
                willChange: 'transform, opacity, filter',
                transition:
                  'opacity 1250ms cubic-bezier(0.22, 1, 0.36, 1), transform 1400ms cubic-bezier(0.22, 1, 0.36, 1), filter 1250ms cubic-bezier(0.22, 1, 0.36, 1)',
              }}
            >
              <img
                src={bareTreeSource}
                alt=""
                aria-hidden="true"
                className="absolute left-1/2 top-[-20%] h-[148%] w-[148%] -translate-x-1/2 object-cover object-top opacity-95"
                style={{ background: 'transparent', backgroundColor: 'transparent' }}
              />
              <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-x-0 bottom-0"
                style={{
                  height: '128%',
                  transform: treeVisible ? 'translateY(-106%)' : 'translateY(0%)',
                  background:
                    'linear-gradient(to top, rgba(16,20,23,0.98) 0%, rgba(16,20,23,0.98) 18%, rgba(16,20,23,0.88) 30%, rgba(16,20,23,0.58) 46%, rgba(16,20,23,0.18) 66%, rgba(16,20,23,0) 82%)',
                  transition: 'transform 1400ms cubic-bezier(0.22, 1, 0.36, 1)',
                }}
              />
              <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-x-0 bottom-[22%]"
                style={{
                  height: '2.2rem',
                  transform: treeVisible ? 'translateY(-28rem)' : 'translateY(0)',
                  opacity: treeVisible ? 0 : 1,
                  background:
                    'linear-gradient(to top, rgba(74,222,128,0) 0%, rgba(74,222,128,0.32) 45%, rgba(74,222,128,0.06) 72%, transparent 100%)',
                  filter: 'blur(10px)',
                  transition:
                    'transform 1400ms cubic-bezier(0.22, 1, 0.36, 1), opacity 1250ms cubic-bezier(0.22, 1, 0.36, 1)',
                }}
              />
              <div
                className="pointer-events-none absolute inset-0"
                style={{
                  background:
                    'linear-gradient(to top, rgba(16,20,23,0.98) 0%, rgba(16,20,23,0.94) 14%, rgba(16,20,23,0.72) 28%, rgba(16,20,23,0.34) 48%, rgba(16,20,23,0.08) 68%, transparent 82%)',
                  opacity: treeVisible ? 0 : 1,
                  transform: treeVisible ? 'translateY(-22px)' : 'translateY(28px)',
                  transition:
                    'opacity 1250ms cubic-bezier(0.22, 1, 0.36, 1), transform 1400ms cubic-bezier(0.22, 1, 0.36, 1)',
                }}
              />
            </div>

            {relatedThreads.length > 0 ? (
              <svg
                aria-hidden="true"
                className="pointer-events-none absolute inset-0 z-[3] h-full w-full"
                viewBox="0 0 100 100"
                preserveAspectRatio="none"
              >
                <defs>
                  <filter id="tree-thread-glow" x="-20%" y="-20%" width="140%" height="140%">
                    <feGaussianBlur stdDeviation="1" result="blur" />
                    <feColorMatrix
                      in="blur"
                      type="matrix"
                      values="
                        1 0 0 0 0
                        0 1 0 0 0
                        0 0 1 0 0
                        0 0 0 0.9 0"
                    />
                  </filter>
                </defs>
                {relatedThreads.map(({ id, path, overlapCount }) => {
                  const style = getThreadStyle(overlapCount);
                  return (
                    <g key={id}>
                      <path
                        d={path}
                        fill="none"
                        stroke={`rgba(134,230,144,${style.glowOpacity})`}
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={0.95}
                        filter="url(#tree-thread-glow)"
                      />
                      <path
                        d={path}
                        fill="none"
                        stroke={`rgba(134,230,144,${style.opacity})`}
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={style.strokeWidth}
                      />
                    </g>
                  );
                })}
              </svg>
            ) : null}

            <div className="absolute inset-0 z-[4]">
              {positionedLeaves.map(({ leaf, position, index }) => {
                const isActive = activeLeafId === leaf.id;
                const isRelated = activeLeafId ? relatedLeafIds.has(leaf.id) : false;
                const leafOpacity = hasActiveFocus
                  ? isActive
                    ? 1
                    : isRelated
                      ? 0.76
                      : 0.4
                  : position.opacity;
                const leafColor = hasActiveFocus
                  ? isActive
                    ? 'rgba(134,230,144,1)'
                    : isRelated
                      ? 'rgba(74,222,128,0.55)'
                      : 'rgba(74,222,128,0.25)'
                  : 'rgba(74,222,128,0.7)';
                const leafShapeClass = position.x < 50 ? 'rounded-leaf-reverse' : 'rounded-leaf';

                return (
                  <button
                    key={leaf.id}
                    type="button"
                    onMouseEnter={() => setActiveLeafId(leaf.id)}
                    onMouseLeave={() => setActiveLeafId(null)}
                    onFocus={() => setActiveLeafId(leaf.id)}
                    onBlur={() => setActiveLeafId(null)}
                    onClick={() => navigate(`/cards/${leaf.id}`)}
                    className="absolute cursor-pointer border-0 bg-transparent p-0 outline-none transition-[opacity,transform,filter] duration-300 ease-out"
                    style={{
                      top: `${position.y}%`,
                      left: `${position.x}%`,
                      opacity: treeVisible ? leafOpacity : 0,
                      transform: `translate(-50%, -50%) translateY(${treeVisible ? 0 : 10}px) scale(${
                        (isActive ? 1.12 : 1) * position.scale
                      }) rotate(${position.rotate}deg)`,
                      transitionDelay: treeVisible ? `${120 + index * 78}ms` : '0ms',
                      zIndex: isActive ? 20 : isRelated ? 12 : 6,
                      color: leafColor,
                      filter: hasActiveFocus
                        ? isActive
                          ? 'drop-shadow(0 0 10px rgba(74,222,128,0.52))'
                          : isRelated
                            ? 'drop-shadow(0 0 8px rgba(74,222,128,0.22))'
                            : 'drop-shadow(0 0 3px rgba(74,222,128,0.06))'
                        : 'drop-shadow(0 0 3px rgba(74,222,128,0.08))',
                    }}
                    >
                    <span
                      aria-hidden="true"
                      className={`relative block transition-[opacity,transform,filter] duration-180 ease-out ${leafShapeClass}`}
                      style={{
                        width: 38,
                        height: 52,
                        opacity: hasActiveFocus ? (isActive ? 1 : isRelated ? 0.62 : 0.2) : leafOpacity,
                        transform: `rotate(${position.rotate}deg) scale(${isActive ? 1.12 : 1})`,
                        transformOrigin: '50% 72%',
                        filter: hasActiveFocus
                          ? isActive
                            ? 'saturate(1.08) brightness(1.08)'
                            : isRelated
                              ? 'saturate(1.02) brightness(1.02)'
                              : 'saturate(0.95) brightness(0.92)'
                          : 'saturate(1) brightness(1)',
                      }}
                    >
                      <span
                        className={`absolute inset-0 ${leafShapeClass}`}
                        style={{
                          background:
                            'linear-gradient(160deg, rgba(134,230,144,0.95) 0%, rgba(74,222,128,0.88) 45%, rgba(45,160,84,0.82) 100%)',
                          boxShadow: isActive
                            ? '0 0 16px rgba(74,222,128,0.72), 0 0 30px rgba(74,222,128,0.28)'
                            : isRelated
                              ? '0 0 10px rgba(74,222,128,0.38)'
                              : '0 0 6px rgba(74,222,128,0.32)',
                          opacity: isActive ? 1 : isRelated ? 0.58 : 0.75,
                          transition:
                            'opacity 180ms ease-out, transform 180ms ease-out, filter 180ms ease-out, box-shadow 180ms ease-out',
                        }}
                      />
                      <span
                        aria-hidden="true"
                        style={{
                          position: 'absolute',
                          top: '12%',
                          left: '15%',
                          width: '25%',
                          height: '35%',
                          background: 'color-mix(in oklab, var(--color-text-primary) 25%, transparent)',
                          borderRadius: '50%',
                          filter: 'blur(1.5px)',
                          transform: 'rotate(-15deg)',
                          opacity: isActive ? 1 : 0.85,
                          transition: 'opacity 180ms ease-out',
                        }}
                      />
                    </span>
                    <span
                      className="pointer-events-none absolute left-1/2 top-[28px] -translate-x-1/2 translate-y-1 whitespace-nowrap rounded-full border border-text-secondary/10 glass-panel bg-surface-lowest/70 px-3 py-1 text-xs text-text-primary/78 opacity-0 shadow-[0_10px_26px_rgba(0,0,0,0.24)] backdrop-blur-md transition-[opacity,transform] duration-180 ease-out"
                      style={{
                        opacity: isActive ? 1 : 0,
                        transform: isActive ? 'translate(-50%, 0)' : 'translate(-50%, 4px)',
                      }}
                    >
                      {leaf.title}
                    </span>
                  </button>
                );
              })}

              {hiddenLeafCount > 0 ? (
                <button
                  type="button"
                  onClick={() => navigate('/result')}
                  className="absolute left-[74%] top-[58%] -translate-x-1/2 -translate-y-1/2 rounded-full border border-text-secondary/12 glass-popover bg-surface-lowest/75 px-3 py-1 text-xs text-text-primary/78 backdrop-blur-sm transition hover:bg-surface-container/90 bg-surface-container/80"
                  style={{
                    opacity: activeLeafId ? 0.78 : 0.9,
                    transform: 'translate(-50%, -50%)',
                  }}
                >
                  +{hiddenLeafCount}
                </button>
              ) : null}
            </div>
          </div>
        </div>

        {!isPending && leaves.length === 0 ? (
          <div className="absolute inset-x-0 bottom-8 text-center">
            <p className="text-sm text-text-primary/58">이 카테고리에는 아직 지식 카드가 없습니다.</p>
            <p className="mt-2 text-xs text-text-primary/38">새 지식을 저장하면 가지 끝에 잎이 자라납니다.</p>
          </div>
        ) : null}
      </div>
    </div>
  );
}

function createCurvePath(from: { x: number; y: number }, to: { x: number; y: number }, index: number) {
  const midX = (from.x + to.x) / 2;
  const midY = (from.y + to.y) / 2;
  const distanceX = Math.abs(from.x - to.x);
  const distanceY = Math.abs(from.y - to.y);
  const curveDistance = clamp(10 + Math.max(distanceX, distanceY) * 0.15, 10, 22);
  const direction = index % 2 === 0 ? 1 : -1;
  const controlBias = clamp(distanceX * 0.08, 2, 8) * direction;

  return `M ${from.x} ${from.y} C ${midX + controlBias} ${midY - curveDistance}, ${midX - controlBias} ${midY + curveDistance}, ${to.x} ${to.y}`;
}

function getThreadStyle(overlapCount: number): ThreadStyle {
  if (overlapCount >= 4) {
    return { strokeWidth: 0.35, opacity: 0.3, glowOpacity: 0.22 };
  }

  if (overlapCount === 3) {
    return { strokeWidth: 0.35, opacity: 0.22, glowOpacity: 0.16 };
  }

  if (overlapCount === 2) {
    return { strokeWidth: 0.35, opacity: 0.16, glowOpacity: 0.1 };
  }

  return { strokeWidth: 0.35, opacity: 0.1, glowOpacity: 0.06 };
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

