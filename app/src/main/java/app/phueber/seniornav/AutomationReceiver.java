package app.phueber.seniornav;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Commands from automation apps (Trigly, Tasker, ...), sent as explicit
 * broadcasts: pause and show the bar, choose the button sides, press Back or
 * Home, hide the bar while unlocked or not, what the volume buttons do while
 * locked, the volume limits, remove the lock pattern. There is no command
 * that unlocks or sets a pattern. Settings go to {@link NavStore}, which the running service listens to;
 * Back and Home need the running service and are dropped without it.
 *
 * Open to any sender by decision (Trigly and Tasker must work without setup),
 * so Back and Home, which a plain app cannot do on its own, are rate-limited
 * ({@link AutomationLimit}): a flood cannot lock the user out of Settings.
 * BAR_HIDE has a budget of its own, so the helper's "Show the bar again" wins
 * against an app that keeps hiding it. The bar's own buttons and the main
 * screen do not pass through here and are never limited.
 */
public final class AutomationReceiver extends BroadcastReceiver {
    private static final String TAG = "SeniorNav";

    /** Back and Home share one budget. Process-wide; onReceive runs on the main thread. */
    private static final AutomationLimit presses = new AutomationLimit();
    /** Hides that actually hide the bar; a BAR_HIDE while paused costs nothing. */
    private static final AutomationLimit hides = new AutomationLimit();

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Automation.Command c = Automation.parse(intent.getAction());
        if (c == null) {
            Log.w(TAG, "Ignoring unknown action " + intent.getAction());
            return;
        }
        switch (c) {
            case BAR_HIDE:
                if (NavStore.paused(ctx)) break;
                if (hides.allow(SystemClock.elapsedRealtime())) {
                    NavStore.setPaused(ctx, true);
                } else {
                    Log.w(TAG, "Too many BAR_HIDE commands; ignoring");
                }
                break;
            case BAR_SHOW:
                NavStore.setPaused(ctx, false);
                break;
            case BAR_SIDES:
                Boolean right = Automation.backOnRight(intent.getStringExtra(Automation.EXTRA_BACK));
                if (right == null) {
                    Log.w(TAG, "BAR_SIDES needs extra back=left or back=right");
                } else {
                    NavStore.setBackRight(ctx, right);
                }
                break;
            case BAR_COLOUR:
                Boolean light = Automation.lightBar(intent.getStringExtra(Automation.EXTRA_COLOUR));
                if (light == null) {
                    Log.w(TAG, "BAR_COLOUR needs extra colour=dark or colour=light");
                } else {
                    NavStore.setLightBar(ctx, light);
                }
                break;
            case BACK:
                press(AccessibilityService.GLOBAL_ACTION_BACK);
                break;
            case HOME:
                press(AccessibilityService.GLOBAL_ACTION_HOME);
                break;
            case BAR_UNLOCKED:
                Boolean hide = Automation.onOff(intent.getStringExtra(Automation.EXTRA_HIDE));
                if (hide == null) {
                    Log.w(TAG, "BAR_UNLOCKED needs extra hide=on or hide=off");
                } else {
                    NavStore.setHideUnlocked(ctx, hide);
                }
                break;
            case VOLUME_KEYS:
                VolumeKeys.Mode mode = VolumeKeys.parse(intent.getStringExtra(Automation.EXTRA_MODE));
                if (mode == null) {
                    Log.w(TAG, "VOLUME_KEYS needs extra mode=normal, limits or off");
                } else {
                    NavStore.setVolumeKeys(ctx, mode);
                }
                break;
            case VOLUME_LIMIT:
                volumeLimit(ctx, intent);
                break;
            case PATTERN_REMOVE:
                NavStore.setPattern(ctx, null);
                break;
        }
    }

    /** Sets the min and/or max sent, in the stream's steps, kept within what the phone has. */
    private static void volumeLimit(Context ctx, Intent intent) {
        VolumeLimits.Stream s = Automation.stream(intent.getStringExtra(Automation.EXTRA_STREAM));
        int min = Automation.step(intent.getStringExtra(Automation.EXTRA_MIN), false);
        int max = Automation.step(intent.getStringExtra(Automation.EXTRA_MAX), true);
        if (s == null || min == Automation.BAD || max == Automation.BAD
                || (min == Automation.ABSENT && max == Automation.ABSENT)
                || (!s.hasMax && max != Automation.ABSENT)) {
            Log.w(TAG, "VOLUME_LIMIT needs stream=ring, media or alarm and min and/or max (not for alarm)");
            return;
        }
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        int stream = VolumeGuard.stream(s);
        int lo = VolumeGuard.min(am, stream), hi = am.getStreamMaxVolume(stream);
        VolumeLimits l = NavStore.limits(ctx, s);
        if (min != Automation.ABSENT) l = l.withMin(min, lo, hi);
        if (max != Automation.ABSENT) l = l.withMax(max == VolumeLimits.NO_MAX ? hi : max, lo, hi);
        NavStore.setLimits(ctx, s, l);
    }

    private static void press(int action) {
        if (!presses.allow(SystemClock.elapsedRealtime())) {
            Log.w(TAG, "Too many Back/Home commands; ignoring " + action);
            return;
        }
        if (!NavBarService.perform(action)) Log.w(TAG, "Bar service not running; ignoring " + action);
    }
}
