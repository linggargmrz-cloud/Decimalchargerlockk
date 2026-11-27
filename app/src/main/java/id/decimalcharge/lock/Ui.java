package id.decimalcharge.lock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

/** Warna dan alat bantu tampilan yang dipakai bersama oleh semua halaman. */
final class Ui {
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final int CARD_A = 0xFF0F1833, CARD_B = 0xFF0A1128, STROKE = 0xFF1C2B58;
    static final int BLUE = 0xFF2D7BFF, BLUE2 = 0xFF1450D8, ACCENT = 0xFF4D9BFF;
    static final int TXT = 0xFFFFFFFF, TXT2 = 0xFFA9B8D8, TXT3 = 0xFF6B7AA0;
    static final int GREEN = 0xFF2ED573, RED = 0xFFFF5252, YELLOW = 0xFFFFC107, GRAY = 0xFF7D8890;
    static final int ICE = 0xFF7FDBFF, FIRE = 0xFFFF4D3D;
    static final int B_GREEN = 0xFF3DDC84, B_ORANGE = 0xFFFFA726, B_BLUE = 0xFF3D8BFF;
    static final double DESIGN_MAH = 5000.0;
    static final long[] SPANS = {3600000L, 21600000L, 86400000L, 604800000L};

    static final class Row {
        TextView chip;
        View btn;
    }

    final Context c;
    final Locale idl = new Locale("id", "ID");

    Ui(Context c) { this.c = c; }

    int dp(int v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    LinearLayout.LayoutParams lpw(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h, weight); }

    GradientDrawable gradBox(int[] colors, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    TextView text(String s, int color, int sp, boolean bold) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(sp);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    LinearLayout card() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        l.setBackground(gradBox(new int[]{CARD_A, CARD_B}, STROKE, 20));
        LinearLayout.LayoutParams l2 = lp(MATCH, WRAP);
        l2.bottomMargin = dp(12);
        l.setLayoutParams(l2);
        return l;
    }

    LinearLayout header(int icon, String title) {
        LinearLayout h = new LinearLayout(c);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.addView(new IconView(c, icon, ACCENT), lp(dp(22), dp(22)));
        TextView t = text(title, TXT, 16, true);
        t.setPadding(dp(10), 0, 0, 0);
        h.addView(t, lpw(0, WRAP, 1f));
        return h;
    }

    LinearLayout button(int icon, String label, boolean primary, View.OnClickListener l) {
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setBackground(primary ? gradBox(new int[]{BLUE, BLUE2}, 0xFF5B9BFF, 14)
            : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 14));
        if (icon >= 0) {
            LinearLayout.LayoutParams ilp = lp(dp(20), dp(20));
            ilp.rightMargin = dp(10);
            b.addView(new IconView(c, icon, TXT), ilp);
        }
        TextView t = text(label, TXT, 14, true);
        t.setGravity(Gravity.CENTER);
        b.addView(t);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams blp = lp(MATCH, dp(48));
        blp.topMargin = dp(10);
        b.setLayoutParams(blp);
        return b;
    }

    void chip(TextView v, String s, int color) {
        v.setText(s);
        v.setTextColor(color);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor((color & 0x00FFFFFF) | 0x26000000);
        v.setBackground(g);
    }

    Row row(LinearLayout parent, String title, String desc, String btnText, int icon, boolean primary, View.OnClickListener l) {
        Row r = new Row();
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(14), 0, 0);
        LinearLayout line = new LinearLayout(c);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(text(title, TXT, 15, true), lpw(0, WRAP, 1f));
        r.chip = text("", GRAY, 12, true);
        r.chip.setPadding(dp(10), dp(3), dp(10), dp(3));
        line.addView(r.chip);
        box.addView(line);
        box.addView(text(desc, TXT3, 12, false));
        r.btn = button(icon, btnText, primary, l);
        box.addView(r.btn);
        parent.addView(box);
        return r;
    }

    void state(Row r, boolean ok) {
        chip(r.chip, ok ? "Aktif" : "Belum", ok ? GREEN : RED);
        r.btn.setVisibility(ok ? View.GONE : View.VISIBLE);
    }

    TextView kv(LinearLayout parent, String label) {
        LinearLayout line = new LinearLayout(c);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(0, dp(10), 0, 0);
        line.addView(text(label, TXT2, 13, false), lpw(0, WRAP, 1f));
        TextView v = text("-", TXT, 13, true);
        v.setGravity(Gravity.END);
        line.addView(v);
        parent.addView(line);
        return v;
    }

    void tintSwitch(Switch sw) {
        int[][] st = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(st, new int[]{0xFFFFFFFF, 0xFFB7C3E0}));
        sw.setTrackTintList(new ColorStateList(st, new int[]{BLUE, 0xFF2A3558}));
    }

    ScrollView pageScroll() {
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        return sv;
    }

    LinearLayout column() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(6), dp(18), dp(24));
        return l;
    }

    String f1(double v) { return String.format(idl, "%.1f", v); }
}
