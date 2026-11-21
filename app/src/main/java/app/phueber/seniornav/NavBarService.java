package app.phueber.seniornav;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

/**
 * The accessibility service that shows the bar. It is the only way for an app
 * to press Back for another app (performGlobalAction) and to draw above the
 * system navigation bar (TYPE_ACCESSIBILITY_OVERLAY).
 *
 * It listens only to window changes (no content, no text, no key events) and
 * cannot read window content. The bar is hidden over the lock screen, whose
 * own buttons and PIN pad must work, and while an automation app has paused
 * it (NavStore.paused, BAR_HIDE). It stays over the keyboard,
 * where it covers only the system bar below the keys (Back then closes the
 * keyboard, as the system button does).
 *
 * The bar's window is attached once and stays attached while the service
 * runs; hiding makes it invisible and untouchable instead of removing it.
 */
public final class NavBarService extends AccessibilityService {
    /** Whether the service is connected right now (for the main screen). */
    private static volatile boolean running;

    static boolean isRunning() {
        return running;
    }

    /** Whether the bar's window is attached right now (it may still be hidden). */
    private static volatile boolean windowAttached;

    static boolean isAttached() {
        return windowAttached;
    }

    /**
     * Whether the bar is on screen for apps to rely on: running, its window
     * attached, not paused by automation. Published by BarProvider.
     */
    static boolean barVisible(Context c) {
        return running && windowAttached && !NavStore.paused(c);
    }

    /** The connected service, for Back/Home from automation; null when not running. */
    @SuppressLint("StaticFieldLeak") // set on connect, cleared in stop(): never outlives the service
    private static volatile NavBarService instance;

    /** Presses Back or Home through the running service; false if it is not running. */
    static boolean perform(int globalAction) {
        NavBarService s = instance;
        return s != null && s.performGlobalAction(globalAction);
    }

    /** Window events come in bursts during a transition; decide once it settles. */
    private static final long SETTLE_MS = 150;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = this::refresh;
    /** A refused window is tried again later (AttachRetry), apart from refresh's own scheduling. */
    private final Runnable retryAttach = this::refresh;
    private int failedAttaches;
    private long nextAttachAt;

    private WindowManager wm;
    private FrameLayout root;   // the attached window's view, always visible
    private View bar;           // the bar inside it, visible or invisible
    private int barHeight;
    private boolean attached;
    private boolean visible;

    /** Settings changed (by the screen or by automation): show them at once. */
    private final SharedPreferences.OnSharedPreferenceChangeListener settings = (p, key) -> {
        if (!running) return;
        if (key == null || NavStore.KEY_BACK_RIGHT.equals(key) || NavStore.KEY_LIGHT_BAR.equals(key)) {
            detach(); // a new bar with the new order or colours
            attach();
        }
        refresh();
    };

    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            scheduleRefresh();
        }
    };

    @Override
    protected void onServiceConnected() {
        running = true;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screen, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screen, f);
        }
        NavStore.prefs(this).registerOnSharedPreferenceChangeListener(settings);
        instance = this;
        attach();
        refresh();
        BarProvider.notifyChanged(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        scheduleRefresh();
    }

    @Override
    public void onInterrupt() {
        // no feedback of our own to stop
    }

    /**
     * Rotation, Display size, font or language changed: a new bar for the new
     * screen, again at the bottom (also in landscape, see ARCHITECTURE.md).
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!running) return;
        detach();
        attach();
        refresh();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stop();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    private void stop() {
        if (!running) return;
        running = false;
        instance = null;
        NavStore.prefs(this).unregisterOnSharedPreferenceChangeListener(settings);
        handler.removeCallbacks(refresh);
        handler.removeCallbacks(retryAttach);
        try {
            unregisterReceiver(screen);
        } catch (IllegalArgumentException ignored) {
            // never registered
        }
        detach();
        BarProvider.notifyChanged(this);
    }

    private void scheduleRefresh() {
        handler.removeCallbacks(refresh);
        handler.postDelayed(refresh, SETTLE_MS);
    }

    private void attach() {
        if (attached || wm == null) return;
        barHeight = Bar.height(this, wm);
        bar = Bar.view(this, barHeight, NavStore.backRight(this), NavStore.lightBar(this), new Bar.Actions() {
            @Override public void back() {
                performGlobalAction(GLOBAL_ACTION_BACK);
            }

            @Override public void home() {
                performGlobalAction(GLOBAL_ACTION_HOME);
            }
        });
        bar.setVisibility(View.INVISIBLE);
        root = new FrameLayout(this);
        root.addView(bar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        visible = false;
        try {
            wm.addView(root, Bar.params(barHeight, false));
            attached = true;
            windowAttached = true;
            failedAttaches = 0;
            handler.removeCallbacks(retryAttach);
            BarProvider.notifyChanged(this);
        } catch (RuntimeException e) {
            // The window manager refused. The service is still running, so
            // without a retry there would be no bar until the next reconnect:
            // try again later, waiting longer each time (refresh does it).
            root = null;
            bar = null;
            failedAttaches++;
            long wait = AttachRetry.delayMs(failedAttaches);
            nextAttachAt = SystemClock.uptimeMillis() + wait;
            handler.removeCallbacks(retryAttach);
            handler.postDelayed(retryAttach, wait);
        }
    }

    private void detach() {
        if (!attached) return;
        try {
            wm.removeViewImmediate(root);
        } catch (RuntimeException ignored) {
            // already gone
        }
        attached = false;
        windowAttached = false;
        visible = false;
        root = null;
        bar = null;
        BarProvider.notifyChanged(this);
    }

    private void refresh() {
        if (!running) return;
        if (!attached && SystemClock.uptimeMillis() >= nextAttachAt) attach(); // refused before
        if (!attached) return;
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();
        setVisible(!locked && !NavStore.paused(this));
    }

    private void setVisible(boolean show) {
        if (show == visible) return;
        try {
            wm.updateViewLayout(root, Bar.params(barHeight, show));
            bar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            visible = show;
        } catch (RuntimeException ignored) {
            // window gone (service stopping): nothing to show
        }
    }
}
