# TIL 카드 요약 비동기 측정

이 도구는 SAN의 카드별 TIL 요약 단계만 측정합니다. 최종 TIL 통합, 임베딩, 애플리케이션 캐시, Java 백엔드와 UI는 범위에서 제외합니다.

동일한 세 개의 합성 카드를 사용해 순차 호출 대조군과 `asyncio.gather` 병렬 호출을 각 10회 이상 실행합니다. 실행 순서는 고정 시드로 섞고, 원본 결과에는 시간·성공 여부·오류 코드·출력 문자 수만 저장합니다. 모델 응답 본문과 비밀값은 저장하지 않습니다.

재현 명령은 실행 환경의 `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_MODEL`, `OPENAI_USE_RESPONSES_API`를 주입한 상태에서 다음과 같습니다.

```bash
cd ai
./.venv/bin/python benchmarks/til_summary_async/benchmark.py \
  --runs-per-mode 10 \
  --seed 20260914 \
  --output benchmarks/til_summary_async/results/<timestamp>.json
```

통계의 p95는 nearest-rank 방식이며 표준편차는 성공한 실행의 표본 표준편차입니다. 이 결과는 통제 실험의 순차 대조군과 현재 비동기 호출 구조의 비교일 뿐, 배포 환경 전후 성능이나 운영 SLA를 뜻하지 않습니다.
