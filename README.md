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
- **Action Center:** drag down from the very top edge (or use Settings > Action Center) to see notifications, open Wi-Fi/Bluetooth/airplane/location settings, toggle the flashlight and set brightness. Notifications appear only after you enable MyMetroUI under *Settings > Notifications > Device & app notifications* (notification access); brightness needs the "Modify system settings" permission. Notification content stays in memory on the device and is never stored or sent anywhere. Tiles show a count badge for unread notifications.
- **Welcome screen:** after the screen turns off and on with the launcher in front, a first page shows a big clock, date, next calendar event and a notification summary; swipe up to reach Start. It is display-only and not a security lock (your PIN/biometric stays with the system), needs no extra permissions, and can be switched off in Settings. Not verified on a real device: screen-off ordering on different manufacturers and behaviour right after the system keyguard is dismissed.
- **Hubs:** in *Settings > Hubs* pin a Photos, Quick settings or People tile to Start; tapping it opens that hub full-screen (Back closes). Photos shows the pictures you picked (no media permission); Quick settings gathers the Action Center toggles; People lists contacts and opens the contact card, and is the only feature that asks for `READ_CONTACTS`. Contact data stays in memory only. Not verified on a real device: the contacts query on real data, decoding of very large photos, and the contact card and photo viewer across manufacturers.
- In **Settings > Live tiles** add a Clock, Calendar or Photos tile. Live tiles flip every few seconds (medium and wide sizes only). Calendar asks for calendar access when added; Photos opens a picker, so no broad photo permission is needed. Remove them with unpin in edit mode.

## Status

Core launcher (sub-project 1) is implemented: Start grid (small / medium / wide tiles, pin, unpin, drag, resize),
alphabetical app list with jump list, tilt / turnstile / fly-in / parallax motion, default-launcher prompt,
and database recovery. Work profiles are supported (a tile's key includes the user id).

Theme (accent palette + dark/light) and live tiles (clock, calendar, photos with flip) are implemented.

Action Center, notification badges and Hubs are implemented. The Play Store listing must disclose the notification-listener use and the contacts access (People hub).

Not yet done: search, widgets.

**Device testing:** a manual pass on a Samsung SM-A566B (Android 16) confirmed Start, the app list, the welcome screen (screen off, unlock, swipe up), the Action Center panel, Settings, and pinning and opening the Photos hub. That pass found and fixed a start-up crash and two layout bugs. Still not verified on a device: notification access and badges, the Calendar live tile, the People and Quick settings hubs, picking photos, animation feel, and setting the launcher as the default home app.

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
