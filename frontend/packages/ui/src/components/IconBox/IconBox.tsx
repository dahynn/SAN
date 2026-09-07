// packages/ui/src/components/IconBox/IconBox.tsx
// 아이콘을 감싸는 박스 컨테이너
// extension: 원형(circle) + bg-[#313539]
// dashboard: 나뭇잎(leaf) + bg-[#1e5056]/30
// variant prop으로 두 가지 형태 모두 지원

import type { ReactNode } from 'react';

interface IconBoxProps {
  children: ReactNode;
  // circle: 익스텐션 카드용 원형 박스
  // leaf:   대시보드 카드용 나뭇잎 박스
  variant?: 'circle' | 'leaf';
  // sm: w-10 h-10 / md: w-12 h-12
  size?: 'sm' | 'md';
  className?: string;
}

const variantMap = {
  circle: 'rounded-full bg-surface-highest',
  leaf:   'rounded-leaf bg-misty-teal',
};

const sizeMap = {
  sm: 'w-10 h-10',
  md: 'w-12 h-12',
};

export function IconBox({
  children,
  variant = 'circle',
  size = 'sm',
  className = '',
}: IconBoxProps) {
  return (
    <div
      className={`
        flex items-center justify-center flex-shrink-0
        ${sizeMap[size]}
        ${variantMap[variant]}
        ${className}
      `}
    >
      {children}
    </div>
  );
}
