package app.phueber.seniornav;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Commands from automation apps (Trigly, Tasker, ...), sent as explicit
 * broadcasts: pause and show the bar, choose the button sides, press Back or
 * Home. Settings go to {@link NavStore}, which the running service listens to;
 * Back and Home need the running service and are dropped without it.
 */
public final class AutomationReceiver extends BroadcastReceiver {
    private static final String TAG = "SeniorNav";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Automation.Command c = Automation.parse(intent.getAction());
        if (c == null) {
            Log.w(TAG, "Ignoring unknown action " + intent.getAction());
            return;
        }
        switch (c) {
            case BAR_HIDE:
                NavStore.setPaused(ctx, true);
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
        if (!NavBarService.perform(action)) Log.w(TAG, "Bar service not running; ignoring " + action);
    }
}
