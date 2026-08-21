# 일정위젯 (PlanWidget)

iOS 일정 관리 앱 + 홈 화면 위젯.

- 로그인 없이 바로 사용 (추후 카카오 로그인 예정)
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
- **로그인**: 현재 비활성. 추후 카카오 로그인 SDK 연동 예정
  (Apple 로그인은 유료 개발자 계정이 필요해 보류).
- App Group 엔타이틀먼트가 없어도 앱은 동작합니다 (로컬 저장 폴백).
  단, 이 경우 위젯이 앱 데이터를 읽지 못합니다.
