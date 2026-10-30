# NewTermux Progress Log

## 2026-10-30 — Layout modes: status line + dual screen (feature/layout-modes, phase 1 built, NOT device-tested)

**Why:** On the Retroid Pocket Duo in landscape (1920×1080 top screen) the terminal only gets
685 px of height (≈22 rows): the toolbar (95 px) and session preview row (158 px) take the rest.
The user picked two layouts from a mockup page (`/sdcard/Download/NewTermux-landscape-layouts.html`):
6 = tmux-style status line (portrait AND landscape) and 12 = dual screen (Duo bottom screen).
They go in Settings → Appearance as optional layouts, default Standard.

**Plan:**
- Phase 1 — status line, both orientations (this commit).
- Phase 2 — dual screen: control deck activity on display 2 (Duo bottom screen, 1280×960,
  `dumpsys display` id 2, runs Launcher3 SecondaryDisplayLauncher), top screen terminal only.

**Built (phase 1, commit 2c5dcadf):**
- `NewTermuxSettings`: `layout_portrait` / `layout_landscape` (String: `standard`, `status_line`).
- `newtermux_status_line.xml` (40dp, included at the bottom of `activity_termux.xml`):
  ☰ (left drawer), text session tabs `1:name` (tap = switch, long-press = Rename/Close),
  toolbar buttons, ⌨ (right drawer in drawer mode, else show/hide the extra keys row).
- `TermuxActivity`: `applyLayoutMode()` from `onResume()` and a new `onConfigurationChanged()`
  (the manifest's configChanges includes orientation, so the activity is not recreated).
  `setStatusLineMode()` MOVES the toolbar buttons between the toolbar row and the status line,
  so their listeners and state colors (root, STT, AC) keep working. `updateSessionTabs()` also
  fills the status-line tabs.
- `ThemePickerActivity` (Settings → Appearance): new "Layout" section at the top with
  Portrait / Landscape choices.

**CI:** pending. **Device test:** not done.

**Resume point:** branch `feature/layout-modes` in `/root/work/NewTermux` (Duo). Next: CI green →
stage APK in `/sdcard/Download/NewTermux-layout-modes-<sha8>.apk` with sha256 → ask before
install → test both orientations, drawers, session switching, logcat for FATAL.

## 2026-04-25 — Session Pip View (feature/session-pip-view)

**Status: CI green, NOT merged to main. Awaiting user testing.**

### What was built
Replaced the flat Material chip session tabs with live mini terminal previews ("pips").

**Files changed:**
- `app/src/main/java/com/termux/app/terminal/MiniTerminalPipView.java` (NEW)
  - Custom View using `TerminalRenderer.render()` scaled via `Canvas.scale()` to fit 72×48dp
  - Renders full session screen: all colors, cursor, output — scaled down
  - `notifyUpdate()` calls `postInvalidate()` (thread-safe) for live updates
  - Active session: accent-color border (#BB86FC); inactive: dim border (#444444)
- `app/src/main/res/layout/newtermux_toolbar.xml`
  - Removed `ChipGroup` (`session_chip_group`)
  - Added `LinearLayout` (`session_pip_container`) inside existing `HorizontalScrollView`
  - Row height: 40dp → 56dp
- `app/src/main/java/com/termux/app/TermuxActivity.java`
  - Field: `mSessionChipGroup` (ChipGroup) → `mSessionPipContainer` (LinearLayout)
  - `updateSessionTabs()` now creates `MiniTerminalPipView` instances (72×48dp, 4dp margin)
  - Added `notifyPipUpdate(TerminalSession)` — finds and invalidates only the changed pip
- `app/src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java`
  - `onTextChanged()` now calls `mActivity.notifyPipUpdate(changedSession)` for live refresh
- `.github/workflows/debug_build.yml`
  - Added `feature/**` to push trigger list so feature branches build automatically

**CI:** Run 24946104721 — success on `feature/session-pip-view`

### Next steps after testing
- If pips look good → merge `feature/session-pip-view` to `main`, bump version, trigger release
- Possible follow-ups:
  - Add close button drawn in pip corner (currently close is via long-press rename; no X on pip)
  - Swipe-to-close gesture on pip
  - Tweak pip size (currently 72×48dp) if too small or too large on device

---

## 2026-04-21 — v1.5.4 (claude-integration merged to main)

**Status: Released, tagged v1.5.4**

- Claude Chat button in toolbar (removed in later commit — see note below)
- Background kill fix: START_STICKY + PARTIAL_WAKE_LOCK in onStop/onStart
- RUN_COMMAND permission: dangerous → normal
- Settings toggle for Claude Chat button

**Note:** Claude Chat button was subsequently removed (commit 7799e1f) — not in current main.
Current main also has: "Allow scrolling terminal while a command is running" (commit 158231c).
