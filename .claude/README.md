# .claude 디렉토리

Claude Code가 이 프로젝트에서 작업할 때 참조하는 문서 모음.
(pling-webapp의 문서 체계를 이 프로젝트에 맞게 각색)

## 구조

| 경로              | 용도                                                     |
| ----------------- | -------------------------------------------------------- |
| `../CLAUDE.md`    | 핵심 가이드 — 스택, 명령어, 타깃 구조, 주의사항          |
| `rules/`          | 상세 규칙 (아키텍처 패턴, 코딩 컨벤션)                   |
| `agents/`         | 전문 에이전트 정의 (code-reviewer, swiftui-dev)          |
| `skills/`         | 슬래시 커맨드 (`/commit`, `/push`, `/build-check`, `/simulator-run`) |
| `settings.json`   | 도구 권한 허용 목록 (xcodebuild/simctl/git 등)           |

## 스킬 요약

| 스킬             | 호출            | 역할                                       |
| ---------------- | --------------- | ------------------------------------------ |
| commit-message   | `/commit`       | Conventional Commits 영어 커밋 (승인 후)   |
| git-push         | `/push`         | main 푸시, 개인 SSH 별칭 사용              |
| build-check      | `/build-check`  | 타입체크 + 시뮬레이터 빌드 검증            |
| simulator-run    | `/simulator-run`| 빌드·설치·실행·스크린샷 확인               |

## 유지 원칙

- 문서는 한국어, 코드/커밋은 영어
- 규칙이 바뀌면 문서를 먼저 고치고 코드에 적용
- 프로젝트 특화 지식이 쌓이면 `rules/`에 추가 (도메인 문서 등)
