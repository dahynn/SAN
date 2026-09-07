# SAN 초기 통합 검증 — 2026-09-07

## 범위와 결과

- 개인 통합 대상: `frontend/`, `backend/`, `ai/`.
- 제외한 별도 저장소: `Scrap-Notify/docs`, `Scrap-Notify/san-engine`, `Scrap-Notify/.github`.
- 세 원본 기본 브랜치의 서로 다른 커밋 총 1,135개를 squash·재작성 없이 통합 이력의 조상으로 보존했습니다.
- 각 서비스의 Git tree hash가 원본과 일치합니다. 따라서 파일 내용·파일 모드·디렉터리 구조는 접두 경로 외에 변경하지 않았습니다.
- 커밋 이력의 옛 경로는 원본대로 남습니다. 과거 경로까지 일괄 재작성하지 않았습니다.
- 기존 `/Users/dahyeon/san-frontend`와 조직 저장소는 변경하지 않았습니다. 다른 조직 브랜치는 로컬 원격 추적 참조로만 가져왔습니다.
- 원본 서비스별 `.github/workflows`는 각 서비스 아래에 비활성 상태로 보존했습니다. 개인 저장소 루트의 Actions·운영 배포·GitLab 동기화·외부 알림은 추가하지 않았습니다.

## 실행한 검증

| 검증 | 결과 |
| --- | --- |
| `python3 scripts/verify-imports.py` | 세 서비스 트리·조상·커밋 수·작업 파일 일치, submodule 없음 |
| `frontend`: `pnpm install --frozen-lockfile` | pnpm 10.33.2, workspace 5개 설치 성공; lockfile 변경 없음 |
| 대시보드 production build | 성공 |
| 확장 프로그램 production build | 모의 로컬 URL 2개를 프로세스 환경변수로 주입해 성공 |
| 확장 프로그램 `test:og` | fixture 2건, 필드 검증 12개 PASS |
| `backend`: Java 21 / Gradle 8.14.4 전체 테스트 | 298개 중 295개 통과, 3개 실패 |
| 원본 백엔드 커밋의 별도 worktree 재검증 | 해당 3개 실패 동일 재현; 통합으로 생긴 회귀가 아님 |
| `ai`: `uv sync --frozen` | Python 3.11.14, 잠금 의존성 설치 성공; lockfile 변경 없음 |
| `ai`: Ruff | 통과 |
| `scripts/test-ai-offline.py` | 77 통과, 키 필요 테스트 11 스킵, 실외부 요청 테스트 2 제외 |

프론트엔드 빌드에서 사용한 Node는 v23.10.0입니다. 원본 CI의 Node 22와 동일한 환경 검증은 아닙니다. 모의 URL은 `http://localhost:8080`, `http://localhost:5173`이며 서버나 API를 호출하지 않고 번들 생성만 확인했습니다. 환경변수 없이 실행한 최초 확장 프로그램 production build는 필수 설정 부족으로 중단됐습니다.

백엔드 원본 실패는 다음 세 건입니다. 인증 객체가 null일 때 `BusinessException`을 기대하지만 `NullPointerException`이 발생합니다.

- `GithubLinkControllerTest.statusRejectsMissingAuthentication`
- `GithubLinkControllerTest.authorizeUrlRejectsMissingAuthentication`
- `GithubRepositoryControllerTest.repositoriesRejectsMissingAuthentication`

원본 기준선: `5f859cc1957c2de87e2602d34479e4628669ac02`. 코드 수정을 통해 테스트를 억지로 통과시키지 않았습니다. 테스트 리포트는 로컬 `backend/build/reports/tests/test/index.html`에 있습니다.

## 비밀정보 점검과 한계

- 가져온 원본 main들의 텍스트 blob 2,947개에서 제한적인 고확신 패턴 6종(개인키 헤더, GitHub, AWS access ID, OpenAI 형태, Slack, Google API 키)을 검사했고 일치 0건이었습니다.
- 바이너리 25개와 2 MiB 초과 blob 14개는 이 패턴 검사에서 제외했습니다. 이는 완전한 보안 감사·일반 비밀번호 탐지·실제 키 유효성 검증이 아닙니다.
- 통합 이력에서 실제 `.env` 및 `.pem/.key/.p12/.pfx` 경로가 나타나지 않았습니다. 예제 환경변수 파일과 원본 코드의 테스트 값은 유지했습니다.
- 실제 환경 파일이나 개인 인증 정보를 복사하지 않았고, AI 검증은 키 제거·dotenv 자동 로딩 금지·네트워크 연결 차단으로 실행했습니다.

## 미실행 및 후속 작업

- GitHub push·PR·배포는 이 검증에 포함하지 않습니다. 공개 업로드는 별도 승인 후 실행하고 원격 SHA를 확인합니다.
- 운영 DB, OAuth, S3, 벡터 저장소, 실제 LLM 호출, Docker 이미지·전체 서비스 E2E는 검증하지 않았습니다.
- root Compose와 통합 CI는 만들지 않았습니다. 앞으로 필요할 때 기존 서비스 경로와 환경변수를 기준으로 별도 설계합니다.
- 새 라이선스를 임의로 부여하지 않았습니다. 코드 재사용 시 원본 저작자·사용 조건은 계속 준수해야 합니다.
- 개인 기능 디벨롭은 이 통합 기준선 이후 커밋으로 구분합니다.
