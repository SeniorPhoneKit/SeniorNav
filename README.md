# SeniorNav

Big Back and Home buttons for Android, made for seniors.

A fixed bar at the bottom of the screen with two big buttons, **Back** and
**Home**, in every app (WhatsApp, the camera, the browser, ...). It lies over
the phone's own small navigation buttons. It cannot be moved or swiped away by
accident.

English and German. The app follows the phone's language.

[Deutsch weiter unten](#seniornav-deutsch)

## What you see

At the bottom of the screen, a dark bar:

- **← Back** on the left: one step back, like the phone's own Back button.
- **⌂ Home** on the right: straight to the home screen.

The bar steps aside on the lock screen. With the keyboard open, Back closes it.

## Install

1. Download the latest `SeniorNav-*.apk` from the
   [Releases](https://github.com/philipphueber/SeniorNav/releases) page on the
   phone.
2. Open it. Android asks whether to allow installing from this source; allow it
   once.

Needs Android 8.0 or newer.

## Setting it up (for the helper)

1. Open **Back & Home**. It says whether the bar is on.
2. Tap **Open accessibility settings**, find **Back & Home** (often under
   "Downloaded apps"), open it and switch it on. Android asks for permission;
   allow it.
3. On Android 13 and newer, Android may say **"Restricted setting"**. Then go
   back to the app, tap **Open App info**, tap **⋮** at the top right, choose
   **Allow restricted settings**, and do step 2 again.
4. Switch the phone to **3-button navigation** (the app shows which mode is on
   and has a button for it; otherwise search Settings for "navigation"). The bar
   then sits exactly over the phone's own buttons. With swipe navigation, a swipe
   from the bottom edge still goes past the bar.
5. If the buttons are still too small, make everything bigger in Settings >
   Display > **Display size**.
6. Under **Button order**, choose **Back on the left** (the default) or **Back
   on the right**. The bar changes at once.

If the app later says **"turned on but not running"**, Android has stopped it:
switch it off and on again in accessibility settings. Some phone makers stop
apps to save battery; then also allow the app to run in the background in the
phone's battery settings.

When SeniorLauncher is installed and locked, this app hides its settings
buttons; unlock on the home screen to change them.

## Good to know

- In landscape the bar stays at the bottom, also on phones whose own buttons
  move to the side.
- During full-screen videos the bar stays and covers the bottom edge of the
  picture. An automation app can pause it meanwhile (see below).

## Automation (Trigly, Tasker, …)

Other apps can control the bar, for example Trigly or Tasker: hide it while a
video app is open and show it again afterwards. Each command is a broadcast to
the package `app.phueber.seniornav`; extras are text.

| Send mode | Action | Extras | What it does |
|---|---|---|---|
| Broadcast | `app.phueber.seniornav.action.BAR_HIDE` | none | Pauses the bar: hidden until `BAR_SHOW`, also after a restart |
| Broadcast | `app.phueber.seniornav.action.BAR_SHOW` | none | Shows the paused bar again |
| Broadcast | `app.phueber.seniornav.action.BAR_SIDES` | `back` = `left` or `right` | Puts Back on that side, Home on the other |
| Broadcast | `app.phueber.seniornav.action.BACK` | none | Presses Back (only while the bar's service is on) |
| Broadcast | `app.phueber.seniornav.action.HOME` | none | Presses Home (only while the bar's service is on) |

While the bar is paused, the app's screen says **"paused by automation"** and
has a button **Show the bar again**, also while SeniorLauncher is locked. On the
lock screen the bar is hidden anyway. Other values are ignored.

**Example (Trigly):** trigger *App opened: YouTube*, action *Fire intent*, send
mode *Broadcast*, action `app.phueber.seniornav.action.BAR_HIDE`, package
`app.phueber.seniornav`. A second rule with *App closed: YouTube* sends
`app.phueber.seniornav.action.BAR_SHOW`.

## Privacy

The app has no internet access at all (it does not even ask for it), no
account, no tracking and no ads. It uses Android's accessibility service only
to press Back and Home and to notice when the lock screen shows. It never reads what is on the screen.

## Contributing and licence

Building, technical details and how to help: [CONTRIBUTING.md](CONTRIBUTING.md)
and [ARCHITECTURE.md](ARCHITECTURE.md). Licensed under the
[Apache License 2.0](LICENSE).

---

# SeniorNav (Deutsch)

Große Tasten für Zurück und Start auf Android, gemacht für Senioren.

Eine feste Leiste unten auf dem Bildschirm mit zwei großen Tasten, **Zurück**
und **Start**, in jeder App (WhatsApp, Kamera, Browser, ...). Sie liegt über den
kleinen Navigationstasten des Telefons. Sie lässt sich nicht aus Versehen
verschieben oder wegwischen.

Deutsch und Englisch. Die App folgt der Sprache des Telefons.

## Was man sieht

Unten auf dem Bildschirm eine dunkle Leiste:

- **← Zurück** links: einen Schritt zurück, wie die eigene Zurück-Taste.
- **⌂ Start** rechts: direkt zum Startbildschirm.

Auf dem Sperrbildschirm macht die Leiste Platz. Bei offener Tastatur schließt
Zurück die Tastatur.

## Installieren

1. Die neueste `SeniorNav-*.apk` auf der Seite
   [Releases](https://github.com/philipphueber/SeniorNav/releases) auf dem
   Telefon herunterladen.
2. Öffnen. Android fragt, ob Installationen aus dieser Quelle erlaubt werden
   sollen; einmal erlauben.

Benötigt Android 8.0 oder neuer.

## Einrichten (für die helfende Person)

1. **Zurück & Start** öffnen. Die App zeigt, ob die Leiste an ist.
2. **Bedienungshilfen öffnen** antippen, **Zurück & Start** suchen (oft unter
   „Heruntergeladene Apps“), öffnen und einschalten. Android fragt nach der
   Erlaubnis; erlauben.
3. Ab Android 13 meldet Android eventuell **„Eingeschränkte Einstellung“**. Dann
   zurück in die App, **App-Info öffnen** antippen, oben rechts auf **⋮** tippen,
   **Eingeschränkte Einstellungen zulassen** wählen und Schritt 2 wiederholen.
4. Das Telefon auf die **Bedienung über 3 Schaltflächen** umstellen (die App
   zeigt, welche Art an ist, und hat eine Taste dafür; sonst in den Einstellungen
   nach „Navigation“ suchen). Dann liegt die Leiste genau über den eigenen Tasten.
   Bei der Bedienung über Gesten geht ein Wisch vom unteren Rand weiter an der
   Leiste vorbei.
5. Sind die Tasten noch zu klein, in Einstellungen > Display die
   **Anzeigegröße** erhöhen.
6. Unter **Reihenfolge der Tasten** **Zurück links** (Standard) oder **Zurück
   rechts** wählen. Die Leiste ändert sich sofort.

Zeigt die App später **„eingeschaltet, läuft aber nicht“**, hat Android sie
angehalten: in den Bedienungshilfen aus- und wieder einschalten. Manche
Hersteller beenden Apps, um Akku zu sparen; dann in den Akku-Einstellungen des
Telefons zusätzlich erlauben, dass die App im Hintergrund läuft.

Ist SeniorLauncher installiert und gesperrt, blendet diese App ihre
Einstellungstasten aus; zum Ändern auf dem Startbildschirm entsperren.

## Gut zu wissen

- Im Querformat bleibt die Leiste unten, auch auf Telefonen, deren eigene Tasten
  dann an die Seite wandern.
- Bei Videos im Vollbild bleibt die Leiste und verdeckt den unteren Bildrand.
  Eine Automatisierungs-App kann sie solange pausieren (siehe unten).

## Automatisierung (Trigly, Tasker, …)

Andere Apps können die Leiste steuern, zum Beispiel Trigly oder Tasker: sie
ausblenden, solange eine Video-App offen ist, und danach wieder einblenden.
Jeder Befehl ist ein Broadcast an das Paket `app.phueber.seniornav`; Extras sind
Text.

| Sendeart | Aktion | Extras | Was passiert |
|---|---|---|---|
| Broadcast | `app.phueber.seniornav.action.BAR_HIDE` | keine | Pausiert die Leiste: ausgeblendet bis `BAR_SHOW`, auch nach einem Neustart |
| Broadcast | `app.phueber.seniornav.action.BAR_SHOW` | keine | Zeigt die pausierte Leiste wieder |
| Broadcast | `app.phueber.seniornav.action.BAR_SIDES` | `back` = `left` oder `right` | Legt Zurück auf diese Seite, Start auf die andere |
| Broadcast | `app.phueber.seniornav.action.BACK` | keine | Drückt Zurück (nur, wenn der Dienst der Leiste an ist) |
| Broadcast | `app.phueber.seniornav.action.HOME` | keine | Drückt Start (nur, wenn der Dienst der Leiste an ist) |

Solange die Leiste pausiert ist, sagt der Bildschirm der App **„durch
Automatisierung pausiert“** und hat eine Taste **Leiste wieder zeigen**, auch
wenn SeniorLauncher gesperrt ist. Auf dem Sperrbildschirm ist die Leiste ohnehin
ausgeblendet. Andere Werte werden ignoriert.

**Beispiel (Trigly):** Auslöser *App geöffnet: YouTube*, Aktion *Intent senden*,
Sendeart *Broadcast*, Aktion `app.phueber.seniornav.action.BAR_HIDE`, Paket
`app.phueber.seniornav`. Eine zweite Regel mit *App geschlossen: YouTube* sendet
`app.phueber.seniornav.action.BAR_SHOW`.

## Datenschutz

Die App hat keinerlei Internetzugang (sie fragt nicht einmal danach), kein
Konto, kein Tracking und keine Werbung. Sie nutzt den Bedienungshilfe-Dienst von
Android nur, um Zurück und Start zu drücken und um zu bemerken, wann der
Sperrbildschirm gezeigt wird. Sie liest nie, was auf dem Bildschirm steht.
