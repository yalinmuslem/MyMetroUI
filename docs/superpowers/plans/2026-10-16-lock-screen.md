# Welcome Screen (Lock Screen) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a Metro-style welcome screen (clock, date, next event, notification summary) inside the launcher after the screen turns off and on, dismissed by swiping up.

**Architecture:** `WelcomeGate` (pure, app singleton) turns screen-off signals into a one-shot "show" decision read in `HomeActivity.onResume`; `HomeEvents` holds the visible flag; `WelcomeScreen` is a stateless overlay drawn last in `HomeScreen`. Clock and calendar faces come from the existing `LiveTileSource`; the summary comes from `ActionCenterController.state`. `WelcomePrefs` stores the on/off switch.

**Tech Stack:** Kotlin, Compose, SharedPreferences, Robolectric 4.13 (SDK 34).

**Spec:** `docs/superpowers/specs/2026-10-16-lock-screen-design.md`

## Global Constraints

- Package `com.mymetroui.launcher`; minSdk 26; compile/targetSdk 35.
- No new permissions. No "Windows"/"Phone"/Microsoft names or assets.
- Not a security feature; never claim device-level verification.
- Defaults: switch on; dismiss threshold = `PanelDrag.settle` (25% or fling > 1200 px/s); duration `MotionSpec.WelcomeDurationMs = 300`.
- Commit trailer: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Work on branch `feat/welcome-screen`.

## Review Focus

- Home button / cold start / rotation must not show the screen (gate tests + `consume` clears flag).
- Screen off while another app is in front must not flag the launcher.
- Switch off while a flag is pending: nothing shows, flag cleared (no stale show later).
- Locked device (`deviceLocked`): no notification titles on the welcome screen.
- Notification access not granted / calendar permission missing: section hidden, clock still shown.
- Welcome visible: Action Center edge strip absent, Back dismisses.

## Rulings (made while planning)

- Calendar line shows any `LiveFace.Text` from the calendar source (including "No events today"); `NeedsPermission`/null hide it. Spec wording "hanya bila ada acara" is relaxed accordingly.

---

### Task 1: `WelcomePrefs` and `WelcomeGate`

**Files:**
- Create: `app/src/main/java/com/mymetroui/launcher/data/WelcomePrefs.kt`
- Create: `app/src/main/java/com/mymetroui/launcher/ui/welcome/WelcomeGate.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/data/WelcomePrefsTest.kt`, `app/src/test/java/com/mymetroui/launcher/ui/welcome/WelcomeGateTest.kt`

**Interfaces:**
- Produces: `class WelcomePrefs(prefs: SharedPreferences) { val enabled: StateFlow<Boolean>; fun setEnabled(on: Boolean) }` (default true, key `"enabled"`).
- Produces: `class WelcomeGate(private val enabled: () -> Boolean)` with `fun onScreenOff()`, `fun onPause(interactive: Boolean)`, `fun onResume(): Boolean`.

- [ ] **Step 1: Write failing tests.** `WelcomePrefsTest` (Robolectric, prefs name `"welcome-test"`, cleared in `@Before`): `defaultsToEnabled`; `setEnabled(false)` updates `enabled.value` and a second `WelcomePrefs` on the same prefs reads `false`. `WelcomeGateTest` (plain JUnit, `var on = true`, `gate = WelcomeGate { on }`):
  - `screenOffWhileResumed_showsOnNextResume`: `onResume()` (returns false) → `onScreenOff()` → `onPause(false)` → `onResume()` is true.
  - `pauseWhileNotInteractive_showsOnNextResume`: `onResume()`; `onPause(false)`; `onResume()` true.
  - `pauseWhileInteractive_doesNotShow` (Home/app launch): `onResume(); onPause(true); onResume()` false.
  - `coldStart_doesNotShow`: first `onResume()` false.
  - `screenOffWhilePaused_doesNotFlag`: `onResume(); onPause(true); onScreenOff(); onResume()` false.
  - `flagIsConsumed`: after a true `onResume()`, `onPause(true); onResume()` is false.
  - `switchOff_holdsAndClears`: flag set, `on = false`, `onResume()` false; then `on = true; onPause(true); onResume()` false.
- [ ] **Step 2: Run** `./gradlew.bat :app:testDebugUnitTest --tests "*WelcomePrefsTest" --tests "*WelcomeGateTest"`. Expected: compile FAIL (classes undefined).
- [ ] **Step 3: Implement** both classes. Gate keeps `resumed` and `pending` booleans: `onScreenOff` sets `pending` only if `resumed`; `onPause` sets `resumed=false` and `pending` if `!interactive`; `onResume` sets `resumed=true`, returns `pending && enabled()`, always clears `pending`. Prefs mirrors `ThemeRepository` (MutableStateFlow + `apply()`).
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** `feat: add welcome prefs and screen-off gate`.

### Task 2: `welcomeSummary`

**Files:**
- Create: `app/src/main/java/com/mymetroui/launcher/ui/welcome/WelcomeSummary.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/ui/welcome/WelcomeSummaryTest.kt`

**Interfaces:**
- Consumes: `NotifItem` (`data/notif/NotifItem.kt`).
- Produces: `data class WelcomeSummary(val count: Int, val titles: List<String>)` and `fun welcomeSummary(items: List<NotifItem>, connected: Boolean, locked: Boolean): WelcomeSummary?`.

- [ ] **Step 1: Write failing tests** (build items with `NotifItem(key,"p",0,title,"",whenMs,true,1,null,false)`): `notConnected_isNull`; `connectedEmpty_countZeroNoTitles`; `countsAll_titlesAreThreeNewest` (5 items → count 5, titles are the 3 largest `whenMs`, newest first); `blankTitlesSkipped`; `locked_countOnly` (titles empty, count kept).
- [ ] **Step 2: Run** `--tests "*WelcomeSummaryTest"`. Expected: compile FAIL.
- [ ] **Step 3: Implement** as above (sort by `whenMs` descending, filter non-blank, take 3; locked → empty titles).
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add welcome notification summary`.

### Task 3: `WelcomeScreen`

**Files:**
- Create: `app/src/main/java/com/mymetroui/launcher/ui/welcome/WelcomeScreen.kt`
- Modify: `app/src/main/java/com/mymetroui/launcher/ui/motion/MotionSpec.kt` (add `const val WelcomeDurationMs = 300`), `app/src/main/res/values/strings.xml` (add `<plurals name="welcome_notifications">` "%d notification"/"%d notifications", and `<string name="welcome_setting">Show welcome screen after screen off</string>`)
- Test: `app/src/test/java/com/mymetroui/launcher/ui/WelcomeScreenTest.kt`

**Interfaces:**
- Consumes: `LiveFace` (`LiveFace.Text(title, lines)`), `WelcomeSummary`, `PanelDrag.settle(fraction, velocityPxPerSec, wasOpen)`.
- Produces: `@Composable fun WelcomeScreen(clock: LiveFace.Text?, calendar: LiveFace?, summary: WelcomeSummary?, onDismiss: () -> Unit, modifier: Modifier = Modifier)`. Tags: `welcome-screen` (root), `welcome-clock`, `welcome-date`, `welcome-calendar`, `welcome-notifs` (count text), `welcome-title-<index>`.

- [ ] **Step 1: Write failing tests** (`createComposeRule`, `MyMetroUITheme`, Robolectric): `showsClockAndDate` (title "09:41", lines ["Fri, Oct 16"]); `calendarRow_onlyForTextFace` (Text face shown; `LiveFace.NeedsPermission` and null → `welcome-calendar` absent); `notificationSection_hiddenWhenSummaryNull`; `showsCountAndTitles` (count 5, 3 titles → plural text "5 notifications", titles displayed); `swipeUp_dismisses` (`performTouchInput { swipeUp() }`, `waitForIdle`, counter == 1); `smallSlowDrag_doesNotDismiss` (`swipe` about 10% of height over 1000 ms, then idle, counter == 0 and `welcome-screen` still displayed).
- [ ] **Step 2: Run** `--tests "*WelcomeScreenTest"`. Expected: compile FAIL.
- [ ] **Step 3: Implement** `WelcomeScreen`: full-size `Box` with the Start backdrop look (`MaterialTheme.colorScheme.background` blended with `primary` at 25% alpha, vertical gradient); content column with large clock, date, calendar lines, notification summary; vertical `draggable` updating an `offset` (px, ≤ 0) shown via `graphicsLayer { translationY }`; on drag stop call `PanelDrag.settle(-offset/height, -velocity, false)`: true → animate to `-height` over `MotionSpec.WelcomeDurationMs` then `onDismiss()`; false → animate back to 0. Handles the screen-height measurement itself (`onSizeChanged`).
- [ ] **Step 4: Run.** Expected: PASS.
- [ ] **Step 5: Commit** `feat: add welcome screen composable`.

### Task 4: Integrate (HomeEvents, HomeScreen, Settings, HomeActivity, LauncherApp)

**Files:**
- Modify: `HomeActivity.kt` (`HomeEvents` gets `val welcomeVisible: StateFlow<Boolean>`, `showWelcome()`, `dismissWelcome()`; `onPause`/`onResume` call the gate; collect `welcomePrefs.enabled`), `LauncherApp.kt` (`welcomePrefs`, `welcomeGate`, dynamic `ACTION_SCREEN_OFF` receiver registered in `onCreate` via `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`), `ui/home/HomeScreen.kt`, `ui/settings/SettingsScreen.kt` (all under `app/src/main/java/com/mymetroui/launcher/`)
- Test: `app/src/test/java/com/mymetroui/launcher/ui/WelcomeHomeTest.kt`; extend `SettingsScreenTest.kt`

**Interfaces:**
- Consumes: Tasks 1–3 outputs; `StartViewModel.live.face(kind)` (clock/calendar faces); `controller.state` (`items`, `connected`, `deviceLocked`).
- Produces: new defaulted params on `HomeScreen`: `welcomeVisible: Boolean = false`, `onWelcomeDismiss: () -> Unit = {}`, `welcomeEnabled: Boolean = true`, `onWelcomeEnabled: (Boolean) -> Unit = {}`; on `SettingsScreen`: `welcomeEnabled: Boolean = true`, `onWelcomeEnabled: (Boolean) -> Unit = {}` rendering a labelled `Switch` tagged `welcome-switch` (label `R.string.welcome_setting`).

- [ ] **Step 1: Write failing tests.** `WelcomeHomeTest` (mirror `ActionCenterHostTest.showHome` setup with `FakeTileSource`/`FakeAppSource`/`testController`): `visible_showsWelcomeScreen`; `notVisible_noWelcomeScreen`; `visible_actionCenterEdgeAbsent` (`action-center-edge` does not exist); `swipeUp_callsOnWelcomeDismiss`; `back_callsOnWelcomeDismiss` (`Espresso`-free: use `rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }`); `lockedDevice_hidesTitles` (controller with `locked = true` and one connected notification → count shown, title absent). `SettingsScreenTest`: `welcomeSwitch_reflectsAndTogglesValue` (click calls `onWelcomeEnabled(false)` when on).
- [ ] **Step 2: Run** `--tests "*WelcomeHomeTest" --tests "*SettingsScreenTest"`. Expected: compile FAIL.
- [ ] **Step 3: Implement.** `HomeScreen`: collect clock/calendar faces with `start.live.face(LiveKind.CLOCK/CALENDAR).collectAsState(null)`, `controller.state`, compute `welcomeSummary`; draw `WelcomeScreen` last in the root `BoxWithConstraints` when `welcomeVisible`; `canOpen` additionally requires `!welcomeVisible`; `BackHandler(enabled = welcomeVisible)` registered after the panel handler; `LaunchedEffect(welcomeVisible)` when it becomes true: leave edit mode, close settings and panel, `pager.scrollToPage(0)`, `controller.refresh()`; on dismiss also `turnstile.flyIn()`. Pass the switch params to `SettingsScreen`. `HomeActivity`: `onPause` → `gate.onPause(isInteractive)` where `isInteractive` is `runCatching { getSystemService(PowerManager::class.java).isInteractive }.getOrDefault(true)`; `onResume` → `if (gate.onResume()) events.showWelcome()`; wire the new `HomeScreen` params from `events.welcomeVisible` and `app.welcomePrefs`. `LauncherApp`: receiver calls `welcomeGate.onScreenOff()`.
- [ ] **Step 4: Run** the new tests, then the whole suite `./gradlew.bat :app:testDebugUnitTest`. Expected: all PASS (176 existing + new), 0 failures; then `./gradlew.bat :app:assembleDebug :app:assembleRelease` BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: integrate welcome screen, gate and settings switch`.

### Task 5: README

**Files:** Modify `README.md`.

- [ ] **Step 1:** Add a "Welcome screen" feature note (in-launcher, not secure, switch in Settings, no extra permissions) and update the "Not yet done" line to "Hubs, search, widgets." Note as untested on device: screen-off ordering on OEMs and behaviour after system keyguard unlock.
- [ ] **Step 2: Commit** `docs: README documents the welcome screen`.
