package app.phueber.seniornav;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Shared building blocks, the same as in the other Senior apps. All UI is built
 * in code; there are no layout files.
 *
 * Rules:
 * - Buttons are at least 64dp high, full width, text 22sp.
 * - Green = the main action, blue = neutral. White text on both.
 * - The screen has a big Back button pinned at the bottom, which goes home.
 */
final class Ui {
    static final int TEXT_BODY = 20;
    static final int TEXT_BUTTON = 22;
    static final int TEXT_TITLE = 28;
    static final int BUTTON_HEIGHT = 64;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    static int color(Palette.Role role) {
        return Palette.color(role);
    }

    static TextView text(Context c, CharSequence s, int sp) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color(Palette.Role.TEXT));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        return t;
    }

    static TextView bold(Context c, CharSequence s, int sp) {
        TextView t = text(c, s, sp);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static TextView muted(Context c, CharSequence s) {
        TextView t = text(c, s, TEXT_BODY);
        t.setTextColor(color(Palette.Role.TEXT_MUTED));
        return t;
    }

    /** Rounded fill with a press ripple; stroke 0 means no border. */
    static RippleDrawable ripple(Context c, Palette.Role press, int fill, int stroke, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) shape.setStroke(dp(c, 2), stroke);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(Math.max(1, dp(c, radiusDp)));
        return new RippleDrawable(ColorStateList.valueOf(color(press)), shape, mask);
    }

    /** A rounded card with a border, for the status boxes. */
    static GradientDrawable card(Context c, int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, 20));
        g.setStroke(dp(c, 2), stroke);
        return g;
    }

    /** Solid button, white bold text. role: GREEN / BLUE. */
    static Button button(Context c, CharSequence label, Palette.Role role) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Palette.ON_COLOR);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_BUTTON);
        b.setMinHeight(dp(c, BUTTON_HEIGHT));
        b.setMinimumHeight(dp(c, BUTTON_HEIGHT));
        b.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        b.setStateListAnimator(null);
        b.setBackground(ripple(c, Palette.Role.PRESS_ON_COLOR, color(role), 0, 20));
        return b;
    }

    /** White card button with a dark border and dark text: secondary actions. */
    static Button outlined(Context c, CharSequence label) {
        Button b = button(c, label, Palette.Role.CARD);
        b.setTextColor(color(Palette.Role.TEXT));
        b.setBackground(ripple(c, Palette.Role.PRESS_ON_CARD, color(Palette.Role.CARD),
                color(Palette.Role.TEXT), 20));
        return b;
    }

    static LinearLayout.LayoutParams fullWidth(Context c, int topMarginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        return lp;
    }

    /** The pinned "← Back" button: straight to the home screen (see goHome). */
    static View backButton(Activity a) {
        int fg = color(Palette.Role.TEXT);
        LinearLayout b = new LinearLayout(a);
        b.setGravity(Gravity.CENTER);
        b.setMinimumHeight(dp(a, BUTTON_HEIGHT));
        b.setPadding(dp(a, 16), dp(a, 8), dp(a, 16), dp(a, 8));
        b.setBackground(ripple(a, Palette.Role.PRESS_ON_CARD, color(Palette.Role.CARD), fg, 20));
        b.setClickable(true);
        b.setFocusable(true);
        b.setContentDescription(a.getString(R.string.back));
        b.setOnClickListener(v -> goHome(a));

        TextView label = bold(a, a.getString(R.string.back), TEXT_BUTTON);
        label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ImageView arrow = new ImageView(a);
        arrow.setImageResource(R.drawable.ic_arrow_back);
        arrow.setImageTintList(ColorStateList.valueOf(fg));
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        int size = Math.round(label.getTextSize() * 1.2f);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(size, size);
        alp.setMarginEnd(dp(a, 10));
        b.addView(arrow, alp);
        b.addView(label);
        NavBar.hideWhileOn(b); // SeniorNav's bar already has Back and Home
        return b;
    }

    /**
     * Back goes straight to the home screen, in one tap, like in every Senior
     * app; the screen is closed behind it.
     */
    static void goHome(Activity a) {
        try {
            a.startActivity(new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException ignored) {
            // no home app answered: closing is the next best thing
        }
        a.finishAffinity();
    }

    /**
     * Sets up the screen: a title, a scrolling content column, and Back pinned
     * below it. Returns the content column to fill. Calls setContentView.
     */
    static LinearLayout screen(Activity a, CharSequence title) {
        int p = dp(a, 20);
        LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setFitsSystemWindows(true);

        TextView t = bold(a, title, TEXT_TITLE);
        t.setPadding(p, dp(a, 16), p, dp(a, 8));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) t.setAccessibilityHeading(true);
        page.addView(t);

        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(p, 0, p, dp(a, 16));
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(p, dp(a, 8), p, dp(a, 12));
        page.addView(backButton(a), blp);
        a.setContentView(page);
        return content;
    }
}
