import { useNavigate } from 'react-router-dom';
import { useArchiveCategories } from '@san/shared';
import { ContentEmptyState } from '../../../components/shared/empty/ContentEmptyState';
import { ArchiveFolderIcon } from './ArchiveFolderIcon';

export function ArchiveFolderGrid() {
  const navigate = useNavigate();
  const archiveCategoriesQuery = useArchiveCategories();
  const categories = archiveCategoriesQuery.data?.categories ?? [];

  return (
    <section className="flex flex-col gap-5">
      {archiveCategoriesQuery.isPending ? (
        <p className="py-10 text-sm text-text-secondary">폴더를 불러오는 중입니다...</p>
      ) : archiveCategoriesQuery.isError ? (
        <p className="py-10 text-sm text-red-400">폴더를 불러오지 못했습니다.</p>
      ) : categories.length === 0 ? (
        <ContentEmptyState
          title="아직 보여줄 카드가 없습니다"
          description="지식 카드가 쌓이면 카테고리별 폴더가 이곳에 생깁니다."
        />
      ) : (
        <div className="px-2 py-6">
          <div className="grid grid-cols-1 gap-x-12 gap-y-14 md:grid-cols-2 xl:grid-cols-3">
            {categories.map((category) => (
              <ExplorerFolderTile
                key={category.categoryId}
                name={category.categoryName}
                count={category.cardCount}
                onClick={() => navigate(`/archive/${category.categoryId}`)}
              />
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

function ExplorerFolderTile({ name, count, onClick }: { name: string; count: number; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="group flex min-w-0 items-center gap-3 rounded-lg px-3 py-2 text-left transition duration-150 hover:brightness-[1.02] active:translate-y-0.5 active:scale-[0.99] focus:outline-none focus-visible:ring-2 focus-visible:ring-action-accent/45"
    >
      <ArchiveFolderIcon
        isOpen={count > 0}
        className="h-[112px] w-[178px] shrink-0 drop-shadow-[0_14px_18px_rgba(45,101,63,0.16)] transition duration-150 group-hover:drop-shadow-[0_18px_24px_rgba(45,101,63,0.2)] group-active:drop-shadow-[0_7px_10px_rgba(45,101,63,0.18)]"
      />
      <span className="min-w-0">
        <span className="block truncate text-lg font-bold text-text-primary">{name}</span>
        <span className="mt-1 block text-sm font-medium text-text-primary/40">{count} cards</span>
      </span>
    </button>
  );
}
