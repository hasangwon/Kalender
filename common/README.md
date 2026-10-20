# common/

iOS 와 Android 가 **같은 값을 봐야 하는 것**을 모아 둔 폴더. 코드는 없다 — Swift 와 Kotlin 은 각자 구현하고,
결과가 같아야 하는 데이터·기대값·명세·자산만 여기에 둔다.

| 폴더        | 내용                                                                                  |
| ----------- | ------------------------------------------------------------------------------------- |
| `data/`     | `korean-holidays.json` — 한국 공휴일 확정 공고분 (2025~2027). Android 가 assets 로 읽음 |
| `fixtures/` | 음력 환산·공휴일 자동 계산·반복 판정 기대값, 아이폰에서 내보낸 샘플 `.hscal`           |
| `spec/`     | `backup-format.md` — 백업 파일(.hscal) 형식과 호환 규칙                                |
| `design/`   | `colors.json` 색 토큰, `cal-icon.png` 아이콘 원본                                      |
| `store/`    | `ios/screenshots/`, `android/` — 스토어 제출용 스크린샷·문구                            |
| `docs/`     | 요구사항, 결정 기록, 플랫폼 구현 현황, Play Console 절차                               |

## 바꿀 때 규칙

- `data/korean-holidays.json` 을 고치면 `ios/Shared/Sources/KoreanHolidays.swift` 의 표도 함께 고친다 (iOS 가 JSON 을 읽게 바뀌기 전까지).
- `fixtures/` 기대값은 두 플랫폼 테스트가 공유한다. 규칙을 바꾸면 기대값을 먼저 고치고 양쪽 테스트를 돌린다.
- `spec/backup-format.md` 는 호환성 계약이다. 필드를 더할 수는 있지만 기존 필드의 이름·형식은 바꾸지 않는다.
