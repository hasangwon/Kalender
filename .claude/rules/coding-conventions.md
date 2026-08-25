# Coding Conventions

> plan-widget 프로젝트의 Swift/SwiftUI 코딩 규칙

---

## 목차

1. [Swift 규칙](#1-swift-규칙)
2. [SwiftUI 컴포넌트 규칙](#2-swiftui-컴포넌트-규칙)
3. [SwiftData 규칙](#3-swiftdata-규칙)
4. [네이밍](#4-네이밍)
5. [코드 품질 체크리스트](#5-코드-품질-체크리스트)

---

## 1. Swift 규칙

- 강제 언래핑(`!`) 금지 — `guard let` / `??` / `try?` 사용
  - 예외: 앱 구동 불가 상황의 `fatalError` (컨테이너 생성 실패 등)
- `switch`는 가능하면 exhaustive하게, `default` 남용 금지
- 열거형 원시값 저장 패턴: SwiftData에는 `xxxRaw: String` 저장 + 계산 프로퍼티로 노출
  (`Schedule.recurrence` / `Schedule.color` 참조)
- 날짜 연산은 반드시 `Calendar` API 사용 — `86400`초 더하기 금지 (DST/윤초)
- 사용자 노출 문자열은 한국어, 날짜 포맷은 `Locale(identifier: "ko_KR")` 명시

## 2. SwiftUI 컴포넌트 규칙

- 화면 1개 = 파일 1개 (`XxxView.swift`), 하위 컴포넌트는 같은 파일 `private` 뷰/함수로
- body가 80줄 이상이면 `private var` 섹션 또는 하위 View로 분리 (`// MARK: -` 구분)
- 상태 규칙:
  - 화면 로컬 상태 → `@State`
  - 모델 CRUD → `@Environment(\.modelContext)` + `@Query`
  - 시트 표시는 `@State` Bool 또는 `@State var editing: Model?` (item 시트)
- 매 렌더마다 생성되는 `DateFormatter` 금지 — `formatted(.dateTime...)` API 우선
- 색상/스타일 하드코딩보다 시스템 시맨틱 컬러 우선 (`Color(.systemBackground)` 등)

## 3. SwiftData 규칙

- 모델 변경(마이그레이션 유발) 시 신중히 — 필드 추가는 기본값 필수
- 저장은 명시적 `try? modelContext.save()` 후 `WidgetCenter.shared.reloadAllTimelines()`
- 위젯 등 뷰 밖에서는 `ModelContext(container)` 생성해 동기 fetch
- 필터/정렬 로직은 `ScheduleStore`에 모은다 — 뷰에 흩뿌리지 말 것

## 4. 네이밍

| 대상          | 규칙                      | 예시                          |
| ------------- | ------------------------- | ----------------------------- |
| 타입/뷰       | PascalCase                | `ScheduleFormView`            |
| 프로퍼티/함수 | camelCase                 | `makeMonthDays()`             |
| 파일          | 타입명과 동일             | `ScheduleFormView.swift`      |
| SwiftData Raw | `xxxRaw`                  | `recurrenceRaw`               |
| 상수 그룹     | `enum` 네임스페이스       | `SharedConstants`, `Keys`     |

## 5. 코드 품질 체크리스트

- [ ] 강제 언래핑 없음
- [ ] 날짜 연산에 Calendar 사용
- [ ] 일정 변경 후 위젯 reload 호출
- [ ] Shared 코드에 앱 전용 API 없음
- [ ] project.yml 변경 시 xcodegen generate 실행
- [ ] 시뮬레이터 빌드 통과 (`/build-check`)
