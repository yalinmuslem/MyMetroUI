# Action Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An in-launcher Action Center panel (opened by dragging down from the top edge) showing notifications, quick-settings shortcuts, a flashlight toggle and a brightness slider, plus notification-count badges on app tiles.

**Architecture:** A `NotificationListenerService` feeds a process-singleton `NotificationRepository` (`StateFlow<List<NotifItem>>`). An `ActionCenterController` combines it with `Torch` and `Brightness` abstractions into one `ActionCenterState` plus actions. `ActionCenter` is a stateless composable; `HomeScreen` hosts it as a drag-driven overlay and passes badge counts to `StartGrid`.

**Tech Stack:** unchanged (Kotlin 2.0.20, Compose BOM 2024.09.03, Robolectric 4.13 JVM tests in `app/src/test`). No new dependencies. New manifest entries: the listener service, `WRITE_SETTINGS`.

**Spec:** `docs/superpowers/specs/2026-10-16-action-center-design.md`

## Global Constraints

- Notification data stays in memory only: never persisted, logged or sent anywhere.
- Our own package and group-summary notifications are never listed or counted.
- Non-clearable (ongoing) notifications show no dismiss button and are skipped by "Clear all" and by badges.
- No direct toggles for Wi-Fi/Bluetooth/airplane/location: quick buttons only open the matching settings screen.
- Brightness needs the special `WRITE_SETTINGS` access; without it the slider requests access instead of writing.
- Panel open threshold fraction `0.25` and animation duration live in `ui/motion/MotionSpec.kt` only.
- No notification actions/reply, grouping, per-app mute, Do Not Disturb or history; no Microsoft assets or marks.
- Tests run on the JVM (`./gradlew :app:testDebugUnitTest`); no device is available.

## Review Focus

- Notification with null/empty title, text or extras (or `SpannableString` text) → blank-safe, no crash.
- Listener disconnected or access revoked → list empties, panel shows the grant prompt, badges disappear.
- Same app in personal and work profile → separate badge counts, both dismissable.
- Drag in progress when edit mode/turnstile starts or the screen rotates → panel settles fully open or closed, never stuck half-way.
- Tapping an item whose `contentIntent` is null or canceled → panel closes, no crash, notification untouched.
- Torch unavailable/busy and `WRITE_SETTINGS` revoked while the panel is open → controls degrade (disabled / request access), no crash.

---

## File Structure

```
data/notif/NotifItem.kt, NotificationRepository.kt, NotificationListener.kt, NotificationAccess.kt, BadgeCounts.kt
data/quick/QuickSettings.kt, Torch.kt, Brightness.kt
ui/actioncenter/PanelDrag.kt, ActionCenterController.kt, ActionCenter.kt
ui/motion/MotionSpec.kt, ui/start/TileView.kt, ui/start/StartGrid.kt   (modify)
ui/home/HomeScreen.kt, ui/settings/SettingsScreen.kt, LauncherApp.kt, HomeActivity.kt, AndroidManifest.xml, README.md   (modify)
```

### Task 1: Notification data, listener and badge counts

**Files:**
- Create: `data/notif/NotifItem.kt`, `NotificationRepository.kt`, `NotificationListener.kt`, `NotificationAccess.kt`, `BadgeCounts.kt`
- Modify: `AndroidManifest.xml` (service with `BIND_NOTIFICATION_LISTENER_SERVICE`), `LauncherApp.kt` (`notificationRepository`)
- Test: `app/src/test/java/com/mymetroui/launcher/data/notif/NotificationMappingTest.kt`, `BadgeCountsTest.kt` (Robolectric)

**Interfaces:**
- Produces:
  - `data class RawNotif(val key: String, val packageName: String, val userId: Int, val title: CharSequence?, val text: CharSequence?, val whenMs: Long, val flags: Int, val visibility: Int, val contentIntent: PendingIntent?)` and `fun StatusBarNotification.toRaw(): RawNotif` (thin adapter, not unit-tested).
  - `data class NotifItem(val key: String, val packageName: String, val userId: Int, val title: String, val text: String, val whenMs: Long, val clearable: Boolean, val visibility: Int, val contentIntent: PendingIntent?, val autoCancel: Boolean)`
  - `fun mapNotifications(raw: List<RawNotif>, ownPackage: String): List<NotifItem>` — drops own package and `Notification.FLAG_GROUP_SUMMARY`; `clearable = flags & (FLAG_ONGOING_EVENT or FLAG_NO_CLEAR) == 0`; `autoCancel = flags & FLAG_AUTO_CANCEL != 0`; null title/text → `""` (`toString()` for any `CharSequence`); newest `whenMs` first.
  - `interface NotificationController { fun cancel(key: String); fun cancelAll(keys: List<String>) }`
  - `class NotificationRepository { val items: StateFlow<List<NotifItem>>; val connected: StateFlow<Boolean>; fun onConnected(raw: List<RawNotif>, ownPackage: String, controller: NotificationController); fun onChanged(raw: List<RawNotif>, ownPackage: String); fun onDisconnected(); fun dismiss(key: String); fun clearAll() }` — `onDisconnected` empties `items` and sets `connected = false`; `dismiss`/`clearAll` call the controller (no-op when disconnected); `clearAll` passes only clearable keys.
  - `fun badgeCounts(items: List<NotifItem>): Map<String, Int>` keyed `"<package>#<userId>"`, counting only clearable items; `fun badgeKey(packageName: String, userId: Int): String`.
  - `fun isNotificationAccessGranted(context: Context): Boolean` (`NotificationManagerCompat.getEnabledListenerPackages`); `fun notificationAccessIntent(): Intent` (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`).
  - `class NotificationListener : NotificationListenerService` calling the repository on `onListenerConnected`, `onNotificationPosted`, `onNotificationRemoved`, `onListenerDisconnected` (reads `activeNotifications`, catches `SecurityException`).

- [ ] **Step 1: Write failing tests:** `NotificationMappingTest`: `mapsTitleTextAndFlags`; `ownPackageAndGroupSummaryDropped`; `nullAndSpannedTextAreBlankSafe` (null title, `SpannableString` text, empty extras); `newestFirst`; `repository_onDisconnected_clearsAndFlagsDisconnected`; `repository_clearAll_passesOnlyClearableKeys`; `repository_dismiss_whenDisconnected_isNoOp`. `BadgeCountsTest`: `countsPerPackageAndUser` (work profile separate), `ongoingNotCounted`, `emptyIsEmptyMap`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*data.notif*"` → Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement per Interfaces; the listener's `NotificationController` calls `cancelNotification`; register the service in the manifest (`exported="true"`, permission `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`, intent filter `android.service.notification.NotificationListenerService`).
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug` → Expected: all PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: add notification repository, listener and badge counts`

### Task 2: Quick settings, torch and brightness

**Files:**
- Create: `data/quick/QuickSettings.kt`, `Torch.kt`, `Brightness.kt`
- Modify: `AndroidManifest.xml` (`WRITE_SETTINGS`)
- Test: `app/src/test/java/com/mymetroui/launcher/data/quick/QuickSettingsTest.kt`, `BrightnessTest.kt`

**Interfaces:**
- Produces:
  - `enum class QuickKind { WIFI, BLUETOOTH, AIRPLANE, LOCATION }`; `fun quickIntent(kind: QuickKind, sdk: Int = Build.VERSION.SDK_INT): Intent` — WIFI: `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` when `sdk >= 29` else `Settings.ACTION_WIFI_SETTINGS`; BLUETOOTH `ACTION_BLUETOOTH_SETTINGS`; AIRPLANE `ACTION_AIRPLANE_MODE_SETTINGS`; LOCATION `ACTION_LOCATION_SOURCE_SETTINGS`.
  - `interface Torch { val available: Boolean; val isOn: StateFlow<Boolean>; fun set(on: Boolean) }` with `class CameraTorch(context: Context) : Torch`: picks the first camera with `FLASH_INFO_AVAILABLE`; none → `available = false`; `TorchCallback` updates `isOn` (and sets it false on `onTorchModeUnavailable`); `set` catches `CameraAccessException`/`IllegalArgumentException` and leaves `isOn` false.
  - `interface Brightness { fun canWrite(): Boolean; fun read(): Float; fun write(fraction: Float) }` with `class SystemBrightness(context: Context) : Brightness` (`Settings.System.SCREEN_BRIGHTNESS`, switches `SCREEN_BRIGHTNESS_MODE` to manual on write, swallows `SecurityException`); `fun fractionToSystem(f: Float): Int` (`0..1` → `1..255`, clamped, never 0) and `fun systemToFraction(v: Int): Float` (clamped `0..1`); `fun writeSettingsIntent(context: Context): Intent` (`ACTION_MANAGE_WRITE_SETTINGS`, `package:` URI).

- [ ] **Step 1: Write failing tests:** `QuickSettingsTest`: `intentActionsPerKind`; `wifi_usesPanelFromApi29_andSettingsBefore` (sdk 28 vs 29). `BrightnessTest`: `fractionToSystem_clampsAndNeverZero` (`-1f→1`, `0f→1`, `0.5f→128`, `1f→255`, `2f→255`); `systemToFraction_clamps` (`0→0f`, `255→1f`, `999→1f`, `-5→0f`); `writeSettingsIntent_targetsPackage`.
- [ ] **Step 2: Run** `--tests "*data.quick*"` → Expected: FAIL.
- [ ] **Step 3:** Implement per Interfaces. `CameraTorch`/`SystemBrightness` are verified manually only (no JVM test).
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug` → Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: add quick settings intents, torch and brightness`

### Task 3: Panel logic and Action Center UI

**Files:**
- Create: `ui/actioncenter/PanelDrag.kt`, `ActionCenterController.kt`, `ActionCenter.kt`
- Modify: `ui/motion/MotionSpec.kt` (`ActionCenterDurationMs = 300`, `ActionCenterOpenFraction = 0.25f`), `app/src/main/res/values/strings.xml`
- Test: `.../ui/actioncenter/PanelDragTest.kt`, `ActionCenterControllerTest.kt`, `.../ui/ActionCenterTest.kt`

**Interfaces:**
- Consumes: `NotifItem`, `NotificationRepository`, `QuickKind`, `Torch`, `Brightness`, `notificationAccessIntent` (Tasks 1–2).
- Produces:
  - `object PanelDrag { const val FlingVelocity = 1200f; fun settle(fraction: Float, velocityPxPerSec: Float, wasOpen: Boolean): Boolean }` — velocity above `FlingVelocity` → open, below `-FlingVelocity` → closed; otherwise a panel that was closed opens when `fraction >= MotionSpec.ActionCenterOpenFraction`, and a panel that was open stays open while `fraction > 1 - MotionSpec.ActionCenterOpenFraction`; `fraction` is clamped to `0..1` first.
  - `data class ActionCenterState(val items: List<NotifItem>, val connected: Boolean, val torchAvailable: Boolean, val torchOn: Boolean, val brightness: Float, val canWriteSettings: Boolean, val deviceLocked: Boolean)`
  - `class ActionCenterController(notifs: NotificationRepository, torch: Torch, brightness: Brightness, isDeviceLocked: () -> Boolean, scope: CoroutineScope)` with `val state: StateFlow<ActionCenterState>` (combines `items`, `connected`, `torch.isOn`; reads `torch.available`, `brightness.canWrite()/read()`, `isDeviceLocked()` on each refresh), `fun refresh()` (re-reads brightness/permission/lock; called when the panel opens), `fun setTorch(on: Boolean)`, `fun setBrightness(fraction: Float)` (only writes when `canWrite()`), `fun dismiss(key: String)`, `fun clearAll()`.
  - `data class ActionCenterActions(val onQuick: (QuickKind) -> Unit, val onTorch: (Boolean) -> Unit, val onBrightness: (Float) -> Unit, val onRequestWriteSettings: () -> Unit, val onDismiss: (String) -> Unit, val onClearAll: () -> Unit, val onOpen: (NotifItem) -> Unit, val onGrantAccess: () -> Unit, val onClose: () -> Unit)`
  - `@Composable fun ActionCenter(state: ActionCenterState, actions: ActionCenterActions, modifier: Modifier = Modifier)`; tags: `quick-<kind lowercase>`, `torch-toggle`, `brightness-slider`, `notif-<key>`, `notif-dismiss-<key>`, `notif-clear-all`, `notif-empty`, `notif-grant-access`. When `deviceLocked` and an item's `visibility != Notification.VISIBILITY_PUBLIC`, the row shows only the app's package name instead of title/text. The brightness slider is overlaid with a click target calling `onRequestWriteSettings` while `!canWriteSettings`; the torch toggle is disabled when `!torchAvailable`; "Clear all" shows only when at least one item is clearable; `connected = false` shows the grant prompt instead of the list.

- [ ] **Step 1: Write failing tests:** `PanelDragTest`: `smallDragFromClosed_staysClosed` (`0.1`), `pastThresholdFromClosed_opens` (`0.25`, `0.6`), `fastFlingOpens_evenWithSmallFraction`, `fastUpFlingCloses_evenWhenMostlyOpen`, `closeDragFromOpen_needsMoreThanQuarter` (`0.8` stays open, `0.7` closes), `fractionClamped` (`-1`, `5`). `ActionCenterControllerTest` (fakes + `UnconfinedTestDispatcher`): `state_reflectsNotificationsAndTorch`; `setBrightness_withoutPermission_doesNotWrite`; `setBrightness_withPermission_writes`; `refresh_picksUpRevokedWritePermission`; `dismissAndClearAll_forwardToRepository`; `disconnected_stateIsEmptyAndNotConnected`. `ActionCenterTest` (compose): `listsNotifications`; `dismissButton_callsOnDismiss`; `ongoingItem_hasNoDismiss`; `clearAll_onlyWhenClearableExists`; `emptyState_whenConnectedAndNoItems`; `grantPrompt_whenNotConnected`; `tapItem_callsOnOpen`; `torchDisabled_whenUnavailable`; `brightnessWithoutPermission_requestsAccess`; `quickButtons_callOnQuick`; `lockedPrivateItem_hidesContent`.
- [ ] **Step 2: Run** `--tests "*PanelDragTest*" --tests "*ActionCenterControllerTest*" --tests "*ActionCenterTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement per Interfaces; strings for labels (`action_center`, `clear_all`, `no_notifications`, `grant_notification_access`, `quick_wifi`, `quick_bluetooth`, `quick_airplane`, `quick_location`, `torch`, `brightness`); colors from `MaterialTheme.colorScheme`.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest` → Expected: all PASS.
- [ ] **Step 5: Commit** `feat: add action center panel, controller and drag logic`

### Task 4: Integration, badges and Settings entry

**Files:**
- Modify: `ui/home/HomeScreen.kt`, `ui/start/StartGrid.kt`, `ui/start/TileView.kt`, `ui/settings/SettingsScreen.kt`, `LauncherApp.kt`, `HomeActivity.kt`, `README.md`
- Test: `.../ui/TileBadgeTest.kt`, `.../ui/ActionCenterHostTest.kt`; extend `SettingsScreenTest.kt`

**Interfaces:**
- Consumes: everything from Tasks 1–3.
- Produces:
  - `TileView(ui, icon, modifier, badge: Int = 0)` draws a circle at the top-right with the count (`"99+"` above 99; SMALL tiles show `"9+"` above 9); `badge <= 0` or a live tile → nothing; test tag `badge-<tile id>`.
  - `StartGrid(..., badgeFor: (AppInfo) -> Int = { 0 })` passes the count to app tiles only.
  - `HomeScreen(..., controller: ActionCenterController, badgeCounts: Map<String, Int>, onOpenNotification: (NotifItem) -> Unit, onQuick: (QuickKind) -> Unit, onRequestWriteSettings: () -> Unit, onGrantAccess: () -> Unit)`: panel overlay with fraction `0..1` (`Animatable`, `MotionSpec.ActionCenterDurationMs`); a `TopEdgeHeight = 24.dp` strip under the status-bar inset and the panel itself use vertical `draggable` (velocity from `onDragStopped`) and settle with `PanelDrag.settle`; scrim tap, upward drag and Back close it; the Back handler is registered after the Settings one so it wins; dragging is ignored while `editing` or `turnstile.progress > 0`, and if either becomes true mid-drag the panel animates to its `settle` target; `controller.refresh()` runs when the panel starts opening.
  - `SettingsScreen(..., onOpenActionCenter: () -> Unit = {})`: button tagged `open-action-center` that closes Settings and opens the panel.
  - `LauncherApp`: `notificationRepository`, `torch` (`CameraTorch`), `brightness` (`SystemBrightness`); `HomeActivity` builds the `ActionCenterController` (lock check via `KeyguardManager.isDeviceLocked`), opens notifications by sending `contentIntent` (try/catch `PendingIntent.CanceledException`; then `cancelNotification` through the repository when `autoCancel`), starts quick intents and the access/write-settings intents inside try/catch `ActivityNotFoundException`, and passes `badgeCounts(items)` from `notificationRepository.items`.

- [ ] **Step 1: Write failing tests:** `TileBadgeTest`: `badgeShowsCount`; `badgeCapsAt99Plus`; `smallTileShowsNinePlus`; `zeroCount_noBadge`; `liveTile_neverShowsBadge`; `workProfileTile_usesItsOwnCount` (two tiles, same package, different `userId`). `ActionCenterHostTest` (compose via `HomeScreen` with fakes): `dragFromTopEdge_opensPanel` (drag past the threshold on the strip → `notif-empty` or list visible); `smallDrag_staysClosed`; `backClosesPanelBeforeSettings`; `panelIgnoredWhileEditing`; `scrimTapCloses`. `SettingsScreenTest`: `openActionCenterButton_callsCallback`.
- [ ] **Step 2: Run** `--tests "*TileBadgeTest*" --tests "*ActionCenterHostTest*" --tests "*SettingsScreenTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement per Interfaces; update `PinUnpinFlowTest`/`SettingsOverlayTest` call sites for the new `HomeScreen` parameters (defaults or fakes); README gains an "Action Center" bullet and a note that notification access must be enabled manually (and Play Store listing must disclose it).
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` → Expected: all PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: integrate action center, tile badges and settings entry`
