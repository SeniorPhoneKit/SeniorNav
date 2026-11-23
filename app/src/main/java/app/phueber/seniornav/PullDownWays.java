package app.phueber.seniornav;

/**
 * Other ways the phone can open the pull-down menu, which the strip over the
 * status bar cannot stop: the AOSP/Pixel gestures "Swipe fingerprint for
 * notifications" and One-handed mode set to "Show notifications". Decides
 * which note the main screen shows under "Block while locked". Pure, so it is
 * unit tested; NavSettings reads the setting.
 *
 * Only the fingerprint swipe can be read: {@code system_navigation_keys_enabled}
 * is a hidden secure setting apps may still read. One-handed mode
 * ({@code one_handed_mode_enabled} with {@code swipe_bottom_to_notification_enabled})
 * came with Android 12, which refuses those keys to apps ("not readable",
 * checked on the Android 15 emulator), so it is only ever "maybe".
 */
final class PullDownWays {
    /** The setting was never stored on this phone. */
    static final int ABSENT = -1;
    /** The phone refused it, or it was not a number. */
    static final int UNREADABLE = -2;
    /** Android 12, where One-handed mode came in. */
    static final int ONE_HANDED_SDK = 31;

    /** The note: none, the general line, or the gesture that is on. */
    enum Note { NONE, GENERIC, FINGERPRINT }

    private PullDownWays() {}

    /**
     * stock: a Google phone (Pixel, the emulator), whose Settings has no other
     * such gesture and where a setting never stored is off (the AOSP default).
     * On other phones the maker may add gestures SeniorNav cannot read, so
     * they always get at least the general line. fingerprint: 1 on, 0 off,
     * ABSENT or UNREADABLE; anything else is unknown. Only a clear 1 is "on".
     */
    static Note note(boolean stock, int fingerprint, int sdk, NavMode mode) {
        if (fingerprint == 1) return Note.FINGERPRINT;
        boolean fingerprintOff = fingerprint == 0 || (fingerprint == ABSENT && stock);
        return stock && fingerprintOff && !oneHandedPossible(sdk, mode) ? Note.NONE : Note.GENERIC;
    }

    /**
     * Whether One-handed mode could open the menu. Its swipe exists only with
     * gesture navigation: with 2 or 3 buttons Settings greys it out (checked
     * on the Android 15 emulator), whatever is stored.
     */
    static boolean oneHandedPossible(int sdk, NavMode mode) {
        return sdk >= ONE_HANDED_SDK && mode != NavMode.THREE_BUTTON && mode != NavMode.TWO_BUTTON;
    }
}
