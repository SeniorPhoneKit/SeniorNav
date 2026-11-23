package app.phueber.seniornav;

import android.content.Context;

/**
 * Saves and restores the helper's settings as a backup file (see Backup,
 * NavBackup): used by the main screen's Backup buttons and by BackupProvider,
 * through which SeniorLauncher backs up every Senior app at once.
 */
final class NavSettingsBackup {
    private NavSettingsBackup() {}

    /** The outcome of a restore. */
    static final class Result {
        /** null when restored, else one of Backup.ERROR_*. */
        final String error;
        /** What could not be restored on this phone; SeniorNav has nothing phone-specific, so empty. */
        final String[] skipped;

        Result(String error, String[] skipped) {
            this.error = error;
            this.skipped = skipped;
        }
    }

    /** This app's file, with one entry. */
    static String export(Context c) {
        return Backup.writeOne(BackupFiles.now(), c.getPackageName(), BackupFiles.appVersion(c),
                NavStore.snapshot(c).toSettings());
    }

    /**
     * Replaces the settings with the file's (this app's entry of a file of one
     * or of all Senior apps). Refused while SeniorLauncher is locked, like the
     * settings buttons. Nothing changes unless the whole entry is readable.
     */
    static Result restore(Context c, String json) {
        if (LauncherLock.locked(c)) return new Result(Backup.ERROR_LOCKED, new String[0]);
        NavBackup b;
        try {
            b = NavBackup.fromSettings(Backup.settingsFor(json, c.getPackageName()));
        } catch (Backup.Failure f) {
            return new Result(f.code, new String[0]);
        }
        NavStore.restore(c, b);
        return new Result(null, new String[0]);
    }
}
