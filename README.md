# BestEdgingPanel

**BestEdgingPanel** is a lightweight, high-performance edge sidebar and multitasking enhancement built natively for custom Android ROMs. It is currently integrated as a core system privileged application in **BestGSI (Android 17)**.

Designed to replace heavy, proprietary OEM sidebars, BestEdgingPanel provides seamless access to your favorite apps, tools, and actions via customizable gesture handles on either screen edge, coupled with true desktop-grade floating freeform multitasking.

---

##  How It Works (System-Privileged Integration)



Unlike standard user-installed sidebar utilities that require constant battery-optimization exemptions, accessibility workarounds, or ADB permissions after every reboot, **BestEdgingPanel is built and pre-granted as an Android system `priv-app`**:

0.first enable freeform windows by modifying the system server or use adb command : `adb shell settings put global enable_freeform_support 1`   
1. **System `priv-app` Integration (`privapp-permissions.xml`)**:
   - Resides directly under `/system/priv-app/BestEdgingPanel/` or `/system_ext/priv-app/BestEdgingPanel/`.
   - Granted elevated system permissions automatically out-of-the-box (`SYSTEM_ALERT_WINDOW`, `WRITE_SECURE_SETTINGS`, privileged window management, and package querying).
2. **Zero-Setup Experience**:
   - No intrusive onboarding wizards, permission request dialogs, or battery-restriction prompts on fresh ROM boot.
   - Automatically activates and registers edge handles seamlessly upon device startup.
3. **Privileged Accessibility & Window Operations**:
   - Leverages elevated platform privileges to trigger instant system actions (Back, Home, Recents, Notifications, Power Menu, Quick Settings, Screenshot) without lag or accessibility crashes.
4. **Android 17 Treble Ready**: tested on BestGSI

---

##  Key Changes

BestEdgingPanel was evolved from open-source foundations into a clean, system-grade utility tailored for BestGSI:

* **Complete Rebrand & Bloat Removal**:
  - Fully rebranded to **BestEdgingPanel** (launcher label **BEP**).
  - Stripped all donation buttons, promotional links, third-party analytics, and user-space permission warnings.
* **Modern Freeform Windowing**:
  - Implemented modern floating window multitasking with soft-keyboard adaptation, custom default aspect ratios, and seamless window dragging.
  - Added clean termination logic to prevent lingering transparent ghost windows or recents clutter upon closing freeform apps.
  - Eliminated launch flicker on transparent activity initialization.
* **Android 16/17 Material Expressive UI**:
  - Completely redesigned settings architecture into **Material Expressive Inset Grouped Pods** and a streamlined **Hero Switch**.
  - Flat unboxed, typography-focused layouts with crisp hairline dividers and dynamic search bar.
  - Dynamic **Follow System** theme engine that instantly harmonizes with system Light and AMOLED Dark modes.
* **Quick Settings Tile & Stealth Integration**:
  - Added a dedicated system Quick Settings tile for instant one-tap toggle of edge handles.
  - Long-pressing the QS tile opens BestEdgingPanel settings directly.
  - Stealth mode support to hide the launcher icon while retaining full accessibility through system settings and the QS tile.
* **Performance & Touch Latency**:
  - Tuned edge swipe triggers for near-zero touch latency on RenderThread animators.
  - Implemented aggressive `LruCache` and coroutines pre-loading for buttery smooth app drawer scrolling.

---

## What This Project Is For

* **OEM-Free Multitasking**: Bringing Samsung Edge Panel / Xiaomi Sidebar style multitasking to AOSP,Treble GSIs without OEM framework dependencies.
* **Productivity & Speed**: Fast, one-handed app switching, floating calculator/browser popups, and quick gestures on any screen size.

---

## 🙏 Credits & Acknowledgments

This project is made possible thanks to the following open-source contributions and inspirations:

* **[Smart Edge](https://github.com/Imtiaz-Official/Smart-Edge)**
* **[Mi-Freeform](https://github.com/sunshine0523/Mi-Freeform)**
* **[SmartDock](https://github.com/Axel358/SmartDock)**
* **[HiddenApiBypass](https://github.com/LSPosed/Android-HiddenApiBypass)**
* **[ColorPickerView](https://github.com/skydoves/ColorPickerView)**

---
