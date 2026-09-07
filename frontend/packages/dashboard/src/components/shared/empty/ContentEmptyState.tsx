import { Leaf } from 'lucide-react';

interface ContentEmptyStateProps {
  title: string;
  description: string;
  variant?: 'default' | 'til';
}

export function ContentEmptyState({ title, description, variant = 'default' }: ContentEmptyStateProps) {
  const isTilEmptyState = variant === 'til';
  const displayTitle = isTilEmptyState ? '복습할 TIL이 없어요' : title;
  const displayDescription = isTilEmptyState
    ? '03:00 자동 생성 대상 날짜에 생성된 TIL이 아직 없어요.\n지식카드를 저장하면 다음 자동 생성 시점에 TIL을 확인할 수 있어요.'
    : description;

  return (
    <div className="flex w-full flex-col-reverse items-center justify-center py-14 text-center">
      <div className="flex max-w-xl flex-col items-center gap-3">
        <h2 className="text-h2-bold text-text-primary">{displayTitle}</h2>
        <p className="whitespace-pre-line text-body-main font-medium leading-6 text-text-secondary">
          {displayDescription}
        </p>
      </div>

      <div className="pb-8 text-action-accent">
        <Leaf size={72} strokeWidth={1.4} aria-hidden="true" />
      </div>
    </div>
  );
}
