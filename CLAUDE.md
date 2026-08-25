# CLAUDE.md

이 파일은 Claude Code가 이 리포지토리에서 작업할 때 참고하는 핵심 가이드입니다.

> **상세 가이드**: `.claude/rules/` 필요 시 참조

## 기본 원칙

- **언어**: 한국어 사용 (커밋 메시지는 영어, Conventional Commits)
- **커밋**: Co-Authored-By 등 AI 서명 금지 (`.claude/skills/commit-message` 참조)
- **우선순위**: 기존 패턴 준수 > 일관성 > 새 기술 도입

## 핵심 스택

- **SwiftUI + SwiftData** (iOS 17+)
- **WidgetKit** — 홈 화면 위젯 (Small/Medium)
- **XcodeGen** — `project.yml`이 프로젝트 원본. `.xcodeproj`는 생성물이라 git 무시
- 로그인 없음 (추후 카카오 로그인 예정. Apple 로그인은 유료 계정 문제로 보류)

## 빌드 및 개발 명령어

```bash
# xcode-select가 CLT를 가리키는 환경이면 항상 먼저:
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer

xcodegen generate     # project.yml 변경 후 .xcodeproj 재생성 (필수)
xcodebuild -project PlanWidget.xcodeproj -scheme PlanWidget \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build

# 시뮬레이터 실행: .claude/skills/simulator-run 참조
```

- ⚠️ **project.yml 수정 후 `xcodegen generate` 필수** — 안 하면 Xcode 프로젝트에 반영 안 됨
- 시뮬레이터 빌드는 `CODE_SIGNING_ALLOWED=NO` (팀 미설정 환경)

## 타깃 / 번들 구조

| 타깃                  | 번들 ID                          | 소스                             |
| --------------------- | -------------------------------- | -------------------------------- |
| `PlanWidget` (앱)     | `com.hasangwon.planwidget`        | `App/Sources` + `Shared/Sources` |
| `PlanWidgetExtension` | `com.hasangwon.planwidget.widget` | `Widget/Sources` + `Shared/Sources` |

- ⚠️ 위젯 번들 ID는 **앱 번들 ID의 prefix 확장**이어야 함 (아니면 임베드 에러)
- `Shared/Sources`는 프레임워크가 아니라 **양쪽 타깃에 중복 컴파일**됨 — 앱 전용 API 사용 금지

## 데이터 (SwiftData + App Group)

- 모델: `Schedule` (제목/메모/날짜/시간 유무/반복/색상)
- 반복: `Recurrence` — 단일(none)/매주(weekly)/매달(monthly). 발생 판정은 `Schedule.occurs(on:)`
- 저장소: `ScheduleStore.makeContainer()` — **App Group(`group.com.hasangwon.planwidget`) 우선, 엔타이틀먼트 없으면 로컬 폴백**
- ⚠️ 폴백 상태에선 앱↔위젯 데이터가 분리됨 (시뮬레이터 무서명 환경에서 발생)
- 일정 변경 후 `WidgetCenter.shared.reloadAllTimelines()` 호출 필수
- App Group ID 변경 시 `Shared/Sources/SharedConstants.swift` + `project.yml` 두 곳 동시 수정

## 브랜치 / 배포

- 단일 `main` 브랜치 (개인 프로젝트). 원격 push 전 사용자 확인
- 배포(TestFlight/App Store)는 미정 — 유료 개발자 계정 등록 후 진행

## 문서 구조

| 폴더              | 용도                                             |
| ----------------- | ------------------------------------------------ |
| `.claude/rules/`  | 아키텍처 패턴, Swift 코딩 컨벤션                 |
| `.claude/agents/` | code-reviewer, swiftui-dev                       |
| `.claude/skills/` | commit-message / git-push / build-check / simulator-run |
