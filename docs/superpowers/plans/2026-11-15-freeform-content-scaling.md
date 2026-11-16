# Freeform Window Content Auto-Scaling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the cramped, clipped content in freeform windows (where search bars and text like "Search Set..." get cut off) by implementing dynamic proportional density scaling in `services.jar` WindowManager, ensuring all apps render a complete, full-page 360dp phone layout scaled to fit the freeform window.

**Architecture:**
- In Android WindowManager's `TaskFragment.computeConfigResourceOverrides` (`services.jar`), when a task is in `WINDOWING_MODE_FREEFORM` (mode 5):
  - Detect the window width ($W_{\text{window}}$) and parent display width ($W_{\text{display}}$) and display density ($D_{\text{display}}$).
  - Compute proportional task density:
    $$D_{\text{task}} = \text{clamp}\left(\frac{W_{\text{window}} \times D_{\text{display}}}{W_{\text{display}}},\; 160,\; D_{\text{display}}\right)$$
  - Set `inOutConfig.densityDpi = D_{\text{task}}`.
- When $D_{\text{task}}$ is set on the task override configuration:
  - The client application automatically calculates `screenWidthDp = 360dp` (matching standard phone width).
  - Skia and Android view hierarchy render all layouts, search bars, lists, and fonts natively scaled down to fit the window width without any truncation, letterboxing, or layout breakage.
- Rebuild `services_patched.jar`, package it into the active Magisk overlay (`/data/adb/modules/sysui_overlay/system/framework/services.jar`), and verify live on the connected device.

**Tech Stack:** Smali, AOSP WindowManager (`services.jar`), Apktool, Android DisplayMetrics & Configuration.

---

## Global Constraints
- Target device: Android 14/15/17 (BestGSI on `07670331CC004153`, root/Magisk).
- Target file: `/data/adb/modules/sysui_overlay/system/framework/services.jar`.
- Source workspace: `work_on_next_17/workspace/nov13_core_patches/services_src`.
- Safety: Only apply density scaling when `windowingMode == 5` (`WINDOWING_MODE_FREEFORM`) and $W_{\text{window}} < W_{\text{display}}$. Fullscreen, PiP, and split-screen must remain 100% untouched.

## Review Focus
1. **Zero Text Truncation**: When opening Settings or any app in a narrow freeform window (e.g. 660px), "Search Settings" must show completely rather than being truncated to "Search Set...".
2. **Proportional Layout**: The app inside the window must look like a miniature version of the phone screen (360dp standard width), with intact columns and proper padding.
3. **Resizing Stability**: Changing window bounds must recompute $D_{\text{task}}$ dynamically without crashing `system_server`.
4. **No Impact on Fullscreen/Split-screen**: Fullscreen apps must maintain full native density (480dpi).

---

### Task 1: Smali Patch for Proportional Freeform Density in `TaskFragment.smali`

**Files:**
- Modify: `work_on_next_17/workspace/nov13_core_patches/services_src/smali_classes3/com/android/server/wm/TaskFragment.smali`

- [ ] **Step 1: Locate Freeform Windowing Mode in `computeConfigResourceOverrides`**
  In `TaskFragment.computeConfigResourceOverrides`:
  Right where `v9` holds the resolved `windowingMode` (line ~1760) and before `v10` (`densityDpi / 160`) is calculated:
  Check if `v9 == 0x5` (`WINDOWING_MODE_FREEFORM`).

- [ ] **Step 2: Add Proportional Density Calculation Smali Logic**
  If `windowingMode == 5`:
  - Fetch $W_{\text{window}}$ from `v6.windowConfiguration.getBounds().width()`.
  - Fetch $W_{\text{display}}$ from `v7.windowConfiguration.getBounds().width()`.
  - Fetch $D_{\text{display}}$ from `v7.densityDpi`.
  - If $W_{\text{window}} > 0$ and $W_{\text{window}} < W_{\text{display}}$:
    - Compute: `scaledDensity = (W_window * D_display) / W_display`.
    - If `scaledDensity < 160`: clamp to `160`.
    - If `scaledDensity > D_display`: clamp to `D_display`.
    - Assign `v6.densityDpi = scaledDensity`.
    - Update the local register holding density so `v10` is calculated from `scaledDensity`.

- [ ] **Step 3: Verification of Smali Syntax**
  Verify register usage, labels, and branching to ensure zero syntax or register allocation conflicts.

---

### Task 2: Build `services_patched.jar` & Verify Bytecode

**Files:**
- Execute: Build script compiling `services_src` into `services_patched.jar` using Apktool.

- [ ] **Step 1: Compile `services_src`**
  Run `apktool b services_src -o services_patched.jar`.
  Expected: Returncode 0, valid jar output.

- [ ] **Step 2: Verify Smali in Built Dex**
  Disassemble or inspect the generated dex in `services_patched.jar` to ensure `TaskFragment` contains the density patch.

---

### Task 3: Deploy to Device & Live Test

**Files:**
- Deploy to: `/data/adb/modules/sysui_overlay/system/framework/services.jar`

- [ ] **Step 1: Backup current `services.jar`**
  Keep a backup of the current running `services.jar` in `workspace`.

- [ ] **Step 2: Push to Magisk Overlay & Soft Reboot / System Server Restart**
  Push `services_patched.jar` to `/data/local/tmp/services.jar`.
  Copy as root to `/data/adb/modules/sysui_overlay/system/framework/services.jar` with permissions `644`.
  Restart system server (`killall system_server` or `stop && start`).

- [ ] **Step 3: Verify Freeform Window Scaling Live**
  Launch Settings in Freeform mode.
  Capture screenshot and analyze:
  - Check that search bar shows "Search Settings" without being cut off.
  - Check `dumpsys activity activities` to confirm `Task` has scaled `densityDpi` and `screenWidthDp = 360`.

- [ ] **Step 4: Commit patch script and changes to git**
  Save reproducible patch scripts and commit.
