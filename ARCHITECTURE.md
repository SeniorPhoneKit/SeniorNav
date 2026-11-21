# Architecture

Package `app.phueber.seniornav`. A fixed bar with two big buttons, **Back** and
**Home**, drawn at the bottom of the screen over the system navigation bar, in
every app. It cannot be moved, resized or swiped away. The app's one screen is
for the helper: it says whether the bar is on and how to switch it on.

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
  buttons and PIN pad must work, and while an automation app has paused it
  (see Automation). It stays over the keyboard: in 3-button mode
  the keyboard sits above the system bar, so the bar covers only that bar, and
  Back closes the keyboard like the system button does.
- **Hidden, not removed.** The window is attached once per connection and
  configuration. Hiding makes the bar invisible and the window
  `FLAG_NOT_TOUCHABLE`, so every touch goes to what is below.
- **Minimal events.** The service config asks for `typeWindowsChanged` and
  `typeWindowStateChanged` only, as a cue to re-check the lock screen, and
  `canRetrieveWindowContent` is off: the service cannot read window content or
  the window list. Bursts of events are folded into one check 150 ms after the
  last.
- **Reconfiguration.** On rotation, Display size, font or language change the
  window is rebuilt with the new height and labels, again at the bottom.

## Files

| Class | Role |
|---|---|
| `NavBarService` | The accessibility service. Attaches the bar's window, hides and shows it, presses Back/Home. Listens to window events, screen on/off/unlock broadcasts, and the navigation bar's visibility. |
| `Bar` | The bar's view (Back left, Home right, or swapped by the helper's choice; always the screen's left/right, also in RTL; each half the width, icon plus word, white on near-black) and its window parameters; reads the system navigation bar height. |
| `MainActivity` | The helper's screen: status in words and colour, "paused by automation" with Show the bar again, the button order and the bar colour (hidden while locked), the button to Accessibility settings, the "Allow restricted settings" hint (Android 13+), the navigation mode and the advice to use 3-button navigation. Back at the bottom goes home in one tap. |
| `NavStore` | The settings, `paused`, `back_right` and `light_bar`, in one SharedPreferences file that the service, the screen and the receiver share. |
| `AutomationReceiver` | Exported; BAR_HIDE/BAR_SHOW/BAR_SIDES/BAR_COLOUR write `NavStore`, BACK/HOME go to `NavBarService.perform`. |
| `Automation` | Pure: action names, which command an action is, the `back` extra (`left`/`right`), the `colour` extra (`dark`/`light`). Unit tested. |
| `NavSettings` | Reads the enabled-services setting and the navigation mode; opens Accessibility settings, App info and System navigation (no public action exists, so known AOSP screens are tried, then general Settings). |
| `LauncherLock` | Reads SeniorLauncher's shared lock (`content://app.phueber.seniorlauncher.lock/state`, `locked` 0/1). Locked, the main screen hides its settings buttons. Read-only; unlocking happens on the home screen. |
| `Ui` | Shared buttons and the screen frame, as in the other Senior apps. |
| `Palette` | Pure: the colours by role (the family's Light look plus the bar) and the WCAG contrast check. Unit tested. |
| `BarHeight` | Pure: which height source wins, and the 48dp floor. Unit tested. |
| `NavMode` | Pure: the raw navigation-mode value to 3-button / 2-button / gesture / unknown. Unit tested. |
| `BarStatus` | Pure: on / off / turned on but not running, and parsing of the enabled-services setting. Unit tested. |

Pure classes have JVM tests in `app/src/test`; Android-facing classes have none.

## Automation

Automation apps (Trigly, Tasker, ...) send explicit broadcasts to the exported
`AutomationReceiver`: `BAR_HIDE`, `BAR_SHOW`, `BAR_SIDES` (extra `back` =
`left`/`right`), `BAR_COLOUR` (extra `colour` = `dark`/`light`), `BACK`, `HOME`, all under `app.phueber.seniornav.action.`.

- **Broadcasts**, because they need nothing from the sender (no foreground, no
  overlay permission) and no screen has to appear. Extras are strings, because
  that is all Trigly can send; anything malformed is logged and ignored.
- **Settings through `NavStore`, one signal.** The receiver only writes
  SharedPreferences. The service registers an `OnSharedPreferenceChangeListener`
  while connected: `paused` triggers `refresh()` (hidden = lock screen OR
  paused), `back_right` and `light_bar` rebuild the window. The main screen listens the same
  way while resumed. All three run in one process, so they share one
  SharedPreferences instance and `apply()` notifies at once. Being on disk,
  `paused` survives a service or phone restart; it ends only with BAR_SHOW or
  the screen's button, which is shown even while SeniorLauncher is locked
  (showing the bar only restores the normal state).
- **Back and Home** need the connected service: `NavBarService.perform` uses a
  static reference set on connect and cleared in `stop()`, and drops the
  command (log line) when the service is not running.
- **No permission guard.** Nothing here gives the sender more than it already
  has: any app can start the home screen, and hiding the bar only takes away
  this app's own overlay. A malicious app could pause the bar; the main screen
  says so in words and undoes it in one tap.

## Status on the main screen

"On" means the service is connected in this process (`NavBarService.isRunning`).
If Android lists the service as enabled but it is not connected (it crashed and
Android stopped it, or a battery killer ended it), the screen says "turned on
but not running" and tells the helper to switch it off and on again. The screen
rebuilds on every resume, so it shows the result of what the helper just did in
Settings.

## Permissions

None. An accessibility overlay needs no permission beyond the helper enabling
the service; the service is bound only by the system
(`BIND_ACCESSIBILITY_SERVICE`). No internet. The `<queries>` entry only makes
SeniorLauncher's lock provider visible.

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
  navigation. The button tries the AOSP screen, then falls back to general
  Settings, where the helper searches for "navigation".
- **Not device-tested yet.** Built and unit tested only; the overlay behaviour
  (layering, insets, keyboard, keyguard) still needs a run on real phones.
