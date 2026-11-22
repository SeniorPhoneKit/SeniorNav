package app.phueber.seniornav;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

/**
 * Commands from automation apps (Trigly, Tasker, ...), sent as explicit
 * broadcasts: pause and show the bar, choose the button sides, press Back or
 * Home. Settings go to {@link NavStore}, which the running service listens to;
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
        }
    }

    private static void press(int action) {
        if (!presses.allow(SystemClock.elapsedRealtime())) {
            Log.w(TAG, "Too many Back/Home commands; ignoring " + action);
            return;
        }
        if (!NavBarService.perform(action)) Log.w(TAG, "Bar service not running; ignoring " + action);
    }
}
