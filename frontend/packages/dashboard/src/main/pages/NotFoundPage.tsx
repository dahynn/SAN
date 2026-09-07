import { ArrowLeft, Home, SearchX } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { CurvedButton, IconBox } from '@san/ui';

export function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <section className="grid min-h-[calc(100vh-9rem)] w-full place-items-center py-12 text-text-primary">
      <div className="grid w-full max-w-2xl justify-items-center gap-8 text-center">
        <div className="relative">
          <IconBox variant="leaf" size="md" className="h-20 w-20 text-primary-signal shadow-neon-sm">
            <SearchX size={34} strokeWidth={1.7} />
          </IconBox>
          <span className="absolute -right-3 -top-3 rounded-full border border-primary-signal/25 bg-background px-3 py-1 text-caption-bold text-primary-signal shadow-neon-sm">
            404
          </span>
        </div>

        <div className="grid gap-3">
          <p className="text-caption-bold uppercase text-primary-signal">
            Page not found
          </p>
          <h1 className="text-4xl font-extrabold leading-tight tracking-tight text-text-primary sm:text-5xl">
            요청한 페이지를 찾을 수 없습니다
          </h1>
          <p className="mx-auto max-w-lg text-body-main leading-7 text-text-secondary">
            주소가 변경되었거나 삭제된 페이지일 수 있습니다.
          </p>
          <p className="mx-auto max-w-lg text-body-main leading-7 text-text-secondary">
            홈으로 돌아가거나 이전 화면에서 다시 시도하세요.
          </p>
        </div>

        <div className="flex w-full flex-col items-center justify-center gap-3 sm:flex-row">
          <CurvedButton
            type="button"
            size="md"
            leadingIcon={<Home size={18} />}
            onClick={() => navigate('/')}
            className="w-full sm:w-fit"
          >
            홈으로 이동
          </CurvedButton>
          <CurvedButton
            type="button"
            tone="ghost"
            size="md"
            leadingIcon={<ArrowLeft size={18} />}
            onClick={() => navigate(-1)}
            className="w-full rounded-md border border-text-secondary/10 [border-radius:0.375rem] sm:w-fit"
          >
            이전으로
          </CurvedButton>
        </div>
      </div>
    </section>
  );
}
