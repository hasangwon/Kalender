---
name: emulator-run
description: Android 에뮬레이터에 앱 설치·실행하고 스크린샷으로 확인. 위젯 3크기 미리보기 포함. '/emulator-run' 또는 '에뮬레이터 실행해줘'.
---

# Emulator Run Skill

> 에뮬레이터(Pixel_8_API_36)에서 앱 실행 및 화면 확인

**전제**: `cd android` 후

```bash
export JAVA_HOME=$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
```

## 워크플로우

```bash
# 1. 에뮬레이터 부팅 (화면 없이, 스크린샷은 가능)
nohup emulator -avd Pixel_8_API_36 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot-save > /tmp/emu.log 2>&1 &
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done

# 2. 빌드·설치·실행
./gradlew :app:assembleDebug --console=plain | grep -E 'FAILED|BUILD'
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.hasangwon.planwidget/.MainActivity --ez seed true   # seed: 샘플 일정 (디버그 빌드만)

# 3. 화면 확인 (Read 로 열어 본다)
adb exec-out screencap -p > shot.png
adb logcat -d -s AndroidRuntime:E | tail -20     # 크래시 확인

# 4. 위젯 3크기 미리보기 (런처 없이 한 화면에)
adb shell am start -W -n com.hasangwon.planwidget/.debug.DebugWidgetHostActivity
sleep 4; adb exec-out screencap -p > widgets.png
```

## 자주 쓰는 명령

| 목적        | 명령                                                        |
| ----------- | ----------------------------------------------------------- |
| 탭          | `adb shell input tap X Y` (좌표는 캡처 원본 1080×2400 기준) |
| 뒤로가기    | `adb shell input keyevent BACK`                             |
| 앱 종료     | `adb shell am force-stop com.hasangwon.planwidget`          |
| 앱 삭제     | `adb uninstall com.hasangwon.planwidget`                    |
| 다크 모드   | `adb shell cmd uimode night yes` / `no`                     |
| 날짜 바꾸기 | `adb shell su 0 date 012010002027.00` 은 구글 이미지에서 불가 — 자동 시간 끄고 `adb shell settings put global auto_time 0` 후 `adb shell date`
| 종료        | `adb emu kill`                                              |

## 주의사항

1. **UI 변경 검증은 스크린샷까지** — 빌드 성공만으로 "동작 확인" 이라고 말하지 않는다. 캡처는 화면당 1~2장
2. 한글 입력은 `adb shell input text` 로 안 된다 — 샘플 데이터는 `--ez seed true` 로 넣는다
3. 에뮬레이터를 창 있게 보려면 `-no-window` 를 빼고 띄운다 (사용자가 직접 볼 때)
