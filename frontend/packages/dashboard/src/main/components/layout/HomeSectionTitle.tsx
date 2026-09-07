import type { ReactNode } from 'react';

export function HomeSectionTitle({ children }: { children: ReactNode }) {
  return (
    <h2 className="text-h1-bold text-text-primary">
      {children}
    </h2>
  );
}
