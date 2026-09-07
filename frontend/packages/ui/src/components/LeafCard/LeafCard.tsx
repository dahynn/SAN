// packages/ui/src/components/LeafCard/LeafCard.tsx
// 지식 카드 — extension 사이드패널 + dashboard 모두 사용
// ai_status에 따라 skeleton / 실제 내용 분기 처리
import type { ReactNode } from 'react';

import type { KnowledgeCardView } from '@san/shared/';
import { ChevronRight, FileText, Image, Link2 } from 'lucide-react';
import { IconBox } from '../IconBox/IconBox.tsx';
import { TagBadge } from '../TagBadge/TagBadge.tsx';
import { MetaLabel } from '../MetaLabel/MetaLabel.tsx';
import { NeonDot } from '../NeonDot/NeonDot.tsx';
import { formatRelativeTime } from '@san/shared/utils/format';

// source_type별 아이콘 SVG (인라인 — 외부 의존 없이 독립 동작)
function SourceIcon({ type }: { type: KnowledgeCardView['source_type'] }) {
  if (type === 'LINK') {
    return <Link2 size={20} className="text-text-secondary" aria-hidden="true" />;
  }

  if (type === 'IMAGE') {
    return <Image size={20} className="text-text-secondary" aria-hidden="true" />;
  }

  return <FileText size={20} className="text-text-secondary" aria-hidden="true" />;
}

// ai_status가 PENDING/PROCESSING일 때 보여주는 스켈레톤
function LeafCardSkeleton() {
  return (
    <div className="flex items-center gap-4 p-4 rounded-leaf bg-surface-container animate-pulse">
      <div className="w-10 h-10 rounded-full bg-surface-highest" />
      <div className="flex flex-col gap-2 flex-1">
        <div className="h-3 bg-surface-highest rounded w-3/4" />
        <div className="h-2 bg-surface-highest rounded w-1/3" />
      </div>
    </div>
  );
}

// ──────────────────────────────────────────────
// LeafCard Props
// ──────────────────────────────────────────────
interface LeafCardProps {
  card: KnowledgeCardView;
  // compact: 익스텐션 사이드패널 (제목 + 메타만 표시)
  // full:    대시보드 (제목 + 요약 + 태그 모두 표시)
  variant?: 'compact' | 'full';
  onClick?: () => void;
}

export function LeafCard({ card, variant = 'full', onClick }: LeafCardProps) {
  // AI 처리 중이면 스켈레톤 표시
  if (card.ai_status === 'PENDING' || card.ai_status === 'PROCESSING') {
    return <LeafCardSkeleton />;
  }

  const metaSegments = [
    card.source_type,
    formatRelativeTime(card.created_at),
  ];

  return (
    <div
      onClick={onClick}
      className={`
        group flex items-center gap-4 p-4
        rounded-leaf bg-surface-container
        border border-primary-signal/10
        transition-all duration-200
        ${onClick ? 'cursor-pointer hover:border-primary-signal/20 hover:bg-surface-highest hover:glow-neon' : ''}
      `}
    >
      {/* 아이콘 박스 */}
      <IconBox variant="circle" size="sm">
        <SourceIcon type={card.source_type} />
      </IconBox>

      {/* 본문 영역 */}
      <div className="flex flex-col gap-1 flex-1 min-w-0">
        {/* 제목 + 상태 점 */}
        <div className="flex items-center gap-2">
          <p className="text-body-sm-bold text-text-primary truncate">
            {card.title}
          </p>
          {card.ai_status === 'COMPLETED' && (
            <NeonDot size="sm" intensity="dim" className="flex-shrink-0" />
          )}
        </div>

        {/* full variant: 요약 텍스트 */}
        {variant === 'full' && card.summary && (
          <p className="text-body-main text-text-secondary line-clamp-2">
            {card.summary}
          </p>
        )}

        {/* 메타 레이블 */}
        <MetaLabel segments={metaSegments} />

        {/* full variant: 태그 목록 */}
        {variant === 'full' && card.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 pt-2">
            {card.tags.map((tag) => (
              <TagBadge key={tag.tag_id} label={tag.name} />
            ))}
          </div>
        )}
      </div>

      {/* 우측 화살표 (클릭 가능한 경우만) */}
      {onClick && (
        <ChevronRight
          size={20}
          className="flex-shrink-0 opacity-50 group-hover:opacity-100 transition-opacity text-text-secondary"
          aria-hidden="true"
        />
      )}
    </div>
  );
}
