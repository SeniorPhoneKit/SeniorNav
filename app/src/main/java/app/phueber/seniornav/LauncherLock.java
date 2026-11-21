package app.phueber.seniornav;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * The lock of SeniorLauncher, read through its LockProvider: while the helper
 * has locked the home screen, this app hides its settings buttons, so the
 * senior cannot switch the bar off by accident. Read-only: this app can neither
 * lock nor unlock; the helper unlocks on the home screen. Without SeniorLauncher
 * (or when it cannot be asked) the app is unlocked: it has no lock of its own,
 * because turning the bar off still needs Android's accessibility settings.
 */
final class LauncherLock {
    static final Uri STATE = Uri.parse("content://app.phueber.seniorlauncher.lock/state");

    private LauncherLock() {}

    static boolean locked(Context c) {
        try (Cursor cur = c.getContentResolver().query(STATE, null, null, null, null)) {
            return cur != null && cur.moveToFirst() && cur.getInt(0) == 1;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
