import { ChevronRight, Search } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useArchiveCategories } from '@san/shared';
import { ContentEmptyState } from '../../components/shared/empty/ContentEmptyState';
import { ArchiveFolderIcon } from '../components/archive/ArchiveFolderIcon';

export function ArchivePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const keyword = searchParams.get('query')?.trim() ?? '';
  const categoriesQuery = useArchiveCategories();
  const folderCount = categoriesQuery.data?.categories.length ?? 0;

  return (
    <section className="flex w-full min-w-0 flex-col gap-7 py-10 text-text-primary">
      <ArchiveExplorerHeader rightText={`${folderCount} folders`} />

      {keyword ? (
        <div className="rounded-lg border border-text-secondary/10 bg-surface-lowest/70 px-4 py-3">
          <div className="flex items-center gap-3 text-action-accent">
            <Search size={16} />
            <span className="text-sm font-bold">"{keyword}" 검색 결과</span>
          </div>
        </div>
      ) : null}

      <CategoryGrid
        categories={categoriesQuery.data?.categories ?? []}
        isPending={categoriesQuery.isPending}
        isError={categoriesQuery.isError}
        onSelect={(categoryId) => {
          const query = searchParams.toString();
          navigate(`/archive/${categoryId}${query ? `?${query}` : ''}`);
        }}
      />
    </section>
  );
}

function ArchiveExplorerHeader({ rightText }: { rightText: string }) {
  return (
    <header className="flex flex-col gap-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-4xl font-extrabold tracking-tight">Archive</h1>
          <p className="mt-2 text-text-primary/50">카테고리 폴더에서 저장한 지식 카드를 찾아보세요.</p>
        </div>
        <span className="text-xs font-bold uppercase tracking-wide text-text-primary/35">{rightText}</span>
      </div>

      <div className="flex min-w-0 items-center gap-2 rounded-lg border border-text-secondary/10 bg-surface-lowest/70 px-3 py-2 text-sm text-text-primary/55 shadow-sm">
        <span className="font-semibold text-text-primary/75">SAN</span>
        <ChevronRight size={14} className="shrink-0 text-text-primary/25" aria-hidden="true" />
        <span className="font-semibold text-text-primary/75">Archive</span>
        <ChevronRight size={14} className="shrink-0 text-text-primary/25" aria-hidden="true" />
        <span className="truncate">Categories</span>
      </div>
    </header>
  );
}

function CategoryGrid({
  categories,
  isPending,
  isError,
  onSelect,
}: {
  categories: Array<{ categoryId: string; categoryName: string; cardCount: number }>;
  isPending: boolean;
  isError: boolean;
  onSelect: (categoryId: string) => void;
}) {
  if (isPending) {
    return <p className="py-16 text-center text-sm text-text-primary/40">폴더를 불러오는 중입니다...</p>;
  }

  if (isError) {
    return <p className="py-16 text-center text-sm text-red-400">폴더를 불러오지 못했습니다.</p>;
  }

  if (categories.length === 0) {
    return (
      <ContentEmptyState
        title="아직 폴더가 없습니다"
        description="지식 카드가 저장되면 카테고리별 폴더가 여기에 생깁니다."
      />
    );
  }

  return (
    <div className="px-2 py-6">
      <div className="grid grid-cols-1 gap-x-12 gap-y-14 md:grid-cols-2 xl:grid-cols-3">
        {categories.map((category) => (
          <ArchiveFolderTile key={category.categoryId} category={category} onSelect={onSelect} />
        ))}
      </div>
    </div>
  );
}

function ArchiveFolderTile({
  category,
  onSelect,
}: {
  category: { categoryId: string; categoryName: string; cardCount: number };
  onSelect: (categoryId: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onSelect(category.categoryId)}
      className="group flex min-w-0 items-center gap-3 rounded-lg px-3 py-2 text-left transition duration-150 hover:brightness-[1.02] active:translate-y-0.5 active:scale-[0.99] focus:outline-none focus-visible:ring-2 focus-visible:ring-action-accent/45"
    >
      <ArchiveFolderIcon
        isOpen={category.cardCount > 0}
        className="h-[112px] w-[178px] shrink-0 drop-shadow-[0_14px_18px_rgba(45,101,63,0.16)] transition duration-150 group-hover:drop-shadow-[0_18px_24px_rgba(45,101,63,0.2)] group-active:drop-shadow-[0_7px_10px_rgba(45,101,63,0.18)]"
      />
      <span className="min-w-0">
        <span className="block truncate text-lg font-bold text-text-primary">{category.categoryName}</span>
        <span className="mt-1 block text-sm font-medium text-text-primary/40">{category.cardCount} cards</span>
      </span>
    </button>
  );
}
