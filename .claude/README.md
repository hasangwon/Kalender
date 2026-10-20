# .claude 디렉토리 (저장소 루트)

저장소 전체에 적용되는 Claude Code 설정. 플랫폼별 규칙·에이전트·스킬은 `ios/.claude/`, `android/.claude/` 에 있다.

## 구조

| 경로            | 용도                                                      |
| --------------- | --------------------------------------------------------- |
| `../CLAUDE.md`  | 루트 가이드 — 모노레포 구조, 두 플랫폼 공통 규칙          |
| `skills/`       | 저장소 공용 스킬 (`/commit`, `/push`)                      |
| `settings.json` | 도구 권한 허용 목록 (xcodebuild / gradlew / adb / git 등)  |

## 스킬 요약

| 스킬           | 호출      | 역할                                     | 위치            |
| -------------- | --------- | ---------------------------------------- | --------------- |
| commit-message | `/commit` | Conventional Commits 영어 커밋 (승인 후) | 루트            |
| git-push       | `/push`   | main 푸시, 개인 SSH 별칭 사용            | 루트            |
| build-check    | `/build-check` | iOS 타입체크 + 시뮬레이터 빌드      | `ios/.claude/`  |
| simulator-run  | `/simulator-run` | iOS 시뮬레이터 설치·실행·캡처     | `ios/.claude/`  |
| android-check  | `/android-check` | Android 단위 테스트 + 디버그 빌드  | `android/.claude/` |
| emulator-run   | `/emulator-run` | Android 에뮬레이터 설치·실행·캡처   | `android/.claude/` |

## 유지 원칙

- 문서는 한국어, 코드/커밋은 영어
- 규칙이 바뀌면 문서를 먼저 고치고 코드에 적용
- 한 플랫폼에만 해당하는 지식은 그 플랫폼의 `.claude/` 에, 둘 다 해당하면 `common/docs/` 에
