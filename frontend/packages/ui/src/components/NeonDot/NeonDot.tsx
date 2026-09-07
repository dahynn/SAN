// packages/ui/src/components/NeonDot/NeonDot.tsx
// 발광하는 점 — 카드 상태 표시, 배경 장식용으로 재사용

interface NeonDotProps {
  // 점 크기: sm(w-1) | md(w-1.5) | lg(w-2)
  size?: 'sm' | 'md' | 'lg';
  // 발광 강도
  intensity?: 'dim' | 'full';
  className?: string;
}

const sizeMap = {
  sm: 'w-1 h-1',
  md: 'w-1.5 h-1.5',
  lg: 'w-2 h-2',
};

const intensityMap = {
  dim:  'opacity-50',
  full: 'opacity-100',
};

export function NeonDot({ size = 'sm', intensity = 'full', className = '' }: NeonDotProps) {
  return (
    <span
      className={`inline-block rounded-full bg-primary-signal glow-neon ${sizeMap[size]} ${intensityMap[intensity]} ${className}`}
    />
  );
}
