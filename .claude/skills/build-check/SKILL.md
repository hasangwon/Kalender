---
name: build-check
description: Swift 타입체크 + xcodegen + 시뮬레이터 빌드 검증. '/build-check' 또는 '빌드 체크해줘'.
---

# Build Check Skill

> Swift 타입 체크 및 시뮬레이터 빌드 검증

---

## 개요

이 스킬은 프로젝트의 빌드 상태를 검증합니다.

**호출 방법**: `/build-check` 또는 "빌드 체크해줘"

**전제**: `export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`

---

## 검증 단계

### 1단계: 프로젝트 생성물 동기화

```bash
xcodegen generate
```

- project.yml 변경이 있었으면 필수, 아니면 스킵 가능

### 2단계: 빠른 타입 체크 (빌드보다 훨씬 빠름)

```bash
SDK=$(xcrun --sdk iphonesimulator --show-sdk-path)
xcrun swiftc -typecheck -target arm64-apple-ios17.0-simulator -sdk "$SDK" \
  App/Sources/PlanWidgetApp.swift App/Sources/Views/*.swift \
  Shared/Sources/*.swift Shared/Sources/Models/*.swift Shared/Sources/Store/*.swift

xcrun swiftc -typecheck -target arm64-apple-ios17.0-simulator -sdk "$SDK" -parse-as-library \
  Widget/Sources/*.swift \
  Shared/Sources/*.swift Shared/Sources/Models/*.swift Shared/Sources/Store/*.swift
```

### 3단계: 전체 빌드 (에셋/링크/임베드 검증 포함)

```bash
xcodebuild -project PlanWidget.xcodeproj -scheme PlanWidget \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

---

## 워크플로우

```
1. (project.yml 변경 시) xcodegen generate
2. swiftc 타입 체크 (앱 + 위젯)
   └─ 실패 → 오류 출력, 수정 후 재시도
3. xcodebuild 전체 빌드
   └─ 실패 → 로그에서 "error:" 추출해 분석
```

---

## 출력 예시

### 성공

```
✓ 타입 체크 통과 (앱/위젯)
✓ 시뮬레이터 빌드 성공 (BUILD SUCCEEDED)

빌드 검증이 완료되었습니다. 커밋해도 됩니다.
```

---

## 일반적인 오류 및 해결

| 오류                                                     | 원인/해결                                             |
| -------------------------------------------------------- | ----------------------------------------------------- |
| `Embedded binary's bundle identifier is not prefixed...` | 위젯 번들 ID가 앱 ID prefix가 아님 → project.yml 확인 |
| `No available simulator runtimes`                        | iOS 런타임 미설치 → Xcode Settings > Components       |
| `Found no destinations`                                  | 런타임/DEVELOPER_DIR 문제 → export 확인               |
| 새 파일이 빌드에 없음                                    | xcodegen generate 미실행                              |

---

## 주의사항

1. **새 소스 파일 추가 후에는 xcodegen generate** (기존 폴더 하위라도 프로젝트 재생성이 안전)
2. 전체 빌드는 오래 걸림 — 코드만 고쳤으면 타입 체크 먼저
