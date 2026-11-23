package app.phueber.seniornav;

import java.util.Locale;

/**
 * What the volume buttons do while SeniorLauncher's shared lock is on, as the
 * helper chose it, and the decision for one press. Pure, so it is unit tested.
 */
final class VolumeKeys {
    private VolumeKeys() {}

    /** The helper's choice. Unlocked, the buttons always work normally. */
    enum Mode {
        /** As without SeniorNav. The default. */
        NORMAL("normal"),
        /** Within the helper's limits (VolumeLimits); changes from elsewhere are brought back too. */
        LIMITS("limits"),
        /** The buttons do nothing (the pattern still works). */
        OFF("off");

        final String id;

        Mode(String id) {
            this.id = id;
        }
    }

    /** What to do with one press. */
    enum Action {
        /** Let the system have it, untouched. */
        PASS,
        /** Let it through only if the step stays within the limits. */
        LIMIT,
        /** Swallow it. */
        BLOCK
    }

    /** The stored or sent mode ("normal", "limits", "off"; German too), or null if unreadable. */
    static Mode parse(String s) {
        if (s == null) return null;
        switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "normal": return Mode.NORMAL;
            case "limits": case "limit": case "grenzen": return Mode.LIMITS;
            case "off": case "aus": case "nothing": return Mode.OFF;
            default: return null;
        }
    }

    /**
     * The decision for one press. During a call or while the phone rings the
     * buttons are left alone, so a press can still silence the ringer or set
     * the call volume.
     */
    static Action decide(Mode mode, boolean locked, boolean call) {
        if (call || !locked || mode == null) return Action.PASS;
        switch (mode) {
            case LIMITS: return Action.LIMIT;
            case OFF: return Action.BLOCK;
            default: return Action.PASS;
        }
    }

    /** Whether the limits are enforced on changes from elsewhere (the slider, other apps). */
    static boolean enforceLimits(Mode mode, boolean locked) {
        return locked && mode == Mode.LIMITS;
    }

    /**
     * Whether the service needs to see volume presses at all: a pattern is
     * set, or the buttons do something other than normal. Otherwise key
     * filtering stays off.
     */
    static boolean needKeys(String pattern, Mode mode) {
        return VolumePattern.normalize(pattern) != null || (mode != null && mode != Mode.NORMAL);
    }
}
