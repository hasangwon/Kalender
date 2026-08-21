# 일정위젯 (PlanWidget)

iOS 일정 관리 앱 + 홈 화면 위젯.

- Apple 로그인 (게스트 모드 지원)
- 달력에서 날짜 선택 → 일정 작성/수정/삭제
- 반복: 단일 / 매주 / 매달
- 홈 화면 위젯 (Small/Medium) — 오늘 + 다가오는 일정
- SwiftUI + SwiftData + WidgetKit, iOS 17+

## 빌드 방법

```bash
brew install xcodegen   # 최초 1회
xcodegen generate       # PlanWidget.xcodeproj 생성
open PlanWidget.xcodeproj
```

Xcode에서:
1. PlanWidget 타깃 → Signing & Capabilities → Team 선택
2. 시뮬레이터 또는 기기에서 실행

## 참고

- **App Group**: 앱↔위젯 데이터 공유용 (`group.com.hasangwon.planwidget`).
  개발자 계정에 맞게 바꾸려면 `Shared/Sources/SharedConstants.swift`와 `project.yml` 두 곳 수정.
- **Apple 로그인**은 유료 Apple Developer Program 멤버십이 필요합니다.
  무료 계정이면 서명 에러가 나는데, `project.yml`에서 `com.apple.developer.applesignin` 줄을 지우고
  다시 `xcodegen generate` 하면 됩니다 (앱은 "로그인 없이 시작"으로 사용 가능).
- App Group 엔타이틀먼트가 없어도 앱은 동작합니다 (로컬 저장 폴백).
  단, 이 경우 위젯이 앱 데이터를 읽지 못합니다.
