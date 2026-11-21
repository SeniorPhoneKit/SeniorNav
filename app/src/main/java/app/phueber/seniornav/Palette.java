package app.phueber.seniornav;

/**
 * The colours, by role: the Light look of the other Senior apps (warm
 * off-white page, white cards, near-black text) plus the bar. Pure (no
 * android.* imports), so the contrast is checked by a JVM test. There is no
 * theme choice: the helper sees this app for a minute during setup, and the
 * senior only ever sees the bar.
 */
final class Palette {
    enum Role {
        PAGE,           // screen background
        CARD,           // cards, outlined buttons
        CARD_BORDER,    // card outline
        TEXT,           // all normal text, and outlined-button text
        TEXT_MUTED,     // explanations
        GREEN,          // primary action, "bar is on"
        GREEN_LIGHT,    // "bar is on" card
        BLUE,           // neutral action
        RED,            // "bar is off" outline
        RED_LIGHT,      // "bar is off" card
        PRESS_ON_CARD,  // ripple over CARD
        PRESS_ON_COLOR, // ripple over GREEN/BLUE/RED and the bar
        BAR,            // the navigation bar's background
        ON_BAR,         // icons and words on the bar
        BAR_DIVIDER     // the line between Back and Home
    }

    /** Text on GREEN, BLUE and RED buttons. */
    static final int ON_COLOR = 0xFFFFFFFF;

    private static final int[] LIGHT = {
            0xFFF4F2ED, 0xFFFFFFFF, 0xFFD9D5CC, 0xFF1C1B19, 0xFF5F5B54,
            0xFF2E7D32, 0xFFDCEFD9, 0xFF1F5F8B, 0xFFC62828, 0xFFFBE1E0,
            0x33000000, 0x55FFFFFF,
            // The bar is near-black with white, like most system navigation bars.
            0xFF1C1B19, 0xFFFFFFFF, 0xFF8A857C,
    };

    private Palette() {}

    static int color(Role role) {
        return LIGHT[role.ordinal()];
    }

    static int count() {
        return LIGHT.length;
    }

    /** WCAG 2 contrast ratio of two opaque colours, 1..21. */
    static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(int argb) {
        return 0.2126 * channel((argb >> 16) & 0xFF)
                + 0.7152 * channel((argb >> 8) & 0xFF)
                + 0.0722 * channel(argb & 0xFF);
    }

    private static double channel(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }
}
