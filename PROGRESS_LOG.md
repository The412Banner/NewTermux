# NewTermux Progress Log

## 2026-10-30 — Cleanup: merged branches + old v2.3.0 release deleted (user)

User: "delete the merged branches and the v2.3.0 release".
- Branches (all verified ancestors of main 3efa7aef) backed up, then deleted on GitHub + locally:
  feature/layout-modes 0f248d60 → `refs/backup/20261030/branch-feature-layout-modes`;
  feature/updater-dual-screen d06c0ee4 → `refs/backup/20261030/branch-feature-updater-dual-screen`;
  feature/deck-session-refresh 4ab3b161 → `refs/backup/20261030/branch-feature-deck-session-refresh`.
- Release v2.3.0 (old numbering, published 2026-03-06, 10 APKs / 554 MB) deleted; its JSON (notes,
  asset names/sizes/sha256 digests) saved locally at /root/work/backups/newtermux-release-v2.3.0-
  20261030.json (APK files not kept). Tag v2.3.0 KEPT (lightweight → 51b96a8d). Releases list now
  starts with NewTermux 2.0.1 (Latest), 2.0.0, 1.6.2.
- Left as they were (not part of this request): claude-integration, test, feature/compose-migration,
  feature/session-pip-view, 4 dependabot branches; old-numbering tags.

## 2026-10-30 — NewTermux 2.0.1 RELEASED (stable, Latest): deck session list + long-press menu

- **Updater proven end to end with a real release (22:12 UTC):** the Duo (test build 2.0.0+c586f49)
  updated itself to v2.0.1 through the in-app updater: cache/updates/termux-app_apt-android-7-debug_
  arm64-v8a.apk (53975850 bytes, sha256 8765cccd… = release asset) → package installer InstallSuccess
  → installed versionName 2.0.1 / versionCode 30, base.apk sha256 8765cccd…. No ANR/FATAL.

- Release run 41130305467 (headSha 61c13912) success; 10 assets; notes from
  /root/work/release-notes-v2.0.1.md (2 highlights), title "NewTermux 2.0.1", `--latest`;
  releases/latest = v2.0.1. Verified: arm64 APK sha256 8765cccdef1e6f69c09d49580bea5079d69c72ccfca064402a93bed6be67f54e
  = GitHub digest; cert B6:DA:01:48…; manifest versionName 2.0.1; tag → 61c139123f07.
- Slip: PROGRESS_LOG commit ffce3e5e was pushed to main during the release run; the run built
  61c13912 (from the tag), so the release is unaffected.

- Release-cut 61c13912 "Release 2.0.1 (versionCode 30)"; main Build 41129393649 green.
- Annotated tag v2.0.1 → 61c139123f073508166fd5bfb6b67b286b6b6a78; release workflow run 41130305467.
- **RESUME POINT (21:20 UTC, user going offline):** when back: confirm run 41130305467 success →
  `gh release edit v2.0.1 --title "NewTermux 2.0.1" --notes-file /root/work/release-notes-v2.0.1.md
  --latest` → verify latest = v2.0.1, assets, APK sha256 == digest, cert, versionName 2.0.1.

**User decision (2026-10-30):** "go ahead and release it as v2.0.1". Tested build = c586f49b; branch
head differs only in PROGRESS_LOG.md. Backup ref before the merge:
`refs/backup/20261030/main-before-deck-session-refresh` = 6fe69410.

## 2026-10-30 — Fix: dual-screen deck showed no sessions + long-press menu (feature/deck-session-refresh, DEVICE-PROVEN)

**Device test (c586f49b installed 16:57 EDT; sha256 21d52cfb… verified):** user: "installed and it's
working correctly". Evidence: notification went 5 → "1 session" (Close session on the deck);
screencap display 2 (20:58 UTC) shows the long-press menu for "1:Claude" (Rename / Close session /
Cancel over the dimmed deck), deck lists 1:Claude + New; no new ANR (newest still 14:31 from
95ff96a2), no com.termux FATAL; tombstone_28 = Magisk `content` notice. Copies:
/sdcard/Download/NewTermux-dual-screen-{top,bottom}-20261030-2058.png. → Ready for v2.0.1 on the
user's word.


**CI:** Build run 41125794792 on c586f49b — success (BUILD SUCCESSFUL, 0 errors, no Kotlin w:/e:).
**Staged:** `/sdcard/Download/NewTermux-deck-longpress-c586f49b.apk` (versionName 2.0.0+c586f49,
versionCode 29), sha256 21d52cfb99f339d18cfb113a2757a240212fd911a25159e540c574468a2d8cbf
(CI = host = device), same cert.

**CHECKPOINT / RESUME POINT (21:00 UTC):** user installs (ends the session). After `cc -c`: verify
sha256 21d52cfb… + versionName 2.0.0+c586f49; test long press on a deck session → menu; Rename →
dialog on the main screen, keyboard on the bottom; Close session → tile disappears, notification
count drops; Cancel / tap outside closes the menu; no ANR/FATAL. Then v2.0.1 only on the user's word.


**Device test (df01851c installed 16:42 EDT; sha256 5a70ea38… verified):** session list FIXED —
screencap display 2 shows 3:#3 (current, highlighted), 4:#4, 5:#5, New with 1–2 scrolled off left;
notification "5 sessions". No new ANR. User: "I cannot long press any of them to rename or close"
→ tiles only had onClick. Added: long press → menu drawn inside the deck (label, Rename, Close
session, Cancel; scrim tap = cancel), calling new TermuxActivity.renameSession/closeSession (same
as the session tab menu; rename dialog on the main screen).


**CI:** Build run 41123994051 on df01851c — success (BUILD SUCCESSFUL, 0 errors, no Kotlin w:/e:).
**Staged:** `/sdcard/Download/NewTermux-deck-refresh-df01851c.apk` (apt-android-7 arm64-v8a,
versionName 2.0.0+df01851, versionCode 29 = installed release → in-place), sha256
5a70ea388bb25ade7019ad32e5e19c61d94b7779281943f0d0fb14220cad877e (CI = host = device), same cert.

**CHECKPOINT / RESUME POINT (20:42 UTC):** user installs (ends the session). After `cc -c`: verify
sha256 5a70ea38… + versionName 2.0.0+df01851; screencap display 2: SESSIONS shows every session the
notification counts; open/close/switch a session → deck follows; no ANR/FATAL. Then (user's word)
release as v2.0.1: merge to main with backup ref, release-cut commit (versionCode 30, 2.0.1), tag,
notes, --latest, verify.


**Found on v2.0.0 (installed 16:27 EDT via the in-app updater; base.apk sha256 e31821f6… = release
asset, versionCode 29):** user: "my second screen is not showing all of my open sessions and I know
there should be three or four". screencap display 2: SESSIONS row = only "New"; the service
notification: "4 sessions (wake lock held)"; one TermuxActivity (t169) bound to TermuxService.
Cause: with strong skipping (Kotlin 2.2.20 Compose compiler) Sessions(act, colW) was skipped on
every recomposition (same args) → it kept its first composition from before the service bound
(cold start) → empty forever; same latent issue for Commands/Header and the current-session
highlight. Fix: each reads DeckBridge.version itself.

## 2026-10-30 — NewTermux 2.0.0 RELEASED (stable, Latest)

- Release-cut commit 639078bf "Release 2.0.0 (versionCode 29)"; main Build 41116534274 green.
- Annotated tag `v2.0.0` (tagger The412Banner) → 639078bfaead88081d8efe3ef32e773b3efff5b4; release
  workflow run 41117352348 (headSha 639078bf) success; 10 assets (termux-app_apt-android-7-debug_
  {arm64-v8a,armeabi-v7a,x86,x86_64,universal}.apk + newtermux-test-coexist_demo_*), not prerelease.
- Notes replaced (title "NewTermux 2.0.0"; What's new = 4 "- **Title.**" highlights for the in-app
  dialog; updating/installation; updater credited as ported from Bannerlator) and `--latest` set
  (old v2.3.0 release has higher semver); releases/latest = v2.0.0.
- Verified: downloaded arm64 APK sha256 e31821f6fddd6faa4f85a20d5d91910be15561cda2332f0b8732203b824b070f
  = GitHub asset digest; cert B6:DA:01:48… (same as 1.6.2); manifest versionName 2.0.0; dex has
  UpdateManager + ControlDeckPresentation.
- Open (user's call): old v2.3.0 release page (old numbering) still listed; merged branches
  feature/layout-modes + feature/updater-dual-screen kept (delete only on the user's word).

## 2026-10-30 — Updater + dual screen MERGED to main (e70fdfd5); stable v2.0.0 release

**User decision (2026-10-30, after ✕/reopen and leave/return checks):** "all good, merge and
release a new stable". Tested build = c39726b4; branch head differs only in PROGRESS_LOG.md.
Backup ref before the merge: `refs/backup/20261030/main-before-updater-dual-screen` = ca866a17.
Merge e70fdfd5 (--no-ff): tree == branch tree, code == tested c39726b4.
**Version decision:** v1.7.0 is taken by an old-numbering tag; user first picked v1.6.3, then:
"make it 2.0 actually" → versionName 2.0.0 / tag v2.0.0 (free: no tag, no release), versionCode 29.
Release-cut commit next, tag after main CI is green.

## 2026-10-30 — In-app updater + dual screen (feature/updater-dual-screen, updater + dual screen DEVICE-PROVEN; ✕/reopen + leave/return pending)

**Device test round 6 (c39726b4, user-driven, 15:29–15:30 EDT):** updater end to end with the
test-build "Pretend 0.0.0" switch: check → pill "Update available · v1.6.2 · 51.3 MB" (Screenshot_
20261030-153016, top-right of the terminal); download to cache/updates/termux-app_apt-android-7-
debug_arm64-v8a.apk = 53818921 bytes, sha256 8caee7f25efdc1749916af76bb63e81fe3e17c3ba5b5eece4b971ad12570048f
= the GitHub asset digest; installer opened (logcat: InstallStaging → PackageInstallerActivity
displayed 15:29:43); user cancelled (installed build unchanged). Deck back on the bottom screen
with Gboard over it. No new ANR/FATAL. → Updater: DEVICE-PROVEN. "Pretend 0.0.0" still ON
(user should turn it off). Remaining before merge: ✕/reopen button, leave/return.


**Device test round 5 (c39726b4 installed 15:22 EDT; verified sha256 451713b3…, versionName
1.6.2+c39726b; tree clean):** driven by Claude with `input -d 2` (announced to the user first):
- Deck up (screencap display 2), focus: display 0 TermuxActivity, display 2 launcher.
- `input -d 2 tap 1043 56` (deck ⚙) → SettingsActivity task #163 on display 2 (mFocusedApp there);
  screencap: bottom = Settings, top = terminal still visible and updating. PASS.
- `input -d 2 keyevent KEYCODE_BACK` → Settings gone, display 2 focus = launcher, deck back
  (screencap; Gboard over it as the terminal regained focus). PASS.
- No ANR since install (newest still 14:31 from 95ff96a2), no com.termux FATAL.
- Remaining before merge: updater flow on device, ✕/reopen button, leave/return.


**CI:** Build run 41113058327 on c39726b4 — both jobs success. The API showed some steps as
in_progress/null after completion (GitHub reporting glitch); verified via the job log instead:
BUILD SUCCESSFUL, 0 ##[error], all 6 apt-android-7 artifacts uploaded. dex has openOnSecondScreen,
isSecondScreenBusy, setLaunchDisplayId, ControlDeckPresentation.
**Staged:** `/sdcard/Download/NewTermux-updater-dual-c39726b4.apk`, sha256
451713b3f072c22d503889b23afe1288ddf150ae8394d06764db05bbeb2dedd1 (CI = download = host = device),
same cert.

**CHECKPOINT / RESUME POINT (19:20 UTC):** user installs (ends the session). After `cc -c`: verify
sha256 451713b3… + versionName 1.6.2+c39726b, no ANR/FATAL. Test: deck ⚙ → Settings on the bottom
screen, terminal readable on top (screencap both: display 0 = terminal, display 2 = Settings);
Appearance from there stays on the bottom; Back out of Settings → deck returns; status-line ⚙ with
the deck closed → Settings on top as before; then the updater flow + ✕/reopen + leave/return.


**User request (Screenshot_20261030-150724: Settings covering the top screen):** "because we are
using dual screen and the settings cog is on the second screen below, can the app settings open on
the second screen leaving the session viewable and readable on the top screen still?"
→ openSettings(): with the deck showing, SettingsActivity starts on the second display
(setLaunchDisplayId, NEW_TASK|MULTIPLE_TASK). DeckBridge tracks our activities on that display
(ActivityLifecycleCallbacks, weak set); the deck is hidden while any exist and returns when the last
is destroyed (also after a 3 s launch timeout). applyLayoutMode keeps the top screen terminal-only
meanwhile. Settings' own screens (Appearance, Package/SSH/File manager, installer prompt) open on
the same display.


**Device test round 4 (4a57e94a installed 14:46 EDT; verified sha256 340106bb…, versionName
1.6.2+4a57e94):** user: "things look like they are working now". Measured 18:47 UTC:
- screencap display 0 (1920×1080): terminal only, system bars hidden. screencap display 2
  (1280×960): deck with Gboard drawn over it. Copies: /sdcard/Download/NewTermux-dual-screen-
  {top,bottom}-20261030-1447.png.
- dumpsys: display 2 mCurrentFocus/mFocusedApp = SecondaryDisplayLauncher; display 0 =
  TermuxActivity; mDisplayIdToShowIme=2, mInputShown=true.
- No ANR since install (only the 14:31 one from 95ff96a2; /data/anr unchanged), no com.termux
  FATAL. → Dual screen: DEVICE-PROVEN (deck, keyboard over deck, typing, no ANR — user + dumpsys).
- Still to test: updater flow on device (pretend 0.0.0 → dialog → download → cancel at the
  installer; pill), ✕/reopen button, leave/return.


**CI:** Build run 41108355586 on 4a57e94a — success (both jobs; BUILD SUCCESSFUL, no Kotlin w:/e:).
APK: manifest has no ControlDeckActivity; dex has ControlDeckPresentation (extends
android.app.Presentation), its onCreate addFlags(0x00020008).
**Staged:** `/sdcard/Download/NewTermux-updater-dual-4a57e94a.apk`, sha256
340106bbc5e038aae678fb8ac449347df96dd00d49fb5507f3551d05dcac002d (CI = host = device), same cert.
User had set Landscape → Status line (interim advice) — must set Dual screen again to test.

**CHECKPOINT / RESUME POINT (18:55 UTC):** user installs (ends the session). After `cc -c`: verify
sha256 340106bb… + versionName 1.6.2+4a57e94, crash buffer + `ANR in com.termux` (must be none).
Test: Landscape → Dual screen; deck on the bottom screen; tap the terminal → keyboard on the bottom
screen over the deck AND typing works (dumpsys: display 2 mFocusedApp = launcher, not com.termux);
press the bottom ▼/Back → no ANR; tap deck buttons/keys/scroll pad → terminal keeps input; ✕ →
status line + reopen button; leave/return; then the updater flow (pretend 0.0.0 → CANCEL at the
installer; pill).


**Device test round 3 (95ff96a2 installed 14:30 EDT; verified sha256 20f1d1bc…):**
- User: "getting an error now and the keyboard still does not show despite getting a navigation
  button and when I press that the keyboard does appear for a second until I try to type then it
  disappears again" + "not responding pop-up still keeps happening".
- logcat: `ANR in com.termux (com.termux/com.newtermux.deck.ControlDeckActivity)`, Reason:
  `Input dispatching timed out (Application does not have a focused window)` (14:31:05; trace
  /data/anr/anr_2026-10-30-14-31-03-321). Cause: an Activity on display 2 = that display's focused
  app; with a NOT_FOCUSABLE window, key events to display 2 (its Back/▼ button) have no target →
  ANR. Touching it also moved focus off the terminal (this device does not multi-resume) → the
  keyboard closes on typing. The translucent theme could not fix focus.
- Fix: deck = `ControlDeckPresentation` (android.app.Presentation of TermuxActivity on display 2,
  FLAG_NOT_FOCUSABLE | FLAG_ALT_FOCUSABLE_IM, Compose with the activity's lifecycle owners). No
  activity on display 2 → launcher stays focused/visible there (keys + keyboard work as without the
  deck). Removed ControlDeckActivity, its manifest entry and Theme.NewTermux.Deck. Interim advice
  to the user: Landscape → Status line until the new build.


**CI:** Build run 41106177559 on 95ff96a2 — success (both jobs; BUILD SUCCESSFUL, no Kotlin w:/e:).
resources.arsc contains Theme.NewTermux.Deck.
**Staged:** `/sdcard/Download/NewTermux-updater-dual-95ff96a2.apk`, sha256
20f1d1bc23606efa9a02e0fdf5208579b686407b82ab95b34fd1382cd21d7b96 (CI = host = device), same cert.

**CHECKPOINT / RESUME POINT (18:35 UTC):** user installs it (ends the session). After `cc -c`:
verify base.apk sha256 == 20f1d1bc… + versionName 1.6.2+95ff96a + crash buffer. Test FIRST: tap
the terminal with the deck up → Gboard visible on the bottom screen over the deck (dumpsys:
launcher task visible=true behind the translucent deck). Then ✕ → status line stays (touching the
terminal must NOT reopen the deck) → dual-screen button reopens it; leave/return reopens it; then
the updater steps (pretend 0.0.0 → dialog → download → CANCEL at the installer; pill).


**Device test round 2 (9edc8ab0 installed 14:16 EDT; verified sha256 13d1f3c3…, versionName
1.6.2+9edc8ab; no NewTermux crash):**
- Deck grid layout as requested (screencap of display 2): New tile = preview size, 4 aligned
  columns, uniform 36dp header buttons, scroll pad right column. OK.
- Events log: deck launched 14:17:51 (394 ms), finished 14:18:00 by app-request right after a tap
  (✕). The Duo pauses/resumes TermuxActivity when focus moves between screens
  (`wm_pause_activity … top-resumed-changed`) → resetting userHidden in onResume would reopen a
  closed deck → moved to onStart.
- BUG (user): "keyboard still does not show up on the second screen like it should on top of the
  app". dumpsys: IME window #21 above deck #22 (ALT_FOCUSABLE_IM works), isVisible=true,
  mInputShown=true, nav bar shows the IME-back ▼ — but nothing drawn. Cause:
  `imeControlTarget in display# 2` = SecondaryDisplayLauncher, which is `visible=false` under the
  opaque deck → never runs the IME show animation. Fix: translucent deck theme (launcher stays
  visible behind it). Making the deck focusable was rejected: Gboard text would go to the deck.


**CI:** Build run 41104339557 on 9edc8ab0 — success (both jobs, no failed steps; job log BUILD
SUCCESSFUL, no Kotlin w:/e: lines). APK checked with dexdump: ControlDeckActivity.onCreate loads
0x00020008 (FLAG_NOT_FOCUSABLE | FLAG_ALT_FOCUSABLE_IM) before addFlags; onStop → finish();
status_btn_deck and ic_dual_screen present.
**Staged:** `/sdcard/Download/NewTermux-updater-dual-9edc8ab0.apk` (apt-android-7 arm64-v8a),
sha256 13d1f3c3a61b378c795b67de08b9d5e334a283777650d5862615022d0d21b72e (CI = host = device),
cert B6:DA:01:48… (same as installed 1.6.2+8a761e9) → in-place update.

**CHECKPOINT / RESUME POINT (18:16 UTC):** user installs it (ends the session). After `cc -c`:
verify base.apk sha256 == 13d1f3c3… + versionName 1.6.2+9edc8ab + crash buffer. Then test:
deck grid (+ New tile = session size, columns line up, header buttons equal), scroll pad on the
right, keyboard (tap the terminal → Gboard shows on the bottom screen ABOVE the deck), switch to
another app → deck closes / bottom screen back → return → deck back, ✕ → status line +
dual-screen button → tap → deck back, Home on the bottom screen → deck closes + status line;
then the updater steps (Updates → Check now, pretend 0.0.0 → dialog → download → CANCEL at the
installer, pill).


**Device test round 1 (8a761e9c installed 14:01 EDT; verified base.apk sha256 3dbb7f7d…,
versionName 1.6.2+8a761e9):**
- Startup update check ran: cache/update_latest.json = v1.6.2 at 14:01, prefs update_last_check
  set; correctly no pill (installed 1.6.2+…). Release asset has a GitHub digest (sha256:8caee7f2…).
  Highlights parse to 4 items; title kept backticks → fixed. (Info-level logcat lines from the app
  are not shown on this ROM, so file evidence was used.)
- Dual screen works (Screenshot_20261030-140244, both screens 1920×2040): top = terminal only,
  bars hidden; deck on the bottom with header, sessions (live preview), commands, keys.
- BUG: scroll pad had zero height (deck content > 480 dp minus the bottom nav bar).
- BUG (user): "my keyboard pops up on the second screen but it wouldn't with that on it".
  Cause: Retroid system setting `ime_show_on_second=1` → IME on display 2
  (`mDisplayIdToShowIme=2`, InputMethod window mDisplayId=2); a FLAG_NOT_FOCUSABLE window is
  Z-ordered above the IME, so the deck covered the keyboard. Also the deck stayed up while the user
  was in Discord on the top screen.
- User: how does the second screen come back after closing it? (was: only by resuming NewTermux)
- User: "+" tile was bigger than the session tiles; wants everything lined up and organized.

**Fix commit e2d02ab:** deck FLAG_ALT_FOCUSABLE_IM (keyboard above the deck); deck closes in
TermuxActivity.onStop and reopens in onResume, and closes itself when covered on the second
screen; status-line button `status_btn_deck` (ic_dual_screen) reopens it; deck on one 4-column
grid (session/new/command tiles one column wide, 8dp gaps, 36dp header buttons, scroll pad right
column, keys full width); highlight titles without backticks; deck refresh before early returns.


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

**CI:** Build run 41099978064 on 8a761e9c — success (both jobs, no failed/skipped steps; job log
BUILD SUCCESSFUL). Checked in the APK: manifest has ControlDeckActivity, UpdateFileProvider with
authority com.termux.updates, deck taskAffinity com.termux.deck; dex has the update/deck classes and
TerminalView.scrollByRows; res/xml/newtermux_update_paths.xml present.
**Staged:** `/sdcard/Download/NewTermux-updater-dual-8a761e9c.apk` (apt-android-7 arm64-v8a),
sha256 3dbb7f7d0ef2d75e6cb5d23cfdd27ef04d64061f50ad9c2acf3ad7bd76a1aac2 (CI list = host = device),
cert B6:DA:01:48… (same as installed 1.6.2+3541ee1) → in-place update.

**CHECKPOINT / RESUME POINT (17:56 UTC):** user installs it (ends the session). After `cc -c`:
verify base.apk sha256 == 3dbb7f7d… and versionName 1.6.2+8a761e9, crash buffer. Test plan:
1. Settings → Updates: installed version, Check now ("up to date (v1.6.2)").
2. Test build → "Pretend this is version 0.0.0" on → Check now → dialog for v1.6.2 (highlights
   from the release notes) → Update now → install-permission prompt if needed → download card →
   Android installer appears → CANCEL there (installing v1.6.2 would remove the test build).
3. Back on the terminal (pretend still on, Dismiss not used): pill top-right below the header.
4. Settings → Appearance → Landscape → Dual screen → back: top = terminal only, bars hidden;
   bottom screen = deck. Try: tabs/previews switch, a drawer command, extra keys incl. CTRL+C,
   scroll pad, AC/mic/settings/+, ✕ → status line returns on top; reopen NewTermux → deck back.
5. Hardware/controller input still goes to the terminal while the deck is up.
6. logcat NT_UPDATE / NT_DECK, crash buffer (ignore Magisk `content` tombstones, gms lowpool).

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
