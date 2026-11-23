package app.phueber.seniornav;

/**
 * Blocking the pull-down (notifications and Quick Settings) while
 * SeniorLauncher's lock is on: when it applies, how tall the strip over the
 * status bar is, and how often the open shade is closed again. Pure, so it is
 * unit tested; NavBarService does the windows and the global action.
 */
final class PullDown {
    /** The status bar on most phones; used when the system reports nothing. */
    static final int FALLBACK_DP = 24;
    /** The safety net closes the shade at most this often, so it never fights in a tight loop. */
    static final long CLOSE_GAP_MS = 300;
    static final String SYSTEM_UI = "com.android.systemui";

    private PullDown() {}

    /**
     * Whether blocking applies right now: the helper's setting is on,
     * SeniorLauncher answered and is locked, and the phone is in use. Never on
     * the phone's own lock screen (its shade and PIN pad stay as they are),
     * nor with the screen off or a screensaver running. Unlocked, or without
     * SeniorLauncher, the pull-down works as usual.
     */
    static boolean blocks(boolean setting, LockState lock, boolean keyguard,
            boolean interactive, boolean dreaming) {
        return setting && lock != null && lock.available && lock.locked
                && !keyguard && interactive && !dreaming;
    }

    /**
     * Whether the setting can do anything on this phone: SeniorLauncher
     * answered, and (where it can say) a PIN is set. For the one line on the
     * main screen.
     */
    static boolean lockInUse(LockState lock) {
        return lock != null && lock.available && (!lock.supportsNav || lock.hasPin || lock.locked);
    }

    /**
     * The strip's height in pixels: the status bar (or the cutout, if that
     * reaches further down) as the system reports it, else the framework's
     * status_bar_height, else the fallback. Unknown values are 0 or negative.
     */
    static int stripHeight(int insetPx, int cutoutPx, int resourcePx, int fallbackPx) {
        int system = Math.max(insetPx, cutoutPx);
        if (system > 0) return system;
        return resourcePx > 0 ? resourcePx : fallbackPx;
    }

    /** Whether the shade may be closed now, or the last close was too recent. */
    static boolean mayClose(long nowMs, long lastCloseMs) {
        return lastCloseMs == 0 || nowMs - lastCloseMs >= CLOSE_GAP_MS;
    }

    /** A window event that may mean the shade opened: anything from System UI. */
    static boolean fromSystemUi(CharSequence packageName) {
        return packageName != null && SYSTEM_UI.contentEquals(packageName);
    }
}
