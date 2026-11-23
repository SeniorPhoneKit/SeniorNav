package app.phueber.seniornav;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

/**
 * The only screen, for the helper: whether the bar is on (in words, and by
 * colour), the way to switch it on, and the advice to use 3-button
 * navigation. The senior never needs it. Rebuilt on every resume, so it shows
 * the result of whatever the helper just did in Settings.
 */
public final class MainActivity extends Activity {
    private LinearLayout content;
    /** The pattern being recorded ("UUD..."), or null when not recording. */
    private StringBuilder recording;
    private static final int REQUEST_SAVE = 1;
    private static final int REQUEST_LOAD = 2;
    /** A loaded file waiting for the helper's "Replace", or null. */
    private String pendingRestore;
    /** The last backup outcome shown under the Backup buttons, or null. */
    private String backupMessage;
    /** Automation may pause the bar or swap its sides while this screen is open. */
    private final SharedPreferences.OnSharedPreferenceChangeListener settings = (p, key) -> build();
    /**
     * The helper may lock or unlock on the home screen, or the bar's window
     * may come or go, while this screen is open (split screen, automation):
     * show it at once, not at the next resume.
     */
    private final ContentObserver changes = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) {
            build();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = Ui.screen(this, getString(R.string.app_name));
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavStore.prefs(this).registerOnSharedPreferenceChangeListener(settings);
        observe(LauncherLock.STATE);
        observe(BarProvider.STATE);
        build();
    }

    @Override
    protected void onPause() {
        stopRecording(); // left half-way: nothing saved
        NavStore.prefs(this).unregisterOnSharedPreferenceChangeListener(settings);
        getContentResolver().unregisterContentObserver(changes);
        super.onPause();
    }

    private void observe(Uri uri) {
        try {
            getContentResolver().registerContentObserver(uri, false, changes);
        } catch (RuntimeException ignored) {
            // SeniorLauncher not installed: nothing to watch, the resume check stays
        }
    }

    private void build() {
        content.removeAllViews();
        LockState lock = LauncherLock.read(this);
        boolean locked = lock.locked;
        if (locked) {
            stopRecording();
            pendingRestore = null; // a restore asked for before the lock is not carried out
        }
        // "On" only with the window attached: a running service whose window the
        // system refused shows no bar, so it reads as "turned on but not running".
        BarStatus status = BarStatus.of(NavSettings.serviceTurnedOn(this),
                NavBarService.isRunning() && NavBarService.isAttached());

        boolean paused = NavStore.paused(this);
        boolean helper = lock.hidesBar(NavStore.hideUnlocked(this));
        content.addView(statusCard(status, paused, helper), Ui.fullWidth(this, 8));
        if (paused) addPaused();
        content.addView(Ui.text(this, getString(R.string.what_it_does), Ui.TEXT_BODY),
                Ui.fullWidth(this, 16));

        if (locked) {
            content.addView(Ui.bold(this, getString(R.string.locked), Ui.TEXT_BODY),
                    Ui.fullWidth(this, 24));
        } else {
            if (status != BarStatus.ON) {
                content.addView(Ui.text(this, getString(status == BarStatus.STUCK
                        ? R.string.how_to_restart : R.string.how_to_turn_on), Ui.TEXT_BODY),
                        Ui.fullWidth(this, 24));
            }
            Button a11y = Ui.button(this, getString(status == BarStatus.ON
                    ? R.string.open_accessibility_off : R.string.open_accessibility),
                    status == BarStatus.ON ? Palette.Role.BLUE : Palette.Role.GREEN);
            a11y.setOnClickListener(v -> NavSettings.openAccessibility(this));
            content.addView(a11y, Ui.fullWidth(this, 16));

            if (status != BarStatus.ON && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                content.addView(Ui.muted(this, getString(R.string.restricted_hint)),
                        Ui.fullWidth(this, 16));
                Button info = Ui.outlined(this, getString(R.string.open_app_info));
                info.setOnClickListener(v -> NavSettings.openAppInfo(this));
                content.addView(info, Ui.fullWidth(this, 12));
            }
        }

        if (!locked) {
            addSidesSection();
            addColourSection();
            addUnlockedSection();
            addPullDownSection(lock);
            addPatternSection(lock);
            addKeysSection();
            addBackupSection();
        }
        addNavigationSection(NavSettings.navMode(this), locked);
    }

    /**
     * An automation app (Trigly, ...) has paused the bar. Said in words, with a
     * way back. Shown also while locked: showing the bar again only restores
     * the normal state.
     */
    private void addPaused() {
        TextView t = Ui.bold(this, getString(R.string.paused), Ui.TEXT_BODY);
        int p = Ui.dp(this, 16);
        t.setPadding(p, p, p, p);
        t.setBackground(Ui.card(this, Ui.color(Palette.Role.CARD), Ui.color(Palette.Role.OUTLINE)));
        t.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(t, Ui.fullWidth(this, 12));
        Button show = Ui.button(this, getString(R.string.show_again), Palette.Role.GREEN);
        show.setOnClickListener(v -> NavStore.setPaused(this, false));
        content.addView(show, Ui.fullWidth(this, 12));
    }

    /** Which side Back is on: two big buttons, the chosen one says so in words. */
    private void addSidesSection() {
        TextView heading = Ui.bold(this, getString(R.string.sides_heading), 24);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) heading.setAccessibilityHeading(true);
        content.addView(heading, Ui.fullWidth(this, 32));
        boolean right = NavStore.backRight(this);
        content.addView(Ui.text(this, getString(right ? R.string.sides_now_right : R.string.sides_now_left),
                Ui.TEXT_BODY), Ui.fullWidth(this, 8));
        content.addView(sideButton(false, !right), Ui.fullWidth(this, 12));
        content.addView(sideButton(true, right), Ui.fullWidth(this, 12));
    }

    /** Dark or light bar: two big buttons, the chosen one says so in words. */
    private void addColourSection() {
        TextView heading = Ui.bold(this, getString(R.string.colour_heading), 24);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) heading.setAccessibilityHeading(true);
        content.addView(heading, Ui.fullWidth(this, 32));
        boolean light = NavStore.lightBar(this);
        content.addView(colourButton(false, !light), Ui.fullWidth(this, 12));
        content.addView(colourButton(true, light), Ui.fullWidth(this, 12));
    }

    private Button colourButton(boolean light, boolean chosen) {
        String label = getString(light ? R.string.colour_light : R.string.colour_dark);
        Button b = chosen
                ? Ui.button(this, getString(R.string.sides_chosen, label), Palette.Role.BLUE)
                : Ui.outlined(this, label);
        b.setOnClickListener(v -> NavStore.setLightBar(this, light));
        return b;
    }

    private Button sideButton(boolean backRight, boolean chosen) {
        String label = getString(backRight ? R.string.sides_back_right : R.string.sides_back_left);
        Button b = chosen
                ? Ui.button(this, getString(R.string.sides_chosen, label), Palette.Role.BLUE)
                : Ui.outlined(this, label);
        b.setOnClickListener(v -> NavStore.setBackRight(this, backRight));
        return b;
    }

    private TextView statusCard(BarStatus status, boolean paused, boolean helper) {
        int text, fill, stroke;
        switch (status) {
            case ON:
                // paused: the bar is hidden right now, so do not say "on"
                text = paused ? R.string.status_paused
                        : helper ? R.string.status_unlocked : R.string.status_on;
                fill = Ui.color(paused ? Palette.Role.RED_LIGHT : Palette.Role.GREEN_LIGHT);
                stroke = Ui.color(paused ? Palette.Role.RED : Palette.Role.GREEN);
                break;
            case STUCK:
                text = R.string.status_stuck;
                fill = Ui.color(Palette.Role.RED_LIGHT);
                stroke = Ui.color(Palette.Role.RED);
                break;
            default:
                text = R.string.status_off;
                fill = Ui.color(Palette.Role.RED_LIGHT);
                stroke = Ui.color(Palette.Role.RED);
                break;
        }
        TextView t = Ui.bold(this, getString(text), 26);
        int p = Ui.dp(this, 20);
        t.setPadding(p, p, p, p);
        t.setBackground(Ui.card(this, fill, stroke));
        t.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
        return t;
    }

    private TextView heading(int text) {
        TextView heading = Ui.bold(this, getString(text), 24);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) heading.setAccessibilityHeading(true);
        content.addView(heading, Ui.fullWidth(this, 32));
        return heading;
    }

    /** A choice among big buttons; the chosen one says so in words. */
    private Button choice(String label, boolean chosen, Runnable choose) {
        Button b = chosen
                ? Ui.button(this, getString(R.string.sides_chosen, label), Palette.Role.BLUE)
                : Ui.outlined(this, label);
        b.setOnClickListener(v -> choose.run());
        content.addView(b, Ui.fullWidth(this, 12));
        return b;
    }

    /** Whether the bar steps aside while the helper has unlocked SeniorLauncher. */
    private void addUnlockedSection() {
        heading(R.string.unlocked_heading);
        content.addView(Ui.text(this, getString(R.string.unlocked_explain), Ui.TEXT_BODY),
                Ui.fullWidth(this, 8));
        boolean hide = NavStore.hideUnlocked(this);
        choice(getString(R.string.unlocked_hide), hide, () -> NavStore.setHideUnlocked(this, true));
        choice(getString(R.string.unlocked_keep), !hide, () -> NavStore.setHideUnlocked(this, false));
    }

    /**
     * Whether the notifications and Quick Settings can be pulled down while
     * locked (PullDown). One line: what it does, or that it needs the lock.
     */
    private void addPullDownSection(LockState lock) {
        heading(R.string.pulldown_heading);
        content.addView(Ui.text(this, getString(PullDown.lockInUse(lock)
                ? R.string.pulldown_explain : R.string.pulldown_needs_lock), Ui.TEXT_BODY),
                Ui.fullWidth(this, 8));
        boolean block = NavStore.blockPullDown(this);
        choice(getString(R.string.pulldown_allow), !block, () -> NavStore.setBlockPullDown(this, false));
        choice(getString(R.string.pulldown_block), block, () -> NavStore.setBlockPullDown(this, true));
    }

    /** The volume-button pattern: what it is, record it, remove it. */
    private void addPatternSection(LockState lock) {
        heading(R.string.pattern_heading);
        content.addView(Ui.text(this, getString(R.string.pattern_explain), Ui.TEXT_BODY),
                Ui.fullWidth(this, 8));
        if (!lock.available || !lock.supportsNav) {
            content.addView(Ui.bold(this, getString(R.string.pattern_needs_launcher), Ui.TEXT_BODY),
                    Ui.fullWidth(this, 12));
            return;
        }
        if (!lock.hasPin) {
            content.addView(Ui.bold(this, getString(R.string.pattern_no_pin_hint), Ui.TEXT_BODY),
                    Ui.fullWidth(this, 12));
        }
        if (recording != null) {
            content.addView(Ui.bold(this, getString(R.string.pattern_recording), Ui.TEXT_BODY),
                    Ui.fullWidth(this, 12));
            TextView so = Ui.bold(this, recording.length() == 0 ? getString(R.string.pattern_nothing_yet)
                    : getString(R.string.pattern_recorded, VolumePattern.arrows(recording.toString())), 26);
            so.setContentDescription(recording.length() == 0 ? getString(R.string.pattern_nothing_yet)
                    : getString(R.string.pattern_recorded, words(recording.toString())));
            so.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
            content.addView(so, Ui.fullWidth(this, 8));
            String p = VolumePattern.normalize(recording.toString());
            if (p == null && recording.length() >= VolumePattern.MIN_LENGTH) {
                content.addView(Ui.muted(this, getString(R.string.pattern_invalid)), Ui.fullWidth(this, 8));
            }
            if (p != null) {
                Button save = Ui.button(this, getString(R.string.pattern_save), Palette.Role.GREEN);
                save.setOnClickListener(v -> {
                    stopRecording();
                    NavStore.setPattern(this, p); // rebuilds through the listener
                });
                content.addView(save, Ui.fullWidth(this, 12));
            }
            Button cancel = Ui.outlined(this, getString(R.string.pattern_cancel));
            cancel.setOnClickListener(v -> {
                stopRecording();
                build();
            });
            content.addView(cancel, Ui.fullWidth(this, 12));
            return;
        }
        String p = NavStore.pattern(this);
        TextView now = Ui.bold(this, p == null ? getString(R.string.pattern_none)
                : getString(R.string.pattern_now, VolumePattern.arrows(p)), Ui.TEXT_BODY);
        if (p != null) now.setContentDescription(getString(R.string.pattern_now, words(p)));
        content.addView(now, Ui.fullWidth(this, 12));
        Button record = Ui.button(this, getString(R.string.pattern_record), Palette.Role.GREEN);
        record.setOnClickListener(v -> {
            recording = new StringBuilder();
            VolumeGuard.recording = true;
            build();
        });
        content.addView(record, Ui.fullWidth(this, 12));
        if (p != null) {
            Button remove = Ui.outlined(this, getString(R.string.pattern_remove));
            remove.setOnClickListener(v -> NavStore.setPattern(this, null));
            content.addView(remove, Ui.fullWidth(this, 12));
        }
    }

    /** "Up, Up, Down, Down" for TalkBack, instead of reading out arrows. */
    private String words(String p) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < p.length(); i++) {
            if (i > 0) b.append(", ");
            b.append(getString(p.charAt(i) == VolumePattern.UP ? R.string.pattern_up : R.string.pattern_down));
        }
        return b.toString();
    }

    private void stopRecording() {
        recording = null;
        VolumeGuard.recording = false;
    }

    /** While recording, the volume buttons are the input: no volume change. */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (recording != null && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            if (event.getRepeatCount() == 0 && recording.length() < VolumePattern.MAX_LENGTH) {
                recording.append(keyCode == KeyEvent.KEYCODE_VOLUME_UP ? VolumePattern.UP : VolumePattern.DOWN);
                build();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (recording != null && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /** What the volume buttons do while locked, and the limits for "Stay within the limits". */
    private void addKeysSection() {
        heading(R.string.keys_heading);
        VolumeKeys.Mode mode = NavStore.volumeKeys(this);
        choice(getString(R.string.keys_normal), mode == VolumeKeys.Mode.NORMAL,
                () -> NavStore.setVolumeKeys(this, VolumeKeys.Mode.NORMAL));
        choice(getString(R.string.keys_limits), mode == VolumeKeys.Mode.LIMITS,
                () -> NavStore.setVolumeKeys(this, VolumeKeys.Mode.LIMITS));
        choice(getString(R.string.keys_off), mode == VolumeKeys.Mode.OFF,
                () -> NavStore.setVolumeKeys(this, VolumeKeys.Mode.OFF));
        content.addView(Ui.muted(this, getString(R.string.keys_explain)), Ui.fullWidth(this, 12));
        if (mode != VolumeKeys.Mode.LIMITS) return;

        heading(R.string.limits_heading);
        content.addView(Ui.text(this, getString(R.string.limits_explain), Ui.TEXT_BODY),
                Ui.fullWidth(this, 8));
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        stepper(am, VolumeLimits.Stream.RING, false, R.string.limit_ring_min);
        stepper(am, VolumeLimits.Stream.RING, true, R.string.limit_ring_max);
        stepper(am, VolumeLimits.Stream.MEDIA, false, R.string.limit_media_min);
        stepper(am, VolumeLimits.Stream.MEDIA, true, R.string.limit_media_max);
        stepper(am, VolumeLimits.Stream.ALARM, false, R.string.limit_alarm_min);

        VolumeLimits ring = NavStore.limits(this, VolumeLimits.Stream.RING);
        int rs = AudioManager.STREAM_RING;
        if (ring.low(VolumeGuard.min(am, rs), am.getStreamMaxVolume(rs)) > 0 && !VolumeGuard.policyAccess(this)) {
            content.addView(Ui.text(this, getString(R.string.dnd_hint), Ui.TEXT_BODY), Ui.fullWidth(this, 16));
            Button dnd = Ui.outlined(this, getString(R.string.dnd_open));
            dnd.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                } catch (RuntimeException e) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            });
            content.addView(dnd, Ui.fullWidth(this, 12));
        }
    }

    /** One limit: "Ring volume, lowest: 2 of 7", then − Lower and + Higher side by side. */
    private void stepper(AudioManager am, VolumeLimits.Stream s, boolean max, int label) {
        int stream = VolumeGuard.stream(s);
        int lo = VolumeGuard.min(am, stream), hi = am.getStreamMaxVolume(stream);
        VolumeLimits l = NavStore.limits(this, s);
        int value = max ? l.high(lo, hi) : l.low(lo, hi);
        String text = getString(label, value, hi);
        content.addView(Ui.text(this, text, Ui.TEXT_BODY), Ui.fullWidth(this, 16));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button lower = Ui.outlined(this, getString(R.string.limit_lower));
        Button higher = Ui.outlined(this, getString(R.string.limit_higher));
        lower.setContentDescription(getString(R.string.limit_lower_desc, text));
        higher.setContentDescription(getString(R.string.limit_higher_desc, text));
        lower.setEnabled(value > lo);
        higher.setEnabled(value < hi);
        lower.setAlpha(value > lo ? 1f : 0.4f);
        higher.setAlpha(value < hi ? 1f : 0.4f);
        lower.setOnClickListener(v -> NavStore.setLimits(this, s,
                max ? l.withMax(value - 1, lo, hi) : l.withMin(value - 1, lo, hi)));
        higher.setOnClickListener(v -> NavStore.setLimits(this, s,
                max ? l.withMax(value + 1, lo, hi) : l.withMin(value + 1, lo, hi)));
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        left.setMarginEnd(Ui.dp(this, 12));
        row.addView(lower, left);
        row.addView(higher, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        content.addView(row, Ui.fullWidth(this, 8));
    }

    /**
     * Save the settings to a file the helper picks, or load them from one
     * (see NavSettingsBackup). Loading asks first, in place, like the
     * pattern's Save; the outcome stays under the buttons.
     */
    private void addBackupSection() {
        heading(R.string.backup_heading);
        content.addView(Ui.text(this, getString(R.string.backup_explain), Ui.TEXT_BODY),
                Ui.fullWidth(this, 8));
        if (pendingRestore != null) {
            content.addView(Ui.bold(this, getString(R.string.backup_confirm), Ui.TEXT_BODY),
                    Ui.fullWidth(this, 12));
            Button replace = Ui.button(this, getString(R.string.backup_replace), Palette.Role.RED);
            replace.setOnClickListener(v -> {
                String json = pendingRestore;
                pendingRestore = null;
                restore(json);
            });
            content.addView(replace, Ui.fullWidth(this, 12));
            Button cancel = Ui.outlined(this, getString(R.string.pattern_cancel));
            cancel.setOnClickListener(v -> {
                pendingRestore = null;
                build();
            });
            content.addView(cancel, Ui.fullWidth(this, 12));
            return;
        }
        Button save = Ui.button(this, getString(R.string.backup_save), Palette.Role.BLUE);
        save.setOnClickListener(v -> pick(BackupFiles.saveIntent(
                Backup.fileName("SeniorNav", BackupFiles.today())), REQUEST_SAVE));
        content.addView(save, Ui.fullWidth(this, 12));
        Button load = Ui.button(this, getString(R.string.backup_load), Palette.Role.BLUE);
        load.setOnClickListener(v -> pick(BackupFiles.openIntent(), REQUEST_LOAD));
        content.addView(load, Ui.fullWidth(this, 12));
        if (backupMessage != null) {
            TextView t = Ui.bold(this, backupMessage, Ui.TEXT_BODY);
            t.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);
            content.addView(t, Ui.fullWidth(this, 12));
        }
    }

    private void pick(Intent intent, int request) {
        backupMessage = null;
        try {
            startActivityForResult(intent, request);
        } catch (ActivityNotFoundException e) {
            backupMessage = getString(R.string.backup_no_picker);
            build();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQUEST_SAVE && request != REQUEST_LOAD) return;
        Uri uri = result == RESULT_OK && data != null ? data.getData() : null;
        if (uri == null) return; // cancelled in the picker
        // Locked while the picker was open: change nothing, show nothing.
        if (LauncherLock.locked(this)) return;
        if (request == REQUEST_SAVE) {
            try {
                BackupFiles.write(this, uri, NavSettingsBackup.export(this));
                backupMessage = getString(R.string.backup_saved);
            } catch (IOException | RuntimeException e) {
                backupMessage = getString(R.string.backup_error_save);
            }
            return; // onResume rebuilds
        }
        String json;
        try {
            json = BackupFiles.read(this, uri);
        } catch (IOException | RuntimeException e) {
            backupMessage = getString(R.string.backup_error_read);
            return;
        }
        // Check the file before asking, so a bad file is refused at once.
        try {
            Backup.settingsFor(json, getPackageName());
            pendingRestore = json;
        } catch (Backup.Failure f) {
            backupMessage = errorText(f.code);
        }
    }

    private void restore(String json) {
        NavSettingsBackup.Result r = NavSettingsBackup.restore(this, json);
        if (r.error != null) {
            backupMessage = errorText(r.error);
        } else if (r.skipped.length > 0) {
            backupMessage = getString(R.string.backup_restored) + " "
                    + getString(R.string.backup_not_found, String.join(", ", r.skipped));
        } else {
            backupMessage = getString(R.string.backup_restored);
        }
        build();
    }

    private String errorText(String code) {
        switch (code) {
            case Backup.ERROR_NEWER: return getString(R.string.backup_error_newer);
            case Backup.ERROR_NO_ENTRY: return getString(R.string.backup_error_no_entry, getString(R.string.app_name));
            case Backup.ERROR_LOCKED: return getString(R.string.locked);
            default: return getString(R.string.backup_error_malformed);
        }
    }

    private void addNavigationSection(NavMode mode, boolean locked) {
        TextView heading = Ui.bold(this, getString(R.string.nav_heading), 24);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) heading.setAccessibilityHeading(true);
        content.addView(heading, Ui.fullWidth(this, 32));

        int now;
        switch (mode) {
            case THREE_BUTTON: now = R.string.nav_three_button; break;
            case TWO_BUTTON: now = R.string.nav_two_button; break;
            case GESTURE: now = R.string.nav_gesture; break;
            default: now = R.string.nav_unknown; break;
        }
        content.addView(Ui.text(this, getString(now), Ui.TEXT_BODY), Ui.fullWidth(this, 8));

        if (mode.barFits()) {
            content.addView(Ui.muted(this, getString(R.string.nav_good)), Ui.fullWidth(this, 8));
            return;
        }
        content.addView(Ui.text(this, getString(R.string.nav_advice), Ui.TEXT_BODY),
                Ui.fullWidth(this, 12));
        if (!locked) {
            Button open = Ui.outlined(this, getString(R.string.open_navigation));
            open.setOnClickListener(v -> NavSettings.openNavigation(this));
            content.addView(open, Ui.fullWidth(this, 12));
        }
    }
}
