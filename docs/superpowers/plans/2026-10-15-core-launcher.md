# Core Launcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An Android home-screen launcher with a Metro-style Start screen (tiles, drag/resize/unpin) and alphabetical app list, with Metro-style motion.

**Architecture:** Single `app` module, Compose-only UI in one `HomeActivity` (`HorizontalPager`: Start | App list). Pure-Kotlin `TileLayout` holds all grid logic; Room persists tiles; `LauncherApps` supplies apps. ViewModels combine repository `Flow`s into UI state. All animation constants live in `MotionSpec`.

**Tech Stack:** Kotlin 2.0.20, AGP 8.5.2, Compose BOM 2024.09.03, Room 2.6.1 (KSP 2.0.20-1.0.25), JUnit4, Robolectric 4.13, Compose UI test, kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-10-15-core-launcher-design.md`

## Global Constraints

- `minSdk` 26; `targetSdk`/`compileSdk` 35.
- Package/applicationId: `com.mymetroui.launcher`; app label: `MyMetroUI` (placeholder name, original; must not contain "Windows", "Phone" or Microsoft marks).
- No Microsoft assets: font is Open Sans (bundled, OFL), icons drawn by us, no Microsoft logos.
- Grid is 4 columns; sizes SMALL 1×1, MEDIUM 2×2, WIDE 4×2; `col + width <= 4`; no overlaps; unique positions.
- `componentKey` = `package/activity#userId`, unique per tile.
- `TileLayout` is pure Kotlin: no `android.*` imports.
- Only tiles are persisted; the app list always comes from `LauncherApps`.
- All durations/easings/angles in `ui/motion/MotionSpec.kt`, nowhere else.
- Out of scope: live tiles, theming beyond one default accent, lock screen, Action Center, Hubs, search, widgets.

## Review Focus

- App label empty/starts with a digit or symbol → listed under a `#` group, not dropped or crashing.
- Two profiles (personal + work) of the same app → two distinct tiles, both launchable.
- Font scale 2.0 or display size large → tile labels truncate with ellipsis, no overflow or crash.
- App uninstalled while in edit mode → its tile disappears, edit mode stays valid.
- Persisted tile with out-of-range `col`/`row`/oversized `size` (corrupt or old data) → sanitized to a valid layout on load.

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/res/font/open_sans_*.ttf (+ values/strings.xml)
app/src/main/java/com/mymetroui/launcher/
  HomeActivity.kt
  domain/Tile.kt, TileLayout.kt
  data/AppInfo.kt, AppRepository.kt, TileRepository.kt, db/{TileEntity,TileDao,AppDatabase}.kt
  ui/StartViewModel.kt, AppListViewModel.kt
  ui/theme/{Theme,Type}.kt
  ui/motion/{MotionSpec,TileModifiers}.kt
  ui/home/{HomeScreen,DefaultLauncherPrompt}.kt
  ui/start/{StartGrid,TileView,EditOverlay}.kt
  ui/applist/{AppListScreen,JumpList,AppMenu}.kt
app/src/test/...        JVM tests (TileLayout, repos via Robolectric, ViewModels)
app/src/androidTest/... Compose UI tests
```

### Task 1: Project scaffold and launcher registration

**Files:**
- Create: Gradle files above, `app/src/main/AndroidManifest.xml`, `HomeActivity.kt`, `ui/theme/{Theme,Type}.kt`, font resources, `strings.xml`
- Test: `app/src/test/java/com/mymetroui/launcher/ManifestSmokeTest.kt`

**Interfaces:**
- Produces: `@Composable fun MyMetroUITheme(content: @Composable () -> Unit)` (dark background, one accent `Color(0xFF1BA1E2)`, Open Sans typography); `HomeActivity` registered with `HOME` + `DEFAULT` + `LAUNCHER` categories, `launchMode="singleTask"`.

- [ ] **Step 1:** Create the Gradle project with the stack versions above, one `app` module, Compose enabled, Room/KSP and test deps declared. Bundle Open Sans Regular/Light/SemiBold in `res/font`.
- [ ] **Step 2: Write the failing test** `ManifestSmokeTest.homeActivityResolvesAsHome` (Robolectric): `PackageManager.resolveActivity(Intent(ACTION_MAIN).addCategory(CATEGORY_HOME), 0)` returns `com.mymetroui.launcher.HomeActivity`.
- [ ] **Step 3: Run** `./gradlew :app:testDebugUnitTest --tests "*ManifestSmokeTest*"` → Expected: FAIL (no activity registered).
- [ ] **Step 4:** Implement manifest and `HomeActivity` (`enableEdgeToEdge()`, `setContent { MyMetroUITheme { Box(Modifier.fillMaxSize()) } }`), `MyMetroUITheme`.
- [ ] **Step 5: Run** the test and `./gradlew :app:assembleDebug` → Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 6: Commit** `feat: project scaffold and HOME activity`

### Task 2: TileLayout (pure grid logic)

**Files:**
- Create: `domain/Tile.kt`, `domain/TileLayout.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/domain/TileLayoutTest.kt`

**Interfaces:**
- Produces:
  - `enum class TileSize(val w: Int, val h: Int) { SMALL(1,1), MEDIUM(2,2), WIDE(4,2) }`
  - `data class Tile(val id: Long, val key: String, val size: TileSize, val col: Int, val row: Int)`
  - `object TileLayout { const val COLUMNS = 4; fun isValid(tiles: List<Tile>): Boolean; fun place(tiles: List<Tile>, new: Tile): List<Tile>; fun move(tiles: List<Tile>, id: Long, col: Int, row: Int): List<Tile>; fun resize(tiles: List<Tile>, id: Long, size: TileSize): List<Tile>; fun compact(tiles: List<Tile>): List<Tile>; fun sanitize(tiles: List<Tile>): List<Tile> }`
  - `place` ignores `new.col/row` and chooses the first free spot (row-major scan); `move` clamps `col` to `0..COLUMNS - w` and `row` to `>= 0`; WIDE always snaps to `col = 0`; overlapped tiles are pushed down to `mover.row + mover.h`, cascading; `resize` clamps `col` the same way and pushes overlapped tiles down; `compact` moves each tile up as far as it can without overlap, preserving `(row, col)` order; `sanitize` drops duplicate keys, clamps bad cols/rows, then re-places overlapping tiles.

- [ ] **Step 1: Write failing tests**, each asserting `TileLayout.isValid(result)` plus the specific outcome:
  - `place_firstFreeSlot`: SMALL into empty → `(0,0)`; second SMALL → `(1,0)`; WIDE after full row 0 → `row 2`.
  - `move_pushesOverlappedDown`: MEDIUM at (0,0) moved onto a MEDIUM at (2,0) at col 2 → the other ends at `row 2`.
  - `move_clampsColumn`: MEDIUM moved to `col=3` ends at `col=2`; WIDE moved to `col=2` ends at `col=0`.
  - `resize_growPushesNeighbor`: SMALL→MEDIUM next to a SMALL pushes it below.
  - `resize_shrinkLeavesGap`: MEDIUM→SMALL leaves other tiles unchanged.
  - `compact_closesEmptyRows`: tiles at rows 0 and 5 end at rows 0 and 2 (for a MEDIUM at row 0).
  - `sanitize_fixesCorruptData`: a tile with `col=9, row=-3, WIDE`, and a duplicate key → valid layout, single tile per key.
  - `randomOperationsKeepInvariants`: seeded `Random(42)`, 500 iterations of random place/move/resize/compact from an empty list, `isValid` after every step.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*TileLayoutTest*"` → Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement `Tile.kt` and `TileLayout.kt` per the Interfaces block.
- [ ] **Step 4: Run** the same command → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add TileLayout grid logic with tests`

### Task 3: Tile persistence

**Files:**
- Create: `data/db/{TileEntity,TileDao,AppDatabase}.kt`, `data/TileRepository.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/data/TileRepositoryTest.kt` (Robolectric, in-memory Room)

**Interfaces:**
- Consumes: `Tile`, `TileSize`, `TileLayout.sanitize/compact/place/move/resize` (Task 2).
- Produces:
  - `class TileRepository(dao: TileDao)` with `val tiles: Flow<List<Tile>>` (emits sanitized list), `suspend fun pin(key: String)`, `suspend fun unpin(id: Long)`, `suspend fun move(id: Long, col: Int, row: Int)`, `suspend fun resize(id: Long, size: TileSize)`, `suspend fun removeKeys(keys: Set<String>)`, `suspend fun compact()`, `suspend fun isEmpty(): Boolean`, `suspend fun seed(keys: List<String>)`.
  - `AppDatabase.create(context, inMemory: Boolean = false): AppDatabase`; table `tiles` with unique index on `componentKey`.

- [ ] **Step 1: Write failing tests:** `pin_thenEmits`, `pin_sameKeyTwice_keepsOne`, `unpin_removes`, `move_persistsPositions`, `resize_persists`, `corruptRowOnDisk_emitsValidLayout` (insert entity with `col=9` via DAO, assert `tiles.first()` passes `TileLayout.isValid`).
- [ ] **Step 2: Run** `--tests "*TileRepositoryTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement entity/DAO/DB and repository; every mutation reads the current list, applies the `TileLayout` function, and writes back the whole list in one transaction.
- [ ] **Step 4: Run** → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add Room tile persistence`

### Task 4: AppRepository and default tiles

**Files:**
- Create: `data/AppInfo.kt`, `data/AppRepository.kt`, `data/DefaultTiles.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/data/AppGroupingTest.kt`, `.../DefaultTilesTest.kt`

**Interfaces:**
- Produces:
  - `data class AppInfo(val key: String, val label: String, val userId: Int, val component: ComponentName, val user: UserHandle)`
  - `class AppRepository(context: Context)` with `val apps: Flow<List<AppInfo>>` (re-queries `LauncherApps.getActivityList(null, user)` for every profile on each `LauncherApps.Callback` event; sorted case-insensitively by label), `fun launch(app: AppInfo, sourceBounds: Rect?)`, `fun loadIcon(app: AppInfo, density: Int): Drawable`.
  - Top-level pure functions: `fun groupByLetter(apps: List<AppInfo>): Map<Char, List<AppInfo>>` (first char uppercased; non-letter or empty label → `'#'`; `'#'` sorts first) and `fun defaultTileKeys(apps: List<AppInfo>, resolveDefaults: (category: String) -> String?): List<String>` choosing phone, messaging, browser, camera, gallery handlers that are present.
  - `fun makeKey(component: ComponentName, userId: Int): String` = `"${pkg}/${cls}#${userId}"`.

- [ ] **Step 1: Write failing tests:** `groupByLetter` (empty label, `"1Password"`, `"émoji"`, `"Zed"` → groups `#`, `E`/`É` as chosen by uppercase(), `Z`); `makeKey` format; `defaultTileKeys` returns only keys present in the installed list and skips missing categories.
- [ ] **Step 2: Run** the tests → Expected: FAIL.
- [ ] **Step 3:** Implement. Work-profile apps get a distinct `userId` in `key`. `launch` wraps `LauncherApps.startMainActivity` and rethrows nothing: it returns `Boolean` (false on `ActivityNotFoundException`/`SecurityException`).
- [ ] **Step 4: Run** → Expected: PASS. Manually: `./gradlew :app:assembleDebug` succeeds.
- [ ] **Step 5: Commit** `feat: add AppRepository, grouping and default tiles`

### Task 5: ViewModels

**Files:**
- Create: `ui/StartViewModel.kt`, `ui/AppListViewModel.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/ui/StartViewModelTest.kt`

**Interfaces:**
- Consumes: `TileRepository` (Task 3), `AppRepository`/`AppInfo` (Task 4). For testability both ViewModels take interfaces `TileSource`/`AppSource` that the repositories implement; tests use fakes.
- Produces:
  - `data class TileUi(val tile: Tile, val app: AppInfo)`
  - `class StartViewModel` — `val state: StateFlow<List<TileUi>>` (tiles joined with installed apps; tiles whose app is missing are removed via `removeKeys` then `compact`), `fun pin(key: String)`, `unpin(id: Long)`, `move(id: Long, col: Int, row: Int)`, `resize(id: Long, size: TileSize)`, `launch(app: AppInfo, bounds: Rect?)`, `onEditFinished()` (calls `compact`); seeds default tiles when the repository is empty.
  - `class AppListViewModel` — `val groups: StateFlow<Map<Char, List<AppInfo>>>`, `val pinnedKeys: StateFlow<Set<String>>`, `fun pin(key: String)`, `fun uninstall(app: AppInfo)` (fires `Intent.ACTION_DELETE`).

- [ ] **Step 1: Write failing tests** (coroutines-test): `uninstalledApp_removesTile` (app disappears from fake `AppSource` → state drops the tile and positions compact); `emptyRepo_seedsDefaults`; `launchFailure_removesTile` (fake `launch` returns false → tile removed); `pin_alreadyPinned_noDuplicate`.
- [ ] **Step 2: Run** `--tests "*StartViewModelTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement with `viewModelScope`, `combine(tiles, apps)`, `stateIn(Eagerly)`.
- [ ] **Step 4: Run** → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add Start and AppList view models`

### Task 6: MotionSpec and tile modifiers

**Files:**
- Create: `ui/motion/MotionSpec.kt`, `ui/motion/TileModifiers.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/ui/motion/TiltMathTest.kt`

**Interfaces:**
- Produces:
  - `object MotionSpec` with named constants: `TiltMaxDegrees = 8f`, `TiltPressedScale = 0.945f`, `TiltReturnSpring`, `TurnstileDurationMs = 350`, `TurnstileStaggerMs = 25`, `TurnstileAngle = 90f`, `FlyInDurationMs = 400`, `ParallaxTileFactor = 1.0f`, `ParallaxBackgroundFactor = 0.3f`, `EditScale = 0.9f`, `EditLiftScale = 1.05f`, `EditDimAlpha = 0.6f`, `ReflowSpring`. (Values are starting points; tune by eye.)
  - `fun tiltFor(touch: Offset, size: Size): Pair<Float, Float>` pure function returning `(rotationX, rotationY)` in degrees, `(0,0)` at center, magnitude ≤ `TiltMaxDegrees`, sign such that the pressed side moves away from the viewer.
  - `fun Modifier.tilt(onTap: () -> Unit, onLongPress: () -> Unit): Modifier` — press→tilt via `graphicsLayer`; cancels on drag-out; returns tap only if not cancelled.

- [ ] **Step 1: Write failing tests** `tiltFor`: center → `(0,0)`; top-left corner → both nonzero with `abs <= TiltMaxDegrees`; right-edge midpoint → `rotationX == 0`, `rotationY != 0`; touch outside bounds is clamped.
- [ ] **Step 2: Run** `--tests "*TiltMathTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement `MotionSpec`, `tiltFor`, and the modifier (`detectTapGestures` with `onPress`, `Animatable` for rotation/scale, `cameraDistance = 12f * density`).
- [ ] **Step 4: Run** → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add MotionSpec and tilt modifier`

### Task 7: Home pager and App list

**Files:**
- Create: `ui/home/HomeScreen.kt`, `ui/applist/{AppListScreen,JumpList,AppMenu}.kt`; modify `HomeActivity.kt`
- Test: `app/src/androidTest/java/com/mymetroui/launcher/ui/AppListScreenTest.kt`

**Interfaces:**
- Consumes: `StartViewModel`, `AppListViewModel` (Task 5), `MyMetroUITheme`, `MotionSpec`.
- Produces:
  - `@Composable fun HomeScreen(start: StartViewModel, appList: AppListViewModel)` — `HorizontalPager(pageCount = 2)`; bottom-right circular arrow button on page 0 scrolls to page 1; `BackHandler` on page 1 scrolls to page 0.
  - `@Composable fun AppListScreen(groups: Map<Char, List<AppInfo>>, onLaunch: (AppInfo) -> Unit, onPin: (AppInfo) -> Unit, onUninstall: (AppInfo) -> Unit)`; letter headers; tap header → `JumpList` (4-column letter grid, letters without apps at alpha 0.3 and non-clickable, tap scrolls the `LazyColumn`); long-press app → `AppMenu` with "Pin to Start" and "Uninstall".
  - `HomeActivity.onNewIntent` with `ACTION_MAIN`+`CATEGORY_HOME` scrolls the pager back to Start (via a `SharedFlow<Unit>` exposed by the activity's ViewModel store).

- [ ] **Step 1: Write failing Compose tests:** `groupsAreShownUnderHeaders` (`#`, `A`, `B`); `tapHeader_showsJumpListWithDisabledLetters`; `tapLetter_scrollsToGroup`; `longPressApp_showsPinAndUninstall`; `appLabelWithLargeFontScale_ellipsizes` (set `LocalDensity` fontScale 2.0, no crash).
- [ ] **Step 2: Run** `./gradlew :app:connectedDebugAndroidTest --tests "*AppListScreenTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement screens; labels `maxLines = 1`, `overflow = Ellipsis`; wire into `HomeActivity` via `by viewModels` with a simple factory.
- [ ] **Step 4: Run** → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add home pager and app list with jump list`

### Task 8: Start grid and tiles

**Files:**
- Create: `ui/start/{StartGrid,TileView}.kt`
- Test: `app/src/androidTest/java/com/mymetroui/launcher/ui/StartGridTest.kt`

**Interfaces:**
- Consumes: `TileUi` (Task 5), `Modifier.tilt` (Task 6), `Tile`/`TileSize`.
- Produces:
  - `@Composable fun StartGrid(tiles: List<TileUi>, editing: Boolean, onTileTap: (TileUi, Rect) -> Unit, onTileLongPress: (TileUi) -> Unit, loadIcon: (AppInfo) -> Drawable, modifier: Modifier = Modifier)` — tile cell size = `(width - 5 * gap) / 4` with `gap = 4.dp`; tiles positioned absolutely from `(col,row)`; vertical scroll with fling; content height = `max(row + h)` rows.
  - `@Composable fun TileView(ui: TileUi, icon: Drawable, modifier: Modifier)` — accent-filled rectangle, centered icon, label bottom-left (hidden for SMALL), `maxLines = 1`.

- [ ] **Step 1: Write failing tests:** `tilesRenderedAtExpectedPositions` (assert bounds of a WIDE at (0,0) and a MEDIUM at (0,2) via `onNodeWithTag("tile-<id>")`); `smallTilesHideLabel`; `tapTile_callsCallbackWithBounds`; `longPressTile_callsLongPress`; `contentScrollsWhenTilesExceedScreen`.
- [ ] **Step 2: Run** `--tests "*StartGridTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement. Render tiles in a `Box` inside `verticalScroll`; position with `Modifier.offset`.
- [ ] **Step 4: Run** → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add Start grid and tile rendering`

### Task 9: Edit mode (drag, resize, unpin)

**Files:**
- Create: `ui/start/EditOverlay.kt`; modify `StartGrid.kt`, `HomeScreen.kt`
- Test: `app/src/androidTest/java/com/mymetroui/launcher/ui/EditModeTest.kt`

**Interfaces:**
- Consumes: `StartViewModel.move/resize/unpin/onEditFinished` (Task 5), `MotionSpec.Edit*`/`ReflowSpring` (Task 6), `StartGrid` (Task 8).
- Produces: `StartGrid` gains params `onMove: (Long, Int, Int) -> Unit`, `onResize: (Long) -> Unit` (cycles S→M→W→S), `onUnpin: (Long) -> Unit`, `onExitEdit: () -> Unit`. In edit mode: all tiles scale to `EditScale`, dragged tile `EditLiftScale` and follows finger, non-selected tiles at `EditDimAlpha`; selected tile shows unpin button (top-right) and resize button (bottom-right) with tags `unpin-<id>` / `resize-<id>`; drop converts pixel offset to `(col,row)` by rounding to nearest cell then calls `onMove`; tapping empty space or back calls `onExitEdit`; position changes animate with `ReflowSpring`.

- [ ] **Step 1: Write failing tests:** `longPress_entersEditAndShowsButtons`; `resizeButton_cyclesSmallMediumWide` (fake callback records `SMALL→MEDIUM→WIDE→SMALL`); `unpinButton_callsUnpin`; `dragTile_callsMoveWithRoundedCell` (drag a tile by 1.2 cells right, 0.9 down → `onMove(id, col+1, row+1)`); `tapEmpty_exitsEdit`; `tileDisappearsWhileEditing_editStaysValid` (remove the selected tile from the list → no crash, buttons gone).
- [ ] **Step 2: Run** `--tests "*EditModeTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement with `detectDragGesturesAfterLongPress`; `onExitEdit` triggers `onEditFinished()` (`compact`).
- [ ] **Step 4: Run** → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add Start edit mode`

### Task 10: Turnstile, parallax, fly-in

**Files:**
- Create: `ui/motion/Turnstile.kt`; modify `StartGrid.kt`, `HomeScreen.kt`, `HomeActivity.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/ui/motion/TurnstileTimingTest.kt`

**Interfaces:**
- Consumes: `MotionSpec.Turnstile*`/`FlyIn*`/`Parallax*` (Task 6), `StartGrid.onTileTap` (Task 8).
- Produces:
  - `fun turnstileDelayMs(tile: Tile): Int` = `(tile.row * MotionSpec.TurnstileStaggerMs * 2) + (tile.col * MotionSpec.TurnstileStaggerMs)`.
  - `class TurnstileState { suspend fun playOut(); suspend fun playIn(); val progress: Float }` driving each tile's `rotationY` around the left edge (`TransformOrigin(0f, 0.5f)`) from 0 to `-TurnstileAngle`, offset per tile by `turnstileDelayMs`.
  - Behavior: tapping a tile runs `playOut()` then `StartViewModel.launch`; `HomeActivity.onResume`/home intent runs `playIn()` and fly-in; pager scroll offset drives parallax (tiles at `ParallaxTileFactor`, wallpaper/backdrop at `ParallaxBackgroundFactor`).

- [ ] **Step 1: Write failing tests:** `turnstileDelay_growsWithRowAndCol` (`(0,0)→0`, `(2,0)→2*stagger`, `(0,2)→4*stagger`); `turnstileDelay_isMonotonicInRowThenCol` over a sample grid.
- [ ] **Step 2: Run** `--tests "*TurnstileTimingTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement delay function, `TurnstileState`, and wiring; if launch returns false, run `playIn()` immediately so tiles never stay rotated.
- [ ] **Step 4: Run** unit test → Expected: PASS. Manual: on an emulator, tap a tile → tiles swing out, app opens; press Home → tiles swing in.
- [ ] **Step 5: Commit** `feat: add turnstile, parallax and fly-in motion`

### Task 11: Default-launcher prompt and failure handling

**Files:**
- Create: `ui/home/DefaultLauncherPrompt.kt`; modify `HomeActivity.kt`, `data/db/AppDatabase.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/data/DatabaseFallbackTest.kt`, `app/src/androidTest/java/com/mymetroui/launcher/ui/DefaultLauncherPromptTest.kt`

**Interfaces:**
- Consumes: `AppDatabase.create` (Task 3), `HomeScreen` (Task 7).
- Produces: `fun isDefaultLauncher(context: Context): Boolean` (resolves `CATEGORY_HOME` and compares package); `@Composable fun DefaultLauncherPrompt(onChoose: () -> Unit)` shown only when `isDefaultLauncher` is false; `onChoose` uses `RoleManager.createRequestRoleIntent(ROLE_HOME)` on API ≥ 29, otherwise `Settings.ACTION_HOME_SETTINGS`. `AppDatabase.create` catches `SQLiteException` on open/migration failure, deletes the DB file, and recreates it empty (StartViewModel then reseeds defaults).

- [ ] **Step 1: Write failing tests:** `corruptDbFile_recreatesEmptyDatabase` (write garbage bytes to the DB file, `create()` returns a usable DB, `tiles` is empty); `prompt_shownWhenNotDefault` and `prompt_hiddenWhenDefault` (inject `isDefault: Boolean`).
- [ ] **Step 2: Run** both → Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4: Run** both → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add default launcher prompt and DB recovery`

### Task 12: End-to-end Start flow and release hygiene

**Files:**
- Create: `app/src/androidTest/java/com/mymetroui/launcher/ui/PinUnpinFlowTest.kt`, `app/proguard-rules.pro` (Room keep rules if needed), `README.md`
- Modify: `app/build.gradle.kts` (release `minifyEnabled = true`)

**Interfaces:**
- Consumes: everything above.

- [ ] **Step 1: Write failing test** `pinFromAppListAppearsOnStart_thenResizeAndUnpin`: launch `HomeActivity` with a fake injected `AppSource`, long-press an app → "Pin to Start" → swipe to Start → tile `tile-*` exists → long-press → tap resize → bounds width grows → tap unpin → tile gone.
- [ ] **Step 2: Run** `--tests "*PinUnpinFlowTest*"` → Expected: FAIL until all wiring is complete; fix wiring gaps found.
- [ ] **Step 3:** Fix gaps; add README with build, install and "set as default launcher" instructions; confirm `./gradlew :app:assembleRelease` builds with minification.
- [ ] **Step 4: Run** the full suite: `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` → Expected: all PASS.
- [ ] **Step 5: Commit** `feat: end-to-end flow test and release build`
