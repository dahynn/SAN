import { Leaf } from 'lucide-react';

interface LoadingDotProps {
  delayMs?: number;
}

export function KnowledgeLoadingCard() {
  return (
    <section className="glass-card relative flex h-[190px] w-full items-center justify-center overflow-hidden rounded-leaf border border-primary-signal/20 bg-surface-container/90 px-6 py-4 backdrop-blur-xl">
      <div className="absolute inset-0 bg-primary-signal/5" aria-hidden="true" />
      <div className="absolute inset-x-10 top-4 h-24 rounded-full bg-primary-signal/10 blur-3xl" aria-hidden="true" />

      <div className="relative flex flex-col items-center gap-4 text-center">
        <div className="relative flex h-10 w-10 items-center justify-center text-primary-signal">
          <div className="absolute inset-1 rounded-full bg-primary-signal/20 blur-xl" aria-hidden="true" />
          <Leaf size={30} className="relative" aria-hidden="true" />
        </div>

        <div className="flex flex-col items-center gap-2">
          <p className="text-body-sm font-medium text-text-primary">
            AI가 정보를 잎사귀로 변환 중...
          </p>
          <div className="flex items-center justify-center gap-2" aria-hidden="true">
            <LoadingDot />
            <LoadingDot delayMs={150} />
            <LoadingDot delayMs={300} />
          </div>
        </div>
      </div>
    </section>
  );
}

function LoadingDot({ delayMs = 0 }: LoadingDotProps) {
  return (
    <span
      className="h-2 w-2 animate-pulse rounded-full bg-primary-signal shadow-neon"
      style={{
        animationDelay: `${delayMs}ms`,
        animationDuration: '900ms',
      }}
    />
  );
}
