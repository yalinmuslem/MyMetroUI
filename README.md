# MyMetroUI

An Android home-screen launcher with a Metro-style Start screen: resizable, draggable tiles and an
alphabetical app list with a jump list. Placeholder name; original code, icons and layout (font: Open Sans, OFL).

## Build

Requires JDK 17+ and the Android SDK (platform 35). Put `sdk.dir=<path>` in `local.properties`.

```
./gradlew :app:assembleDebug        # debug APK: app/build/outputs/apk/debug
./gradlew :app:assembleRelease      # minified, unsigned release APK
./gradlew :app:testDebugUnitTest    # unit + Robolectric Compose UI tests
```

## Install and set as default launcher

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open MyMetroUI and tap **Choose** on the banner (Android 10+: role dialog; Android 8-9: Home settings),
or go to *Settings > Apps > Default apps > Home app* and pick MyMetroUI.

## Using it

- Tap a tile to launch; long-press a tile to enter edit mode (drag to move, buttons to resize / unpin).
- Tap empty space or press Back to leave edit mode.
- Swipe left or tap the round arrow for the app list; tap a letter header for the jump list; long-press an app to pin or uninstall.
