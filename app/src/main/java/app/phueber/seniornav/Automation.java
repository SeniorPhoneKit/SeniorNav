package app.phueber.seniornav;

import java.util.Locale;

/**
 * The commands other apps (Trigly, Tasker, ...) can send to
 * {@link AutomationReceiver} as explicit broadcasts, and the parsing of their
 * string extras. Pure, so it is unit tested. Lenient: case and surrounding
 * spaces do not matter; anything else is ignored (null).
 */
final class Automation {
    private Automation() {}

    private static final String PREFIX = "app.phueber.seniornav.action.";
    static final String BAR_HIDE = PREFIX + "BAR_HIDE";
    static final String BAR_SHOW = PREFIX + "BAR_SHOW";
    static final String BAR_SIDES = PREFIX + "BAR_SIDES";
    static final String BACK = PREFIX + "BACK";
    static final String HOME = PREFIX + "HOME";

    /** BAR_SIDES: which side Back goes to, "left" or "right". */
    static final String EXTRA_BACK = "back";

    enum Command { BAR_HIDE, BAR_SHOW, BAR_SIDES, BACK, HOME }

    /** The command an action stands for, or null if it is none of ours. */
    static Command parse(String action) {
        if (action == null) return null;
        String a = action.trim().toUpperCase(Locale.ROOT);
        if (!a.startsWith(PREFIX.toUpperCase(Locale.ROOT))) return null;
        String verb = a.substring(PREFIX.length());
        for (Command c : Command.values()) {
            if (c.name().equals(verb)) return c;
        }
        return null;
    }

    /** The extra "back": true for "right", false for "left", null for anything else. */
    static Boolean backOnRight(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.equals("right")) return Boolean.TRUE;
        if (v.equals("left")) return Boolean.FALSE;
        return null;
    }
}
