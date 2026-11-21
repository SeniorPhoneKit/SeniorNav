package app.phueber.seniornav;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * Tells the other Senior apps whether the bar is on screen right now, so they
 * hide their own pinned Back button exactly while the bar's Back is there to
 * use, and show it again while the bar is paused by automation, or turned on
 * but not running (stopped by the system, or its window refused).
 *
 * content://app.phueber.seniornav.bar/state -> one row, column "visible" (0/1):
 * the service runs, its window is attached and automation has not paused it.
 * (Over the lock screen it is hidden too, but no app's screen shows there.)
 * Observers of that uri are notified on every change of these. Read-only. The
 * state is visible on screen anyway, so no permission guards it, like
 * SeniorLauncher's lock provider.
 */
public final class BarProvider extends ContentProvider {
    public static final String AUTHORITY = "app.phueber.seniornav.bar";
    public static final Uri STATE = Uri.parse("content://" + AUTHORITY + "/state");
    public static final String COLUMN_VISIBLE = "visible";

    @Override public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        Context c = getContext();
        MatrixCursor cursor = new MatrixCursor(new String[] {COLUMN_VISIBLE});
        cursor.addRow(new Object[] {c != null && NavBarService.barVisible(c) ? 1 : 0});
        return cursor;
    }

    /** Called whenever running, attached or paused changes. */
    static void notifyChanged(Context c) {
        try {
            c.getContentResolver().notifyChange(STATE, null);
        } catch (RuntimeException ignored) {
            // nobody listening is fine
        }
    }

    @Override public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.app.phueber.seniornav.bar";
    }

    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }
}
