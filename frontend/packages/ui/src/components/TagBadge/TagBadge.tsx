// packages/ui/src/components/TagBadge/TagBadge.tsx
// 태그 뱃지 — 나뭇잎 radius, 어두운 칩 배경
// 사용 예) <TagBadge label="Design" /> → "#Design"

interface TagBadgeProps {
  label: string;   // # 접두사 없이 전달: "Design" → "#Design" 으로 렌더링
  className?: string;
}

export function TagBadge({ label, className = '' }: TagBadgeProps) {
  return (
    <span
      className={`
        inline-flex items-center
        px-3 py-1
        rounded-leaf
        bg-surface-highest
        text-caption text-text-secondary
        whitespace-nowrap
        ${className}
      `}
    >
      #{label}
    </span>
  );
}
