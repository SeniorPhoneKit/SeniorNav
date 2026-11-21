package app.phueber.seniornav;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = Ui.screen(this, getString(R.string.app_name));
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    private void build() {
        content.removeAllViews();
        boolean locked = LauncherLock.locked(this);
        BarStatus status = BarStatus.of(NavSettings.serviceTurnedOn(this), NavBarService.isRunning());

        content.addView(statusCard(status), Ui.fullWidth(this, 8));
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

        addNavigationSection(NavSettings.navMode(this), locked);
    }

    private TextView statusCard(BarStatus status) {
        int text, fill, stroke;
        switch (status) {
            case ON:
                text = R.string.status_on;
                fill = Ui.color(Palette.Role.GREEN_LIGHT);
                stroke = Ui.color(Palette.Role.GREEN);
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
