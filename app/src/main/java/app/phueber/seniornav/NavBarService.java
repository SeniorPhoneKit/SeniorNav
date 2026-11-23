package app.phueber.seniornav;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.KeyEvent;
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
 * It listens to window changes and to the volume buttons (no content, no
 * text, no other keys) and cannot read window content. The volume buttons go
 * to VolumeGuard: the helper's pattern that locks and unlocks SeniorLauncher's
 * shared lock, and what the buttons may do while locked; key filtering is
 * switched on only while one of these is in use. The bar is hidden over the
 * lock screen, whose own buttons and PIN pad must work, while an automation
 * app has paused it (NavStore.paused, BAR_HIDE), and while the helper has
 * unlocked SeniorLauncher's lock (NavStore.hideUnlocked). It stays over the keyboard,
 * where it covers only the system bar below the keys (Back then closes the
 * keyboard, as the system button does).
 *
 * The bar's window is attached once and stays attached while the service
 * runs; hiding makes it invisible and untouchable instead of removing it.
 *
 * While SeniorLauncher is locked and the helper chose so (NavStore.blockPullDown),
 * a second, invisible window over the status bar takes the touches that would
 * pull down the notifications and Quick Settings, and a System UI window event
 * closes the shade again if it opened some other way (PullDown).
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
     * Whether the senior can rely on the bar, so apps need no Back of their
     * own: running, its window attached, not paused by automation. Also true
     * while the bar steps aside because the helper unlocked: the helper then
     * uses the phone's own buttons, and the apps' Back would only be in the
     * way. Published by BarProvider.
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
    /** A screensaver (dream) is running, e.g. the charging clock. */
    private boolean dreaming;
    /** SeniorLauncher's lock, re-read on every change of its uri. */
    private LockState lock = LockState.NONE;
    private VolumeGuard volume;

    /** The invisible strip over the status bar, attached only while blocking applies. */
    private View strip;
    /** The window manager refused the strip: not tried again until the setting, a rotation or a reconnect. */
    private boolean stripRefused;
    /** Blocking applied at the last refresh. */
    private boolean blocking;
    private long lastClose;
    private final Runnable closeShade = this::closeShade;

    /** SeniorLauncher locked or unlocked: show or hide the bar, apply the limits. */
    private final ContentObserver lockChanges = new ContentObserver(handler) {
        @Override public void onChange(boolean selfChange) {
            readLock();
        }
    };

    /** Settings changed (by the screen or by automation): show them at once. */
    private final SharedPreferences.OnSharedPreferenceChangeListener settings = (p, key) -> {
        if (!running) return;
        if (key == null || NavStore.KEY_BACK_RIGHT.equals(key) || NavStore.KEY_LIGHT_BAR.equals(key)) {
            detach(); // a new bar with the new order or colours
            attach();
        }
        if (key == null || NavStore.KEY_PATTERN.equals(key) || NavStore.KEY_VOLUME_KEYS.equals(key)) {
            updateKeyFilter();
        }
        if (key == null || NavStore.KEY_VOLUME_KEYS.equals(key) || NavStore.isLimitKey(key)) {
            volume.enforce();
        }
        if (key == null || NavStore.KEY_BLOCK_PULLDOWN.equals(key)) stripRefused = false;
        refresh();
    };

    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (Intent.ACTION_DREAMING_STARTED.equals(a)) dreaming = true;
            else if (Intent.ACTION_DREAMING_STOPPED.equals(a)) dreaming = false;
            else if (Intent.ACTION_SCREEN_ON.equals(a)) readLock(); // in case a change went unannounced
            scheduleRefresh();
        }
    };

    /**
     * SeniorLauncher installed, updated, removed or its data cleared: none of
     * these notify the lock uri, so read the lock again (without it, a bar
     * hidden "while unlocked" would stay hidden after SeniorLauncher is gone).
     */
    private final BroadcastReceiver launcherPackage = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (i.getData() != null && LauncherLock.PACKAGE.equals(i.getData().getSchemeSpecificPart())) readLock();
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
        f.addAction(Intent.ACTION_DREAMING_STARTED);
        f.addAction(Intent.ACTION_DREAMING_STOPPED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screen, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screen, f);
        }
        IntentFilter pkg = new IntentFilter();
        pkg.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkg.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pkg.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkg.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        pkg.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(launcherPackage, pkg, Context.RECEIVER_EXPORTED); // system broadcasts
        } else {
            registerReceiver(launcherPackage, pkg);
        }
        volume = new VolumeGuard(this, handler);
        volume.start();
        try {
            getContentResolver().registerContentObserver(LauncherLock.STATE, false, lockChanges);
        } catch (RuntimeException ignored) {
            // SeniorLauncher not installed: no lock to follow
        }
        readLock();
        updateKeyFilter();
        NavStore.prefs(this).registerOnSharedPreferenceChangeListener(settings);
        instance = this;
        attach();
        refresh();
        BarProvider.notifyChanged(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // The shade opened anyway (a launcher's "swipe down anywhere", a
        // swipe while the strip was not there yet): close it at once. Only
        // the package of the event is looked at; anything else from System UI
        // (volume panel, ...) just gets a close that does nothing.
        if (blocking && PullDown.fromSystemUi(event.getPackageName())) closeShade();
        scheduleRefresh();
    }

    /** Volume Up/Down only, and only while key filtering is on (updateKeyFilter). */
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return running && volume != null && volume.onKey(event);
    }

    /**
     * Asks for key events only while the pattern or the locked behaviour needs
     * them; otherwise the service sees no key at all.
     */
    private void updateKeyFilter() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;
        boolean need = VolumeKeys.needKeys(NavStore.pattern(this), NavStore.volumeKeys(this));
        int flags = need
                ? info.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                : info.flags & ~AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        if (flags == info.flags) return;
        info.flags = flags;
        setServiceInfo(info);
    }

    private void readLock() {
        if (!running) return;
        lock = LauncherLock.read(this);
        volume.setLock(lock);
        refresh();
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
        detachStrip(); // again at the new status bar height
        stripRefused = false;
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
        getContentResolver().unregisterContentObserver(lockChanges);
        if (volume != null) volume.stop();
        handler.removeCallbacks(refresh);
        handler.removeCallbacks(retryAttach);
        handler.removeCallbacks(closeShade);
        try {
            unregisterReceiver(screen);
            unregisterReceiver(launcherPackage);
        } catch (IllegalArgumentException ignored) {
            // never registered
        }
        detach();
        detachStrip();
        blocking = false;
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
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();
        // Screen off (the always-on display) and a screensaver (the charging
        // clock, a dream: the screen counts as on) draw the overlay where a tap
        // only wakes the phone. The keyguard check alone misses both: when they
        // start the lock may not be engaged yet (lock delay), and no later
        // event re-checks until the screen wakes.
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean interactive = pm == null || pm.isInteractive();
        // Unlocked by the helper, while the lock is in use: the helper has the
        // phone and uses its own buttons (BarProvider still says 1, so the
        // apps do not bring back their own Back meanwhile).
        boolean helper = lock.hidesBar(NavStore.hideUnlocked(this));
        updatePullDown(PullDown.blocks(NavStore.blockPullDown(this), lock, locked, interactive, dreaming));
        if (!attached && SystemClock.uptimeMillis() >= nextAttachAt) attach(); // refused before
        if (!attached) return;
        setVisible(interactive && !dreaming && !locked && !NavStore.paused(this) && !helper);
    }

    /**
     * Blocking starts or stops: the strip comes or goes. Not paused by
     * BAR_HIDE: that pauses the bar, not the lock. Starting also closes a
     * shade that is open at that moment.
     */
    private void updatePullDown(boolean block) {
        boolean was = blocking;
        blocking = block;
        if (!block) {
            detachStrip();
            handler.removeCallbacks(closeShade);
            return;
        }
        attachStrip();
        if (!was) closeShade();
    }

    @SuppressLint("ClickableViewAccessibility") // it only swallows touches; there is nothing to click
    private void attachStrip() {
        if (strip != null || stripRefused || wm == null) return;
        View v = new View(this);
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        v.setOnTouchListener((view, e) -> true);
        try {
            wm.addView(v, Bar.stripParams(Bar.statusBarHeight(this, wm)));
            strip = v;
        } catch (RuntimeException e) {
            stripRefused = true; // the safety net still closes the shade
        }
    }

    private void detachStrip() {
        if (strip == null) return;
        try {
            wm.removeViewImmediate(strip);
        } catch (RuntimeException ignored) {
            // already gone
        }
        strip = null;
    }

    /**
     * Closes the notification shade (Android 12+; a no-op when it is closed),
     * at most every PullDown.CLOSE_GAP_MS, so it can never run in a tight
     * loop; a request within the gap is done once the gap is over. Older
     * versions have no such action, and Back would be a guess (the service
     * cannot tell the shade from other System UI windows), so there only the
     * strip blocks.
     */
    private void closeShade() {
        if (!blocking || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        long now = SystemClock.uptimeMillis();
        handler.removeCallbacks(closeShade);
        if (!PullDown.mayClose(now, lastClose)) {
            handler.postDelayed(closeShade, lastClose + PullDown.CLOSE_GAP_MS - now);
            return;
        }
        lastClose = now;
        performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
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
