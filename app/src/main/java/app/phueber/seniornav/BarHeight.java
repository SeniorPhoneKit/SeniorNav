package app.phueber.seniornav;

/**
 * How tall the bar is. Pure, so the rule is unit tested.
 *
 * The bar covers the system navigation bar and nothing more, so an edge-to-edge
 * app loses no more of its screen than it already gives to the system buttons.
 * Sources in order: the navigation-bar inset the system reports, then the
 * framework's {@code navigation_bar_height}. It is never lower than the floor
 * (48dp, the usual 3-button height): in gesture mode or landscape the system
 * value is small or zero, and a strip that thin is no target for a senior.
 * Seniors who need it bigger enlarge everything with Display size.
 */
final class BarHeight {
    static final int FLOOR_DP = 48;

    private BarHeight() {}

    /** All values in pixels; unknown or unreadable values are 0 or negative. */
    static int choose(int insetPx, int resourcePx, int floorPx) {
        int system = insetPx > 0 ? insetPx : resourcePx;
        return Math.max(system, floorPx);
    }
}
