package app.phueber.seniornav;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Lets SeniorLauncher save and restore this app's settings along with every
 * other Senior app's, in one file. content://app.phueber.seniornav.backup;
 * every Senior app answers the same calls under its own package name.
 *
 * call("export") -> Bundle "json": this app's backup file (one entry).
 * call("import", json) -> Bundle "ok" (boolean), on success "skipped"
 * (String[], what this phone lacks), else "error": malformed,
 * newer_version, no_entry, locked or not_allowed.
 *
 * Only SeniorLauncher and this app itself are answered (getCallingPackage,
 * Backup.callerAllowed): the settings include the lock pattern, which no other
 * app should read. A sanity check, not security, like the lock itself.
 */
public final class BackupProvider extends ContentProvider {
    @Override public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Context c = getContext();
        if (c == null) throw new IllegalStateException("not attached");
        Bundle out = new Bundle();
        if (!Backup.METHOD_EXPORT.equals(method) && !Backup.METHOD_IMPORT.equals(method)) {
            throw new UnsupportedOperationException(method);
        }
        if (!Backup.callerAllowed(getCallingPackage(), c.getPackageName())) {
            out.putBoolean(Backup.EXTRA_OK, false);
            out.putString(Backup.EXTRA_ERROR, Backup.ERROR_NOT_ALLOWED);
            return out;
        }
        if (Backup.METHOD_EXPORT.equals(method)) {
            out.putString(Backup.EXTRA_JSON, NavSettingsBackup.export(c));
            return out;
        }
        NavSettingsBackup.Result r = NavSettingsBackup.restore(c, arg);
        out.putBoolean(Backup.EXTRA_OK, r.error == null);
        if (r.error != null) out.putString(Backup.EXTRA_ERROR, r.error);
        else out.putStringArray(Backup.EXTRA_SKIPPED, r.skipped);
        return out;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        throw new UnsupportedOperationException("call only");
    }

    @Override public String getType(Uri uri) {
        return null;
    }

    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("call only");
    }

    @Override public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("call only");
    }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("call only");
    }
}
