# Theme and Accent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The user picks an accent color from a palette and dark/light mode from a Settings screen opened from the app list; the choice persists and themes the whole launcher.

**Architecture:** `ThemeRepository` (SharedPreferences, `StateFlow<ThemeSettings>`) is a process singleton in `LauncherApp`. `MyMetroUITheme(settings)` maps settings to a Material3 `ColorScheme` and wraps content in a `Surface`; hard-coded colors move to the scheme. `SettingsScreen` is an overlay in `HomeScreen`.

**Tech Stack:** unchanged (Kotlin 2.0.20, Compose BOM 2024.09.03, Robolectric 4.13 for JVM Compose tests in `app/src/test`). No new dependencies.

**Spec:** `docs/superpowers/specs/2026-10-16-theme-accent-design.md`

## Global Constraints

- Default accent `Color(0xFF1BA1E2)` (id `cyan`), default mode `DARK`.
- Only accent id and mode are persisted; unknown or corrupt stored values fall back to the default per field and never crash.
- No new dependencies (no DataStore); no Microsoft assets or marks.
- Out of scope: custom colors, wallpaper, transparent tiles, per-tile colors, following system theme.
- Tests run on the JVM (`./gradlew :app:testDebugUnitTest`); no device is available.
- Existing tests call `MyMetroUITheme { }` with no argument: keep a default for `settings`.

## Review Focus

- Prefs file holds a wrong type under a known key (e.g. an Int under `accent`) → defaults, no `ClassCastException`.
- Accent id removed from a future palette → default accent, stored id untouched until the user picks again.
- Back pressed with Settings open → closes Settings first, not the pager or edit mode.
- App list with zero apps → "Settings" row still shown and usable.
- Text on yellow/lime/amber tiles in LIGHT mode stays readable (tile text is fixed white; accepted limitation, listed for manual check).

---

## File Structure

```
data/ThemeSettings.kt        ThemeMode, ThemeSettings
data/ThemeRepository.kt      ThemeSource interface + SharedPreferences implementation
ui/theme/Accents.kt          AccentColor, Accents palette, DefaultAccent
ui/theme/Theme.kt            MyMetroUITheme(settings) (modify)
ui/settings/SettingsScreen.kt
LauncherApp.kt, HomeActivity.kt, ui/home/HomeScreen.kt, ui/applist/AppListScreen.kt (modify)
```

### Task 1: Settings model, palette and repository

**Files:**
- Create: `data/ThemeSettings.kt`, `data/ThemeRepository.kt`, `ui/theme/Accents.kt`
- Modify: `LauncherApp.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/data/ThemeRepositoryTest.kt` (Robolectric)

**Interfaces:**
- Produces:
  - `enum class ThemeMode { DARK, LIGHT }`
  - `data class ThemeSettings(val accentId: String = DefaultAccent.id, val mode: ThemeMode = ThemeMode.DARK)`
  - `data class AccentColor(val id: String, val name: String, val color: Color)`; `val Accents: List<AccentColor>`; `val DefaultAccent: AccentColor`; `fun accentFor(id: String): AccentColor` (unknown id → `DefaultAccent`)
  - `interface ThemeSource { val settings: StateFlow<ThemeSettings>; fun setAccent(id: String); fun setMode(mode: ThemeMode) }`
  - `class ThemeRepository(prefs: SharedPreferences) : ThemeSource`; `LauncherApp.themeRepository` (lazy, prefs name `theme`).
- Palette (id, hex): lime A4C400, green 60A917, emerald 008A00, teal 00ABA9, cyan 1BA1E2 (default), cobalt 0050EF, indigo 6A00FF, violet AA00FF, pink F472D0, magenta D80073, crimson A20025, red E51400, orange FA6800, amber F0A30A, yellow E3C800, brown 825A2C, olive 6D8764, steel 647687, mauve 76608A, taupe 87794E (name = capitalized id).

- [ ] **Step 1: Write failing tests** in `ThemeRepositoryTest`: `defaults_whenEmpty` (cyan, DARK); `setAccentAndMode_persistAcrossInstances` (new `ThemeRepository` on the same prefs reads them); `unknownAccentId_fallsBackToDefaultAccent` (`accentFor("nope") == DefaultAccent`, and a stored `"nope"` yields `settings.value.accentId == "cyan"`); `invalidMode_fallsBackToDark`; `wrongTypeInPrefs_fallsBackToDefaults` (put an Int under `accent`/`mode`); `setAccent_unknownId_isIgnored`; `paletteIdsAreUnique`.
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests "*ThemeRepositoryTest*"` → Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement the types above; read prefs with try/catch around `getString` (catch `ClassCastException`); expose `MutableStateFlow` as `StateFlow`; setters write with `apply()` and update the flow.
- [ ] **Step 4: Run** the same command → Expected: PASS.
- [ ] **Step 5: Commit** `feat: add theme settings, palette and repository`

### Task 2: Theme mapping and color cleanup

**Files:**
- Modify: `ui/theme/Theme.kt`, `ui/applist/JumpList.kt`, `ui/applist/AppMenu.kt`, `ui/home/HomeScreen.kt`, `ui/home/DefaultLauncherPrompt.kt`, `ui/start/EditOverlay.kt`
- Test: `app/src/test/java/com/mymetroui/launcher/ui/ThemeMappingTest.kt`

**Interfaces:**
- Consumes: `ThemeSettings`, `accentFor` (Task 1).
- Produces: `@Composable fun MyMetroUITheme(settings: ThemeSettings = ThemeSettings(), content: @Composable () -> Unit)`. Mapping: DARK → background/surface black, onBackground/onSurface white; LIGHT → white / black; `primary` = accent color; `onPrimary` white. Content is wrapped in `Surface(color = background, contentColor = onBackground)` so `Text` without explicit color uses `onBackground` (today it falls back to black, which is unreadable on the black background).

- [ ] **Step 1: Write failing tests** (compose rule): `dark_background_isBlack_andContentColorIsWhite`; `light_background_isWhite_andContentColorIsBlack` (read `MaterialTheme.colorScheme.background` and `LocalContentColor.current` inside the theme); `primary_isSelectedAccent` (e.g. `accentId = "red"` → `Color(0xFFE51400)`); `unknownAccent_usesDefaultPrimary`.
- [ ] **Step 2: Run** `--tests "*ThemeMappingTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement `MyMetroUITheme`; remove `MetroAccent` (grep for users first). Replace literal colors: overlays use `colorScheme.background.copy(alpha = …)`; menu/prompt panels use `colorScheme.surface` blended with `onSurface` at low alpha (`surfaceVariant`), arrow-button border uses `onBackground`; edit buttons keep black 0.7 scrim with white glyph (they sit on tiles); backdrop gradient is `listOf(background, primary.copy(alpha = 0.25f).compositeOver(background), background)`.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest` → Expected: PASS (all existing tests still pass).
- [ ] **Step 5: Commit** `feat: map theme settings to colors and fix content color`

### Task 3: Settings screen, entry row and wiring

**Files:**
- Create: `ui/settings/SettingsScreen.kt`
- Modify: `ui/applist/AppListScreen.kt`, `ui/home/HomeScreen.kt`, `HomeActivity.kt`, `README.md`
- Test: `app/src/test/java/com/mymetroui/launcher/ui/SettingsScreenTest.kt`; extend `AppListScreenTest.kt`, `PinUnpinFlowTest.kt`

**Interfaces:**
- Consumes: `ThemeSource`, `Accents`, `ThemeMode` (Task 1), `MyMetroUITheme(settings)` (Task 2).
- Produces:
  - `@Composable fun SettingsScreen(settings: ThemeSettings, onAccent: (String) -> Unit, onMode: (ThemeMode) -> Unit, onClose: () -> Unit)`: mode toggle with tags `mode-dark` / `mode-light`; 4-column swatch grid, tag `accent-<id>`, selected swatch has a border and `Modifier.semantics { selected = true }`.
  - `AppListScreen(..., onOpenSettings: () -> Unit = {})`: a last row "Settings" with tag `settings-row`, shown even when `groups` is empty.
  - `HomeScreen(..., theme: ThemeSource)`: `showSettings` state; overlay on top of the pager; `BackHandler(enabled = showSettings)` registered last so it wins over the other handlers.
  - `HomeActivity` collects `theme.settings` and passes it to `MyMetroUITheme`; system bar style follows mode (`enableEdgeToEdge` with `SystemBarStyle.dark/light(Color.TRANSPARENT)`, re-applied when mode changes).
  - String resources: `settings`, `theme_dark`, `theme_light`.

- [ ] **Step 1: Write failing tests:** `SettingsScreenTest`: `tapSwatch_callsOnAccent` (`accent-red` → `"red"`); `toggleMode_callsOnMode`; `selectedSwatch_isMarkedSelected` (only the settings' accent has `selected`); `closeButton_callsOnClose`. `AppListScreenTest`: `settingsRow_shownLast_andOpens` and `settingsRow_shownWhenNoApps`. `PinUnpinFlowTest`-style new test `settingsOverlay_backClosesSettingsFirst` (open via `settings-row`, `Espresso`-free back via `rule.activity`-less `OnBackPressedDispatcher`, assert `accent-red` gone and pager still on page 1) and `changingAccent_updatesTheme` (fake `ThemeSource`; tapping a swatch updates `settings`).
- [ ] **Step 2: Run** `--tests "*SettingsScreenTest*" --tests "*AppListScreenTest*" --tests "*PinUnpinFlowTest*"` → Expected: FAIL.
- [ ] **Step 3:** Implement per Interfaces; add `FakeThemeSource` next to the other fakes in `StartViewModelTest.kt`; update the README "Using it" section with the Settings row.
- [ ] **Step 4: Run** `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` → Expected: all PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat: add theme settings screen`
