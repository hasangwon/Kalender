# 백업 파일 형식

두 플랫폼이 서로의 백업을 읽을 수 있어야 한다. 이 문서는 호환성 계약이다 — 필드를 **더할** 수는 있지만
기존 필드의 이름·형식·원시 문자열은 바꾸지 않는다.

## 내보내기 타입 1 — `.hscal` (JSON)

iOS 의 `JSONEncoder` 기본 동작이 기준이다. Android 도 같은 결과를 내야 한다.

| 규칙             | 값                                                                                                   |
| ---------------- | ---------------------------------------------------------------------------------------------------- |
| 인코딩           | UTF-8 JSON, 키 알파벳순 정렬, 들여쓰기 출력                                                           |
| 날짜             | **2001-01-01T00:00:00Z 기준 초(Double)** — Swift `Date.timeIntervalSinceReferenceDate`. Unix 초와 `978307200` 차이 |
| UUID             | 대문자 하이픈 문자열 (`"7C9E6679-7425-40DE-944B-E07FC1F90AE7"`). 읽을 때는 대소문자 무시           |
| 선택 필드        | 값이 없으면 **키를 생략**한다 (`null` 을 쓰지 않는다). 읽을 때는 키 없음과 `null` 모두 없음으로 처리 |
| 파일 이름        | `hscal_yyyyMMdd.hscal`                                                                               |

```json
{
  "anniversaries": [
    {
      "createdAt": 804000000.5,
      "day": 9,
      "id": "4B1F0D2A-...",
      "isLunar": true,
      "month": 7,
      "name": "엄마 생일"
    }
  ],
  "exportedAt": 813000000.123,
  "schedules": [
    {
      "colorRaw": "blue",
      "createdAt": 804000000.5,
      "endDate": 820000000,
      "hasCustomColor": false,
      "hasTime": true,
      "id": "7C9E6679-7425-40DE-944B-E07FC1F90AE7",
      "memo": "",
      "monthlyOnLastDay": false,
      "notifies": true,
      "recurrenceRaw": "weekly",
      "startDate": 812000000,
      "title": "팀 회의"
    }
  ],
  "version": 1
}
```

### 필드

**schedules[]**

| 필드               | 형식            | 비고                                                      |
| ------------------ | --------------- | --------------------------------------------------------- |
| `id`               | UUID 문자열     | 가져올 때 같은 id 가 이미 있으면 건너뜀                   |
| `title`            | 문자열          |                                                           |
| `memo`             | 문자열          | 없으면 빈 문자열                                          |
| `startDate`        | Double (기준시 초) | 종일이면 그날 0시(기기 시간대), 시간 있으면 그 시각       |
| `hasTime`          | Bool            |                                                           |
| `recurrenceRaw`    | `"none"` \| `"weekly"` \| `"monthly"` |                                              |
| `colorRaw`         | `"red"` `"orange"` `"yellow"` `"green"` `"teal"` `"blue"` `"purple"` `"pink"` | |
| `endDate`          | Double, 선택    | 반복 종료일(포함). 단일 일정은 항상 없음                   |
| `hasCustomColor`   | Bool            | false 면 `colorRaw` 대신 유형 기본 색을 따른다            |
| `notifies`         | Bool            | 하루 일괄 알림 대상                                       |
| `monthlyOnLastDay` | Bool            | 매달 반복일 때만 의미                                     |
| `createdAt`        | Double          | 정렬용                                                    |

**anniversaries[]**

| 필드        | 형식        | 비고                       |
| ----------- | ----------- | -------------------------- |
| `id`        | UUID 문자열 |                            |
| `name`      | 문자열      |                            |
| `month`     | Int 1~12    | 음력이면 음력 월           |
| `day`       | Int 1~31    | 음력이면 1~30              |
| `isLunar`   | Bool        |                            |
| `createdAt` | Double      |                            |

**최상위**

| 필드            | 형식   | 비고                                             |
| --------------- | ------ | ------------------------------------------------ |
| `version`       | Int    | 현재 1. 읽는 쪽은 자기 버전보다 크면 거절한다    |
| `exportedAt`    | Double |                                                  |

### 가져오기 규칙

- 같은 `id` 가 이미 있으면 건너뛴다 (같은 파일을 두 번 가져와도 중복 없음). 결과는 "N개 가져왔어요 (M개 건너뜀)".
- 알 수 없는 `recurrenceRaw`/`colorRaw` 는 각 플랫폼의 기본값(`none`, `blue`)으로 읽는다.

## 내보내기 타입 2 — `.redcalendar` (Realm DB)

타사 달력 앱의 내보내기 파일. Realm 코어 파일(형식 24)을 라이브러리 없이 직접 해석한다.
`class_RC_Event` 테이블의 `year`/`month`/`day` 와 글자 칸 `body`/`bodyS`/`bodySS`/`continuousBody` 만 읽어
**한 줄 = 종일 단일 일정 하나**로 만든다. `isDeleted != 0` 행은 버린다. 색·굵게·반복·음력·메모는 무시한다.
가져오기 중복 기준은 "같은 날 + 같은 제목".

해석기 구현: `ios/App/Sources/Backup/ExportType2Importer.swift`, `android/.../app/backup/ExportType2Importer.kt`.
두 구현은 같은 바이트 규칙을 따른다 (노드 헤더 "AAAA" 8바이트, 정수 배열 폭, 문자열 리프 3형태, 클러스터 트리).
