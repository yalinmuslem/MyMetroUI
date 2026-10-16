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
- Open **Settings** (last row of the app list) to pick an accent color and dark/light mode; the choice is saved on the device.
- In **Settings > Live tiles** add a Clock, Calendar or Photos tile. Live tiles flip every few seconds (medium and wide sizes only). Calendar asks for calendar access when added; Photos opens a picker, so no broad photo permission is needed. Remove them with unpin in edit mode.

## Status

Core launcher (sub-project 1) is implemented: Start grid (small / medium / wide tiles, pin, unpin, drag, resize),
alphabetical app list with jump list, tilt / turnstile / fly-in / parallax motion, default-launcher prompt,
and database recovery. Work profiles are supported (a tile's key includes the user id).

Theme (accent palette + dark/light) and live tiles (clock, calendar, photos with flip) are implemented.

Not yet done: notification badges, lock screen, Action Center, Hubs, search, widgets.

## Project layout

```
domain/   Tile, TileLayout (pure Kotlin grid logic)
data/     AppRepository (LauncherApps), TileRepository (Room), default tiles, grouping
ui/       StartViewModel, AppListViewModel, home/ start/ applist/ motion/ theme/
```

All durations, springs and angles live in `ui/motion/MotionSpec.kt`; tune motion there.

## Testing notes

Compose UI tests run on the JVM with Robolectric (`src/test`), so no device is needed.
Motion and gestures have not been verified on a real device or emulator yet; check them manually and tune `MotionSpec`.
