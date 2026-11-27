package id.decimalcharge.lock;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

import static id.decimalcharge.lock.Ui.*;

/** Dasar semua halaman: meneruskan alat bantu tampilan agar kode halaman tetap pendek. */
abstract class Page {
    final MainActivity a;
    final Ui ui;
    final Locale idl;

    Page(MainActivity a) {
        this.a = a;
        this.ui = a.ui;
        this.idl = a.ui.idl;
    }

    abstract View build();

    int dp(int v) { return ui.dp(v); }

    LinearLayout.LayoutParams lp(int w, int h) { return ui.lp(w, h); }

    LinearLayout.LayoutParams lpw(int w, int h, float x) { return ui.lpw(w, h, x); }

    GradientDrawable gradBox(int[] colors, int stroke, int r) { return ui.gradBox(colors, stroke, r); }

    TextView text(String s, int color, int sp, boolean bold) { return ui.text(s, color, sp, bold); }

    LinearLayout card() { return ui.card(); }

    LinearLayout header(int icon, String title) { return ui.header(icon, title); }

    LinearLayout button(int icon, String label, boolean primary, View.OnClickListener l) {
        return ui.button(icon, label, primary, l);
    }

    void chip(TextView v, String s, int color) { ui.chip(v, s, color); }

    Row row(LinearLayout parent, String title, String desc, String btnText, int icon, boolean primary, View.OnClickListener l) {
        return ui.row(parent, title, desc, btnText, icon, primary, l);
    }

    void state(Row r, boolean ok) { ui.state(r, ok); }

    TextView kv(LinearLayout parent, String label) { return ui.kv(parent, label); }

    void tintSwitch(Switch sw) { ui.tintSwitch(sw); }

    ScrollView pageScroll() { return ui.pageScroll(); }

    LinearLayout column() { return ui.column(); }

    String f1(double v) { return ui.f1(v); }

    // Tombol pilihan periode (1 jam, 6 jam, 24 jam, 7 hari)
    LinearLayout periodRow(final TextView[] store) {
        LinearLayout pr = new LinearLayout(a);
        pr.setOrientation(LinearLayout.HORIZONTAL);
        pr.setPadding(0, dp(12), 0, 0);
        String[] names = {"1 jam", "6 jam", "24 jam", "7 hari"};
        for (int k = 0; k < 4; k++) {
            final int kk = k;
            TextView pb = text(names[k], TXT2, 12, true);
            pb.setGravity(Gravity.CENTER);
            pb.setOnClickListener(v -> a.setPeriod(kk));
            LinearLayout.LayoutParams plp = lpw(0, dp(36), 1f);
            plp.rightMargin = k < 3 ? dp(6) : 0;
            pr.addView(pb, plp);
            store[k] = pb;
        }
        return pr;
    }

    void styleArr(TextView[] arr) {
        for (int k = 0; k < 4; k++) {
            if (arr[k] == null) continue;
            boolean sel = k == a.period;
            arr[k].setBackground(sel ? gradBox(new int[]{BLUE, BLUE2}, 0xFF7DB2FF, 12)
                : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 12));
            arr[k].setTextColor(sel ? TXT : TXT2);
        }
    }
}
