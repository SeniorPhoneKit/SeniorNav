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
    static final String BAR_COLOUR = PREFIX + "BAR_COLOUR";
    static final String BACK = PREFIX + "BACK";
    static final String HOME = PREFIX + "HOME";
    static final String BAR_UNLOCKED = PREFIX + "BAR_UNLOCKED";
    static final String VOLUME_KEYS = PREFIX + "VOLUME_KEYS";
    static final String VOLUME_LIMIT = PREFIX + "VOLUME_LIMIT";
    static final String PATTERN_REMOVE = PREFIX + "PATTERN_REMOVE";
    static final String PULLDOWN = PREFIX + "PULLDOWN";

    /** BAR_SIDES: which side Back goes to, "left" or "right". */
    static final String EXTRA_BACK = "back";
    /** BAR_COLOUR: "dark" or "light". */
    static final String EXTRA_COLOUR = "colour";
    /** BAR_UNLOCKED: "on" (hide the bar while unlocked) or "off". */
    static final String EXTRA_HIDE = "hide";
    /** PULLDOWN: "on" (block pulling down while locked) or "off". */
    static final String EXTRA_BLOCK = "block";
    /** VOLUME_KEYS: "normal", "limits" or "off" (VolumeKeys.parse). */
    static final String EXTRA_MODE = "mode";
    /** VOLUME_LIMIT: "ring", "media" or "alarm", and the steps "min" and "max". */
    static final String EXTRA_STREAM = "stream";
    static final String EXTRA_MIN = "min";
    static final String EXTRA_MAX = "max";

    enum Command { BAR_HIDE, BAR_SHOW, BAR_SIDES, BAR_COLOUR, BACK, HOME,
        BAR_UNLOCKED, VOLUME_KEYS, VOLUME_LIMIT, PATTERN_REMOVE, PULLDOWN }

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

    /**
     * The extra "colour": true for a light bar ("light", German "hell"), false
     * for a dark one ("dark", "dunkel"), null for anything else.
     */
    static Boolean lightBar(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (v.equals("light") || v.equals("hell")) return Boolean.TRUE;
        if (v.equals("dark") || v.equals("dunkel")) return Boolean.FALSE;
        return null;
    }

    /** On/off extras: true for "on"/"yes"/"true" (German "an", "ja"), false for the opposites, else null. */
    static Boolean onOff(String value) {
        if (value == null) return null;
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "on": case "yes": case "true": case "1": case "an": case "ja": return Boolean.TRUE;
            case "off": case "no": case "false": case "0": case "aus": case "nein": return Boolean.FALSE;
            default: return null;
        }
    }

    /** The extra "stream": "ring", "media" or "alarm" (German "klingelton", "medien", "wecker"), else null. */
    static VolumeLimits.Stream stream(String value) {
        if (value == null) return null;
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "ring": case "klingelton": return VolumeLimits.Stream.RING;
            case "media": case "music": case "medien": return VolumeLimits.Stream.MEDIA;
            case "alarm": case "wecker": return VolumeLimits.Stream.ALARM;
            default: return null;
        }
    }

    /** Not sent. */
    static final int ABSENT = Integer.MIN_VALUE;
    /** Unreadable. */
    static final int BAD = Integer.MIN_VALUE + 1;

    /**
     * A step extra ("min", "max"): a whole number from 0 to 100, ABSENT when not
     * sent, BAD when unreadable. For "max", "top" (German "oben") is
     * VolumeLimits.NO_MAX: no maximum.
     */
    static int step(String value, boolean allowTop) {
        if (value == null) return ABSENT;
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (allowTop && (v.equals("top") || v.equals("oben") || v.equals("none"))) return VolumeLimits.NO_MAX;
        try {
            int n = Integer.parseInt(v);
            return n >= 0 && n <= 100 ? n : BAD;
        } catch (NumberFormatException e) {
            return BAD;
        }
    }
}
