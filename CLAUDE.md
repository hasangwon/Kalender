# CLAUDE.md

이 파일은 Claude Code가 이 저장소(하상원의 달력 — iOS + Android)에서 작업할 때 참고하는 루트 가이드입니다.
플랫폼별 상세 규칙은 각 폴더의 CLAUDE.md 에 있습니다.

## 저장소 구조 (모노레포)

| 폴더       | 내용                                                                                             | 상세 가이드         |
| ---------- | ------------------------------------------------------------------------------------------------ | ------------------- |
| `ios/`     | SwiftUI + SwiftData + WidgetKit 앱 — App Store 배포 중                                           | `ios/CLAUDE.md`     |
| `android/` | Kotlin + Jetpack Compose + Glance 앱                                                             | `android/CLAUDE.md` |
| `common/`  | 두 플랫폼이 같은 값을 봐야 하는 것 — 공휴일 데이터, 테스트 기대값, 백업 형식 명세, 색 토큰·아이콘 원본, 스토어 자산, 문서 | `common/README.md`  |
| `docs/`    | 개인정보 처리방침 — 스토어에 등록한 URL 이 이 경로를 가리키므로 **옮기지 않는다**                 |                     |

`common/` 에는 두 언어가 함께 컴파일하는 코드는 없다. Swift 와 Kotlin 은 각자 구현하고,
결과가 같아야 하는 것만 common 의 데이터·기대값·명세로 묶는다.

## 공통 원칙

- **언어**: 대화·문서는 한국어, 코드·커밋은 영어 (Conventional Commits — `.claude/skills/commit-message`)
- **커밋**: 사용자 승인 후. Co-Authored-By 등 AI 서명 금지. 원격 push 는 별도 승인 (`.claude/skills/git-push`)
- **우선순위**: 기존 패턴 준수 > 일관성 > 새 기술 도입
- **플랫폼 동등성**: 사용자가 보는 동작과 문구는 두 플랫폼이 같아야 한다.
  한쪽에만 기능을 넣으면 `common/docs/parity.md` 에 다른 쪽 작업 항목을 남긴다

## 두 플랫폼이 함께 지켜야 하는 것

| 항목                                   | 원본                              | 규칙                                                                                                                                      |
| -------------------------------------- | --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| 한국 공휴일 확정 테이블                | `common/data/korean-holidays.json` | Android 는 이 파일을 assets 로 읽는다. iOS 는 아직 `ios/Shared/Sources/KoreanHolidays.swift` 에 같은 표가 있어 **수정 시 양쪽 동시 수정** |
| 공휴일 자동 계산·음력 환산·반복 판정   | 각 플랫폼 코드                    | 기대값은 `common/fixtures/` — 규칙을 바꾸면 기대값도 갱신하고 양쪽 테스트를 돌린다                                                        |
| 백업 파일 `.hscal`                     | `common/spec/backup-format.md`    | 필드명·원시 문자열·날짜 인코딩(Swift 기준시 초)을 바꾸지 않는다. 양쪽에서 서로의 파일을 읽을 수 있어야 한다                                 |
| 설정 키·enum 원시값                    | 각 플랫폼 코드                    | 이름을 같게 유지 (`theme.primaryColor`, `"weekly"` 등) — 두 코드를 나란히 읽기 위해                                                        |
| 색 팔레트                              | `common/design/colors.json`       | 변경 시 양쪽 반영                                                                                                                         |
| 사용자 노출 문구                       | 각 플랫폼 코드                    | 같은 기능은 같은 문구. 플랫폼 고유 명칭만 다르다 (예: "애플 달력" ↔ "안드로이드 달력")                                                     |

## 문서 위치

| 경로                              | 용도                                      |
| --------------------------------- | ----------------------------------------- |
| `common/docs/requirements.md`     | 요구사항 원문 정리 (iOS 1차 개발 당시)    |
| `common/docs/decisions.md`        | 제품·구조 결정 기록                       |
| `common/docs/parity.md`           | 기능별 플랫폼 구현 현황                   |
| `common/docs/play-console.md`     | Play 스토어 등록 절차                     |
| `.claude/skills/`                 | 저장소 공용 스킬 (commit-message, git-push) |
| `ios/.claude/`, `android/.claude/` | 플랫폼별 규칙·에이전트·스킬               |
