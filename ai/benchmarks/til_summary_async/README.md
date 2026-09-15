# TIL 카드 요약 비동기 측정

이 도구는 SAN의 카드별 TIL 요약 단계만 측정합니다. 최종 TIL 통합, 임베딩, 애플리케이션 캐시, Java 백엔드와 UI는 범위에서 제외합니다.

동일한 세 개의 합성 카드를 사용해 순차 호출 대조군과 `asyncio.gather` 병렬 호출을 10개 이상 쌍으로 실행합니다. 각 쌍은 순차→병렬(AB) 또는 병렬→순차(BA)이며, 두 순서를 균형 있게 만들고 고정 시드로 섞습니다. 원본 결과에는 시간·성공 여부·오류 코드·출력 문자 수만 저장합니다. 모델 응답 본문과 비밀값은 저장하지 않습니다.

재현 명령은 실행 환경의 `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL`, `OPENAI_USE_RESPONSES_API`를 주입한 상태에서 다음과 같습니다.

```bash
cd ai
./.venv/bin/python benchmarks/til_summary_async/benchmark.py \
  --pairs 10 \
  --seed 20260914 \
  --output benchmarks/til_summary_async/results/<timestamp>.json
```

통계의 p95는 nearest-rank 방식이며 표준편차는 성공한 실행의 표본 표준편차입니다. paired summary에는 같은 쌍의 순차·병렬 차이와 단축률의 평균·중앙값을 별도로 기록합니다. 이 결과는 통제 실험의 순차 대조군과 현재 비동기 호출 구조의 비교일 뿐, 배포 환경 전후 성능이나 운영 SLA를 뜻하지 않습니다.

## 확정 결과 — paired AB/BA, 2026-09-14

최종 근거는 [paired 10x2 원본 결과](results/2026-09-14-card-summary-paired-10x2.json)입니다. 동일한 `gpt-5-mini` 모델과 합성 카드 3개로 AB 5개·BA 5개의 10개 쌍을 실행했습니다.

- 순차·병렬 모두 10/10 성공, 오류·rate limit 0건
- paired 단축률 평균: **59.7729%**
- paired 단축률 중앙값: 59.4513%
- 자소서·포트폴리오에 사용할 확정 수치: **59.8%**

이 수치는 카드별 요약 단계의 통제 실험에서 순차 대조군과 비동기 병렬 구조를 비교한 값입니다. 최종 TIL 통합, 임베딩, 캐시, Java 백엔드, UI와 실제 운영 트래픽은 제외했습니다. 따라서 배포 전후 성능, 운영 SLA, 사용자 경험 개선으로 해석하면 안 됩니다.

`results/2026-09-14-card-summary-10x2.json`은 실행 순서가 앞부분 순차 6회로 군집된 **preliminary** 결과입니다. 보존만 하며, 위 확정 수치의 근거로 사용하지 않습니다.
