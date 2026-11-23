package app.phueber.seniornav;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The helper's settings as they go into a backup file (see Backup), and back.
 * Pure, so it is unit tested; NavSettingsBackup reads and writes NavStore.
 *
 * Saved: the button order, the bar colour, hiding while unlocked, blocking
 * the pull-down while locked, the volume
 * pattern, the volume-button mode and the limits. Not saved: whether the
 * service is switched on (a system switch the helper turns on in
 * Accessibility settings), whether automation paused the bar (a moment's
 * state, not a setting), and the lock, which is SeniorLauncher's.
 *
 * <pre>
 * {"backRight": false, "lightBar": true, "hideWhileUnlocked": true, "blockPullDown": false,
 *  "pattern": "UUDD", "volumeKeys": "limits",
 *  "limits": {"ring": {"min": 2, "max": 5}, "media": {"min": 0}, "alarm": {"min": 3}}}
 * </pre>
 * A missing field takes the default (no pattern; pull-down not blocked; a
 * missing "max" is the top).
 * A wrong type or an invalid value (a pattern without both directions, an
 * unknown mode, a maximum below the minimum) makes the file unreadable:
 * nothing is restored.
 */
final class NavBackup {
    /** More steps than any phone has; a bigger number is a damaged file. */
    static final int MAX_STEP = 1000;

    final boolean backRight;
    final boolean lightBar;
    final boolean hideUnlocked;
    final boolean blockPullDown;
    /** Normalised, or null for none. */
    final String pattern;
    final VolumeKeys.Mode volumeKeys;
    /** Indexed by VolumeLimits.Stream.ordinal(). */
    final VolumeLimits[] limits;

    NavBackup(boolean backRight, boolean lightBar, boolean hideUnlocked, boolean blockPullDown,
            String pattern, VolumeKeys.Mode volumeKeys, VolumeLimits[] limits) {
        this.backRight = backRight;
        this.lightBar = lightBar;
        this.hideUnlocked = hideUnlocked;
        this.blockPullDown = blockPullDown;
        this.pattern = VolumePattern.normalize(pattern);
        this.volumeKeys = volumeKeys == null ? VolumeKeys.Mode.NORMAL : volumeKeys;
        this.limits = new VolumeLimits[VolumeLimits.Stream.values().length];
        for (VolumeLimits.Stream s : VolumeLimits.Stream.values()) {
            VolumeLimits l = limits == null ? null : limits[s.ordinal()];
            this.limits[s.ordinal()] = l == null ? new VolumeLimits(0, VolumeLimits.NO_MAX)
                    : new VolumeLimits(l.min, s.hasMax ? l.max : VolumeLimits.NO_MAX);
        }
    }

    VolumeLimits limits(VolumeLimits.Stream s) {
        return limits[s.ordinal()];
    }

    Map<String, Object> toSettings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("backRight", backRight);
        m.put("lightBar", lightBar);
        m.put("hideWhileUnlocked", hideUnlocked);
        m.put("blockPullDown", blockPullDown);
        if (pattern != null) m.put("pattern", pattern);
        m.put("volumeKeys", volumeKeys.id);
        Map<String, Object> all = new LinkedHashMap<>();
        for (VolumeLimits.Stream s : VolumeLimits.Stream.values()) {
            VolumeLimits l = limits(s);
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("min", (long) l.min);
            if (s.hasMax && l.max != VolumeLimits.NO_MAX) one.put("max", (long) l.max);
            all.put(s.id, one);
        }
        m.put("limits", all);
        return m;
    }

    static NavBackup fromSettings(Map<String, Object> m) throws Backup.Failure {
        String rawPattern = Backup.string(m, "pattern", null);
        String pattern = VolumePattern.normalize(rawPattern);
        if (rawPattern != null && pattern == null) {
            throw new Backup.Failure(Backup.ERROR_MALFORMED, "pattern " + rawPattern);
        }
        String rawMode = Backup.string(m, "volumeKeys", null);
        VolumeKeys.Mode mode = rawMode == null ? VolumeKeys.Mode.NORMAL : modeById(rawMode);
        VolumeLimits[] limits = new VolumeLimits[VolumeLimits.Stream.values().length];
        Object rawLimits = m.get("limits");
        Map<String, Object> all = rawLimits == null ? new LinkedHashMap<>()
                : Backup.object(rawLimits, "limits");
        for (VolumeLimits.Stream s : VolumeLimits.Stream.values()) {
            Object rawOne = all.get(s.id);
            Map<String, Object> one = rawOne == null ? new LinkedHashMap<>()
                    : Backup.object(rawOne, "limits " + s.id);
            int min = Backup.number(one, "min", 0, 0, MAX_STEP);
            int max = VolumeLimits.NO_MAX;
            if (s.hasMax) {
                max = Backup.number(one, "max", VolumeLimits.NO_MAX, VolumeLimits.NO_MAX, MAX_STEP);
                if (max != VolumeLimits.NO_MAX && max < min) {
                    throw new Backup.Failure(Backup.ERROR_MALFORMED, s.id + " max below min");
                }
            }
            limits[s.ordinal()] = new VolumeLimits(min, max);
        }
        return new NavBackup(Backup.bool(m, "backRight", false), Backup.bool(m, "lightBar", false),
                Backup.bool(m, "hideWhileUnlocked", true), Backup.bool(m, "blockPullDown", false),
                pattern, mode, limits);
    }

    /** Only the stored ids: the file is written by the app, not typed by people. */
    private static VolumeKeys.Mode modeById(String id) throws Backup.Failure {
        for (VolumeKeys.Mode mode : VolumeKeys.Mode.values()) if (mode.id.equals(id)) return mode;
        throw new Backup.Failure(Backup.ERROR_MALFORMED, "volumeKeys " + id);
    }
}
