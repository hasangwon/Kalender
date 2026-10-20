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

**전제**: `cd ios` 후 `export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`

---

## 워크플로우

```
1. 기기 선택 및 부팅
   DEVICE=$(xcrun simctl list devices available | grep -oE "iPhone [^(]*\(([A-F0-9-]+)\)" \
     | head -1 | grep -oE "[A-F0-9-]{36}")
   xcrun simctl boot "$DEVICE" || true          # 이미 부팅됐으면 무시
   xcrun simctl bootstatus "$DEVICE" -b         # 부팅 완료 대기

2. 해당 기기로 서명 빌드 (CODE_SIGNING_ALLOWED=NO 붙이지 말 것)
   xcodebuild -project PlanWidget.xcodeproj -scheme PlanWidget \
     -destination "id=$DEVICE" build

3. 설치 및 실행
   APP=$(find ~/Library/Developer/Xcode/DerivedData \
     -path "*Build/Products/Debug-iphonesimulator/PlanWidget.app" -not -path "*Index*" | head -1)
   xcrun simctl install "$DEVICE" "$APP"
   xcrun simctl launch "$DEVICE" com.hasangwon.planwidget

4. 화면 확인
   open /Applications/Xcode.app/Contents/Applications/DeviceHub.app  # 사용자에게 창 표시 (Simulator.app 대신 Device Hub)
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

### 위젯 기준 날짜 바꾸기 (디버그 빌드 전용)

시뮬레이터 설정엔 날짜 변경 메뉴가 없다. 6주짜리 달 등은 App Group 설정 `debug.widgetToday`로 확인한다.

```bash
G=$(xcrun simctl get_app_container "$DEVICE" com.hasangwon.planwidget group.com.hasangwon.planwidget)
P="$G/Library/Preferences/group.com.hasangwon.planwidget"
xcrun simctl spawn "$DEVICE" defaults write "$P" debug.widgetToday 2027-01-15   # 지정
xcrun simctl launch "$DEVICE" com.hasangwon.planwidget                          # 디버그 빌드는 실행 시 위젯 reload
xcrun simctl spawn "$DEVICE" defaults delete "$P" debug.widgetToday             # 확인 후 반드시 삭제
```

- 홈 화면에 위젯 배치(길게 누르기)는 simctl로 못 하므로 사용자에게 요청

---

## 주의사항

1. **UI 변경 검증은 스크린샷까지** — 빌드 성공만으로 "동작 확인"이라고 말하지 않는다
2. **설치는 반드시 서명 빌드** — 무서명 빌드를 설치하면 App Group 권한이 빠져 위젯이 일정을 못 읽고, 서명 빌드 위에 덮어 설치하면 App Group 컨테이너가 삭제된다
   - 위젯에 일정이 안 보이면 "원래 그렇다"고 넘기지 말고, 설치된 앱의 엔타이틀먼트(`codesign -d --entitlements - "$APP"`)와 App Group 컨테이너부터 확인
3. 위젯 확인: 홈 화면 길게 누르기 → + → "일정위젯" 검색
