# Live Tiles Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standalone Clock, Calendar and Photos tiles that flip between a front (icon + label) and a live back face, added from the Settings screen.

**Architecture:** Live tiles are ordinary `Tile`s whose key is `live:<kind>`, so `TileLayout`, Room, drag, resize and unpin are unchanged. `StartViewModel` maps keys to `TileContent` (`App` or `Live`). Each kind has a source that emits `LiveFace`; a pure `FlipSchedule` plus a gated flip animation in `StartGrid` drives the rotation.

**Tech Stack:** unchanged (Kotlin 2.0.20, Compose BOM 2024.09.03, Room 2.6.1, Robolectric 4.13 JVM tests in `app/src/test`). No new dependencies. New manifest permission: `READ_CALENDAR`.

**Spec:** `docs/superpowers/specs/2026-10-16-live-tiles-design.md`

## Global Constraints

- Live tile keys: `live:clock`, `live:calendar`, `live:photos`; one tile per kind.
- Only MEDIUM and WIDE tiles flip; SMALL stays static.
- Photos use user-picked images (`ACTION_OPEN_DOCUMENT` with persistable URI permission); never request `READ_MEDIA_IMAGES`/`READ_EXTERNAL_STORAGE`.
- Calendar needs `READ_CALENDAR`, requested when the tile is added; denied → tile not added.
- No notification badges, widgets, weather or per-user flip settings; no Microsoft assets or marks.
- All flip durations/intervals in `ui/motion/MotionSpec.kt` only.
- Tests run on the JVM (`./gradlew :app:testDebugUnitTest`); no device available. Existing tests that construct `TileUi(tile, app)` keep compiling.

## Review Focus

- Calendar permission revoked while the tile is pinned → tile stays, shows the permission prompt, never crashes.
- A picked photo is deleted/unreadable → skipped; all unreadable or none picked → "pick photos" face, no crash.
- A DB tile with an unknown `live:*` key → skipped on Start, not deleted.
- Edit mode entered, tile resized or removed mid-flip → tile snaps to front, never stuck rotated.
- Clock change, timezone change or midnight → clock face and "today" window computed from wall clock at each tick, not cached.
- Adding a live kind that is already pinned → ignored (no duplicate).

---

## File Structure

```
data/live/LiveKind.kt, LiveFace.kt, LiveTileSource.kt     model + interface (+ NoLiveTiles)
data/live/ClockSource.kt, CalendarSource.kt, PhotoStore.kt, PhotosSource.kt, CompositeLiveSource.kt
data/live/CalendarQuery.kt                                 interface + ContentResolver implementation
data/live/LiveTileAdder.kt                                 add-flow logic (permission / picker / pin)
ui/StartViewModel.kt (TileUi, TileContent)                 modify
ui/motion/FlipSchedule.kt, MotionSpec.kt                   flip timing + constants
ui/start/LiveTileView.kt, StartGrid.kt, TileView.kt        modify/create
ui/settings/SettingsScreen.kt, ui/home/HomeScreen.kt, HomeActivity.kt, LauncherApp.kt, AndroidManifest.xml  modify
```

### Task 1: Live model and view-model mapping

**Files:**
- Create: `data/live/LiveKind.kt`, `data/live/LiveFace.kt`, `data/live/LiveTileSource.kt`
- Modify: `ui/StartViewModel.kt`, `ui/start/StartGrid.kt`, `ui/start/TileView.kt`, `ui/home/HomeScreen.kt`, `app/src/test/.../ui/StartViewModelTest.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/ui/StartViewModelTest.kt` (extend)

**Interfaces:**
- Produces:
  - `enum class LiveKind { CLOCK, CALENDAR, PHOTOS }` with `val key: String` (`"live:" + name.lowercase()`) and `companion fun fromKey(key: String): LiveKind?`
  - `sealed interface LiveFace { data class Text(val title: String, val lines: List<String> = emptyList()); data class Photo(val uri: String); object NeedsPermission }`
  - `interface LiveTileSource { fun face(kind: LiveKind): Flow<LiveFace?>; fun onFlipped(kind: LiveKind) {} }` (`null` face = nothing to show); `object NoLiveTiles : LiveTileSource` (empty flow)
  - `sealed interface TileContent { data class App(val app: AppInfo); data class Live(val kind: LiveKind) }`
  - `data class TileUi(val tile: Tile, val content: TileContent)` with `constructor(tile: Tile, app: AppInfo)` and `val app: AppInfo?` (null for live)
  - `StartViewModel(tiles: TileSource, apps: AppSource, val live: LiveTileSource = NoLiveTiles)`; `fun addLive(kind: LiveKind)` (pins `kind.key` unless already pinned)
- Behavior: key `live:<known>` → `TileContent.Live`; unknown `live:*` key → skipped from `state` and never passed to `removeKeys`; app tiles behave as before (missing app → removed). Tile launch/tap for live tiles is handled in Task 4; here `StartGrid`/`TileView` must render a `Live` tile as a plain accent tile with its kind's name as label so the app compiles and runs.

- [ ] **Step 1: Write failing tests** (extend `StartViewModelTest`): `liveKey_mapsToLiveContent`; `liveTile_notRemovedWhenAppListChanges`; `unknownLiveKey_skippedAndNotDeleted` (tile `live:weather` absent from `state` but still in the fake `TileSource`); `addLive_pinsKey_onceOnly`; `LiveKind.fromKey("live:clock") == CLOCK`, `fromKey("live:nope") == null`, `fromKey("a/B#0") == null`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*StartViewModelTest*"` → Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement per Interfaces; update `StartViewModel.state`/missing-app logic to skip keys starting with `live:`; adapt `StartGrid`, `TileView`, `HomeScreen` and the existing assertion `it.app.key` → `it.app!!.key`.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest` → Expected: all PASS.
- [ ] **Step 5: Commit** `feat: add live tile model and view-model mapping`

### Task 2: Live sources

**Files:**
- Create: `data/live/ClockSource.kt`, `CalendarQuery.kt`, `CalendarSource.kt`, `PhotoStore.kt`, `PhotosSource.kt`, `CompositeLiveSource.kt`
- Modify: `LauncherApp.kt` (`liveSource` lazy), `AndroidManifest.xml` (`READ_CALENDAR`), `HomeActivity.kt` (pass `live = app.liveSource` to the `StartViewModel` factory)
- Test: `.../data/live/ClockSourceTest.kt`, `CalendarSourceTest.kt`, `PhotoStoreTest.kt` (Robolectric)

**Interfaces:**
- Consumes: `LiveKind`, `LiveFace`, `LiveTileSource` (Task 1).
- Produces:
  - `fun clockFace(now: ZonedDateTime, locale: Locale = Locale.getDefault()): LiveFace.Text` (title `HH:mm` via the locale's short time style, one line = medium weekday + date); `fun millisToNextMinute(nowMs: Long): Long` (1..60000); `class ClockSource(now: () -> ZonedDateTime = { ZonedDateTime.now() }) : LiveTileSource` (emits immediately, then at each minute boundary using wall clock each time; other kinds → empty flow)
  - `data class CalendarEvent(val title: String, val startMs: Long, val allDay: Boolean)`; `interface CalendarQuery { fun hasPermission(): Boolean; fun nextEvent(fromMs: Long, untilMs: Long): CalendarEvent?; fun changes(): Flow<Unit> }`; `class CalendarSource(query: CalendarQuery, now: () -> ZonedDateTime = { ZonedDateTime.now() })`: no permission → `NeedsPermission`; event → `Text(title, listOf(time or "All day"))`; none → `Text("No events today")`; window = now → end of the local day computed at each refresh; refresh on minute tick and `query.changes()`; any exception from the query → `null` face. `class ContentResolverCalendarQuery(context: Context) : CalendarQuery` using `CalendarContract.Instances`.
  - `class PhotoStore(prefs: SharedPreferences)`: `fun uris(): List<String>`, `fun add(uris: List<String>)` (dedupes, keeps order), `fun clear()`; entries that are blank or not `content://` are dropped on read.
  - `class PhotosSource(store: PhotoStore, canOpen: (String) -> Boolean) : LiveTileSource`: face = `Photo(uri)` of the current index among openable URIs; `onFlipped(PHOTOS)` advances the index; no openable photo → `Text("Pick photos")`.
  - `class CompositeLiveSource(vararg sources: LiveTileSource)`: `face(kind)` merges the first non-empty source for the kind (each source returns `emptyFlow()` for kinds it does not own); `onFlipped` forwards to all.

- [ ] **Step 1: Write failing tests:** `ClockSourceTest`: `clockFace_formatsTimeAndDate` (fixed `ZonedDateTime`, `Locale.US` → `"14:05"`-style per US short time), `millisToNextMinute_isWithinOneMinute` (`nowMs = 59_999 → 1`, `60_000 → 60_000`, `0 → 60_000`), `face_recomputedFromWallClock` (change the fake `now` between emissions; second face differs). `CalendarSourceTest` with a fake `CalendarQuery`: `noPermission_needsPermission`, `nextEvent_mapsTitleAndTime`, `allDayEvent_showsAllDay`, `noEvents_showsNone`, `queryThrows_emitsNull`, `window_endsAtLocalMidnight` (assert `untilMs` equals the next local midnight for a zone with DST offset, e.g. `America/New_York`). `PhotoStoreTest`: `addAndReadKeepsOrderAndDedupes`, `blankAndNonContentEntriesDropped`, `persistsAcrossInstances`; `PhotosSource` tests: `rotatesOnFlip`, `unreadableSkipped`, `noneOpenable_showsPickPhotos`.
- [ ] **Step 2: Run** `--tests "*live.*"` → Expected: FAIL.
- [ ] **Step 3:** Implement per Interfaces. Clock/calendar tick: `flow { while (true) { emit(…); delay(millisToNextMinute(System.currentTimeMillis())) } }`. `ContentResolverCalendarQuery` is checked manually only (no JVM test); it must return `null`/emit nothing instead of throwing when the provider is missing.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest` → Expected: all PASS; `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: add clock, calendar and photos live sources`

### Task 3: Flip schedule and live tile view

**Files:**
- Create: `ui/motion/FlipSchedule.kt`, `ui/start/LiveTileView.kt`
- Modify: `ui/motion/MotionSpec.kt`, `ui/start/StartGrid.kt`, `ui/home/HomeScreen.kt`
- Test: `.../ui/motion/FlipScheduleTest.kt`, `.../ui/LiveTileViewTest.kt`; extend `EditModeTest.kt`

**Interfaces:**
- Consumes: `LiveFace`, `LiveKind`, `TileContent.Live`, `StartViewModel.live` (Task 1).
- Produces:
  - `MotionSpec`: `FlipDurationMs = 600`, `FlipHoldMs = 3500`, `FlipIntervalMs = 9000`, `FlipStaggerMs = 700`.
  - `object FlipSchedule { fun nextFlipAt(tile: Tile, nowMs: Long): Long? }` — `null` for SMALL; otherwise the smallest `t > nowMs` with `t ≡ offset (mod FlipIntervalMs)` where `offset = (tile.id % 8) * FlipStaggerMs`.
  - `@Composable fun LiveTileView(ui: TileUi, kind: LiveKind, face: LiveFace?, icon: Drawable, flipped: Boolean, modifier: Modifier)`: front = existing `TileView`; back = `Text` face (title large, lines small), `Photo` face (decoded downsampled bitmap, failure → front only), or `NeedsPermission` ("Tap to allow"); rotation via `graphicsLayer.rotationX` animated 0↔180 with `MotionSpec.FlipDurationMs`, back side counter-rotated so it is not mirrored; `face == null` → never shows back.
  - `StartGrid(..., liveFace: (LiveKind) -> Flow<LiveFace?> = { emptyFlow() }, flipEnabled: Boolean = true, onFlipped: (LiveKind) -> Unit = {})`: per live tile a flip loop (wait until `FlipSchedule.nextFlipAt`, flip, hold `FlipHoldMs`, flip back, call `onFlipped`) that runs only while `flipEnabled && !editing`; when it stops mid-flip the tile snaps to the front. `HomeScreen` passes `flipEnabled = !editing && turnstile.progress == 0f && pager.currentPage == 0 && lifecycle is STARTED`.
  - Test tags: `live-back-<id>` on the back face, `live-permission-<id>` on the prompt.

- [ ] **Step 1: Write failing tests:** `FlipScheduleTest`: `small_neverScheduled`; `offsetsDifferPerTile` (ids 0 and 1 differ by `FlipStaggerMs`); `nextIsStrictlyAfterNow_andWithinInterval`; `sameIdSameOffset_modEight` (ids 3 and 11). `LiveTileViewTest`: `front_showsLabel`; `flipped_showsTextBack`; `needsPermission_showsPrompt`; `nullFace_neverShowsBack` (flipped = true, no back tag); `small_isNotLive` (SMALL tile renders static front). `EditModeTest`: `liveTile_canBeDraggedResizedUnpinned`; `enteringEdit_snapsFlippedTileToFront` (back face absent after `editing = true`).
- [ ] **Step 2: Run** `--tests "*FlipScheduleTest*" --tests "*LiveTileViewTest*" --tests "*EditModeTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement per Interfaces. Photo bitmaps: decode with `ImageDecoder`/`BitmapFactory` `inSampleSize` to ≤ tile size; run off the main thread; any exception → no back.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest` → Expected: all PASS.
- [ ] **Step 5: Commit** `feat: add live tile flip and view`

### Task 4: Adding, tapping and Settings section

**Files:**
- Create: `data/live/LiveTileAdder.kt`
- Modify: `ui/settings/SettingsScreen.kt`, `ui/home/HomeScreen.kt`, `HomeActivity.kt`, `app/src/main/res/values/strings.xml`, `README.md`
- Test: `.../data/live/LiveTileAdderTest.kt`, `.../ui/LiveTapTest.kt`; extend `SettingsScreenTest.kt`

**Interfaces:**
- Consumes: `StartViewModel.addLive`, `LiveKind`, `LiveFace`, `PhotoStore`, `CalendarQuery.hasPermission` (Tasks 1–2).
- Produces:
  - `class LiveTileAdder(pinned: () -> Set<LiveKind>, requestCalendar: ((Boolean) -> Unit) -> Unit, pickPhotos: ((List<String>) -> Unit) -> Unit, store: PhotoStore, pin: (LiveKind) -> Unit)` with `fun add(kind: LiveKind)`: already pinned → no-op; CLOCK → pin; CALENDAR → `requestCalendar { granted -> if (granted) pin }`; PHOTOS → `pickPhotos { uris -> if (uris.isNotEmpty()) { store.add(uris); pin } }`.
  - `sealed interface LiveTap { data class Open(val kind: LiveKind); object RequestCalendar }`; `fun liveTap(kind: LiveKind, face: LiveFace?): LiveTap` — `NeedsPermission` for CALENDAR → `RequestCalendar`, otherwise `Open(kind)`.
  - `fun liveOpenIntent(kind: LiveKind): Intent` — CLOCK `AlarmClock.ACTION_SHOW_ALARMS`; CALENDAR `ACTION_VIEW` on `content://com.android.calendar/time/<now>`; PHOTOS `ACTION_MAIN` + `CATEGORY_APP_GALLERY`. All started with `NEW_TASK` inside try/catch (`ActivityNotFoundException` ignored).
  - `SettingsScreen(..., pinnedLive: Set<LiveKind> = emptySet(), onAddLive: (LiveKind) -> Unit = {})`: "Live tiles" section with buttons tagged `live-add-clock`, `live-add-calendar`, `live-add-photos`; pinned kinds render disabled (no click) with a "On Start" label. `HomeScreen(..., onAddLive: (LiveKind) -> Unit, onLiveTap: (LiveKind, LiveFace?) -> Unit)`.
  - `HomeActivity`: `registerForActivityResult(RequestPermission)` for `READ_CALENDAR`, `OpenMultipleDocuments` (`image/*`, calls `takePersistableUriPermission(READ)` on each result in try/catch), builds `LiveTileAdder`, handles `LiveTap`; strings `live_tiles`, `live_clock`, `live_calendar`, `live_photos`, `live_on_start`, `live_tap_to_allow`, `live_pick_photos`.

- [ ] **Step 1: Write failing tests:** `LiveTileAdderTest`: `clock_pinsImmediately`; `calendar_grantedPins_deniedDoesNot`; `photos_pickedStoresAndPins`; `photos_emptyPickDoesNotPin`; `alreadyPinned_isNoOp` (no callbacks invoked). `LiveTapTest`: `calendarNeedsPermission_requestsInsteadOfOpening`; `calendarWithFace_opens`; `clockAndPhotos_alwaysOpen`; `openIntents_haveExpectedActions`. `SettingsScreenTest`: `liveAddButtons_callOnAddLive`; `pinnedLiveButton_isDisabled`.
- [ ] **Step 2: Run** `--tests "*LiveTileAdderTest*" --tests "*LiveTapTest*" --tests "*SettingsScreenTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement per Interfaces; wire `HomeScreen` tile tap so `TileContent.Live` calls `onLiveTap(kind, latestFace)` (no turnstile for `RequestCalendar`); update README "Using it" with the Live tiles section and the permission note.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` → Expected: all PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: add live tile add flow, tap handling and settings section`
