import { type PointerEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Download, ExternalLink, Trees } from 'lucide-react';
import { authTokenStorage } from '@dashboard/api/client';
import { HomeSectionTitle } from '../layout/HomeSectionTitle';
import { KnowledgePlanetPrototype } from './KnowledgePlanetPrototype';
import { getExtensionInstallUrl } from '../../utils/extensionInstallUrl';

export function GraphSection() {
  const navigate = useNavigate();
  const [isChecking, setIsChecking] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    let ignore = false;
    authTokenStorage.getToken()
      .then((token) => { if (!ignore) setIsAuthenticated(Boolean(token)); })
      .finally(() => { if (!ignore) setIsChecking(false); });
    return () => { ignore = true; };
  }, []);

  if (!isChecking && !isAuthenticated) {
    return <ForestLanding onLogin={() => navigate('/login', { state: { from: '/' } })} />;
  }

  return (
    <div className="relative mt-6 min-h-screen">
      <div className="pointer-events-none absolute left-0 top-6 z-20 px-6">
        <HomeSectionTitle>나의 지식 숲</HomeSectionTitle>
      </div>
      {isChecking
        ? <div className="grid min-h-screen place-items-center"><span className="text-sm text-text-secondary">로딩 중...</span></div>
        : <KnowledgePlanetPrototype showMarkers />
      }
    </div>
  );
}

function ForestLanding({ onLogin }: { onLogin: () => void }) {
  const installUrl = getExtensionInstallUrl();

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width - 0.5) * 2;
    const y = ((event.clientY - rect.top) / rect.height - 0.5) * 2;

    event.currentTarget.style.setProperty('--parallax-x', `${x * 28}px`);
    event.currentTarget.style.setProperty('--parallax-y', `${y * 22}px`);
    event.currentTarget.style.setProperty('--parallax-soft-x', `${x * 14}px`);
    event.currentTarget.style.setProperty('--parallax-soft-y', `${y * 10}px`);
  };

  const handlePointerLeave = (event: PointerEvent<HTMLDivElement>) => {
    event.currentTarget.style.setProperty('--parallax-x', '0px');
    event.currentTarget.style.setProperty('--parallax-y', '0px');
    event.currentTarget.style.setProperty('--parallax-soft-x', '0px');
    event.currentTarget.style.setProperty('--parallax-soft-y', '0px');
  };

  return (
    <div
      onPointerMove={handlePointerMove}
      onPointerLeave={handlePointerLeave}
      className="forest-landing relative left-1/2 flex min-h-screen w-screen -translate-x-1/2 items-center justify-center overflow-hidden bg-background [--parallax-soft-x:0px] [--parallax-soft-y:0px] [--parallax-x:0px] [--parallax-y:0px]"
    >
      <div className="pointer-events-none absolute inset-0">
        <div className="absolute left-[30%] top-[45%] h-[36rem] w-[36rem] -translate-x-1/2 -translate-y-1/2 rounded-full bg-action-accent/[0.08] blur-2xl" />
        <div className="absolute left-[73%] top-[56%] h-[30rem] w-[30rem] -translate-x-1/2 -translate-y-1/2 rounded-full bg-action-accent/[0.06] blur-2xl" />
        <div className="absolute left-[12%] top-[68%] h-2.5 w-2.5 rounded-full bg-action-accent/30 blur-[1px] transition-transform duration-300 ease-out [transform:translate(var(--parallax-x),var(--parallax-y))]" />
        <div className="absolute left-[27%] top-[39%] h-2.5 w-2.5 rounded-full bg-action-accent/35 blur-[1px] transition-transform duration-300 ease-out [transform:translate(var(--parallax-x),var(--parallax-y))]" />
        <div className="absolute left-[58%] top-[31%] h-2 w-2 rounded-full bg-action-accent/26 blur-[1px] transition-transform duration-300 ease-out [transform:translate(var(--parallax-soft-x),var(--parallax-soft-y))]" />
        <div className="absolute left-[82%] top-[45%] h-3 w-3 rounded-full bg-action-accent/24 blur-[1px] transition-transform duration-300 ease-out [transform:translate(calc(var(--parallax-x)*-0.7),calc(var(--parallax-y)*-0.7))]" />
        <div className="absolute left-[18%] top-[78%] h-2 w-2 rounded-full bg-action-accent/28 blur-[1px] transition-transform duration-300 ease-out [transform:translate(calc(var(--parallax-soft-x)*-1),calc(var(--parallax-soft-y)*-1))]" />
        <div className="absolute left-[71%] top-[72%] h-2.5 w-2.5 rounded-full bg-action-accent/26 blur-[1px] transition-transform duration-300 ease-out [transform:translate(var(--parallax-soft-x),calc(var(--parallax-soft-y)*-1))]" />
        <svg className="absolute inset-0 h-full w-full opacity-[0.09] transition-transform duration-300 ease-out [transform:translate(var(--parallax-soft-x),var(--parallax-soft-y))]" viewBox="0 0 100 100" preserveAspectRatio="none">
          <path d="M10 39 Q34 26 48 40 Q61 53 80 47" fill="none" className="forest-stitch-path stroke-action-accent" strokeWidth="0.24" strokeDasharray="1 0.8" />
          <path d="M17 66 Q41 54 58 61 Q73 68 91 57" fill="none" className="forest-stitch-path forest-stitch-path--slow stroke-action-accent" strokeWidth="0.24" strokeDasharray="1 0.8" />
          <path d="M0 48 Q16 40 28 41" fill="none" className="forest-stitch-path forest-stitch-path--short stroke-action-accent" strokeWidth="0.2" strokeDasharray="1 0.8" />
        </svg>
      </div>

      {installUrl ? (
        <a
          href={installUrl}
          target="_blank"
          rel="noreferrer"
          className="absolute left-1/2 top-6 z-20 flex w-[calc(100%-2rem)] max-w-[520px] -translate-x-1/2 items-center justify-between gap-4 rounded-tl-[22px] rounded-br-[22px] rounded-tr-xl rounded-bl-xl border border-action-accent/35 bg-action-accent/12 px-5 py-4 text-left text-text-primary shadow-[0_0_0_1px_rgba(74,222,128,0.08),0_18px_52px_rgba(74,222,128,0.16),inset_0_1px_0_rgba(255,255,255,0.12)] backdrop-blur-xl transition hover:-translate-y-0.5 hover:border-action-accent/55 hover:bg-action-accent/16 active:scale-[0.99]"
        >
          <span className="flex min-w-0 items-center gap-3">
            <span className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-action-accent/25 bg-action-accent/15 text-action-accent">
              <span className="absolute h-10 w-10 animate-ping rounded-xl bg-action-accent/10" aria-hidden="true" />
              <Download size={19} strokeWidth={1.9} className="animate-bounce" />
            </span>
            <span className="min-w-0">
              <span className="block text-sm font-bold">먼저 SAN 익스텐션을 설치해주세요</span>
              <span className="mt-0.5 block text-xs text-text-secondary">웹 페이지 저장 기능을 사용하려면 필요합니다.</span>
            </span>
          </span>
          <ExternalLink size={16} strokeWidth={1.8} className="shrink-0" />
        </a>
      ) : null}

      <div className="relative z-10 flex max-w-md flex-col items-center gap-8 px-6 text-center">
        <Trees
          size={48}
          strokeWidth={1.45}
          aria-hidden="true"
          className="forest-landing-tree-icon text-action-accent"
        />

        <div className="flex flex-col gap-3">
          <h2 className="text-2xl font-bold tracking-tight text-text-primary">
            나만의 지식 숲을 키워보세요
          </h2>
          <p className="text-sm leading-relaxed text-text-secondary">
            흩어진 지식을 한곳에 모으면 자연스럽게 연결되고,<br />
            나무처럼 자라나는 당신만의 지식 체계가 됩니다.
          </p>
        </div>

        <div className="flex flex-col items-center gap-2">
            <button
              type="button"
              onClick={onLogin}
              className="group flex items-center justify-center gap-2.5 rounded-tl-[16px] rounded-br-[16px] rounded-tr-lg rounded-bl-lg border border-action-accent/30 bg-action-accent/10 px-7 py-3.5 text-sm font-bold text-action-accent transition-all hover:border-action-accent/50 hover:bg-action-accent/15"
            >
              로그인하고 시작하기
              <ArrowRight size={16} className="transition-transform group-hover:translate-x-0.5" />
            </button>
        </div>

        <p className="text-xs text-text-secondary/40">SAN · Scrap & Notify</p>
      </div>
    </div>
  );
}
