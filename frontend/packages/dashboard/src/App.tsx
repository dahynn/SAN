import { useScrapStore } from '@dashboard/store/scrapStore'

function App() {
  // 스토어에서 viewMode 읽어오기
  const viewMode = useScrapStore((state) => state.viewMode);

  return (
  <div className="min-h-screen bg-background flex items-center justify-center">
    <h1 className="text-5xl font-bold text-primary-signal drop-shadow-[0_0_10px_var(--color-primary-signal)]">
      SAN: 지식의 숲에 불이 켜졌습니다.
    </h1>
      <h1 className="text-primary-signal text-3xl font-bold">SAN Dashboard</h1>
      <p className="text-text-primary opacity-70">
        현재 뷰 모드: <span className="text-yellow-400 font-mono">{viewMode}</span>
      </p>
  </div>
  )
}

export default App