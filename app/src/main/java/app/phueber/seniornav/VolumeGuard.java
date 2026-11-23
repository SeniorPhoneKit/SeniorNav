package app.phueber.seniornav;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

/**
 * The volume buttons, for {@link NavBarService}: the helper's pattern that
 * locks and unlocks SeniorLauncher's shared lock, what the buttons do while
 * locked, and the volume limits. Thin glue; the decisions are in the pure
 * {@link VolumePattern}, {@link VolumeKeys} and {@link VolumeLimits}.
 *
 * Sees Volume Up and Volume Down only (the service returns false for every
 * other key before it gets here), and only while the system hands keys to
 * apps: with the screen off, or while the phone rings (Telecom takes the press
 * to silence the ringer), they never arrive.
 */
final class VolumeGuard {
    private static final String TAG = "SeniorNav";

    /**
     * The main screen is recording a pattern: presses go to it untouched and
     * do not count as the old pattern. Same process; main thread only.
     */
    static volatile boolean recording;

    /** A second restore after the pattern, in case the system applied its last press late. */
    private static final long RESTORE_AGAIN_MS = 400;

    /** The streams a volume press may change, restored after a pattern. */
    private static final int[] STREAMS = {
            AudioManager.STREAM_RING, AudioManager.STREAM_MUSIC, AudioManager.STREAM_ALARM,
            AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_VOICE_CALL,
            AudioManager.STREAM_ACCESSIBILITY};

    /** The volumes and the ringer mode just before a press. */
    private static final class Volumes {
        final int[] volume = new int[STREAMS.length];
        final int ringerMode;

        Volumes(AudioManager am) {
            for (int i = 0; i < STREAMS.length; i++) volume[i] = am.getStreamVolume(STREAMS[i]);
            ringerMode = am.getRingerMode();
        }
    }

    private final Context context;
    private final Handler handler;
    private final AudioManager am;
    private final VolumePattern.Matcher<Volumes> matcher = new VolumePattern.Matcher<>();
    /** Per key (0 Up, 1 Down): held down right now, and whether its press was swallowed. */
    private final boolean[] down = new boolean[2];
    private final boolean[] swallowed = new boolean[2];
    private LockState lock = LockState.NONE;
    private boolean registered;

    /** Volume changed elsewhere (the slider, another app) or the ringer mode: bring it back into range. */
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            enforce();
        }
    };

    VolumeGuard(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        this.am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    void start() {
        if (registered) return;
        IntentFilter f = new IntentFilter();
        // Not in the SDK, but sent by every Android version for every volume change.
        f.addAction("android.media.VOLUME_CHANGED_ACTION");
        f.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // system broadcasts only; RECEIVER_NOT_EXPORTED would not receive them on all versions
            context.registerReceiver(changes, f, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(changes, f);
        }
        registered = true;
    }

    void stop() {
        handler.removeCallbacksAndMessages(this);
        matcher.reset();
        if (!registered) return;
        try {
            context.unregisterReceiver(changes);
        } catch (IllegalArgumentException ignored) {
            // never registered
        }
        registered = false;
    }

    /** The lock changed (NavBarService watches SeniorLauncher's uri). */
    void setLock(LockState s) {
        lock = s;
        enforce();
    }

    /** One key event; true swallows it. Only Volume Up and Down are handled. */
    boolean onKey(KeyEvent e) {
        int code = e.getKeyCode();
        if (am == null || (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN)) return false;
        int k = code == KeyEvent.KEYCODE_VOLUME_UP ? 0 : 1;
        if (e.getAction() == KeyEvent.ACTION_UP) {
            boolean s = swallowed[k];
            down[k] = false;
            swallowed[k] = false;
            return s; // a release goes where its press went
        }
        if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (e.getRepeatCount() > 0) return swallowed[k]; // held: like its first press, not a new one
        down[k] = true;
        // Ringing or in a call (also a WhatsApp call): never touched, so a press
        // still silences the ringer or sets the call volume.
        boolean call = am.getMode() != AudioManager.MODE_NORMAL;
        if (call || recording || down[1 - k]) {
            // both keys held is Android's accessibility shortcut: not a pattern press
            matcher.reset();
        } else {
            Volumes first = matcher.press(NavStore.pattern(context),
                    k == 0 ? VolumePattern.UP : VolumePattern.DOWN, e.getEventTime(), new Volumes(am));
            if (first != null) {
                swallowed[k] = true; // the last press changes nothing, the earlier ones are undone
                onPattern(first);
                return true;
            }
        }
        if (recording) {
            swallowed[k] = false;
            return false;
        }
        switch (VolumeKeys.decide(NavStore.volumeKeys(context), lock.locked, call)) {
            case BLOCK:
                swallowed[k] = true;
                break;
            case LIMIT:
                swallowed[k] = !stepAllowed(k == 0 ? 1 : -1);
                if (swallowed[k]) showPanel();
                break;
            default:
                swallowed[k] = false;
                break;
        }
        return swallowed[k];
    }

    /**
     * Whether a step stays within the limits. Android does not say which
     * stream a press will change: with music playing it is media; otherwise it
     * depends on the phone (media on recent Android, ring on some makers'), so
     * both are checked. Swallowed only when it would leave the range on every
     * candidate; anything that still slips out is brought back by enforce().
     */
    private boolean stepAllowed(int direction) {
        if (am.isMusicActive()) return allowed(VolumeLimits.Stream.MEDIA, direction);
        return allowed(VolumeLimits.Stream.MEDIA, direction) || allowed(VolumeLimits.Stream.RING, direction);
    }

    private boolean allowed(VolumeLimits.Stream s, int direction) {
        int stream = stream(s);
        if (s == VolumeLimits.Stream.RING && am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) return true;
        return NavStore.limits(context, s).stepAllowed(am.getStreamVolume(stream), direction,
                min(stream), am.getStreamMaxVolume(stream));
    }

    /** At a limit: show the volume panel unchanged, so the press is seen to do something. */
    private void showPanel() {
        try {
            am.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME,
                    AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI);
        } catch (RuntimeException ignored) {
            // nothing to show
        }
    }

    private void onPattern(Volumes before) {
        restore(before);
        handler.postAtTime(() -> restore(before), this,
                android.os.SystemClock.uptimeMillis() + RESTORE_AGAIN_MS);
        LauncherLock.Toggle t = LauncherLock.toggle(context);
        int text;
        switch (t) {
            case LOCKED: text = R.string.pattern_locked; break;
            case UNLOCKED: text = R.string.pattern_unlocked; break;
            case NO_PIN: text = R.string.pattern_no_pin; break;
            default: text = R.string.pattern_no_launcher; break;
        }
        if (t == LauncherLock.Toggle.LOCKED || t == LauncherLock.Toggle.UNLOCKED) vibrate();
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Volume pattern: " + t);
    }

    /** Puts every stream and the ringer mode back to how they were before the pattern. */
    private void restore(Volumes v) {
        setRingerMode(v.ringerMode);
        for (int i = 0; i < STREAMS.length; i++) {
            try {
                if (am.getStreamVolume(STREAMS[i]) != v.volume[i]) am.setStreamVolume(STREAMS[i], v.volume[i], 0);
            } catch (RuntimeException e) {
                // Do Not Disturb without access, or a fixed-volume stream: leave it
            }
        }
        setRingerMode(v.ringerMode); // a ring volume set above may have changed it
        enforce();
    }

    private void setRingerMode(int mode) {
        try {
            if (am.getRingerMode() != mode) am.setRingerMode(mode);
        } catch (RuntimeException e) {
            // leaving or entering silent needs Do Not Disturb access
        }
    }

    /**
     * While locked with "Stay within the limits": every limited stream back
     * into range, and the ringer back from vibrate or silent if the ring has a
     * minimum. Coming back from silent needs Do Not Disturb access; without
     * it the ringer stays as it is and the main screen asks the helper for it.
     */
    void enforce() {
        if (am == null || !VolumeKeys.enforceLimits(NavStore.volumeKeys(context), lock.locked)) return;
        VolumeLimits ring = NavStore.limits(context, VolumeLimits.Stream.RING);
        int ringStream = AudioManager.STREAM_RING;
        if (ring.low(min(ringStream), am.getStreamMaxVolume(ringStream)) > 0
                && am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL
                && (am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE || policyAccess(context))) {
            setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
        for (VolumeLimits.Stream s : VolumeLimits.Stream.values()) {
            int stream = stream(s);
            // in vibrate or silent the ring reads 0; raising it there would fight the ringer mode
            if (s == VolumeLimits.Stream.RING && am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) continue;
            int v = am.getStreamVolume(stream);
            int t = NavStore.limits(context, s).clamp(v, min(stream), am.getStreamMaxVolume(stream));
            if (t == v) continue;
            try {
                am.setStreamVolume(stream, t, 0);
            } catch (RuntimeException e) {
                Log.w(TAG, "Cannot bring stream " + stream + " back into range: " + e);
            }
        }
    }

    static boolean policyAccess(Context c) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    private void vibrate() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;
        try {
            v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (RuntimeException ignored) {
            // no vibration, the toast still says it
        }
    }

    static int stream(VolumeLimits.Stream s) {
        switch (s) {
            case RING: return AudioManager.STREAM_RING;
            case ALARM: return AudioManager.STREAM_ALARM;
            default: return AudioManager.STREAM_MUSIC;
        }
    }

    /** The lowest step a stream allows (the alarm usually 1); 0 before Android 9. */
    static int min(AudioManager am, int stream) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? am.getStreamMinVolume(stream) : 0;
    }

    private int min(int stream) {
        return min(am, stream);
    }
}
