# NewTermux Progress Log

## 2026-10-30 — In-app updater + dual screen (feature/updater-dual-screen, built, CI pending, NOT device-tested)

**Why:** user: "do we have an app updates also and update notification pills similar to how
bannerlator works? if not can we add it also" (verified: NewTermux had no update code) and then
"merge what we have then build the updater and phase 2 together on the same branch and hand me
a new APK to test". Branch `feature/updater-dual-screen` off main ca866a17 (layout modes merged).

**Built (commit 2d50b1d):**
- Updater `com.newtermux.update` — ported from Bannerlator `core/UpdateManager.kt` +
  `ui/UpdateCards.kt` (@9846e8c6, same author), simplified: GitHub releases API only
  (`releases/latest`, or the newest release when "Include prereleases" is on), 6 h cooldown,
  semver compare on the tag vs BuildConfig.VERSION_NAME (`+build` ignored → CI builds don't
  nag). Asset: `termux-app_<TERMUX_PACKAGE_VARIANT>-debug_<abi>.apk` (demo:
  `newtermux-test-coexist_demo_<abi>.apk`, fallback universal). Download to cacheDir/updates,
  size + GitHub asset `digest` sha256 check, install via `UpdateFileProvider`
  (`${applicationId}.updates`). UI: pill (top-right of the terminal, below the header),
  highlights from "- **Title.** text" bullets in the release notes, download/failed cards;
  overlay ComposeView never takes focus. Settings → Updates (check now, notify, prereleases,
  skipped version, release page, test-build-only "pretend 0.0.0").
- Dual screen `com.newtermux.deck` — Landscape → "Dual screen" (only offered when
  `DisplayManager.DISPLAY_CATEGORY_PRESENTATION` has a non-private display; the Duo's display 2
  has FLAG_PRESENTATION|FLAG_TRUSTED). Top: terminal only + system bars hidden.
  `ControlDeckActivity` on the second display via `setLaunchDisplayId` (FLAG_NOT_FOCUSABLE,
  own taskAffinity): toolbar actions (performClick on the real buttons), live session previews
  (MiniTerminalPipView), drawer commands (reuses the drawer's buttons, long-press = edit),
  extra keys (TermuxActivity.getExtraKeysView() returns the deck's keys while up, so CTRL/ALT
  work), scroll pad (new `TerminalView.scrollByRows`). Deck closed (✕/Back/system) → status
  line on the top screen until NewTermux is resumed; launch not seen in 3 s → same fallback.

## 2026-10-30 — Layout modes phase 1 MERGED to main (status line); next: updater + dual screen on one branch

**User decision (2026-10-30):** "merge what we have then build the updater and phase 2 together on
the same branch and hand me a new APK to test". Merged with three small confirmations still open
(drawer −/+ row fully visible at the end of the scroll, AC/mic colors, status line above the soft
keyboard). Tested build = 3541ee19; the branch head differs from it only in PROGRESS_LOG.md.
Backup ref before the merge: `refs/backup/20261030/main-before-layout-modes` = 6001da84.

## 2026-10-30 — Layout modes: status line + dual screen (feature/layout-modes, phase 1 fix build 3541ee19 installed, round 3 mostly PASS)

**Device test round 1 (Duo, build ec53f974 installed 12:39 EDT, verified: base.apk sha256 1338690f…,
versionName 1.6.2+ec53f97):**
- OK: Settings → Appearance → Layout section renders (screencap); Landscape → Status line works
  (bottom line: ☰, tabs `1:Claude` `2:#2`, toolbar buttons, ⌨); ☰ opens the left drawer; ⌨ opens
  the right keys drawer (user has extra keys in drawer mode); tabs listed per session.
- BUG (pre-existing, more visible now): left drawer not scrollable — buttons past the bottom are cut
  off (Screenshot_20261030-124127).
- BUG: long-press on a status-line tab → NtPopupMenu `showAsDropDown()` clipped by the nav bar
  (tiny box under the tab, Screenshot_20261030-124250). Cause: popup height WRAP_CONTENT, so
  PopupWindow can't detect the overflow and never flips above the anchor.
- Request: thin divider under the status line (separate it from the nav bar).
- Crash buffer: one tombstone at 12:39:09 = Magisk's `content call … notify from.uid 10122` (root
  notice when NewTermux's startup `su -c id` ran) → ClassNotFoundException: this ROM has no
  `/system/bin/content` / `content.jar`. Not NewTermux code (RootToggleManager unchanged on branch).
- Not yet tested: AC/mic colors, rotation to portrait, portrait status line, soft keyboard.

**Fix commit 118134e1:** left drawer → ScrollView (`left_drawer` id kept; not referenced in Java),
NtPopupMenu measures its final height (EXACTLY width) and sets it, 1dp divider under the status line.

**Device test round 2 (still on ec53f974):** rotate to portrait → standard header back, toolbar
buttons restored in original order (Screenshot_20261030-124940, 1080×1920); rotate back →
status line again (…-125012). Prefs file: `layout_landscape=status_line`, no `layout_portrait`
(portrait status line not tried yet). No NewTermux crash/FATAL/ANR since install (only the Magisk
`content` tombstone; 07:32–08:47 FATALs are com.google.android.gms). Not yet confirmed: AC/mic
colors, portrait status line, soft keyboard with the status line.

**CI:** Build run 41092918904 on 3541ee19 — success (both jobs, no failed/skipped steps).
**Staged:** `/sdcard/Download/NewTermux-layout-modes-3541ee19.apk`, sha256
0f575fb6a3e8f93a6cc7eeebc31789c13476178f817a72cbe295a525118c9f38 (CI list = download = host =
device), cert B6:DA:01:48… (same as installed) → in-place update.

**Device test round 3 (build 3541ee19 installed 12:58 EDT; verified base.apk sha256 0f575fb6…,
versionName 1.6.2+3541ee1):**
- Divider under the status line: measured on screencap, #333333 at rows 982–983 (top divider
  902–903), nav bar below. PASS.
- Left drawer scrolls (Screenshot_20261030-131009: EXPORT SCREEN scrolled off, all three "Long
  press to set" slots reachable). The −/+ row is half visible in that shot — asked the user
  whether it shows fully at the end of the scroll.
- Tab long-press → Rename/Close opens ABOVE the tab, fully visible (…-131021). PASS.
- Portrait status line (…-131057, 1080×1920): fits, dividers both sides. PASS. Prefs:
  layout_portrait=status_line, layout_landscape=status_line.
- No com.termux FATAL/ANR; newest tombstone = Magisk `content` notice (12:58); 13 FATALs = gms.
- Not confirmed yet: AC/mic colors, soft keyboard with the status line.

**CHECKPOINT (16:53 UTC) — RESUME POINT:** user installs 3541ee19 (ends the Claude session). After
`cc -c`: verify `pm path com.termux` base.apk sha256 == 0f575fb6… and versionName 1.6.2+3541ee1;
then test: left drawer scrolls; tab long-press menu opens ABOVE the tab; divider under the status
line; AC/mic colors; Portrait → Status line; soft keyboard (tap terminal) with the status line;
crash buffer + FATAL check. Then ask the user about phase 2 (dual screen).

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

**CI:** Build run 41087636857 on ec53f974 — success (both jobs; no failed/skipped steps; no
warnings in changed files).

**Staged:** `/sdcard/Download/NewTermux-layout-modes-ec53f974.apk` (apt-android-7 arm64-v8a,
sha256 1338690f87d37a491607c22747b3264d089b00010460e761228a08d6c7c98663, same on CI list, host,
device). Signed with the same cert as the installed v1.6.2 (sha256 B6:DA:01:48…E5:E1), versionCode
28 → in-place update, data kept.

**Device test:** not done. CHECKPOINT 2026-10-30 ~16:45 UTC: the user is installing the staged APK
themselves. The Claude session runs inside NewTermux on the Duo (cgroup `/apps/uid_10122/…`, plain
`cc` alias, no tmux), so the install ends it. Memory (newtermux-project, newtermux-log, devices,
termux-setup, device-launch-tests, session-workflow, confirmed-issues + index) updated and synced
before the install. Restart with `cc -c` (continues the last chat).

**Resume point:** branch `feature/layout-modes` in `/root/work/NewTermux` (Duo). If the install
happened: check `dumpsys package com.termux` lastUpdateTime + base.apk sha256 == 1338690f…, then
test: Settings → Appearance → Layout (Portrait/Landscape), status line in both orientations,
rotate while open, ☰ / ⌨ / tabs / long-press menu / toolbar buttons (root, mic, AC colors),
`logcat -b crash` + FATAL check.

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
