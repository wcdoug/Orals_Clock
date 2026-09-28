# Wenonah's Orals Timer — Android App Plan

## Overview

Build a native Android countdown timer app optimized for oral proposal defenses (RFP presentations to government customers). The app runs on a cheap Android tablet and must:

- Count **down** from a user-selected duration to 00:00
- Have a **giant Play/Pause button** as the primary control
- Provide **preset time buttons** (30, 40, 60, 90, 120 minutes)
- Provide a **custom time entry** (user types in any number of minutes)
- Have a **Reset button**
- Show **color-coded warnings** (yellow at 5 min remaining, red at 1 min remaining)
- **Flash the screen** when the timer hits 00:00 (no sound)
- **Prevent the screen from sleeping or turning off** while the timer is running (wake lock)

**App Name:** Wenonah's Orals Timer
**Package:** `com.wenonah.oralstimer`
**Tech Stack:** Native Android (Kotlin), single Activity, no third-party dependencies beyond Android SDK.

**Target:** Android tablet (landscape-friendly layout), API 21+.

**Output:** A complete Android Studio project at the workspace root.

---

## Sub-Tasks

---

### Sub-Task 1 — Scaffold the Android Project Structure

- **Status:** `[ ] pending`
- **Intent:** Create the full Android project directory structure and all required configuration files so the project can be opened in Android Studio and built immediately.
- **Expected Outcomes:**
  - Standard Android project layout exists (`app/src/main/...`)
  - `build.gradle` (project + app level) configured for Kotlin, API 21+ min SDK
  - `AndroidManifest.xml` includes `WAKE_LOCK` permission
  - `settings.gradle` and `gradle.properties` present
- **Todo List:**
  1. Create `settings.gradle`
  2. Create root `build.gradle`
  3. Create `app/build.gradle` (Kotlin, minSdk 21, targetSdk 34)
  4. Create `gradle.properties`
  5. Create `AndroidManifest.xml` with `WAKE_LOCK` permission and single Activity declaration
  6. Create `app/src/main/res/values/strings.xml`, `colors.xml`, `themes.xml`
- **Relevant Context:**
  - Workspace root: `/Volumes/My Shared Files/Shared_With_UTM_Mac/Timer`
  - No existing files except `session-export-plan.md` and `orals-timer-plan.md`

---

### Sub-Task 2 — Build the Main Layout (XML)

- **Status:** `[ ] pending`
- **Intent:** Create the UI layout for the single screen. Optimized for tablet in landscape, with large readable text and touch-friendly controls.
- **Expected Outcomes:**
  - `activity_main.xml` exists and contains:
    - A large centered countdown display (MM:SS format, full-screen dominant)
    - A row of preset buttons: 30, 40, 60, 90, 120 min
    - A custom time entry field (`EditText` for minutes) with a "Set" button
    - A giant Play/Pause toggle button
    - A Reset button
  - Layout works in both landscape and portrait on a tablet
- **Todo List:**
  1. Create `app/src/main/res/layout/activity_main.xml`
  2. Use `ConstraintLayout` as root
  3. Add `TextView` for countdown display (very large font, centered)
  4. Add horizontal `LinearLayout` with 5 preset `Button` elements (30, 40, 60, 90, 120)
  5. Add `EditText` (number input, minutes) and a "Set" button for custom duration
  6. Add giant Play/Pause `Button`
  7. Add Reset `Button`
- **Relevant Context:**
  - Countdown display should dominate the screen — font size ~120sp or larger
  - Preset buttons should be clearly labeled in minutes
  - Custom entry: `EditText` with `inputType="number"`, labeled "min", next to a "Set" button
  - Play/Pause and Reset buttons should be large enough for easy tapping under pressure

---

### Sub-Task 3 — Implement Timer Logic in MainActivity

- **Status:** `[ ] pending`
- **Intent:** Implement all timer behavior in `MainActivity.kt` — countdown logic, state machine (idle/running/paused/expired), wake lock, color transitions, and screen flash on expiry.
- **Expected Outcomes:**
  - Countdown ticks every second using `CountDownTimer`
  - Display updates every second in MM:SS format
  - Play/Pause button toggles correctly between states
  - Reset returns to the selected preset duration and idle state
  - Selecting a preset while running resets to that preset and stops
  - Custom "Set" button parses the `EditText` value (minutes), validates it (1–999), loads as new duration
  - Background/text color changes:
    - Default (>5 min): neutral/dark
    - Warning (≤5 min, >1 min): yellow background
    - Critical (≤1 min): red background
  - At 00:00: screen flashes (alternating colors) using a `Handler` post-delay loop
  - `WakeLock` acquired when timer starts, released when paused, reset, or finished
- **Todo List:**
  1. Create `app/src/main/java/com/wenonah/oralstimer/MainActivity.kt`
  2. Declare fields: `CountDownTimer`, `WakeLock`, selected duration, current state enum
  3. Wire up all button click listeners (presets, custom Set, play/pause, reset)
  4. Implement `startTimer(durationMs: Long)` — creates and starts `CountDownTimer`
  5. Implement `pauseTimer()` — cancels timer, records remaining time, releases wake lock
  6. Implement `resetTimer()` — cancels timer, resets display to selected preset, releases wake lock
  7. Implement `onTick()` — update display, apply color logic
  8. Implement `onFinish()` — trigger screen flash sequence
  9. Implement `flashScreen()` — `Handler`-based alternating color flash (e.g. 10 flashes)
  10. Acquire `SCREEN_BRIGHT_WAKE_LOCK` on start, release on pause/reset/finish
  11. Override `onDestroy()` to release wake lock safely
- **Relevant Context:**
  - Use `android.os.CountDownTimer` (built-in, no dependencies)
  - Use `android.os.PowerManager.WakeLock` with `SCREEN_BRIGHT_WAKE_LOCK` flag
  - Color values defined in `colors.xml` (Sub-Task 1)
  - State transitions: IDLE → RUNNING → PAUSED → RUNNING → EXPIRED → IDLE (via reset)

---

### Sub-Task 4 — Polish: Keep Screen On Flag + Final Wiring

- **Status:** `[ ] pending`
- **Intent:** Add the `FLAG_KEEP_SCREEN_ON` window flag as a belt-and-suspenders complement to the wake lock, verify all wiring is complete, and ensure the app builds cleanly.
- **Expected Outcomes:**
  - `window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)` set in `onCreate`
  - All resource references resolve (no missing IDs)
  - `AndroidManifest.xml` declares the Activity correctly
  - Project can be opened in Android Studio and built with `./gradlew assembleDebug`
- **Todo List:**
  1. Add `FLAG_KEEP_SCREEN_ON` in `MainActivity.onCreate()`
  2. Verify all view IDs in XML match references in Kotlin
  3. Verify `WAKE_LOCK` permission in manifest
  4. Verify `colors.xml` has all referenced color names
  5. Review and fix any compilation issues in the Kotlin file
- **Relevant Context:**
  - `FLAG_KEEP_SCREEN_ON` works at the window level independently of the wake lock
  - Together they provide robust screen-on behavior across different Android versions and OEMs

---

## State Machine

```
IDLE ──[press Play]──▶ RUNNING
RUNNING ──[press Pause]──▶ PAUSED
RUNNING ──[reaches 00:00]──▶ EXPIRED
RUNNING ──[press preset]──▶ IDLE (new preset loaded)
PAUSED ──[press Play]──▶ RUNNING (resumes remaining time)
PAUSED ──[press Reset]──▶ IDLE
EXPIRED ──[press Reset]──▶ IDLE
```

## Color Rules

| Remaining Time | Background Color |
|---|---|
| > 5 minutes | Dark/neutral (default theme) |
| ≤ 5 min, > 1 min | Yellow (`#FFC107`) |
| ≤ 1 minute | Red (`#F44336`) |
| 00:00 (expired) | Flashing (alternating red/white, 10 cycles) |
