package com.example.qatra;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Ui {
    public static final int RED = Color.parseColor("#D32F2F");
    public static final int DARK_RED = Color.parseColor("#8E0000");
    public static final int GREEN = Color.parseColor("#2E7D32");
    public static final int ORANGE = Color.parseColor("#EF6C00");
    public static final int BLUE = Color.parseColor("#1565C0");
    public static final int GRAY = Color.parseColor("#757575");
    public static final int LIGHT = Color.parseColor("#F3ECEC");
    public static final int BLACK = Color.parseColor("#212121");
    public static final int BG = Color.parseColor("#FAF6F6");

    private static float scale = 1f;

    private final Activity act;
    private final ScrollView scroll;
    private final LinearLayout page;

    public Ui(Activity act) {
        this.act = act;
        scroll = new ScrollView(act);
        scroll.setFitsSystemWindows(true);
        scroll.setBackgroundColor(BG);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        page = new LinearLayout(act);
        page.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(page);
    }

    public View root() { return scroll; }

    public static void toggleLargeText() { scale = (scale == 1f) ? 1.25f : 1f; }

    public void clear() {
        page.removeAllViews();
        page.setLayoutDirection(Lang.isArabic() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        scroll.scrollTo(0, 0);
        page.setAlpha(0f);
        page.setTranslationY(dp(12));
        page.animate().alpha(1f).translationY(0).setDuration(180)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    public int dp(int v) {
        return (int) (v * act.getResources().getDisplayMetrics().density);
    }

    public GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    public Drawable pressable(int color, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(Color.argb(40, 0, 0, 0)), rounded(color, radius), null);
    }

    public TextView text(String s, int size, boolean bold, int color) {
        TextView t = new TextView(act);
        t.setText(Lang.t(s));
        t.setTextSize(size * scale);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setPadding(0, dp(2), 0, dp(2));
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return t;
    }

    public void sectionTitle(String s) {
        TextView t = text(s, 17, true, BLACK);
        t.setPadding(dp(22), dp(20), dp(22), dp(4));
        page.addView(t);
    }

    public void emptyText(String s) {
        TextView t = text(s, 14, false, GRAY);
        t.setPadding(dp(22), dp(4), dp(22), dp(4));
        page.addView(t);
    }

    public void spacer() {
        page.addView(new View(act), new LinearLayout.LayoutParams(-1, dp(40)));
    }

    private LinearLayout headerBox(int bottomPadding) {
        LinearLayout h = new LinearLayout(act);
        h.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{RED, DARK_RED});
        float r = dp(28);
        g.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        h.setBackground(g);
        h.setPadding(dp(22), dp(12), dp(22), dp(bottomPadding));
        return h;
    }

    private LinearLayout topBar(Runnable onBack, Runnable onLogout, Runnable onRefresh) {
        LinearLayout bar = new LinearLayout(act);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        if (onBack != null) bar.addView(headerButton("←  " + Lang.t("Back"), v -> onBack.run()));
        bar.addView(new View(act), new LinearLayout.LayoutParams(0, 1, 1));
        bar.addView(headerButton(Lang.isArabic() ? "EN" : "ع", v -> { Lang.toggle(); onRefresh.run(); }));
        bar.addView(headerButton("A±", v -> { toggleLargeText(); onRefresh.run(); }));
        if (onLogout != null) bar.addView(headerButton(Lang.t("Log out"), v -> onLogout.run()));
        return bar;
    }

    public void header(String title, String subtitle, Runnable onBack, Runnable onLogout, Runnable onRefresh) {
        LinearLayout h = headerBox(26);
        h.addView(topBar(onBack, onLogout, onRefresh));
        h.addView(text(title, 26, true, Color.WHITE));
        h.addView(text(subtitle, 14, false, Color.parseColor("#FFCDD2")));
        page.addView(h);
    }

    public void hero(Runnable onRefresh) {
        LinearLayout h = headerBox(56);
        h.addView(topBar(null, null, onRefresh), new LinearLayout.LayoutParams(-1, -2));

        TextView logo = text("🩸", 40, false, Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.argb(45, 255, 255, 255));
        circle.setStroke(dp(2), Color.argb(90, 255, 255, 255));
        logo.setBackground(circle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(96), dp(96));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = dp(8);
        h.addView(logo, lp);

        TextView name = text("Qatra", 34, true, Color.WHITE);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(12), 0, 0);
        h.addView(name, new LinearLayout.LayoutParams(-1, -2));
        TextView tag = text("Right donor, right time", 15, false, Color.parseColor("#FFCDD2"));
        tag.setGravity(Gravity.CENTER);
        h.addView(tag, new LinearLayout.LayoutParams(-1, -2));
        page.addView(h);
    }

    private TextView headerButton(String label, View.OnClickListener click) {
        TextView t = text(label, 14, true, Color.WHITE);
        t.setContentDescription(label.equals("ع") || label.equals("EN") ? "Switch language" : label.equals("A±") ? "Change text size" : label);
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(60, 255, 255, 255)),
                rounded(Color.argb(45, 255, 255, 255), 16), null));
        t.setPadding(dp(14), dp(7), dp(14), dp(7));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(4), 0, dp(4), dp(10));
        t.setLayoutParams(lp);
        t.setOnClickListener(click);
        return t;
    }

    public LinearLayout card() {
        LinearLayout c = new LinearLayout(act);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(Color.WHITE, 20));
        c.setElevation(dp(2));
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(16), dp(7), dp(16), dp(7));
        page.addView(c, lp);
        return c;
    }

    public LinearLayout floatingCard() {
        LinearLayout c = card();
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) c.getLayoutParams();
        lp.topMargin = -dp(36);
        c.setElevation(dp(6));
        c.setPadding(dp(20), dp(20), dp(20), dp(20));
        return c;
    }

    public void onTap(View v, View.OnClickListener click) {
        v.setBackground(pressable(Color.WHITE, 20));
        v.setOnClickListener(click);
    }

    public LinearLayout row(View main, View... end) {
        LinearLayout r = new LinearLayout(act);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.addView(main, new LinearLayout.LayoutParams(0, -2, 1));
        for (View v : end) r.addView(v);
        return r;
    }

    public LinearLayout column(View... views) {
        LinearLayout c = new LinearLayout(act);
        c.setOrientation(LinearLayout.VERTICAL);
        for (View v : views) c.addView(v);
        return c;
    }

    public Button button(String label, int color, View.OnClickListener click) {
        Button b = new Button(act);
        b.setText(Lang.t(label));
        b.setAllCaps(false);
        b.setTextSize(15 * scale);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setTextColor(Color.WHITE);
        b.setStateListAnimator(null);
        b.setBackground(pressable(color, 26));
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    public Button softButton(String label, int color, View.OnClickListener click) {
        Button b = button(label, color, click);
        b.setTextColor(color);
        b.setBackground(pressable(Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)), 26));
        return b;
    }

    public TextView link(String label, View.OnClickListener click) {
        TextView t = text(label, 14, true, RED);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(14), dp(8), dp(6));
        t.setOnClickListener(click);
        t.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    public TextView badge(String label, int color) {
        TextView t = text(label, 12, true, color);
        t.setBackground(rounded(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)), 12));
        t.setPadding(dp(10), dp(4), dp(10), dp(4));
        return t;
    }

    public TextView riskBadge(String risk) {
        int c = risk.equals("High") || risk.equals("Critical") ? RED
                : risk.equals("Medium") ? ORANGE
                : risk.equals("Normal") ? BLUE : GREEN;
        return badge(risk, c);
    }

    public TextView pill(String s, int color) {
        TextView t = text(s, 15, true, color);
        t.setBackground(rounded(Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)), 14));
        t.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        t.setLayoutParams(lp);
        return t;
    }

    public TextView chip(String label, View.OnClickListener click) {
        TextView t = text(label, 13, true, RED);
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(40, 0, 0, 0)), rounded(LIGHT, 16), null));
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(0, dp(6), dp(8), 0);
        t.setLayoutParams(lp);
        t.setOnClickListener(click);
        return t;
    }

    public TextView bloodDrop(String type) {
        TextView t = text(type, 16, true, Color.WHITE);
        t.setContentDescription("Blood type " + type);
        t.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{RED, DARK_RED});
        circle.setShape(GradientDrawable.OVAL);
        t.setBackground(circle);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        return t;
    }

    public View stockBar(int units, int safeLevel) {
        LinearLayout track = new LinearLayout(act);
        track.setBackground(rounded(Color.parseColor("#EEEEEE"), 6));
        float fraction = Math.min(1f, Math.max(0.03f, units / (safeLevel * 2f)));
        View fill = new View(act);
        fill.setBackground(rounded(units < safeLevel ? RED : GREEN, 6));
        track.addView(fill, new LinearLayout.LayoutParams(0, -1, fraction));
        track.addView(new View(act), new LinearLayout.LayoutParams(0, -1, 1 - fraction));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(10));
        lp.setMargins(0, dp(8), 0, dp(8));
        track.setLayoutParams(lp);
        return track;
    }

    public void statRow(String[] labels, String[] values) {
        LinearLayout stats = new LinearLayout(act);
        stats.setPadding(dp(12), dp(12), dp(12), 0);
        for (int i = 0; i < labels.length; i++) {
            LinearLayout box = new LinearLayout(act);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER);
            box.setBackground(rounded(Color.WHITE, 18));
            box.setElevation(dp(2));
            box.setPadding(dp(6), dp(14), dp(6), dp(14));
            TextView v = text(values[i], 22, true, RED);
            v.setGravity(Gravity.CENTER);
            TextView l = text(labels[i], 12, false, GRAY);
            l.setGravity(Gravity.CENTER);
            box.addView(v);
            box.addView(l);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.setMargins(dp(4), 0, dp(4), 0);
            stats.addView(box, lp);
        }
        page.addView(stats);
    }

    public void label(LinearLayout parent, String s) {
        TextView t = text(s, 13, true, GRAY);
        t.setPadding(dp(4), dp(12), 0, dp(4));
        parent.addView(t);
    }

    public EditText input(LinearLayout parent, String label, String value, int inputType) {
        label(parent, label);
        EditText e = new EditText(act);
        e.setText(value);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setTextSize(16 * scale);
        e.setTextColor(BLACK);
        e.setHint(Lang.t(label));
        e.setHintTextColor(Color.parseColor("#B0A8A8"));
        e.setBackground(rounded(LIGHT, 14));
        e.setPadding(dp(16), dp(14), dp(16), dp(14));
        parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    public EditText textInput(LinearLayout parent, String label, String value) {
        return input(parent, label, value, InputType.TYPE_CLASS_TEXT);
    }

    public EditText numberInput(LinearLayout parent, String label, String value) {
        return input(parent, label, value, InputType.TYPE_CLASS_NUMBER);
    }

    public EditText phoneInput(LinearLayout parent, String label, String value) {
        return input(parent, label, value, InputType.TYPE_CLASS_PHONE);
    }

    public EditText passwordInput(LinearLayout parent, String label) {
        return input(parent, label, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    public Spinner spinner(LinearLayout parent, String label, String[] items, String selected) {
        label(parent, label);
        Spinner s = new Spinner(act);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(act, android.R.layout.simple_spinner_dropdown_item, items);
        s.setAdapter(adapter);
        s.setBackground(rounded(LIGHT, 14));
        s.setPadding(dp(8), dp(6), dp(8), dp(6));
        for (int i = 0; i < items.length; i++) if (items[i].equals(selected)) s.setSelection(i);
        parent.addView(s, new LinearLayout.LayoutParams(-1, dp(52)));
        return s;
    }

    public CheckBox checkBox(LinearLayout parent, String label) {
        CheckBox c = new CheckBox(act);
        c.setText(Lang.t(label));
        c.setTextSize(14 * scale);
        c.setTextColor(BLACK);
        c.setButtonTintList(ColorStateList.valueOf(RED));
        c.setPadding(dp(4), dp(6), 0, dp(6));
        parent.addView(c);
        return c;
    }

    public void toast(String s) {
        Toast.makeText(act, Lang.t(s), Toast.LENGTH_SHORT).show();
    }
}
