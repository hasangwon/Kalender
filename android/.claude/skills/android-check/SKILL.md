---
name: android-check
description: Android 단위 테스트 + 디버그 빌드 검증. '/android-check' 또는 '안드로이드 빌드 체크해줘'.
---

# Android Check Skill

> 공용 로직 단위 테스트와 디버그 APK 빌드를 검증한다

**전제**: `cd android` 후

```bash
export JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home
```

## 검증 단계

```bash
# 1. 단위 테스트 (음력·공휴일·반복 판정·백업 형식 — common/fixtures 기대값과 대조)
./gradlew :app:testDebugUnitTest --console=plain 2>&1 | grep -E 'FAILED|BUILD|tests completed'

# 2. 디버그 빌드 (컴파일·리소스·매니페스트)
./gradlew :app:assembleDebug --console=plain 2>&1 | grep -E '^e: |error:|FAILED|BUILD'

# 3. (릴리스 제출 전) 축소 빌드까지
./gradlew :app:assembleRelease --console=plain 2>&1 | grep -E 'error:|FAILED|BUILD'
```

테스트 결과 상세: `app/build/test-results/testDebugUnitTest/*.xml`, 리포트: `app/build/reports/tests/testDebugUnitTest/index.html`

## 출력 예시

```
✓ 단위 테스트 통과 (N개)
✓ 디버그 빌드 성공 (BUILD SUCCESSFUL)
```

## 일반적인 오류

| 오류                                             | 원인/해결                                                        |
| ------------------------------------------------ | ---------------------------------------------------------------- |
| `Unable to locate a Java Runtime`                | JAVA_HOME 미설정 → 위 export                                     |
| `SDK location not found`                         | `local.properties` 에 `sdk.dir=~/Library/Android/sdk` 기록       |
| `requires ... compile against version 37`        | 의존성이 compileSdk 상향 요구 → `platforms;android-37.x` 설치 후 `compileSdk` 조정 |
| `common/ 폴더를 찾을 수 없습니다` (테스트)       | 테스트는 `android/app` 에서 상위로 `common/` 을 찾는다 — 저장소 구조 확인 |

## 주의사항

1. 새 라이브러리 버전은 `gradle/libs.versions.toml` 에만 적는다
2. `common/data`·`common/fixtures` 를 바꿨으면 테스트를 반드시 다시 돌린다 (iOS 와의 동일성 검증 지점)
