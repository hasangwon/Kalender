# CLAUDE.md (Android)

이 파일은 Claude Code가 안드로이드 앱(`android/`)에서 작업할 때 참고하는 핵심 가이드입니다.
저장소 전체 구조와 두 플랫폼 공통 규칙은 루트 `../CLAUDE.md` 를 먼저 보세요.

> ⚠️ 아래 모든 명령은 **`android/` 폴더에서** 실행한다.

## 기본 원칙

- **언어**: 한국어 사용 (커밋 메시지는 영어, Conventional Commits — `../.claude/skills/commit-message`)
- **iOS 와 1:1**: 파일·객체 이름을 iOS 와 같게 둔다 (`Schedule`, `DayEventResolver`, `ScheduleProvider` …).
  iOS 에 기능이 들어오면 **같은 이름의 Kotlin 파일**을 고치는 것으로 유지보수가 끝나야 한다
- **문구 동일**: 사용자 노출 문구는 iOS 와 같게. 플랫폼 고유 명칭만 다르다 ("애플 달력" ↔ "안드로이드 달력")
- **우선순위**: 기존 패턴 준수 > 일관성 > 새 기술 도입

## 핵심 스택

- **Kotlin 2.4 + Jetpack Compose (Material3) + Room + Glance** — minSdk 26 / compileSdk 37 / targetSdk 36
- AGP 9 **내장 Kotlin** — `org.jetbrains.kotlin.android` 플러그인을 적용하지 않는다 (`build.gradle.kts` 주석 참조)
- 버전은 전부 `gradle/libs.versions.toml` 한 곳에서 관리
- DI 프레임워크 없음 — `PlanWidgetApp` 이 `AppContainer`(DB·저장소·기기 달력)를 하나 들고, 화면은 `context.appContainer` 로 꺼낸다

## 빌드 및 개발 명령어

```bash
# 이 맥의 시스템 java 는 연결돼 있지 않다 — 항상 먼저:
export JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home

./gradlew :app:testDebugUnitTest      # 공용 로직 단위 테스트 (음력·공휴일·반복·백업 형식)
./gradlew :app:assembleDebug          # 디버그 APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:bundleRelease          # 스토어 제출용 AAB (서명 설정 필요)

# 에뮬레이터: .claude/skills/emulator-run 참조 (AVD: Pixel_8_API_36)
```

- SDK 는 `~/Library/Android/sdk` (`local.properties` 에 기록, git 무시). Android Studio 와 공유
- 디버그 전용 adb 훅 (`src/debug/…/debug/`): 샘플 일정 넣기 `--ez seed true`, 위젯 3크기 미리보기 `DebugWidgetHostActivity`

## 패키지 구조 (iOS 타깃 구조와 대응)

| 패키지 (`com.hasangwon.planwidget.`) | iOS 대응            | 내용                                                        |
| ------------------------------------ | ------------------- | ----------------------------------------------------------- |
| `shared.model` / `shared.store` / `shared.*` | `Shared/Sources`   | Room 엔티티, DAO, 설정, 음력, 공휴일, 발생 판정, 테마 색값     |
| `app.ui`                             | `App/Sources/Views` | Compose 화면 (Calendar / ScheduleForm / Settings / …)       |
| `app.notification` / `app.calendar` / `app.backup` | `App/Sources` | 하루 알림, 안드로이드 달력 읽기, 백업                       |
| `widget`                             | `Widget/Sources`    | Glance 위젯 (Receiver / Provider / Views / Refresh)         |

- 앱과 위젯은 **같은 프로세스** — App Group 이 없다. 설정은 `SharedPrefs`(SharedPreferences 하나), DB 는 `AppDatabase` 하나
- 화면 로직은 ViewModel 또는 상태 클래스(`ScheduleFormState`)에, 화면 파일에는 표시와 로컬 상태만

## 데이터 (Room)

- 엔티티: `Schedule`, `AnniversaryEntry` — 필드명·원시 문자열은 iOS 와 동일 (백업 호환 `../common/spec/backup-format.md`)
- 날짜는 epoch millis `Long`. 판정은 `LocalDate` 로 — `86400000` 더하기 금지
- 반복 발생 판정은 `Schedule.occurs(day)` 한 곳. 여러 날짜는 `DayEventResolver.eventsByDay` (후보 줄이기)
- **일정·기념일 변경은 모두 `ScheduleRepository` 를 거친다** — 저장 후 위젯 갱신(`WidgetRefresh`) + 알림 재예약(`DigestScheduler`)이 거기 있다. DAO 직접 호출 금지
- 스키마 변경 시 `version` 올리고 마이그레이션 추가. 스키마 JSON 은 `app/schemas/` 에 내보내진다

## 설정 (SharedPrefs)

- 키 이름은 iOS 와 같다 (`theme.primaryColor`, `calendar.layoutStyle` …). enum 은 `RawValued.raw` 문자열로 저장
- 설정을 바꾼 화면은 `SettingsRefresh.bump()` 를 불러야 다른 화면이 다시 그려진다 (iOS onDismiss 재읽기 대응)
- 시스템 글자 크기는 무시하고 앱 설정 4단계만 따른다 (`PlanWidgetTheme` 에서 fontScale = 1 고정)

## 위젯 (Glance)

- 크기는 `SizeMode.Responsive` 3구간 (110×110 / 250×110 / 250×250 dp) → `WidgetFamily` SMALL/MEDIUM/LARGE
- `provideGlance` 안에서 DB·설정을 먼저 읽고 `provideContent` 는 그리기만
- 갱신은 `WidgetRefresh.requestUpdate()` (저장 경로가 호출) + 자정 알람(`MidnightReceiver`) + `updatePeriodMillis` 보험
- Glance 제약: 컨테이너당 자식 10개, 말줄임 설정 없음 → 글자 사이 U+200B 로 글자 단위 줄바꿈 후 `maxLines = 1`

## 알림 · 기기 달력 · 백업

- 하루 일괄 알림: **다음 1건만** 정확 알람으로 예약하고 울릴 때 다음 것을 건다 (`DigestScheduler` / `DigestReceiver`). 부팅·시간 변경은 `BootReceiver`
- 기기 달력: `CalendarContract.Instances` 읽기 전용. 접근 수준이 기여자 미만인 달력(공휴일·생일)은 제외
- 백업: `.hscal` 저장 MIME 은 `application/octet-stream` (json 으로 주면 확장자가 바뀐다). 가져오기는 확장자 → 내용 순으로 판별

## 코드 품질 체크리스트

- [ ] `!!` 남용 없음, 날짜 연산에 `java.time` 사용
- [ ] 데이터 변경이 `ScheduleRepository` 를 거침
- [ ] 설정 변경 후 `SettingsRefresh.bump()` 와 위젯 갱신
- [ ] 문구가 iOS 와 같음 (플랫폼 고유 명칭 제외)
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` 통과

## 문서 구조

| 경로                          | 용도                                               |
| ----------------------------- | -------------------------------------------------- |
| `.claude/skills/android-check` | 단위 테스트 + 디버그 빌드 검증 (`/android-check`)  |
| `.claude/skills/emulator-run`  | 에뮬레이터 설치·실행·캡처 (`/emulator-run`)        |
| `../common/docs/parity.md`    | 기능별 플랫폼 구현 현황                            |
| `../common/docs/play-console.md` | Play 스토어 등록 절차                           |
