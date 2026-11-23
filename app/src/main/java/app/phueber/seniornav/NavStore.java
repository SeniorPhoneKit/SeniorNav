package app.phueber.seniornav;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The app's settings, in one SharedPreferences file so that the running
 * {@link NavBarService} can listen to it (OnSharedPreferenceChangeListener; the
 * service, the screen and the automation receiver share one process, so they
 * share one SharedPreferences instance and every change reaches the listener).
 *
 * - paused: the bar is hidden because an automation app asked for it
 *   (BAR_HIDE), until BAR_SHOW or the button on the main screen. Kept on disk,
 *   so it survives a restart of the service or the phone.
 * - back_right: the button order. Default Back on the left, Home on the right.
 * - light_bar: white bar with dark icons. Default dark bar with white icons.
 * - hide_unlocked: the bar steps aside while SeniorLauncher's lock is in use
 *   but open (the helper has the phone). Default on.
 * - pattern: the volume-button pattern ("UUDD"), absent when none.
 * - volume_keys: what the volume buttons do while locked (VolumeKeys.Mode).
 * - ring_min/ring_max, media_min/media_max, alarm_min: the volume limits while
 *   locked, in the stream's steps; a max of VolumeLimits.NO_MAX is the top.
 */
final class NavStore {
    static final String PREFS = "nav";
    static final String KEY_PAUSED = "paused";
    static final String KEY_BACK_RIGHT = "back_right";
    static final String KEY_LIGHT_BAR = "light_bar";
    static final String KEY_HIDE_UNLOCKED = "hide_unlocked";
    static final String KEY_PATTERN = "pattern";
    static final String KEY_VOLUME_KEYS = "volume_keys";

    private NavStore() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean paused(Context c) {
        return prefs(c).getBoolean(KEY_PAUSED, false);
    }

    /** No-op when unchanged, so repeated BAR_HIDE/BAR_SHOW do not wake the other apps' observers. */
    static void setPaused(Context c, boolean paused) {
        if (paused(c) == paused) return;
        prefs(c).edit().putBoolean(KEY_PAUSED, paused).apply();
        BarProvider.notifyChanged(c); // the bar shows or hides: other apps' Back follows
    }

    static boolean backRight(Context c) {
        return prefs(c).getBoolean(KEY_BACK_RIGHT, false);
    }

    static void setBackRight(Context c, boolean right) {
        prefs(c).edit().putBoolean(KEY_BACK_RIGHT, right).apply();
    }

    static boolean lightBar(Context c) {
        return prefs(c).getBoolean(KEY_LIGHT_BAR, false);
    }

    static void setLightBar(Context c, boolean light) {
        prefs(c).edit().putBoolean(KEY_LIGHT_BAR, light).apply();
    }

    static boolean hideUnlocked(Context c) {
        return prefs(c).getBoolean(KEY_HIDE_UNLOCKED, true);
    }

    static void setHideUnlocked(Context c, boolean hide) {
        prefs(c).edit().putBoolean(KEY_HIDE_UNLOCKED, hide).apply();
    }

    /** The usable pattern, or null. */
    static String pattern(Context c) {
        return VolumePattern.normalize(prefs(c).getString(KEY_PATTERN, null));
    }

    /** Stores a pattern (null removes it). */
    static void setPattern(Context c, String pattern) {
        String p = VolumePattern.normalize(pattern);
        if (p == null) prefs(c).edit().remove(KEY_PATTERN).apply();
        else prefs(c).edit().putString(KEY_PATTERN, p).apply();
    }

    static VolumeKeys.Mode volumeKeys(Context c) {
        VolumeKeys.Mode m = VolumeKeys.parse(prefs(c).getString(KEY_VOLUME_KEYS, null));
        return m == null ? VolumeKeys.Mode.NORMAL : m;
    }

    static void setVolumeKeys(Context c, VolumeKeys.Mode mode) {
        prefs(c).edit().putString(KEY_VOLUME_KEYS, mode.id).apply();
    }

    static String minKey(VolumeLimits.Stream s) {
        return s.id + "_min";
    }

    static String maxKey(VolumeLimits.Stream s) {
        return s.id + "_max";
    }

    static boolean isLimitKey(String key) {
        if (key == null) return false;
        for (VolumeLimits.Stream s : VolumeLimits.Stream.values()) {
            if (key.equals(minKey(s)) || key.equals(maxKey(s))) return true;
        }
        return false;
    }

    static VolumeLimits limits(Context c, VolumeLimits.Stream s) {
        SharedPreferences p = prefs(c);
        return new VolumeLimits(p.getInt(minKey(s), 0),
                s.hasMax ? p.getInt(maxKey(s), VolumeLimits.NO_MAX) : VolumeLimits.NO_MAX);
    }

    static void setLimits(Context c, VolumeLimits.Stream s, VolumeLimits l) {
        SharedPreferences.Editor e = prefs(c).edit().putInt(minKey(s), Math.max(0, l.min));
        if (s.hasMax) e.putInt(maxKey(s), l.max);
        e.apply();
    }
}
