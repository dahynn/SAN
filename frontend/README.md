# 🌲 SAN (Scrap And Notify) - Frontend README

> **The Knowledge Forest**
> SAN은 사용자가 수집한 데이터를 단순한 '적재'가 아닌 '나만의 지식 숲을 형성'하는 경험으로 전환하는 지식 관리 솔루션입니다.

---

## 🛠 Tech Stack

| 분류                 | 기술 스택                     | 비고                                                             |
| :------------------- | :---------------------------- | :--------------------------------------------------------------- |
| **Package Manager**  | `pnpm` (Workspaces)           | 효율적인 모노레포 관리                                           |
| **Framework**        | `React`(^18.3) + `Vite`(^6.0) | 빠른 개발 서버 및 빌드                                           |
| **Language**         | `TypeScript`(^5.0)            | 타입 안정성 확보                                                 |
| **Styling**          | `Tailwind CSS v4`(^4.0)       | `@tailwindcss/postcss` 기반                                      |
| **State Management** | `Zustand`(^5.0)               | 가볍고 직관적인 상태 관리                                        |
| **Data Fetching**    | `TanStack Query v5`           | 서버 상태 관리 최적화                                            |
| **Extension Tool**   | `CRXJS`(^2.0)                 | Vite 기반 크롬 익스텐션 개발 플러그인(실시간 수정사항 반영 지원) |

---

## 📂 Project Structure

모노레포 구조를 통해 코드 재사용성을 극대화합니다.

```text
san-frontend/
├── packages/
│   ├── shared/      # 공통 타입(ScrapCard), 유틸리티, API 클라이언트
│   ├── ui/          # 디자인 시스템 (UI 컴포넌트)
│   ├── extension/   # Chrome Extension (사이드패널 중심)
│   └── dashboard/   # 웹 대시보드 (React 앱)
├── tailwind.config.js # 전역 디자인 시스템 설정
└── pnpm-workspace.yaml
```

### 모노레포란?

이 프로젝트는 모노레포(Monorepo) 구조를 채택하고 있습니다. 여러 개의 독립적인 서비스(익스텐션, 대시보드)와 공통 모듈을 하나의 코드 저장소(Repository) 안에서 통합 관리하는 방식입니다.

통합 관리 (Single Source of Truth): 디자인 시스템(ui), 공통 로직 및 타입(shared)을 한 곳에서 관리합니다. 따라서 한 곳에서 수정하면 모든 패키지에 즉시 반영되어 유지보수가 매우 효율적입니다.

의존성 효율: pnpm workspaces를 통해 중복 라이브러리 설치를 방지하고, 전체 프로젝트의 라이브러리 버전 일관성을 완벽하게 유지합니다.

협업의 최적화: 팀원들은 자신이 담당하는 패키지뿐만 아니라 프로젝트 전체의 구조를 쉽게 파악하고 기여할 수 있습니다.

---

## 🚀 Getting Started

### 1. Prerequisites

이 프로젝트는 `pnpm`을 사용합니다. 설치되어 있지 않다면 먼저 설치해 주세요.

```bash
npm install -g pnpm
```

### 2. Installation

루트 폴더에서 전체 의존성을 설치합니다.

```bash
pnpm install
```

### 3. Development

각 패키지를 독립적으로 혹은 동시에 실행할 수 있습니다.

- 전체 실행: `pnpm dev`

- 익스텐션 실시간 빌드: `pnpm dev --filter @san/extension`

- 대시보드 실행: `pnpm dev --filter @san/dashboard`

---

## 🧩 Chrome Extension Load Guide

익스텐션 개발 시에는 빌드된 결과물을 브라우저에 수동으로 한 번 로드해야 합니다.

1. 크롬 주소창에 `chrome://extensions` 입력
2. 우측 상단 **개발자 모드** ON
3. **압축해제된 확장 프로그램을 로드** 클릭
4. `packages/extension/dist` 폴더 선택

---

## 🎨 Design System: The Knowledge Forest

시각적 언어는 바이오필릭 디지털(Biophilic Digital) 철학을 따르며, Summer Cool Light 계열의 색상을 사용합니다.

1. Color Palette
   Midnight Forest (#0F172A): 대시보드 배경 및 깊은 그림자 표현

Forest Mint (#A7F3D0): 메인 포인트 컬러 및 강조 텍스트

Neon Cyan (#22D3EE): AI 분석 상태, 하이라이트, 발광 효과

Misty White (#F8FAFC): 라이트 모드 배경 및 카드 베이스

2. Typography
   Font Family: Pretendard (표준 폰트)

Headline (H1): 24pt / Bold / Tracking -2% (대시보드 타이틀)

Body (Main): 14pt / Regular / Line-height 1.6 (스크랩 본문 가독성)

3. UI Component Principles
   🍃 지식 카드 (Dewy Leaf): Glassmorphism (backdrop-blur-md), 16px 곡률, 유기적 형태 지향

🌿 그래프 노드 (Glowing Roots): 빛무리(Glow) 효과가 적용된 원형 및 베지어 곡선(Bezier Curve) 링크

✨ 알림 (Firefly Guide): 하단에서 떠오르는 부드러운 플로팅 애니메이션

---

## 🤝 Collaboration Rules

### Branch Strategy

- `feat/` : 새로운 기능 추가
- `fix/` : 버그 수정
- `chore/` : 빌드 업무, 패키지 매니저 설정 등
- `refactor/` : 코드 리팩토링

### Commit Message

`type(scope): commit message (ticket_number)` 형식으로 작성합니다.

> 예: `feat(user): 회원가입 API 구현 (S14P31A309-123)`

---

## CI/CD 파이프라인

GitHub Actions 기반으로 세 가지 워크플로우가 동작합니다.

### 1. CI/CD Pipeline (`.github/workflows/ci-cd.yml`)

| 트리거 | 대상 브랜치 |
|--------|-------------|
| `push`, `pull_request` | `main`, `develop` |

**Build 단계 (모든 push/PR):**

```
Checkout → pnpm 설정 → Node.js 22 설치 → 의존성 설치(pnpm install --frozen-lockfile) → Lint → Dashboard 빌드 → Extension 빌드
```

- Lint와 Extension 빌드는 `continue-on-error: true`로 설정되어 실패해도 파이프라인이 중단되지 않습니다.

**Deploy 단계 (main push만):**

```
SSH 접속 → git pull → docker compose up --build → 헬스체크(localhost:80)
```

배포 시 Nginx 컨테이너로 Dashboard 정적 파일을 서빙합니다.

### 2. PR Mattermost 알림 (`.github/workflows/pr-mattermost.yml`)

PR이 열리거나, 업데이트되거나, 머지/클로즈될 때 Mattermost 웹훅으로 알림을 전송합니다.

### 3. GitLab 동기화 (`.github/workflows/sync-to-gitlab.yml`)

`main` 브랜치에 push 시 GitLab의 `front/default` 브랜치로 자동 동기화됩니다. `workflow_dispatch`로 수동 실행도 가능합니다.

### 필요한 GitHub Secrets

| Secret | 용도 |
|--------|------|
| `DEPLOY_HOST` | 배포 서버 호스트 |
| `DEPLOY_USER` | 배포 서버 SSH 사용자 |
| `DEPLOY_SSH_KEY` | 배포 서버 SSH 키 |
| `MATTERMOST_WEBHOOK_URL` | Mattermost 알림 웹훅 URL |
| `GITLAB_TOKEN` | GitLab 동기화용 토큰 |

---

## 로컬 테스트

### 직접 실행

```bash
# 의존성 설치
pnpm install

# 린트
pnpm lint

# 전체 개발 서버 실행
pnpm dev

# 대시보드만 실행
pnpm dev --filter @san/dashboard

# 익스텐션 실시간 빌드
pnpm dev --filter @san/extension

# 대시보드 프로덕션 빌드
pnpm --filter dashboard build

# 익스텐션 프로덕션 빌드
pnpm --filter extension build
```

### Docker로 실행 (Dashboard)

```bash
docker compose up --build

# 헬스체크
curl http://localhost:80
```

Docker 빌드는 Dashboard만 대상이며, 멀티스테이지 빌드(Node.js 빌드 → Nginx 서빙)로 구성됩니다.

---

## 💡 Troubleshooting

- **빨간 줄(TypeScript) 발생 시:** `Ctrl+Shift+P` -> `TypeScript: Restart TS server`를 실행해 주세요.
- **Tailwind 스타일 미적용 시:** `postcss.config.js`에 `@tailwindcss/postcss` 플러그인이 설정되어 있는지 확인하세요.
