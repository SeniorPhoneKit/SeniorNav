package app.phueber.seniornav;

import android.app.Activity;
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

/**
 * The only screen, for the helper: whether the bar is on (in words, and by
 * colour), the way to switch it on, and the advice to use 3-button
 * navigation. The senior never needs it. Rebuilt on every resume, so it shows
 * the result of whatever the helper just did in Settings.
 */
public final class MainActivity extends Activity {
    private LinearLayout content;
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
        boolean locked = LauncherLock.locked(this);
        // "On" only with the window attached: a running service whose window the
        // system refused shows no bar, so it reads as "turned on but not running".
        BarStatus status = BarStatus.of(NavSettings.serviceTurnedOn(this),
                NavBarService.isRunning() && NavBarService.isAttached());

        boolean paused = NavStore.paused(this);
        content.addView(statusCard(status, paused), Ui.fullWidth(this, 8));
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
        t.setBackground(Ui.card(this, Ui.color(Palette.Role.CARD), Ui.color(Palette.Role.TEXT)));
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

    private TextView statusCard(BarStatus status, boolean paused) {
        int text, fill, stroke;
        switch (status) {
            case ON:
                // paused: the bar is hidden right now, so do not say "on"
                text = paused ? R.string.status_paused : R.string.status_on;
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
