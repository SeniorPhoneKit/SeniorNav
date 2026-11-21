package app.phueber.seniornav;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

/**
 * Whether the "Back & Home" bar is on screen. Its big Back and Home sit at the
 * bottom of every screen, so the pinned Back button would only repeat them: it
 * is hidden while the bar shows and comes back while it does not, also while
 * automation has paused it (then the bar's Back is not there to use). The same
 * rule the other Senior apps follow through {@link BarProvider}; here, in the
 * bar's own process, it is read directly.
 */
final class NavBar {
    private NavBar() {}

    static boolean on(Context c) {
        return NavBarService.barVisible(c);
    }

    /**
     * Hides {@code back} while the bar is on: re-checked each time the window
     * gets focus, and on every change BarProvider announces while attached.
     */
    static void hideWhileOn(View back) {
        Runnable update = () -> back.setVisibility(on(back.getContext()) ? View.GONE : View.VISIBLE);
        update.run();
        back.getViewTreeObserver().addOnWindowFocusChangeListener(focused -> {
            if (focused) update.run();
        });
        ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override public void onChange(boolean selfChange) {
                update.run();
            }
        };
        View.OnAttachStateChangeListener watch = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {
                v.getContext().getContentResolver().registerContentObserver(BarProvider.STATE, false, observer);
                update.run();
            }

            @Override public void onViewDetachedFromWindow(View v) {
                v.getContext().getContentResolver().unregisterContentObserver(observer);
            }
        };
        back.addOnAttachStateChangeListener(watch);
        if (back.isAttachedToWindow()) watch.onViewAttachedToWindow(back);
    }
}
