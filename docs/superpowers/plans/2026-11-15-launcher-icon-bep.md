# Launcher Icon Panel Alignment & "BEP" Label Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the app's launcher presence by replacing the legacy launcher icon with an adaptive icon matching the Quick Settings panel vector (`ic_tile_sidebar`) in Approach 1 (monochrome clean white style on dark background), and scoping the launcher label strictly to "BEP". Keep Stealth Mode ON by default (`LauncherAlias` disabled by default, `stealthMode = true`), so when the user disables Stealth Mode, the newly restored launcher entry displays "BEP" and the panel icon.

**Architecture:**
- Create an adaptive vector foreground drawable (`ic_launcher_foreground.xml`) using the rescaled vector paths from `ic_tile_sidebar.xml` centered inside a 108dp canvas.
- Update `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` to link to the new foreground and monochrome vector drawables over `@color/bg_main`.
- Add string resource `launcher_app_name` set to `"BEP"` and apply `android:label="@string/launcher_app_name"` directly to `<activity-alias android:name=".LauncherAlias">` in `AndroidManifest.xml`.
- **Preserve Stealth Mode defaults**: Keep `android:enabled="false"` on `.LauncherAlias` in `AndroidManifest.xml` and keep `stealthMode` defaulting to `true` in `PanelPreferences.kt`.

**Tech Stack:** Android XML Vector Drawables, Adaptive Icons (API 26+), AndroidManifest activity-alias.

---

## Global Constraints
- Target device: Android 14/15/17 (minSdk 26).
- Application package: `com.amintum.bestedgingpanel`.
- Approach: Approach 1 (Monochrome clean white icon on dark `#0F0F1A` / `@color/bg_main` background, with Android 13+ themed icon support).
- Naming scope: "BEP" strictly applies to the launcher icon via `LauncherAlias`. Global `<application android:label>` remains untouched so system settings, service notifications, and internal titles remain clean and descriptive.
- **Stealth Mode Preservation**: Stealth Mode MUST stay ON by default. Do not change `KEY_STEALTH_MODE` default or set `android:enabled="true"` on the initial manifest alias.

## Review Focus
1. **Adaptive icon clipping**: The 54dp vector must stay safely within the 66-72dp center safe-zone so no part of the device border or dock is clipped by circular or squircle launcher masks.
2. **Themed icon compatibility**: The monochrome layer must support Android 13+ dynamic Material You tinting.
3. **Stealth Mode preservation**: Verify that on clean install, the launcher icon remains hidden until the user toggles Stealth Mode off in Miscellaneous Settings.
4. **Restoration display**: When Stealth Mode is turned off, the launcher displays "BEP" and the new panel icon.

---

### Task 1: Create Adaptive Vector Foreground & Link Adaptive Icons

**Files:**
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Modify: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Modify: `app/src/main/res/drawable-v26/ic_launcher_monochrome.xml` (ensure correct centering and paths)

- [ ] **Step 1: Create `ic_launcher_foreground.xml`**
  Generate 108dp × 108dp vector with 2.25x scaled `ic_tile_sidebar` path (54dp size, centered at `translateX="27" translateY="27"`), with `android:fillColor="#FFFFFF"` for crisp white rendering on dark background.

- [ ] **Step 2: Update `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`**
  Point `<foreground>` to `@drawable/ic_launcher_foreground` and `<monochrome>` to `@drawable/ic_launcher_monochrome`. Keep `<background android:drawable="@color/bg_main"/>`.

- [ ] **Step 3: Verification of Vector and Adaptive Icon references**
  Ensure drawables reference valid color and path data without resource missing errors.

---

### Task 2: Configure Launcher Name "BEP" strictly on LauncherAlias

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-*/strings.xml` (values-es, values-hi, values-bn, values-uk)
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add `launcher_app_name` string**
  In `app/src/main/res/values/strings.xml` and locale string files, add:
  `<string name="launcher_app_name">BEP</string>`

- [ ] **Step 2: Update `AndroidManifest.xml`**
  On `<activity-alias android:name=".LauncherAlias">`:
  - Set `android:label="@string/launcher_app_name"`
  - Keep `android:enabled="false"` (preserving Stealth Mode ON by default).

---

### Task 3: Build, Verify and Test

**Files:**
- Modify: `app/build.gradle.kts` (bump `versionCode = 26`)

- [ ] **Step 1: Bump `versionCode`**
  Set `versionCode = 26` in `app/build.gradle.kts`.

- [ ] **Step 2: Compile Debug APK**
  Run `./gradlew assembleDebug` and verify zero errors.

- [ ] **Step 3: Verify APK Badging**
  Inspect manifest inside APK to verify:
  - `LauncherAlias` has label `'BEP'` and icon `@mipmap/ic_launcher`.
  - Application label is `'BestEdgingPanel'`.
  - Stealth Mode default remains enabled.

- [ ] **Step 4: Commit changes**
  Commit all changes to git with a descriptive commit message.
