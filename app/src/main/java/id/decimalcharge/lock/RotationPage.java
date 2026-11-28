package id.decimalcharge.lock;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import static id.decimalcharge.lock.Ui.*;

/** Ruang Rotasi: kontrol orientasi layar. */
class RotationPage extends Page {
    private static final int[] PRESET_SO = {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE};
    private static final String[] PRESET_NAME = {"Potret", "Lanskap kiri", "Potret terbalik", "Lanskap kanan", "Sensor potret", "Sensor lanskap"};
    private static final String[] PRESET_DEG = {"0°", "90°", "180°", "270°", "0° / 180°", "90° / 270°"};

    private OrientationView orient;
    private TextView modeText, subText, chipState;
    private final TextView[] modePills = new TextView[3];
    private final LinearLayout[] presets = new LinearLayout[6];
    private Switch bubbleSwitch;
    private CompoundButton.OnCheckedChangeListener bubbleListener;
    private Row rowPerm;

    RotationPage(MainActivity a) { super(a); }

    @Override View build() {
        ScrollView sv = pageScroll();
        LinearLayout root = column();
        sv.addView(root);

        // Status orientasi
        LinearLayout st = card();
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        orient = new OrientationView(a);
        top.addView(orient, lp(dp(120), dp(120)));
        LinearLayout tx = new LinearLayout(a);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, 0, 0);
        tx.addView(text("ORIENTASI AKTIF", TXT3, 11, true));
        modeText = text("", TXT, 19, true);
        tx.addView(modeText);
        subText = text("", TXT2, 12, false);
        tx.addView(subText);
        chipState = text("", GRAY, 12, true);
        chipState.setPadding(dp(10), dp(3), dp(10), dp(3));
        LinearLayout.LayoutParams cl = lp(WRAP, WRAP);
        cl.topMargin = dp(10);
        tx.addView(chipState, cl);
        top.addView(tx, lpw(0, WRAP, 1f));
        st.addView(top);
        root.addView(st);

        // Mode
        LinearLayout mode = card();
        mode.addView(header(IconView.ROTATE, "Mode"));
        LinearLayout pr = new LinearLayout(a);
        pr.setOrientation(LinearLayout.HORIZONTAL);
        pr.setPadding(0, dp(12), 0, 0);
        String[] mn = {"Auto", "Paksa", "Kunci"};
        for (int k = 0; k < 3; k++) {
            final int kk = k;
            TextView pill = text(mn[k], TXT2, 13, true);
            pill.setGravity(Gravity.CENTER);
            pill.setOnClickListener(v -> setMode(kk));
            LinearLayout.LayoutParams plp = lpw(0, dp(42), 1f);
            plp.rightMargin = k < 2 ? dp(6) : 0;
            pr.addView(pill, plp);
            modePills[k] = pill;
        }
        mode.addView(pr);
        TextView mh = text("Auto: semua arah termasuk terbalik, walau aplikasi mengunci orientasi. Paksa: tetap di orientasi pilihan. Kunci: membekukan orientasi saat ini.", TXT3, 11, false);
        mh.setPadding(0, dp(10), 0, 0);
        mode.addView(mh);
        mode.addView(button(IconView.CLOSE, "Kembali ke Bawaan Sistem", false, v -> {
            RotationService.off(a);
            a.handler.postDelayed(a::refresh, 400);
        }));
        root.addView(mode);

        // Preset orientasi
        LinearLayout pc = card();
        pc.addView(header(IconView.PHONE, "Preset Orientasi"));
        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(a);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setPadding(0, dp(10), 0, 0);
            for (int col = 0; col < 2; col++) {
                final int idx = row * 2 + col;
                LinearLayout b = new LinearLayout(a);
                b.setOrientation(LinearLayout.VERTICAL);
                b.setGravity(Gravity.CENTER);
                b.setPadding(dp(6), dp(12), dp(6), dp(12));
                b.addView(new IconView(a, idx < 4 ? IconView.PHONE : IconView.ROTATE, TXT), lp(dp(26), dp(26)));
                TextView nm = text(PRESET_NAME[idx], TXT, 13, true);
                nm.setGravity(Gravity.CENTER);
                nm.setPadding(0, dp(6), 0, 0);
                b.addView(nm);
                TextView dg = text(PRESET_DEG[idx], TXT3, 11, false);
                dg.setGravity(Gravity.CENTER);
                b.addView(dg);
                b.setOnClickListener(v -> force(PRESET_SO[idx]));
                LinearLayout.LayoutParams blp = lpw(0, WRAP, 1f);
                blp.rightMargin = col == 0 ? dp(8) : 0;
                line.addView(b, blp);
                presets[idx] = b;
            }
            pc.addView(line);
        }
        root.addView(pc);

        // Kontrol melayang
        LinearLayout fl = card();
        fl.addView(header(IconView.TOUCH, "Kontrol Melayang"));
        LinearLayout fr = new LinearLayout(a);
        fr.setOrientation(LinearLayout.HORIZONTAL);
        fr.setGravity(Gravity.CENTER_VERTICAL);
        fr.setPadding(0, dp(12), 0, 0);
        fr.addView(text("Tampilkan di atas aplikasi lain", TXT, 15, true), lpw(0, WRAP, 1f));
        bubbleSwitch = new Switch(a);
        tintSwitch(bubbleSwitch);
        fr.addView(bubbleSwitch);
        fl.addView(fr);
        TextView fh = text("Ketuk lingkaran untuk membuka pilihan rotasi. Geser lingkaran untuk memindahkannya.", TXT3, 12, false);
        fh.setPadding(0, dp(6), 0, 0);
        fl.addView(fh);
        root.addView(fl);
        bubbleListener = (v, on) -> {
            if (on && needPerm()) {
                a.handler.postDelayed(a::refresh, 500);
                return;
            }
            RotationService.bubble(a, on);
            a.handler.postDelayed(a::refresh, 600);
        };

        // Izin
        LinearLayout pm = card();
        pm.addView(header(IconView.SHIELD, "Izin"));
        rowPerm = row(pm, "Tampil di atas aplikasi lain", "Dibutuhkan agar layar bisa dipaksa berputar dan agar kontrol melayang tampil.", "Atur", -1, false, v -> needPerm());
        root.addView(pm);

        // Keterbatasan
        LinearLayout info = card();
        info.addView(header(IconView.INFO, "Cara kerja dan keterbatasan"));
        TextView it = text(
            "• Aplikasi memasang jendela transparan 1 piksel yang meminta orientasi tertentu, sehingga sistem memutar layar walau aplikasi di bawahnya mengunci rotasi.\n\n"
            + "• Ini tidak menjamin semua aplikasi. Game atau aplikasi tertentu, dan beberapa ROM MIUI, bisa mengabaikan permintaan itu.\n\n"
            + "• Aplikasi ini sendiri ikut berputar saat orientasi dipaksa.\n\n"
            + "• Saat HP restart, pengaturan dipulihkan otomatis bila izin masih aktif.\n\n"
            + "• Tidak mengubah pengaturan rotasi bawaan Android. Pilih \"Kembali ke Bawaan Sistem\" untuk mengembalikannya.", TXT2, 12, false);
        it.setPadding(0, dp(10), 0, 0);
        info.addView(it);
        root.addView(info);
        return sv;
    }

    private boolean needPerm() {
        if (Settings.canDrawOverlays(a)) return false;
        a.open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.getPackageName())));
        return true;
    }

    private void setMode(int m) {
        if (needPerm()) return;
        if (m == 0) {
            RotationService.apply(a, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        } else if (m == 1) {
            int so = a.getSharedPreferences("rot", Context.MODE_PRIVATE)
                .getInt("force", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            RotationService.apply(a, so);
        } else {
            RotationService.apply(a, ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }
        a.handler.postDelayed(a::refresh, 400);
    }

    private void force(int so) {
        if (needPerm()) return;
        a.getSharedPreferences("rot", Context.MODE_PRIVATE).edit().putInt("force", so).apply();
        RotationService.apply(a, so);
        a.handler.postDelayed(a::refresh, 400);
    }

    @SuppressWarnings("deprecation")
    void refresh() {
        int so = RotationService.current(a);
        boolean on = so != RotationService.OFF;
        int rot = a.getWindowManager().getDefaultDisplay().getRotation();
        orient.set(-rot * 90f, on ? ACCENT : GRAY);
        modeText.setText(on ? RotationService.label(so) : "Bawaan sistem");
        modeText.setTextColor(on ? TXT : TXT2);
        subText.setText("Layar sekarang: " + (rot * 90) + "°");
        chip(chipState, on ? "OVERRIDE AKTIF" : "NONAKTIF", on ? GREEN : GRAY);

        int m = !on ? -1 : (so == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR ? 0
            : (so == ActivityInfo.SCREEN_ORIENTATION_LOCKED ? 2 : 1));
        for (int k = 0; k < 3; k++) {
            boolean sel = k == m;
            modePills[k].setBackground(sel ? gradBox(new int[]{BLUE, BLUE2}, 0xFF7DB2FF, 14)
                : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 14));
            modePills[k].setTextColor(sel ? TXT : TXT2);
        }
        for (int i = 0; i < 6; i++) {
            boolean sel = on && PRESET_SO[i] == so;
            presets[i].setBackground(sel ? gradBox(new int[]{0xFF1E4FB8, 0xFF143A8C}, BLUE, 14)
                : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 14));
        }
        bubbleSwitch.setOnCheckedChangeListener(null);
        bubbleSwitch.setChecked(a.getSharedPreferences("rot", Context.MODE_PRIVATE).getBoolean("bubble", false)
            && RotationService.running);
        bubbleSwitch.setOnCheckedChangeListener(bubbleListener);
        state(rowPerm, Settings.canDrawOverlays(a));
    }
}
