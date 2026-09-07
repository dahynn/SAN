// packages/ui/src/components/MetaLabel/MetaLabel.tsx
// 카드 하단 메타 정보 텍스트
// 사용 예) "ARTICLE • 1H AGO" / "2024. 05. 18"

interface MetaLabelProps {
  // 표시할 텍스트 목록 — 배열로 받아서 • 로 구분
  // 예) ['ARTICLE', '1H AGO'] → "ARTICLE • 1H AGO"
  segments: string[];
  className?: string;
}

export function MetaLabel({ segments, className = '' }: MetaLabelProps) {
  return (
    <p className={`text-caption uppercase tracking-wide text-text-secondary ${className}`}>
      {segments.join(' • ')}
    </p>
  );
}
