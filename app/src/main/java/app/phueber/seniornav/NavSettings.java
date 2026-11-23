package app.phueber.seniornav;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * What the main screen reads from, and opens in, the system settings.
 */
final class NavSettings {
    private NavSettings() {}

    /** Whether Android lists the service as turned on (it may still not be running). */
    static boolean serviceTurnedOn(Context c) {
        try {
            String s = Settings.Secure.getString(c.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return BarStatus.listed(s, c.getPackageName(), NavBarService.class.getName());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Best effort: the hidden secure setting "navigation_mode", else the
     * framework resource behind it. Neither is public API; UNKNOWN when both fail.
     */
    @SuppressLint("DiscouragedApi") // no public API for the navigation mode
    static NavMode navMode(Context c) {
        int setting = -1, resource = -1;
        try {
            setting = Settings.Secure.getInt(c.getContentResolver(), "navigation_mode", -1);
        } catch (RuntimeException ignored) {
            // not readable on this phone
        }
        try {
            Resources r = Resources.getSystem();
            int id = r.getIdentifier("config_navBarInteractionMode", "integer", "android");
            if (id != 0) resource = r.getInteger(id);
        } catch (RuntimeException ignored) {
            // no such resource (Android 9 and older)
        }
        return NavMode.of(setting, resource);
    }

    /**
     * Best effort, no permission: the other ways this phone can open the
     * pull-down (PullDownWays). "Swipe fingerprint for notifications" is a
     * hidden secure setting that may be missing or refused; the note then
     * falls back to the general line. One-handed mode cannot be read at all.
     */
    static PullDownWays.Note pullDownWays(Context c) {
        return PullDownWays.note("Google".equalsIgnoreCase(Build.MANUFACTURER),
                secureInt(c, "system_navigation_keys_enabled"),
                Build.VERSION.SDK_INT, navMode(c));
    }

    private static int secureInt(Context c, String key) {
        try {
            String s = Settings.Secure.getString(c.getContentResolver(), key);
            if (s == null) return PullDownWays.ABSENT;
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            // refused for apps on this phone, or not a number
            return PullDownWays.UNREADABLE;
        }
    }

    static void openAccessibility(Activity a) {
        open(a, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    static void openAppInfo(Activity a) {
        open(a, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", a.getPackageName(), null)));
    }

    /**
     * Android has no public action for System navigation. These are the screens
     * AOSP-based Settings apps (Pixel and others) use, tried in turn (on
     * the Android 15 emulator the first is gone and the second opens it); if none
     * opens, the general settings page, from where the helper searches for
     * "navigation". Every try names the Settings app, since a non-public
     * action may be claimed by any app.
     */
    static void openNavigation(Activity a) {
        Intent[] tries = {
                new Intent().setComponent(new ComponentName("com.android.settings",
                        "com.android.settings.Settings$SystemNavigationGestureSettingsActivity")),
                // pinned to the system Settings app: unpinned, any app could
                // claim these actions and show a fake settings screen.
                // Navigation mode on Pixels with Android 14 and 15:
                new Intent("com.android.settings.NAVIGATION_MODE_SETTINGS")
                        .setPackage("com.android.settings"),
                new Intent("android.settings.GESTURE_NAVIGATION_SETTINGS")
                        .setPackage("com.android.settings"),
                new Intent(Settings.ACTION_SETTINGS),
        };
        for (Intent i : tries) {
            if (start(a, i)) return;
        }
    }

    /**
     * The System page, where Pixel and stock Android keep "Gestures" (Android
     * 15 has no screen of its own for it to open), pinned to the Settings app;
     * else the general settings page.
     */
    static void openGestures(Activity a) {
        Intent[] tries = {
                new Intent().setComponent(new ComponentName("com.android.settings",
                        "com.android.settings.Settings$SystemDashboardActivity")),
                new Intent(Settings.ACTION_SETTINGS),
        };
        for (Intent i : tries) {
            if (start(a, i)) return;
        }
    }

    private static void open(Activity a, Intent i) {
        if (!start(a, i)) start(a, new Intent(Settings.ACTION_SETTINGS));
    }

    private static boolean start(Activity a, Intent i) {
        try {
            a.startActivity(i);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
