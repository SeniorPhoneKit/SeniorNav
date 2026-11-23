# Architecture

Package `app.phueber.seniornav`. A fixed bar with two big buttons, **Back** and
**Home**, drawn at the bottom of the screen over the system navigation bar, in
every app. It cannot be moved, resized or swiped away. The app's one screen is
for the helper: it says whether the bar is on and how to switch it on. With
SeniorLauncher's shared lock it also hides the bar while the helper has
unlocked, and lets the helper lock and unlock with a pattern of volume-button
presses, limit what the volume buttons do while locked (see Volume buttons
and the shared lock) and block pulling down the notifications and Quick
Settings while locked (see Pull-down blocked while locked).

## Design

- **An accessibility service.** `NavBarService` is the only way for an app to
  press Back for another app (`performGlobalAction(GLOBAL_ACTION_BACK)`, and
  `GLOBAL_ACTION_HOME` for Home), and the only way to draw above the system
  navigation bar: its window is `TYPE_ACCESSIBILITY_OVERLAY` (window layer 31,
  above `NAVIGATION_BAR`, 24). A `SYSTEM_ALERT_WINDOW` overlay sits *below* the
  navigation bar and cannot do Back, so it is not used.
- **Over the system buttons, no bigger.** The bar is anchored to the bottom
  edge, full width, and exactly as tall as the system navigation bar
  (`BarHeight`): the inset the window manager reports, else the framework's
  `navigation_bar_height`, never below 48dp. An edge-to-edge app therefore loses
  nothing it did not already give to the system bar. Seniors who need it bigger
  enlarge everything with Display size; icon and word are sized from the bar
  height, not the font scale, so they always fit.
- **3-button navigation is the intended setup.** There the bar sits exactly over
  the system's Back/Home/Recents buttons and takes their taps. With gesture
  navigation the bar still works, but swipes from the bottom edge pass
  underneath it to the system. The main screen reads the mode (`NavMode`, from
  the hidden secure setting `navigation_mode`, else the framework's
  `config_navBarInteractionMode`; best effort) and asks the helper to switch.
- **Out of the way only on the lock screen** (`KeyguardManager`), whose own
  buttons and PIN pad must work, while the screen is off
  (`PowerManager.isInteractive`, the always-on display) or a screensaver runs
  (`ACTION_DREAMING_STARTED`/`STOPPED`, e.g. the charging clock; the screen
  counts as on), while an automation app has paused it (see Automation), and
  while the helper has unlocked SeniorLauncher's lock (see Hidden while
  unlocked).
  The keyguard check alone missed the last two: when they start the lock may
  not be engaged yet, and no event re-checks until the screen wakes.
  It stays over the keyboard: in 3-button mode
  the keyboard sits above the system bar, so the bar covers only that bar, and
  Back closes the keyboard like the system button does.
- **Hidden, not removed.** The window is attached once per connection and
  configuration. Hiding makes the bar invisible and the window
  `FLAG_NOT_TOUCHABLE`, so every touch goes to what is below. If the window
  manager refuses the window, it is tried again after 1 s, 2 s, 4 s, ... at
  most every minute (`AttachRetry`; also at the next `refresh()` once that
  wait is over), never in a tight loop.
- **Minimal events.** The service config asks for `typeWindowsChanged` and
  `typeWindowStateChanged` only, as a cue to re-check the lock screen (and,
while the pull-down is blocked, whether the event came from System UI), and
  `canRetrieveWindowContent` is off: the service cannot read window content or
  the window list. Bursts of events are folded into one check 150 ms after the
  last.
- **Hardware keys, Volume only.** The config also sets
  `canRequestFilterKeyEvents` and `flagRequestFilterKeyEvents`: the service sees
  every hardware key press before apps do, and may swallow it. That is what the
  volume pattern and the locked volume behaviour need; `onKeyEvent` hands
  Volume Up/Down to `VolumeGuard` and returns false (untouched) for every other
  key. The flag is cleared at runtime (`setServiceInfo`) while no pattern is set
  and the buttons work normally, so then the service sees no key at all. The
  capability itself is declared statically, so Android's dialog when the
  helper turns the service on now warns that the app can observe what is typed;
  the service description says it only notices the volume buttons.
- **Reconfiguration.** On rotation, Display size, font or language change the
  window is rebuilt with the new height and labels, again at the bottom.

## Files

| Class | Role |
|---|---|
| `NavBarService` | The accessibility service. Attaches the bar's window, hides and shows it, presses Back/Home. Listens to window events and screen on/off/unlock and screensaver start/stop broadcasts (to hide over the lock screen, the screen-off display and a screensaver), to the settings, to SeniorLauncher's lock uri and package changes (to hide while unlocked), and passes volume key events to `VolumeGuard`. Switches key filtering on only while needed. |
| `VolumeGuard` | The volume buttons: spots the pattern, undoes its volume changes, locks/unlocks through `LauncherLock`, buzzes and toasts; passes, limits or swallows presses while locked; watches volume and ringer-mode changes and brings them back within the limits. Thin glue over the pure classes below. |
| `VolumePattern` | Pure: what a valid pattern is (4-6 presses, both directions), the arrows shown, and the matcher (timing, stray presses, reset). Unit tested. |
| `VolumeKeys` | Pure: the locked modes (normal / limits / off), the decision for one press (pass / limit / swallow, always pass during a call), when limits are enforced and when keys are needed at all. Unit tested. |
| `VolumeLimits` | Pure: a stream's lowest and highest allowed step, clamping, whether a step stays in range, and moving one limit without crossing the other. Unit tested. |
| `PullDown` | Pure: when blocking the pull-down applies (setting, locked, not on the keyguard, screen on, no screensaver), whether the lock is in use (the screen's one line), the strip's height (status bar inset, cutout, `status_bar_height`, 24dp) and the gap between two closes of the shade. Unit tested. |
| `LockState` | Pure: SeniorLauncher's lock as read (answered, new enough, locked, PIN set) and whether the bar steps aside while unlocked. Unit tested. |
| `Bar` | The bar's view (Back left, Home right, or swapped by the helper's choice; always the screen's left/right, also in RTL; each half the width, icon plus word; dark bar with white, or light bar with near-black and a grey top line) and its window parameters; reads the system navigation bar height. Also the pull-down strip's window parameters and the status bar height. |
| `MainActivity` | The helper's screen: status in words and colour ("hidden while unlocked" too), "paused by automation" with Show the bar again, the button order, the bar colour, hide while unlocked, block the pull-down while locked, the volume pattern (record, show, remove), the locked volume-button mode and the limits with the Do Not Disturb hint (all hidden while locked), the button to Accessibility settings, the "Allow restricted settings" hint (Android 13+), the navigation mode and the advice to use 3-button navigation, and Backup (save to / load from a file, hidden while locked). Back at the bottom goes home in one tap (hidden while the bar is on, see `NavBar`). |
| `NavStore` | The settings, `paused`, `back_right`, `light_bar`, `hide_unlocked`, `block_pulldown`, `pattern`, `volume_keys` and the limits (`ring_min`/`ring_max`, `media_min`/`media_max`, `alarm_min`), in one SharedPreferences file that the service, the screen and the receiver share. |
| `AutomationReceiver` | Exported; BAR_HIDE/BAR_SHOW/BAR_SIDES/BAR_COLOUR/BAR_UNLOCKED/VOLUME_KEYS/VOLUME_LIMIT/PATTERN_REMOVE/PULLDOWN write `NavStore`, BACK/HOME go to `NavBarService.perform`. BACK/HOME and BAR_HIDE are rate-limited (`AutomationLimit`). |
| `AutomationLimit` | Pure: at most one automation action a second, six a minute, then a two-minute cooldown. Unit tested. |
| `Automation` | Pure: action names, which command an action is, the `back` extra (`left`/`right`), the `colour` extra (`dark`/`light`), on/off, the `stream` extra and the volume steps. Unit tested. |
| `NavSettings` | Reads the enabled-services setting and the navigation mode; opens Accessibility settings, App info and System navigation (no public action exists, so known AOSP screens are tried, then general Settings). |
| `LauncherLock` | Reads SeniorLauncher's shared lock (`content://app.phueber.seniorlauncher.lock/state`, `locked` 0/1, `has_pin` 0/1 in newer versions) as a `LockState`. Locked, the main screen hides its settings buttons. Locks and unlocks for the pattern (`call` "nav_lock"/"nav_unlock"); there is no other way to change the lock from here. |
| `Ui` | Shared buttons and the screen frame, as in the other Senior apps. |
| `NavBar` | Whether the senior can rely on the bar (`NavBarService.barVisible`, the rule `BarProvider` publishes; also true while it is hidden because the helper unlocked). While it is, the pinned Back button is hidden, since the bar already has Back and Home; shown again while it is paused or not running. Re-checked whenever a screen gets focus and on every `BarProvider` change while attached. |
| `BarProvider` | Exported, read-only: tells the other Senior apps whether they need no Back of their own (see Bar state for other apps). |
| `NavBackup` | Pure: the saved settings (sides, colour, hide while unlocked, block pull-down, pattern, volume-key mode, limits) to and from a backup entry, validated with `VolumePattern` and `VolumeKeys`. Unit tested. |
| `NavSettingsBackup` | Exports this app's backup file from `NavStore` and restores one into it (refused while locked). Used by the screen and `BackupProvider`. |
| `BackupProvider` | Exported, `call` only: "export" and "import" for SeniorLauncher's all-apps backup; answers only SeniorLauncher and this app. |
| `Backup`, `Json` | Pure: the family's backup file (envelope, version check, typed reads) and a small JSON reader/writer; the same in every Senior app. Unit tested. |
| `BackupFiles` | Saves and reads the file through the system file picker (Storage Access Framework). |
| `AttachRetry` | Pure: the growing wait before attaching a refused window again. Unit tested. |
| `Palette` | Pure: the colours by role (the family's Light look, the daylight twin of SeniorLauncher's Dark: blue-grey page #E8EDF4, white cards, navy text #14213D, soft navy `OUTLINE` #6F82A3 for outlined buttons and Back; plus the bar, which keeps its own colours) and the WCAG contrast check. Unit tested. |
| `BarHeight` | Pure: which height source wins, and the 48dp floor. Unit tested. |
| `NavMode` | Pure: the raw navigation-mode value to 3-button / 2-button / gesture / unknown. Unit tested. |
| `BarStatus` | Pure: on / off / turned on but not running, and parsing of the enabled-services setting. Unit tested. |

Pure classes have JVM tests in `app/src/test`; Android-facing classes have none.

## Automation

Automation apps (Trigly, Tasker, ...) send explicit broadcasts to the exported
`AutomationReceiver`: `BAR_HIDE`, `BAR_SHOW`, `BAR_SIDES` (extra `back` =
`left`/`right`), `BAR_COLOUR` (extra `colour` = `dark`/`light`), `BACK`, `HOME`,
`BAR_UNLOCKED` (extra `hide` = `on`/`off`), `VOLUME_KEYS` (extra `mode` =
`normal`/`limits`/`off`), `VOLUME_LIMIT` (extra `stream` =
`ring`/`media`/`alarm`, `min` and/or `max` in the stream's steps, `max` also
`top`; the alarm has no max), `PATTERN_REMOVE`, `PULLDOWN` (extra `block` =
`on`/`off`), all under
`app.phueber.seniornav.action.`. There is deliberately no command that unlocks,
and none that sets a pattern (which would be an unlock path any app could
plant). The new settings commands are not rate-limited, like `BAR_SIDES`:
none of them can keep anyone out of Settings. A hostile app could set
annoying limits (ring volume capped low while locked), but any app can set
the volume itself anyway.

- **Broadcasts**, because they need nothing from the sender (no foreground, no
  overlay permission) and no screen has to appear. Extras are strings, because
  that is all Trigly can send; anything malformed is logged and ignored.
- **Settings through `NavStore`, one signal.** The receiver only writes
  SharedPreferences. The service registers an `OnSharedPreferenceChangeListener`
  while connected: `paused` and `hide_unlocked` trigger `refresh()` (hidden = screen off OR
  screensaver OR lock screen OR paused OR hidden while unlocked), `back_right`
  and `light_bar` rebuild the window, `pattern` and `volume_keys` switch key
  filtering, `volume_keys` and the limits re-apply the limits. The main screen listens the same
  way while resumed. All three run in one process, so they share one
  SharedPreferences instance and `apply()` notifies at once. Being on disk,
  `paused` survives a service or phone restart; it ends only with BAR_SHOW or
  the screen's button, which is shown even while SeniorLauncher is locked
  (showing the bar only restores the normal state).
- **Back and Home** need the connected service: `NavBarService.perform` uses a
  static reference set on connect and cleared in `stop()`, and drops the
  command (log line) when the service is not running.
- **No permission guard, by decision.** Trigly and Tasker must work without
  setup, so any installed app can send every command. That lends it this
  service's power: a plain app cannot press Back in another app, nor bring up
  the home screen while it is in the background, but through `BACK`/`HOME` it
  can. Unbounded, a hostile app could loop them and keep the senior and the
  helper out of Settings, so it could not even be uninstalled. It could also
  keep pausing the bar, or rebuild it over and over by switching sides or
  colour back and forth.
- **Limits.** `BACK` and `HOME` share one `AutomationLimit`: at most one per
  second and six in any minute; the next one starts a two-minute cooldown in
  which all are ignored. A flood therefore gets six presses, then the phone is
  left alone long enough to reach Settings; a rule that fires when an app opens
  or closes never comes near it. `BAR_HIDE` has its own budget with the same
  numbers, counting only hides that hide (a hide while paused is a no-op), so
  the helper's "Show the bar again" wins; the main screen says "paused by
  automation" in words. `NavStore.setPaused` does nothing when the value is
  unchanged, so repeated identical commands do not wake the other apps'
  observers; `back_right` and `light_bar` need no such check, because
  SharedPreferences notifies only on a real change. The bar's own buttons and
  the main screen are never limited. The limits live in memory and start over
  with the process. Not bounded: `BAR_SIDES`/`BAR_COLOUR` alternating values
  (each rebuilds the window; a nuisance, not a lock-out).

## Status on the main screen

"On" means the service is connected in this process (`NavBarService.isRunning`)
and its window is attached (`isAttached`). If Android lists the service as
enabled but it is not connected (it crashed and Android stopped it, or a battery
killer ended it), or its window was refused, the screen says "turned on but not
running" and tells the helper to switch it off and on again. The screen
rebuilds on every resume, so it shows the result of what the helper just did in
Settings, and while open on every change of the settings, of SeniorLauncher's
lock (a `ContentObserver` on its lock uri) and of the bar's state (on
`BarProvider`'s uri). While the bar is hidden because the helper unlocked, the
status says so in words ("on, hidden while unlocked"), in green: nothing is
wrong. The lock query stays on the main thread: it is one row
from a local provider, and an asynchronous read would show the unlocked
buttons for a moment.

## Bar state for other apps

`BarProvider` answers `content://app.phueber.seniornav.bar/state` with one row,
column `visible`. Despite the name it means "the senior does not need the
apps' own Back": 1 while the service runs, its window is attached and
automation has not paused it, else 0 (the lock screen does not count: no app's
screen shows there). It stays 1 while the bar is hidden because the helper
unlocked: the helper then uses the phone's own buttons, which the hidden bar
no longer covers, and the apps' Back would only clutter the screen (decided by
the owner). The column keeps its name, so the sibling apps need no change. Every change of these notifies that uri (attach, detach,
connect, stop, `NavStore.setPaused`). SeniorLauncher, SeniorPhone and
SeniorAlert hide their pinned Back while it is 1 and watch the uri while their
Back is attached, so a pause by automation brings their Back back at once;
without SeniorNav (or an older one) they fall back to the enabled-services
list. insert/update/delete throw. No permission, like SeniorLauncher's lock
provider: the state is visible on screen anyway.

## Hidden while unlocked

SeniorLauncher's lock is the helper's switch between "the senior's phone" and
"the helper is working on it". While the lock is in use (SeniorLauncher has a
PIN, `has_pin` 1) and open, the bar steps aside like on the lock screen
(invisible and `FLAG_NOT_TOUCHABLE`), so the helper uses the phone's own
buttons; locking (PIN screen, pattern, automation) brings it back at once.
`NavBarService` caches the lock as a `LockState`, re-read on every change of
the lock uri (a `ContentObserver`), on screen-on, and when SeniorLauncher is
installed, updated, removed or its data cleared (those do not notify the uri;
without this a bar hidden "while unlocked" would stay hidden after
SeniorLauncher is gone). Without SeniorLauncher, with an older one (no
`has_pin` column) or before a PIN was ever set, nothing changes: a phone that
never uses the lock keeps its bar. A helper setting, `hide_unlocked`, default
on; automation `BAR_UNLOCKED`.

## Pull-down blocked while locked

A helper setting, `block_pulldown`, default off (new behaviour, opt-in);
automation `PULLDOWN`. While it is on and SeniorLauncher is locked, the
senior cannot pull down the notifications or Quick Settings (and with them
Wi-Fi, flight mode, Settings).

- **When** (`PullDown.blocks`): the setting is on, SeniorLauncher answered and
  is locked, and the phone is in use: not on the phone's own lock screen
  (whose shade must stay as it is), not with the screen off, not during a
  screensaver; the same checks and events as the bar. Unlocked, or without
  SeniorLauncher, nothing happens; the main screen then says in one line that
  it needs a PIN on the home screen. `BAR_HIDE` does not pause it: that pauses
  the bar, not the lock.
- **The strip.** A second `TYPE_ACCESSIBILITY_OVERLAY` window, full width at
  the top, exactly as tall as the status bar (`PullDown.stripHeight`: the
  status-bar inset ignoring visibility or the cutout, whichever is deeper,
  else `status_bar_height`, else 24dp), cutout mode `ALWAYS`. It draws
  nothing (the status bar icons stay visible) and swallows every touch in it,
  so a swipe that starts there never reaches System UI. It is added only
  while blocking applies and removed otherwise (not just made untouchable:
  with the setting off there is no window at all), and rebuilt on a
  configuration change, like the bar. Taps just below it reach the app (a
  toolbar right under the status bar works). If the window manager refuses
  it, it is not tried again until the setting changes, a rotation or a
  reconnect; the safety net still works.
- **The safety net.** If the shade opens anyway (a launcher's "swipe down
  anywhere", a heads-up notification pulled down, `cmd statusbar`), any
  window event from `com.android.systemui` while blocking applies closes it
  with `GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE` (Android 12+), as does the
  moment blocking starts. The service cannot tell the shade from other System
  UI windows (volume panel, power menu): it reads only the event's package,
  no text or window list, so the accessibility config is unchanged. That is
  safe because the action only collapses the shade and does nothing when it
  is closed (the volume panel stays). At most one close every 300 ms
  (`PullDown.CLOSE_GAP_MS`), a later request is done once the gap is over,
  so it can never fight in a tight loop; unlocking ends it at once. Below
  Android 12 there is no such action, and Back would be a guess (it would
  also go Back in the app when the event was not the shade), so there only
  the strip blocks.

## Volume buttons and the shared lock

All of it is set up by the helper on the main screen, hidden while locked, and
off by default. Only SeniorLauncher's lock is involved: SeniorNav has no lock
of its own.

- **The pattern** is 4 to 6 presses of Volume Up/Down, stored as `U`/`D`. It
  must contain both directions: four quick Volume Up presses are what a senior
  does to make something louder. Timing (`VolumePattern`): at most 1 s between
  two presses and 3 s from the first to the last; a slower press starts over,
  and presses that cannot start the pattern are dropped from the front, so a
  stray press before it does not spoil it. Recorded on the main screen: the
  activity takes the presses itself (`onKeyDown`, no volume change) while
  `VolumeGuard.recording` keeps the service from matching the old pattern.
- **A match toggles the lock** through SeniorLauncher's provider:
  `call("nav_lock")` (locks with the stored PIN) or `call("nav_unlock")`
  (no PIN). SeniorLauncher answers only the calling package
  `app.phueber.seniornav` and refuses to lock until a PIN was set once
  (`no_pin`); the main screen says "Set a PIN on the home screen first", and a
  pattern then shows that as a toast. Without SeniorLauncher (or an older one)
  the section says it needs it. Feedback: a 150 ms buzz and a toast
  "Locked"/"Unlocked".
- **The buttons never feel sluggish.** Pattern presses go to the system at
  once, so the volume moves as usual. Before every press `VolumeGuard` notes
  every stream's volume and the ringer mode; on a match the completing press
  is swallowed and everything is put back to what it was before the
  pattern's first press (and once more 400 ms later, in case the system
  applied an earlier press late). Presses that do not complete a pattern stay
  ordinary presses.
- **Locked, the buttons** work normally (default), stay within the limits, or
  do nothing (swallowed; the pattern still works). The mode applies only
  while locked; unlocked everything is normal. A press and its release always
  go the same way; auto-repeat of a held key follows its first press.
- **Volume limits** (only in "Stay within the limits", while locked): lowest
  and highest step for ring and media, lowest for the alarm, in the stream's
  own steps (stored absolute, clamped to what the phone has; "highest" at the
  top is stored as "no maximum"). A press is let through only if the step
  stays in range. Android does not say which stream a press will change: with
  music playing it is media, otherwise media or ring depending on the phone,
  so the press is swallowed (the volume panel is shown unchanged) only when it
  would leave the range on every candidate. Every volume change, from the
  panel's slider, Settings or another app, is watched
  (`android.media.VOLUME_CHANGED_ACTION`, not in the SDK but sent by every
  version, and `RINGER_MODE_CHANGED_ACTION`) and brought back into range, as
  is everything at the moment the lock comes on.
- **Ringer mode.** With a ring minimum above 0, vibrate is switched back to
  normal (allowed without permission); silent only with Do Not Disturb
  access (switching out of silent counts as a Do Not Disturb change), which
  the main screen asks the helper for with a button to the settings. Without
  it silent stays silent. While the ringer is not normal the ring volume is
  left alone (raising it there would fight the ringer mode).
- **Calls.** While the phone rings or a call is on (`AudioManager.getMode()`
  is not `MODE_NORMAL`: ringtone, in call, in communication such as WhatsApp,
  call screening; no permission needed) presses pass untouched and do not
  count toward the pattern. While it rings Android takes the press itself to
  silence the ringer before the service sees it (checked on the emulator).
- **Both keys held** is Android's accessibility shortcut, detected by the
  window manager before the service sees the keys; a press while the other
  key is held resets the pattern and is otherwise handled as usual.

## Backup

The helper can save the settings to a file and load them back after a reset
or on a new phone. The file is the Senior family's backup format (`Backup`):
JSON, `{"format": "seniorphonekit-backup", "version": 1, "created": …,
"apps": {"app.phueber.seniornav": {"appVersion": …, "settings": {…}}}}`.
SeniorLauncher's "all Senior apps" file has the same shape with one entry per
app; this app takes only its own entry from either.

- **What is saved** (`NavBackup`): `backRight`, `lightBar`,
  `hideWhileUnlocked`, `blockPullDown` (missing in older files: off), `pattern` (absent when none), `volumeKeys`
  (`normal`/`limits`/`off`) and `limits` per stream (`min`, and `max` for ring
  and media, absent for "up to the top"). Not saved: whether the service is
  on (Android's switch, which the status card asks for on a new phone),
  `paused` (a moment's state from automation) and any lock or PIN, which is
  SeniorLauncher's.
- **Files.** Through the system file picker (`ACTION_CREATE_DOCUMENT` /
  `ACTION_OPEN_DOCUMENT`), so no storage permission; the helper chooses where.
  Suggested name `SeniorNav-settings-<date>.json`.
- **Restore is a replace**, after "Replace the current settings with the ones
  from the file?" asked in place on the screen. The whole entry is read and
  checked first (a newer `version`, a damaged file, an invalid pattern, an
  unknown mode or a maximum below the minimum refuse it, with nothing
  changed), then written to `NavStore` in one edit, so the service and the
  screen follow through their usual listeners. Missing fields take the
  defaults.
- **Locked.** The Backup section is hidden while SeniorLauncher is locked, a
  restore is refused then (`locked`), and a lock that comes while the picker
  is open drops the result.
- **For SeniorLauncher.** `BackupProvider`
  (`content://app.phueber.seniornav.backup`, `call` only): `export` returns
  `json`, this app's file; `import` (the file as `arg`) returns `ok` and
  `skipped` (always empty here: nothing in these settings depends on the
  phone), or `error` (`malformed`, `newer_version`, `no_entry`, `locked`,
  `not_allowed`). Only the calling packages `app.phueber.seniorlauncher` and
  this app are answered (`Backup.callerAllowed`): the pattern unlocks the
  phone, so other apps should not read it. A sanity check, not security, like
  the lock itself.
- **No automation command:** saving and loading go through the file picker,
  where the helper chooses the file, and a restore only after a confirm.

## Permissions

An accessibility overlay needs no permission beyond the helper enabling the
service; the service is bound only by the system (`BIND_ACCESSIBILITY_SERVICE`).
No internet. Two normal permissions: `VIBRATE` (the buzz when the pattern locks
or unlocks) and `ACCESS_NOTIFICATION_POLICY`, which only lists the app under
Do Not Disturb access, so the helper can grant it for the ring minimum (see
Volume buttons and the shared lock); the helper grants it in Settings, never
automatically. The `<queries>` entry only makes
SeniorLauncher's lock provider (and with it its package broadcasts) visible. `BarProvider` is exported without a
permission (see Bar state for other apps); `BackupProvider` too, checking the caller instead (see Backup).

## Known limits

- **Gesture navigation.** The bar is designed for 3-button navigation. In gesture
  mode it is drawn over the gesture area (48dp, more than the gesture bar), and
  swipes from the bottom edge still pass underneath to the system.
- **Landscape.** The bar stays at the bottom edge in landscape, while on phones
  the system navigation bar moves to the side. Simplest and predictable; the
  system buttons on the side remain usable.
- **Keyboard.** The bar stays. The system bar's "close keyboard" and keyboard
  switcher buttons are covered; Back closes the keyboard instead, and switching
  keyboards is a helper task in Settings.
- **Full-screen video and games: the bar stays** and covers the bottom of the
  picture. Android has no direct API for "the app in front hid the navigation
  bar". The bar window's own insets do not work: on the Android 15 emulator an
  accessibility overlay is told `navigationBars()` is not visible while the
  system bar is plainly on screen, so the bar hid itself for good. The window
  list does not work either: a window fully covered by the bar (the system
  navigation bar itself) is dropped from it.
- **OEM battery killers.** Some makers (Xiaomi, Huawei, Samsung, Oppo, ...) stop
  background apps, including accessibility services, or switch them off after
  an update. The main screen then says "turned on but not running". The helper
  may have to exempt the app from battery optimisation in the maker's settings.
- **Restricted settings.** On Android 13+, an app installed outside an app store
  cannot be turned on as an accessibility service until the helper allows it:
  App info, ⋮, "Allow restricted settings". The main screen explains this and
  opens App info.
- **Play accessibility declaration.** Google Play allows the AccessibilityService
  API for apps whose purpose is to help people with disabilities. The service
  sets `android:isAccessibilityTool="true"` (Android 12+), and its description
  (English and German) says what it is for: a navigation aid for seniors and
  people with cognitive or motor impairments, that reads no screen content.
  A Play release would still need the Accessibility API declaration form and a
  prominent disclosure; the app is distributed as an APK today.
- **Navigation settings shortcut.** There is no public intent for System
  navigation. The button tries the AOSP screen, then the
  `GESTURE_NAVIGATION_SETTINGS` action pinned to `com.android.settings` (a
  non-public action left open could be claimed by any app with a fake settings
  screen), then falls back to general Settings, where the helper searches for
  "navigation".
- **Pull-down.** Phones whose shade opens from other places than the status
  bar (some launchers and makers: swipe down anywhere on the home screen,
  Samsung's or Xiaomi's own gestures) are caught only by the safety net, so
  the shade may flash open for a moment; below Android 12 they are not
  caught at all. While an app hides the status bar (full-screen video) the
  strip still covers the top edge of the picture and takes its touches there.
  A maker's System UI that does not honour the dismiss action keeps the shade
  open. Checked on the Android 15 emulator only.
- **Volume keys only while the screen is on.** With the screen off Android
  handles the volume keys before accessibility services see them (checked on
  the emulator): no pattern, no limits for presses, no "Do nothing". Changes
  made that way are still brought back within the limits by the watcher.
- **Not enforceable.** Do Not Disturb itself; the call (voice) volume; volume
  that an app sets for its own playback; notification and system volume
  (they follow the ring on most phones); the ringer coming out of silent
  without Do Not Disturb access. A press that slips through when Android
  picks a stream other than the one checked is corrected afterwards, so the
  panel may show the out-of-range step for a moment.
- **Injected keys.** `adb shell input keyevent` bypasses the accessibility key
  filter, so neither the pattern nor the modes react to it; real hardware
  presses do (see CONTRIBUTING.md).
- **Not device-tested yet.** Built and unit tested, and the volume buttons,
  limits and hiding while unlocked tried on the Android 15 emulator; the
  overlay behaviour (layering, insets, keyboard, keyguard) and the volume
  keys of real phones (OEM volume panels, which stream a press changes) still
  need a run on real phones.
