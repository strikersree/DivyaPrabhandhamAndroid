# Verifying the Android app

## Build
```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

## Emulator
```
export ANDROID_HOME=~/Library/Android/sdk
$ANDROID_HOME/platform-tools/adb devices   # emulator-5554 already up?
# else:
nohup $ANDROID_HOME/emulator/emulator -avd Pixel_10_Pro_XL -no-snapshot-load -no-boot-anim &
# wait for: adb shell getprop sys.boot_completed -> 1
```
Also available: `Small_Tablet`.

## Install & drive — full tap automation works here
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.srinivaskannan.divyaprabhandham.debug \
  -c android.intent.category.LAUNCHER 1
adb shell uiautomator dump /sdcard/ui.xml
adb shell cat /sdcard/ui.xml | tr '>' '\n' | grep -o 'text="..."[^/]*bounds="[x1,y1][x2,y2]"'
adb shell input tap <centerX> <centerY>
adb exec-out screencap -p > out.png
```
This is real: `uiautomator dump` gives exact bounds, tap the center,
re-dump/screenshot. No accessibility-permission blocker like iOS has.

## Deep link — fastest way to a specific reading page
The app registers `divyaprabhandham://` (mirrors iOS). Unlike iOS, this
does **not** show a confirmation dialog:
```
adb shell am start -a android.intent.action.VIEW \
  -d "divyaprabhandham://open?section=<id>" \
  com.srinivaskannan.divyaprabhandham.debug
```
Prefer this over tapping through Home for anything but flows that are
themselves under test (onboarding, navigation, settings toggles).

## Fresh-install verification
For anything about first-run state (a stale claim in about text, a
seed-data change), `adb uninstall <pkg>` first — a plain re-`install -r`
keeps DataStore/SharedPreferences from the previous run and can hide a
regression that only shows up on a clean install.

## Script vs. UI language are independent settings
Switching "Script & Language" changes which script verses/credits render
in; it does *not* follow the chrome language. To see an English credit
name or blurb on screen, switch script to "English · Readable" (not just
set UI language) — Settings → Script & Language → English · Readable.
