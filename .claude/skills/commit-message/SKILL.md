---
name: commit-message
description: 변경사항을 분석해 Conventional Commits 영어 커밋 메시지 생성. 사용자 승인 후 커밋. AI 서명(Co-Authored-By) 금지. '/commit'.
---

# Commit Message Skill

> Git 커밋 메시지를 Conventional Commits 형식으로 자동 생성

---

## 개요

이 스킬은 현재 변경사항을 분석하여 표준화된 커밋 메시지를 생성합니다.

**호출 방법**: `/commit` 또는 "커밋해줘"

---

## Conventional Commits 형식

```
{type}: {subject (한 줄, 100자 이내)}

{body (필수, 최소 1줄)}
- change 1
- change 2
```

### 타입 (Type)

| 타입       | 설명             | 예시                           |
| ---------- | ---------------- | ------------------------------ |
| `feat`     | 새로운 기능 추가 | 새 화면, 새 위젯 패밀리        |
| `fix`      | 버그 수정        | 오류 해결, 동작 수정           |
| `chore`    | 기타 작업        | project.yml, 설정 변경         |
| `docs`     | 문서 수정        | README, .claude 문서           |
| `style`    | 코드 스타일 변경 | 기능 변경 없는 포맷팅          |
| `refactor` | 리팩토링         | 기능 변경 없는 코드 개선       |
| `test`     | 테스트           | 테스트 코드 추가/수정          |
| `perf`     | 성능 개선        | 최적화                         |

---

## 규칙

### 제목 (Subject)

- **언어**: 영어 필수
- **길이**: 100자 이내, 마침표 없음

### 본문 (Body)

- **필수**: 변경이 아무리 작아도 최소 1줄
- 대시(`-`) 불릿, 각 변경 한 줄씩
- 변수명/함수명/파일명은 원래 이름 그대로

### Co-Authored-By

- **금지**: Co-Authored-By 등 AI 서명 헤더 절대 추가하지 않음

---

## 워크플로우

```
1. git status / git diff 로 변경 분석
   ├─ staging 있음 → staged 기준
   └─ staging 없음 → working tree 기준

2. 규칙에 따라 메시지 작성

3. 메시지를 사용자에게 제시 → 승인 후에만 커밋
   승인 키워드: "승인", "커밋해줘", "ok", "go", "좋아", "진행", "ㅇㅇ", "네"

4. git add <files> && git commit -m "<message>"
```

---

## 예시

### 좋은 예시

```
feat: add monthly recurrence badge to day schedule list

- Show recurrence badge with color tint in scheduleRow
- Add badgeText computed property to Recurrence
```

```
fix: reload widget timeline after schedule deletion

- Call WidgetCenter.reloadAllTimelines in deleteSchedules
```

### 나쁜 예시

```
일정 삭제 버그 수정          ❌ 한글, 프리픽스 없음
update code                 ❌ 모호함
feat: add widget            ❌ 본문 없음
```

---

## 주의사항

1. 커밋 전 `git status`로 의도한 파일만 스테이징됐는지 확인
2. 논리 단위로 커밋 분리 (앱 기능 / 문서 / 프로젝트 설정)
3. `.xcodeproj`는 git 무시 대상 — 커밋에 포함되면 .gitignore 확인
