# Architecture Patterns

> plan-widget(일정위젯) 프로젝트의 핵심 아키텍처 패턴

---

## 목차

1. [타깃 구조](#1-타깃-구조)
2. [데이터 아키텍처 (SwiftData + App Group)](#2-데이터-아키텍처)
3. [반복 일정 모델](#3-반복-일정-모델)
4. [위젯 아키텍처](#4-위젯-아키텍처)
5. [디렉토리 구조](#5-디렉토리-구조)

---

## 1. 타깃 구조

```
PlanWidget.xcodeproj  ← XcodeGen 생성물 (원본은 project.yml)
├── PlanWidget            앱 타깃 (App/Sources + Shared/Sources)
└── PlanWidgetExtension   위젯 타깃 (Widget/Sources + Shared/Sources)
```

- `Shared/Sources`는 **양쪽 타깃에 소스 단위로 중복 컴파일** — 별도 프레임워크 아님
  - 앱 전용 API(`UIApplication` 등)를 Shared에 넣으면 위젯 빌드가 깨진다
- 위젯 번들 ID는 앱 번들 ID의 prefix 확장 필수 (`com.hasangwon.planwidget` → `.widget`)

## 2. 데이터 아키텍처

```
┌─────────┐     ┌──────────────────────┐     ┌─────────┐
│  앱      │ ──▶ │ App Group 컨테이너    │ ◀── │  위젯    │
│ (CRUD)  │     │ (SwiftData store)    │     │ (읽기)   │
└─────────┘     └──────────────────────┘     └─────────┘
     │                                            ▲
     └── 저장/삭제 후 WidgetCenter.reloadAllTimelines()
```

- 컨테이너 생성은 **`ScheduleStore.makeContainer()` 단일 경로** — 직접 ModelContainer 만들지 말 것
- App Group 엔타이틀먼트가 없으면(무서명 시뮬레이터 등) 로컬 저장소 폴백
  - 이때 앱과 위젯의 데이터가 분리되므로, 위젯 데이터 공유 테스트는 서명 환경에서
- **일정을 변경하는 모든 코드는 저장 후 `WidgetCenter.shared.reloadAllTimelines()` 호출**

## 3. 반복 일정 모델

- 반복 일정을 인스턴스로 복제하지 않는다 — **원본 1개 + 발생 판정** 방식
- 판정은 `Schedule.occurs(on:calendar:)` 한 곳에만 존재. 화면/위젯 각자 날짜 계산 금지
- 규칙:
  - `none`: 시작일 == 대상일
  - `weekly`: 시작일 이후 && 요일 일치
  - `monthly`: 시작일 이후 && 일(day) 일치 — 29~31일 시작은 해당 일자가 없는 달에 미발생
- 날짜별 목록/정렬은 `ScheduleStore.occurrences(in:on:)` 사용 (종일 → 시간순)

## 4. 위젯 아키텍처

- `TimelineProvider`에서 SwiftData를 **동기 fetch** 후 값 타입(`WidgetScheduleItem`)으로 변환
  - `@Model` 객체를 엔트리에 직접 담지 말 것 (컨텍스트 수명 문제)
- 타임라인: 엔트리 1개 + `.after(다음 자정)` 갱신. 데이터 변경 갱신은 앱의 reload 호출에 의존
- 지원 패밀리: systemSmall, systemMedium
- iOS 17 `containerBackground(for: .widget)` 필수

## 5. 디렉토리 구조

```
plan-widget/
├── project.yml               # 프로젝트 정의 (원본)
├── App/
│   ├── Sources/
│   │   ├── PlanWidgetApp.swift
│   │   └── Views/            # 화면 단위 View
│   └── Resources/            # Assets.xcassets
├── Shared/Sources/           # 앱+위젯 공용
│   ├── Models/               # Schedule, ColorTag, Recurrence
│   ├── Store/                # ScheduleStore
│   └── SharedConstants.swift # App Group ID
└── Widget/Sources/           # 위젯 (Bundle/Provider/Views)
```

**파일 배치 기준**:

- 앱 화면 → `App/Sources/Views/`
- 앱·위젯 공용 로직/모델 → `Shared/Sources/`
- 위젯 전용 뷰/프로바이더 → `Widget/Sources/`
- 새 소스 폴더 추가 시 `project.yml`의 `sources` 확인 (기존 최상위 폴더 하위면 자동 포함)
