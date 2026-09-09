# SAN LLM 신뢰성 평가 — 2026-09-08

## 범위와 원칙

- 외부 OpenAI, GitHub, Tavily, S3 및 실제 서비스는 호출하지 않았습니다.
- 평가는 `tests/fixtures/til_reliability_cases.json`의 한국어 합성 데이터와 mock 모델로 수행했습니다.
- 이 문서는 모델 품질 향상률을 주장하지 않습니다. 실제 모델 응답의 사실성·주입 저항성은 외부 모델을 호출하지 않았으므로 측정하지 않았습니다.

## 읽기 전용 흐름 감사

| 기능 | 호출 흐름 | 확인된 경계/위험 |
| --- | --- | --- |
| 스크랩 분석 | extension → backend AI client → `/ai/analyze` | URL·텍스트·이미지 원본이 AI 전처리로 전달됩니다. |
| 하이브리드 검색 | dashboard → `/search` → embedding client → `/ai/search` | 질의 텍스트가 임베딩으로 전달됩니다. |
| TIL 생성 | dashboard → `/tils` 비동기 job → `/ai/til` | 기존에는 같은 날짜의 현재 카드 목록을 실행 시점에 재조회해 근거가 바뀔 수 있었고, TIL 입력의 개수·크기 제한이 없었습니다. |
| Recall 퀴즈 | dashboard → `/recall/quizzes` 비동기 job → `/ai/quiz` | TIL/카드 콘텐츠를 모델에 전달하고 결과를 비동기 상태로 조회합니다. |
| GitHub Star 추천 | dashboard → backend → `/ai/recommend/github-stars` | AI 서버가 GitHub·Tavily 외부 서비스를 사용합니다. 이번 범위에서는 호출하지 않았습니다. |

공통 확인 사항: AI 서버는 모델·임베딩 오류를 구조화된 AI 오류로 전환하고, 백엔드 TIL job은 실패 상태와 감사 로그를 남깁니다. TIL 결과에는 문장 단위 인용이 없으므로, 원본 패널은 입력 근거 추적이지 생성 문장별 출처 판정은 아닙니다.

## 구현된 최소 변경

1. TIL 요청을 등록할 때 카드 ID, 스크랩 ID, 제목, 원문, URL, 분류, 생성 시각과 모델 입력을 `daily_summary_sources`에 스냅샷으로 저장합니다. 새 TIL의 Source 패널은 이 스냅샷만 반환합니다. 스냅샷 이전 TIL은 근거를 확정할 수 없다고 표시합니다.
2. TIL AI 요청은 최대 20개 카드, 카드당 최대 12,000자, 합계 최대 60,000자로 제한합니다. 원문과 중간 요약을 신뢰하지 않는 구획으로 감싸고, 원문 안의 지시·역할 변경·출력 형식 변경을 따르지 않도록 프롬프트 계약을 보강했습니다.
3. TIL job 실패 화면은 원시 외부 오류 대신 재시도 가능한 안전한 한국어 안내를 표시합니다. timeout 문자열은 별도 안내를 표시합니다.
4. `/async-jobs/{jobId}`는 job의 감사 스냅샷 `actorUserId`와 현재 사용자가 일치할 때만 조회합니다. 다른 사용자의 job 상태는 `UNAUTHORIZED`로 거절합니다.
5. Source 응답은 생성 시점 스냅샷마다 `SRC-01` 형식의 `referenceId`와 `evidenceScope=TIL_INPUT_SNAPSHOT`을 반환합니다. 화면은 이 식별자가 **TIL 입력 근거**이며 문장별 인용·사실 검증이 아님을 명시합니다. 스냅샷이 없는 과거 TIL은 `UNAVAILABLE`로 반환합니다.
6. URL·이미지 전처리는 `http`·`https`만 허용하고 사용자 정보가 포함된 URL, `localhost`, loopback, private, link-local, multicast, reserved, unspecified IPv4/IPv6 목적지를 요청 전에 거절합니다. URL 문서의 리디렉션은 각 hop을 다시 검증하고 최대 3회로 제한하며, 이미지 URL 리디렉션은 거절합니다.
7. AI 처리 예외의 원시 메시지(모델 오류, URL 오류 등)는 응답에 반환하지 않고 오류 코드별 한국어 안전 문구로 치환합니다.

이미지 카드에 정제 텍스트가 없으면 새 TIL 스냅샷은 저장된 raw text를 텍스트 입력으로 사용합니다. private S3 이미지 URL을 새 경로에서 재발급·전송하지 않는 보수적 경계입니다.

## 합성 평가 기준과 결과

| 시나리오 | 통과 기준 | 결과 |
| --- | --- | --- |
| 정상 한국어 원문 | mock 모델로 `# TIL` 응답 계약을 반환 | 통과 |
| 근거 부족 | 빈 contents를 모델 호출 전에 400으로 거절 | 통과 |
| 주입성 원문 | `<source-content>` 구획과 신뢰하지 않는 원문 규칙이 모델 프롬프트에 포함 | 통과 |
| timeout/모델 오류 | 구조화된 `til_generation_failed`, HTTP 422 반환 | 통과 |
| 사용자 격리 | 다른 actor가 async job 조회 시 `UNAUTHORIZED` | 통과 |
| 입력 경계 | 12,001자 카드 입력을 모델 호출 전에 400으로 거절 | 통과 |
| SSRF 주소 정책 | localhost·loopback·private·link-local·metadata·IPv6 ULA를 요청 전에 `blocked_url`로 거절 | 통과 |
| 공개 URL 정책 | 전역 IP와 전역 DNS 결과는 허용하고, private DNS 결과는 거절 | 통과 |
| 근거 참조 격리 | `SRC-01`이 스냅샷 순서에 따라 반환되고 다른 사용자는 스냅샷을 읽지 못함 | 통과 |
| 안전 실패 표현 | timeout 원문 등 내부 오류 문자열 대신 일반 안내 문구만 반환 | 통과 |

실행 결과:

- AI: `.venv/bin/python -m pytest tests/test_preprocessor.py tests/test_til_endpoint.py tests/test_til_reliability_evaluation.py` — 42 passed, 2 skipped. 두 skip은 `RUN_NETWORK_TESTS=1`을 명시해야 실행되는 실제 외부 네트워크 시험이며, 이번 범위에서는 호출하지 않았습니다. pytest cache 쓰기 권한 경고 1건은 테스트 결과와 무관합니다.
- AI 정적 검사: `.venv/bin/ruff check app tests/test_preprocessor.py tests/test_til_endpoint.py tests/test_til_reliability_evaluation.py` — passed.
- Backend 관련 시험: `TilServiceTest` — passed. `SRC-01` 구조·스냅샷 소유자 차단을 포함합니다.
- Dashboard: `build:local`, `CollectedDataPanel.tsx` eslint — passed.
- Backend 전체: 304개 중 301 passed, 기존 GitHub 컨트롤러 무인증 NPE 3 failures. 이번 변경과 무관하며 작업 시작 전 기준선과 같습니다.
- Dashboard build: `build:local` passed. 변경 파일 lint는 기존 `useTilPageLogic.ts`의 React effect setState 오류 2건으로 실패했으며, 이번 변경 줄에는 lint 오류가 없습니다.

## 증거·기여 경계

- 사용자 제공: SAN LLM 신뢰성 개선 범위, 외부 모델·유료 API 호출 금지, 중앙 인계 문서 경로.
- 저장소·테스트로 확인: 이 문서의 호출 흐름, 코드 변경 존재, 위의 로컬 합성 검증 결과.
- Codex 구현: 이 작업 세션에서 TIL 스냅샷·입력 경계·안전 실패 표시·job 조회 소유자 검사와 합성 평가를 추가했습니다.
- 개인 직접 기여: 커밋/PR 또는 사용자 확인이 없으므로 이 문서는 사용자의 직접 구현·운영 성과를 주장하지 않습니다.

## 운영 전 남은 한계와 정확한 다음 단계

현재 DNS 검사와 실제 HTTP 연결 사이에는 DNS 재바인딩 경쟁 조건이 남습니다. 완전한 방어에는 egress proxy·방화벽 또는 검증한 IP로 연결을 고정하는 transport가 필요합니다. 또한 외부 CDN이 반환하는 이후 콘텐츠 변경, 이미지 URL을 모델 공급자가 별도로 재해석하는 문제, 문장별 사실성·인용 정확도는 이 오프라인 검증으로 보장하지 않습니다. 이 구현은 금융 규제 준수를 주장하지 않습니다.

권한을 받은 비운영 staging 환경에서 egress 정책을 적용한 뒤 실제 이미지 카드, 오래된 TIL, 모델 timeout을 포함한 인증 E2E를 실행합니다. 문장 단위 근거 연결이 필요하면 모델 출력 스키마·근거 span·검증 UI를 별도 설계·승인으로 결정합니다.
