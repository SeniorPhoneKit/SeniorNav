package app.phueber.seniornav;

/**
 * When the bar is shown. Pure, so every rule is unit tested.
 *
 * - Never over the lock screen: its own buttons and the PIN pad must work, and
 *   Home there would be meaningless.
 * - Never while a keyboard is up: in 3-button mode the system bar then shows
 *   its own "close keyboard" button, and the bar must not cover the bottom
 *   keyboard row.
 *
 * Full-screen video is not detected: an accessibility overlay is told the
 * navigation bar is hidden even when it is not (seen on Android 15), so that
 * signal would hide the bar for good. See ARCHITECTURE.md.
 */
final class BarRules {
    private BarRules() {}

    static boolean show(boolean keyguardLocked, boolean inputMethodShown) {
        return !keyguardLocked && !inputMethodShown;
    }
}
