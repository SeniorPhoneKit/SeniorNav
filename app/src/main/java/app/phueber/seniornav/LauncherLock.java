package app.phueber.seniornav;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * The lock of SeniorLauncher, read through its LockProvider: while the helper
 * has locked the home screen, this app hides its settings buttons, so the
 * senior cannot switch the bar off by accident. Without SeniorLauncher (or when
 * it cannot be asked) the app is unlocked: it has no lock of its own, because
 * turning the bar off still needs Android's accessibility settings.
 *
 * The helper's volume-button pattern locks and unlocks through the provider's
 * "nav_lock" / "nav_unlock", which SeniorLauncher answers only for this
 * package and which lock only once a PIN was set there.
 */
final class LauncherLock {
    static final String PACKAGE = "app.phueber.seniorlauncher";
    static final Uri STATE = Uri.parse("content://app.phueber.seniorlauncher.lock/state");
    private static final String COLUMN_HAS_PIN = "has_pin";

    /** How a pattern's lock or unlock went. */
    enum Toggle { LOCKED, UNLOCKED, NO_PIN, NO_LAUNCHER }

    private LauncherLock() {}

    static boolean locked(Context c) {
        return read(c).locked;
    }

    static LockState read(Context c) {
        try (Cursor cur = c.getContentResolver().query(STATE, null, null, null, null)) {
            if (cur == null || !cur.moveToFirst()) return LockState.NONE;
            int pin = cur.getColumnIndex(COLUMN_HAS_PIN);
            return new LockState(true, pin >= 0, cur.getInt(0) == 1, pin >= 0 && cur.getInt(pin) == 1);
        } catch (RuntimeException e) {
            return LockState.NONE;
        }
    }

    /**
     * Locks if it is open, unlocks if it is locked, for the pattern. Without
     * SeniorLauncher, or one too old to know "nav_lock", NO_LAUNCHER.
     */
    static Toggle toggle(Context c) {
        LockState s = read(c);
        if (!s.available || !s.supportsNav) return Toggle.NO_LAUNCHER;
        try {
            Bundle out = c.getContentResolver().call(STATE, s.locked ? "nav_unlock" : "nav_lock", null, null);
            if (out == null) return Toggle.NO_LAUNCHER;
            if ("no_pin".equals(out.getString("error"))) return Toggle.NO_PIN;
            if (out.getString("error") != null) return Toggle.NO_LAUNCHER;
            return out.getBoolean("locked") ? Toggle.LOCKED : Toggle.UNLOCKED;
        } catch (RuntimeException e) {
            return Toggle.NO_LAUNCHER;
        }
    }
}
