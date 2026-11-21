package app.phueber.seniornav;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The bar itself: its view (Back on the left, Home on the right, or the other
 * way round if the helper chose so; each half the width, icon and word) and its window, an accessibility overlay pinned to the
 * bottom edge, exactly over the system navigation bar.
 *
 * TYPE_ACCESSIBILITY_OVERLAY is layered above the navigation bar, so the bar
 * takes the taps that would have gone to the system buttons. A
 * SYSTEM_ALERT_WINDOW overlay would sit below the navigation bar, and could not
 * do Back either.
 */
final class Bar {
    interface Actions {
        void back();
        void home();
    }

    private Bar() {}

    /**
     * Window parameters for a bar of this height, at the bottom, full width.
     * Hidden, the window stays attached but lets every touch through to what
     * is below (FLAG_NOT_TOUCHABLE) and draws nothing.
     */
    static WindowManager.LayoutParams params(int heightPx, boolean visible) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT); // transparent while hidden
        lp.gravity = Gravity.BOTTOM | Gravity.FILL_HORIZONTAL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Do not lay out inside the system bars: the whole point is to cover
            // the navigation bar.
            lp.setFitInsetsTypes(0);
        }
        if (!visible) lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        lp.setTitle("SeniorNav");
        return lp;
    }

    /**
     * The height of the system navigation bar (see {@link BarHeight}): the
     * inset the window manager reports (Android 11+), else the framework's
     * navigation_bar_height, never below 48dp.
     */
    @SuppressLint({"DiscouragedApi", "InternalInsetResource"}) // only the fallback; no public API without a window
    static int height(Context c, WindowManager wm) {
        int inset = 0, resource = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                inset = wm.getCurrentWindowMetrics().getWindowInsets()
                        .getInsets(WindowInsets.Type.navigationBars()).bottom;
            } catch (RuntimeException ignored) {
                // not a visual context on this version: use the resource
            }
        }
        try {
            Resources r = c.getResources();
            int id = r.getIdentifier("navigation_bar_height", "dimen", "android");
            if (id != 0) resource = r.getDimensionPixelSize(id);
        } catch (RuntimeException ignored) {
            // the floor below
        }
        int floor = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                BarHeight.FLOOR_DP, c.getResources().getDisplayMetrics()));
        return BarHeight.choose(inset, resource, floor);
    }

    /**
     * {@code backRight}: Home on the left and Back on the right instead of the default.
     * {@code light}: white bar with dark icons instead of the default dark bar.
     */
    static View view(Context c, int heightPx, boolean backRight, boolean light, Actions actions) {
        int bg = Ui.color(light ? Palette.Role.BAR_LIGHT : Palette.Role.BAR);
        int fg = Ui.color(light ? Palette.Role.ON_BAR_LIGHT : Palette.Role.ON_BAR);
        Palette.Role press = light ? Palette.Role.PRESS_ON_CARD : Palette.Role.PRESS_ON_COLOR;
        LinearLayout bar = new LinearLayout(c);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        // "left" and "right" mean the screen's sides, also in right-to-left languages.
        bar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        if (light) {
            // A light bar would melt into white app screens: a line on top marks its edge.
            android.graphics.drawable.ColorDrawable line = new android.graphics.drawable.ColorDrawable(
                    Ui.color(Palette.Role.BAR_DIVIDER_LIGHT));
            android.graphics.drawable.InsetDrawable fill = new android.graphics.drawable.InsetDrawable(
                    new android.graphics.drawable.ColorDrawable(bg), 0, Math.max(1, Ui.dp(c, 2)), 0, 0);
            bar.setBackground(new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[] {line, fill}));
        } else {
            bar.setBackgroundColor(bg);
        }

        View back = half(c, heightPx, fg, bg, press, R.drawable.ic_arrow_back, R.string.bar_back, actions::back);
        View home = half(c, heightPx, fg, bg, press, R.drawable.ic_home, R.string.bar_home, actions::home);
        View first = backRight ? home : back;
        View second = backRight ? back : home;
        bar.addView(first, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        View divider = new View(c);
        divider.setBackgroundColor(Ui.color(light ? Palette.Role.BAR_DIVIDER_LIGHT : Palette.Role.BAR_DIVIDER));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                Math.max(1, Ui.dp(c, 2)), ViewGroup.LayoutParams.MATCH_PARENT);
        dlp.topMargin = dlp.bottomMargin = heightPx / 6;
        bar.addView(divider, dlp);
        bar.addView(second, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return bar;
    }

    /**
     * One half: icon and word side by side, sized from the bar height rather
     * than the font scale, so they always fit the bar. Display size makes the
     * bar and everything on it bigger together.
     */
    private static View half(Context c, int heightPx, int fg, int bg, Palette.Role press,
            int icon, int label, Runnable action) {
        LinearLayout b = new LinearLayout(c);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.ripple(c, press, bg, 0, 0));
        b.setClickable(true);
        b.setFocusable(true);
        b.setContentDescription(c.getString(label));
        b.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            action.run();
        });

        ImageView i = new ImageView(c);
        i.setImageResource(icon);
        i.setImageTintList(ColorStateList.valueOf(fg));
        i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int size = Math.round(heightPx * 0.6f);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(size, size);
        ilp.setMarginEnd(Math.round(heightPx * 0.15f));
        b.addView(i, ilp);

        TextView t = new TextView(c);
        t.setText(label);
        t.setTextColor(fg);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, heightPx * 0.42f);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        b.addView(t);
        return b;
    }
}
