---
name: simulator-run
description: 시뮬레이터에 앱 빌드·설치·실행하고 스크린샷으로 확인. '/simulator-run' 또는 '시뮬레이터 실행해줘'.
---

# Simulator Run Skill

> iOS 시뮬레이터에서 앱 실행 및 화면 확인

---

## 개요

이 스킬은 앱을 시뮬레이터에 빌드·설치·실행하고, 스크린샷으로 결과를 확인합니다.

**호출 방법**: `/simulator-run` 또는 "시뮬레이터 실행해줘"

**전제**: `export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`

---

## 워크플로우

```
1. 기기 선택 및 부팅
   DEVICE=$(xcrun simctl list devices available | grep -oE "iPhone [^(]*\(([A-F0-9-]+)\)" \
     | head -1 | grep -oE "[A-F0-9-]{36}")
   xcrun simctl boot "$DEVICE" || true          # 이미 부팅됐으면 무시
   xcrun simctl bootstatus "$DEVICE" -b         # 부팅 완료 대기

2. 해당 기기로 빌드
   xcodebuild -project PlanWidget.xcodeproj -scheme PlanWidget \
     -destination "id=$DEVICE" build

3. 설치 및 실행
   APP=$(find ~/Library/Developer/Xcode/DerivedData \
     -path "*Build/Products/Debug-iphonesimulator/PlanWidget.app" -not -path "*Index*" | head -1)
   xcrun simctl install "$DEVICE" "$APP"
   xcrun simctl launch "$DEVICE" com.hasangwon.planwidget

4. 화면 확인
   open -a Simulator                             # 사용자에게 창 표시
   xcrun simctl io "$DEVICE" screenshot shot.png # 스크린샷 캡처 → Read로 확인
```

---

## 자주 쓰는 명령

| 목적          | 명령                                                        |
| ------------- | ----------------------------------------------------------- |
| 앱 종료       | `xcrun simctl terminate "$DEVICE" com.hasangwon.planwidget` |
| 앱 삭제       | `xcrun simctl uninstall "$DEVICE" com.hasangwon.planwidget` |
| 홈 화면       | Simulator 앱에서 ⌘+Shift+H                                  |
| 시간 변경     | `xcrun simctl status_bar "$DEVICE" override --time "9:41"`  |
| 기기 종료     | `xcrun simctl shutdown "$DEVICE"`                           |

---

## 주의사항

1. **UI 변경 검증은 스크린샷까지** — 빌드 성공만으로 "동작 확인"이라고 말하지 않는다
2. 무서명 시뮬레이터에선 App Group 미동작 → 위젯이 앱 데이터를 못 읽는 건 정상 (아키텍처 문서 참조)
3. 위젯 확인: 홈 화면 길게 누르기 → + → "일정위젯" 검색
