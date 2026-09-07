# SAN — 개인 디벨롭용 통합 저장소

Scrap-Notify 팀 프로젝트의 프론트엔드·백엔드·AI를 서비스별 디렉터리로 통합합니다. 원본 프로젝트의 저자와 커밋 이력은 보존하고, 이 저장소에서 추가한 개인 개선은 별도 커밋으로 구분합니다.

| 디렉터리 | 원본 | 구성 |
| --- | --- | --- |
| `frontend/` | [san-frontend](https://github.com/Scrap-Notify/san-frontend) | pnpm workspace: 대시보드·확장 프로그램·공유 패키지 |
| `backend/` | [san-backend](https://github.com/Scrap-Notify/san-backend) | Java 21 / Gradle / Spring Boot |
| `ai/` | [san-ai](https://github.com/Scrap-Notify/san-ai) | Python 3.11+ / uv / FastAPI |

조직의 별도 `docs`, `san-engine`, `.github` 저장소는 가져오지 않습니다. 각 서비스에 원래 포함된 문서·설정은 보존합니다. 원본 조직 저장소와 기존 로컬 체크아웃은 수정하지 않습니다.

## 작업과 검증

각 서비스는 자신의 디렉터리에서 기존 도구로 작업합니다. Java 21, Node 22, pnpm 10.33.2, Python 및 uv가 필요합니다. 실제 Python 버전은 `ai/.python-version`도 확인합니다.

```sh
make verify-imports
make frontend-install
make frontend-build
make backend-test
make ai-install
make ai-lint
make ai-test
```

실행 설정과 환경변수는 각 서비스 README와 `.env.example`을 기준으로 따로 준비합니다. 실제 `.env`, 토큰, 인증서, 운영 데이터는 통합하지 않습니다. AI 실서비스는 외부 API 비용이 발생할 수 있으므로 키를 넣고 실행하기 전에 확인해야 합니다.

Docker를 사용할 경우 `frontend/`, `backend/`, `ai/`에서 각각 실행하여 기존 빌드 컨텍스트와 상대 경로를 유지합니다. 이번 변경은 코드·Git 이력 통합이며, 세 서비스를 한꺼번에 기동하는 신규 Compose나 배포 환경을 구현한 것은 아닙니다.

## 이력·배포 경계

- 기준 커밋은 [integration/sources.json](integration/sources.json)에 고정합니다. 기본 브랜치의 도달 가능한 커밋들을 squash 없이 병합합니다.
- 조직의 다른 브랜치들은 로컬 `source-*` 원격 추적 참조로만 보관하며 개인 저장소 `main`에 자동 병합하지 않습니다. 태그·PR·이슈·Actions secrets·서버 설정은 이관하지 않습니다.
- 각 서비스의 `.github/workflows`는 하위 폴더에 원본 그대로 보존합니다. 루트 `.github/workflows`에는 옮기지 않아 운영 배포·Mattermost 알림·GitLab 동기화가 자동으로 실행되지 않습니다.
- 기존 팀 작업 전체를 개인 단독 기여로 표현하지 않습니다. 원본의 라이선스·저작자 표기를 보존하며 새 라이선스를 임의로 부여하지 않습니다.
- GitHub 업로드·PR·배포는 검증 결과와 정확한 대상을 확인한 뒤 별도로 승인받습니다.

원본 추적 참조가 있는 초기 통합 직후 `make verify-imports`는 각 서비스 트리의 파일 내용·실행 권한이 원본과 같은지, 원본 커밋이 통합 이력의 조상인지 검사합니다. 이후 의도적으로 코드를 개선하면 트리 일치 검사는 실패할 수 있습니다.
