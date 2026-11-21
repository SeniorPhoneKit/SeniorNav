package app.phueber.seniornav;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The app's two settings, in one SharedPreferences file so that the running
 * {@link NavBarService} can listen to it (OnSharedPreferenceChangeListener; the
 * service, the screen and the automation receiver share one process, so they
 * share one SharedPreferences instance and every change reaches the listener).
 *
 * - paused: the bar is hidden because an automation app asked for it
 *   (BAR_HIDE), until BAR_SHOW or the button on the main screen. Kept on disk,
 *   so it survives a restart of the service or the phone.
 * - back_right: the button order. Default Back on the left, Home on the right.
 */
final class NavStore {
    static final String PREFS = "nav";
    static final String KEY_PAUSED = "paused";
    static final String KEY_BACK_RIGHT = "back_right";

    private NavStore() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean paused(Context c) {
        return prefs(c).getBoolean(KEY_PAUSED, false);
    }

    static void setPaused(Context c, boolean paused) {
        prefs(c).edit().putBoolean(KEY_PAUSED, paused).apply();
    }

    static boolean backRight(Context c) {
        return prefs(c).getBoolean(KEY_BACK_RIGHT, false);
    }

    static void setBackRight(Context c, boolean right) {
        prefs(c).edit().putBoolean(KEY_BACK_RIGHT, right).apply();
    }
}
