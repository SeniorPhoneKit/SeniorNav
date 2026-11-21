package app.phueber.seniornav;

/**
 * When the bar is shown. Pure, so every rule is unit tested.
 *
 * - Never over the lock screen: its own buttons and the PIN pad must work, and
 *   Home there would be meaningless.
 * - Never while a keyboard is up: in 3-button mode the system bar then shows
 *   its own "close keyboard" button, and the bar must not cover the bottom
 *   keyboard row.
 * - Not while the system navigation bar is hidden (full-screen video, games).
 *   Best effort: see {@link #navBarHidden}.
 */
final class BarRules {
    private BarRules() {}

    static boolean show(boolean keyguardLocked, boolean inputMethodShown, boolean navBarHidden) {
        return !keyguardLocked && !inputMethodShown && !navBarHidden;
    }

    /**
     * Whether the system navigation bar is hidden, from what the bar's own
     * window was last told (insets visibility on Android 11+, system UI
     * visibility flags before). Null means nothing was reported yet, which
     * counts as visible: the bar must not vanish on a guess.
     */
    static boolean navBarHidden(Boolean reportedVisible) {
        return reportedVisible != null && !reportedVisible;
    }
}
