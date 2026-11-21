package app.phueber.seniornav;

/**
 * Which system navigation the phone uses. Pure, so the parsing is unit tested.
 *
 * Android keeps it in the hidden secure setting {@code navigation_mode} (and the
 * framework resource {@code config_navBarInteractionMode}): 0 = 3-button,
 * 1 = 2-button, 2 = gesture. Neither is public API, so anything else, or a value
 * that could not be read, is UNKNOWN.
 */
enum NavMode {
    THREE_BUTTON, TWO_BUTTON, GESTURE, UNKNOWN;

    /** The setting's raw value; pass a negative number when it could not be read. */
    static NavMode parse(int raw) {
        switch (raw) {
            case 0: return THREE_BUTTON;
            case 1: return TWO_BUTTON;
            case 2: return GESTURE;
            default: return UNKNOWN;
        }
    }

    /**
     * The first source that knows: the secure setting, then the framework
     * resource. Either may be negative (unreadable).
     */
    static NavMode of(int setting, int resource) {
        NavMode m = parse(setting);
        return m != UNKNOWN ? m : parse(resource);
    }

    /** The bar sits exactly over the system buttons only in 3-button mode. */
    boolean barFits() {
        return this == THREE_BUTTON;
    }
}
