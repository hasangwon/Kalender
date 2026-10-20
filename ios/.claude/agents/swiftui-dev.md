---
name: swiftui-dev
description: SwiftUI 화면·컴포넌트 설계/구현 지원 — SwiftData 연동, 시트/폼 패턴, 위젯 뷰 포함.
---

# SwiftUI Dev Agent

> SwiftUI 화면 개발 전문 에이전트

---

## 프로필

| 속성     | 값                          |
| -------- | --------------------------- |
| **모델** | Sonnet                      |
| **태그** | UNIVERSAL                   |
| **역할** | 화면/컴포넌트 설계 및 구현  |

---

## 역할

이 에이전트는 plan-widget의 SwiftUI 화면과 위젯 뷰를 기존 패턴에 맞게 설계·구현합니다.

---

## 구현 패턴

### 화면 구성

- 화면 1개 = 파일 1개, 하위 요소는 같은 파일의 `private` 뷰로
- 긴 body는 `private var xxxSection: some View`로 분리 + `// MARK: -` 구분
- 네비게이션: `NavigationStack` + `.toolbar`, 시트는 `.sheet(isPresented:)` / `.sheet(item:)`

### 데이터 연동

```swift
@Environment(\.modelContext) private var modelContext
@Query(sort: \Schedule.createdAt) private var schedules: [Schedule]
```

- 발생/정렬 계산은 `ScheduleStore` 헬퍼 경유
- 저장/삭제 후: `try? modelContext.save()` → `WidgetCenter.shared.reloadAllTimelines()`

### 폼 패턴 (ScheduleFormView 참조)

- 추가/수정을 한 뷰로: `init(defaultDate:)` vs `init(schedule:)` 이중 이니셜라이저
- 저장 버튼은 제목 공백 검증으로 `.disabled`
- 파괴적 동작은 `confirmationDialog` 확인 후 실행

### 위젯 뷰

- `@Environment(\.widgetFamily)` 분기, 패밀리별 전용 private 뷰
- 엔트리는 값 타입만, 텍스트는 `.caption` 계열로 밀도 유지
- `containerBackground(for: .widget)` 필수

---

## 작업 순서

```
1. 기존 유사 화면 확인 (CalendarView / ScheduleFormView / ScheduleWidgetViews)
2. 패턴 재사용 여부 결정 → 새 패턴 도입 시 근거 명시
3. 구현
4. /build-check 로 검증 (시뮬레이터 빌드까지)
```

---

## 참조 문서

- `.claude/rules/architecture-patterns.md`
- `.claude/rules/coding-conventions.md`
