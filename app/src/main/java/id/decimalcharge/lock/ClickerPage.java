package id.decimalcharge.lock;

import android.content.Intent;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.GradientDrawable;

import static id.decimalcharge.lock.Ui.*;

/** Ruang Klik: auto clicker multi-sentuhan sampai 10 titik. */
class ClickerPage extends Page {
    private interface IntSetter { void set(int v); }

    private TextView stateText, subText, chipSvc, cntText, pillSim, pillSeq;
    private LinearLayout svcBtn, svcBtn2;

    ClickerPage(MainActivity a) { super(a); }

    @Override View build() {
        ScrollView sv = pageScroll();
        LinearLayout root = column();
        sv.addView(root);

        // Status layanan
        LinearLayout st = card();
        LinearLayout hd = header(IconView.TOUCH, "Auto Clicker 10 Jari");
        chipSvc = text("", GRAY, 12, true);
        chipSvc.setPadding(dp(10), dp(3), dp(10), dp(3));
        hd.addView(chipSvc);
        st.addView(hd);
        stateText = text("", TXT, 26, true);
        stateText.setPadding(0, dp(12), 0, 0);
        st.addView(stateText);
        subText = text("", TXT2, 12, false);
        st.addView(subText);
        svcBtn = button(IconView.GEAR, "Aktifkan di Aksesibilitas", true,
            v -> a.open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        st.addView(svcBtn);
        svcBtn2 = button(IconView.INFO, "Info Aplikasi (izinkan pengaturan terbatas)", false, v -> a.openAppInfo());
        st.addView(svcBtn2);
        root.addView(st);

        // Pengaturan sentuhan
        LinearLayout cfg = card();
        cfg.addView(header(IconView.GEAR, "Pengaturan Sentuhan"));

        LinearLayout sr = new LinearLayout(a);
        sr.setOrientation(LinearLayout.HORIZONTAL);
        sr.setGravity(Gravity.CENTER_VERTICAL);
        sr.setPadding(0, dp(14), 0, 0);
        sr.addView(text("Jumlah titik sentuh (1 sampai 10)", TXT, 14, true), lpw(0, WRAP, 1f));
        sr.addView(stepBtn(IconView.MINUS, -1), lp(dp(38), dp(38)));
        cntText = text("3", TXT, 16, true);
        cntText.setGravity(Gravity.CENTER);
        sr.addView(cntText, lp(dp(44), WRAP));
        sr.addView(stepBtn(IconView.PLUS, 1), lp(dp(38), dp(38)));
        cfg.addView(sr);

        LinearLayout mr = new LinearLayout(a);
        mr.setOrientation(LinearLayout.VERTICAL);
        mr.setPadding(0, dp(14), 0, 0);
        mr.addView(text("Mode sentuhan", TXT, 14, true));
        LinearLayout pr = new LinearLayout(a);
        pr.setOrientation(LinearLayout.HORIZONTAL);
        pr.setPadding(0, dp(8), 0, 0);
        pillSim = text("Simultan", TXT2, 13, true);
        pillSim.setGravity(Gravity.CENTER);
        pillSim.setOnClickListener(v -> { ClickerConfig.setSimultaneous(a, true); refresh(); });
        LinearLayout.LayoutParams l1 = lpw(0, dp(42), 1f);
        l1.rightMargin = dp(6);
        pr.addView(pillSim, l1);
        pillSeq = text("Bergantian", TXT2, 13, true);
        pillSeq.setGravity(Gravity.CENTER);
        pillSeq.setOnClickListener(v -> { ClickerConfig.setSimultaneous(a, false); refresh(); });
        pr.addView(pillSeq, lpw(0, dp(42), 1f));
        mr.addView(pr);
        TextView mh = text("Simultan: semua titik ditekan bersamaan. Bergantian: titik ditekan satu per satu.", TXT3, 11, false);
        mh.setPadding(0, dp(6), 0, 0);
        mr.addView(mh);
        cfg.addView(mr);

        cfg.addView(labeled("Durasi tekan (ms)", "1 sampai 5000. Makin besar makin lama jari menahan.",
            ClickerConfig.hold(a), 1, 5000, v -> ClickerConfig.setHold(a, v)));
        cfg.addView(labeled("Interval (ms)", "10 sampai 600000. Jeda sebelum sentuhan berikutnya.",
            ClickerConfig.interval(a), 10, 600000, v -> ClickerConfig.setInterval(a, v)));
        cfg.addView(labeled("Jumlah pengulangan", "0 berarti tanpa batas sampai Anda menekan Berhenti.",
            ClickerConfig.repeat(a), 0, 999999, v -> ClickerConfig.setRepeat(a, v)));
        root.addView(cfg);

        // Kontrol
        LinearLayout ctl = card();
        ctl.addView(header(IconView.PLAY, "Kontrol"));
        ctl.addView(button(IconView.TOUCH, "Tampilkan Titik & Kontrol Melayang", true, v -> {
            ClickerService s = svc(true);
            if (s != null) s.showOverlay();
        }));
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(ctrlBtn(IconView.PLAY, "Start", v -> {
            ClickerService s = svc(true);
            if (s == null) return;
            s.showOverlay();
            s.start();
        }, true));
        row.addView(ctrlBtn(IconView.PAUSE, "Jeda", v -> {
            ClickerService s = svc(false);
            if (s != null) s.pause();
        }, false));
        row.addView(ctrlBtn(IconView.STOP, "Stop", v -> {
            ClickerService s = svc(false);
            if (s != null) s.stop();
        }, false));
        ctl.addView(row);
        ctl.addView(button(-1, "Reset Posisi Titik", false, v -> {
            ClickerConfig.resetPositions(a);
            ClickerService s = svc(false);
            if (s != null) s.relayout();
        }));
        TextView ch = text("Geser lingkaran bernomor untuk mengatur posisi tiap titik. Saat berjalan, titik tidak bisa disentuh agar sentuhan menembus ke aplikasi di bawahnya.", TXT3, 11, false);
        ch.setPadding(0, dp(10), 0, 0);
        ctl.addView(ch);
        root.addView(ctl);

        // Izin dan keterbatasan
        LinearLayout info = card();
        info.addView(header(IconView.INFO, "Izin dan keterbatasan"));
        TextView it = text(
            "• Butuh layanan aksesibilitas \"L-CORE Auto Clicker\". Layanan ini hanya mengirim sentuhan yang Anda atur dan tidak membaca isi layar.\n\n"
            + "• Di Android 13, untuk aplikasi di luar Play Store, tombol aksesibilitas bisa abu-abu. Buka Info Aplikasi, ketuk titik tiga di pojok, pilih \"Izinkan pengaturan terbatas\", lalu ulangi.\n\n"
            + "• Maksimal 10 titik sekaligus, batas dari Android.\n\n"
            + "• Posisi disimpan dalam piksel layar. Atur ulang bila orientasi layar berubah.\n\n"
            + "• Sebagian aplikasi, misalnya aplikasi perbankan atau yang memblokir input otomatis, bisa mengabaikan sentuhan ini.\n\n"
            + "• Hentikan kapan saja dengan tombol Stop di panel melayang atau di halaman ini.", TXT2, 12, false);
        it.setPadding(0, dp(10), 0, 0);
        info.addView(it);
        root.addView(info);
        return sv;
    }

    private ClickerService svc(boolean tell) {
        ClickerService s = ClickerService.instance;
        if (s == null && tell) {
            Toast.makeText(a, "Aktifkan dulu layanan aksesibilitas L-CORE Auto Clicker", Toast.LENGTH_LONG).show();
            a.open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
        return s;
    }

    private View stepBtn(int icon, final int delta) {
        FrameLayout f = new FrameLayout(a);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(0xFF141E3E);
        g.setStroke(dp(1), STROKE);
        f.setBackground(g);
        f.addView(new IconView(a, icon, TXT), new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
        f.setOnClickListener(v -> {
            ClickerConfig.setCount(a, ClickerConfig.count(a) + delta);
            ClickerService s = ClickerService.instance;
            if (s != null) s.syncCount();
            refresh();
        });
        return f;
    }

    private LinearLayout ctrlBtn(int icon, String label, View.OnClickListener l, boolean primary) {
        LinearLayout b = button(icon, label, primary, l);
        LinearLayout.LayoutParams p = lpw(0, dp(48), 1f);
        p.topMargin = dp(10);
        p.rightMargin = dp(6);
        b.setLayoutParams(p);
        b.setPadding(dp(6), 0, dp(6), 0);
        return b;
    }

    private View labeled(String label, String help, int initial, final int min, final int max, final IntSetter setter) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(14), 0, 0);
        box.addView(text(label, TXT, 14, true));
        box.addView(text(help, TXT3, 11, false));
        EditText e = new EditText(a);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setSingleLine(true);
        e.setTextColor(TXT);
        e.setTextSize(15);
        e.setBackground(gradBox(new int[]{0xFF0C1430, 0xFF0C1430}, STROKE, 12));
        e.setPadding(dp(14), dp(10), dp(14), dp(10));
        e.setText(String.valueOf(initial));
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int cn, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int bf, int cn) { }
            @Override public void afterTextChanged(Editable s) {
                try {
                    int v = Integer.parseInt(s.toString().trim());
                    setter.set(Math.max(min, Math.min(max, v)));
                } catch (Throwable ignored) { }
            }
        });
        LinearLayout.LayoutParams l = lp(MATCH, dp(46));
        l.topMargin = dp(8);
        box.addView(e, l);
        return box;
    }

    private boolean accessibilityEnabled() {
        try {
            String s = Settings.Secure.getString(a.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return s != null && s.contains("ClickerService");
        } catch (Throwable t) {
            return false;
        }
    }

    void refresh() {
        boolean conn = ClickerService.instance != null;
        boolean enabled = accessibilityEnabled();
        int s = ClickerService.state;
        chip(chipSvc, conn ? "Layanan aktif" : (enabled ? "Menghubungkan" : "Belum aktif"), conn ? GREEN : RED);
        String t;
        int col;
        if (!conn) { t = "LAYANAN MATI"; col = RED; }
        else if (s == ClickerService.RUNNING) { t = "BERJALAN"; col = GREEN; }
        else if (s == ClickerService.PAUSED) { t = "JEDA"; col = YELLOW; }
        else { t = "SIAP"; col = ACCENT; }
        stateText.setText(t);
        stateText.setTextColor(col);
        String err = ClickerService.lastError;
        subText.setText(conn ? ("Siklus: " + ClickerService.cycles + (err.isEmpty() ? "" : " · " + err))
            : "Aktifkan layanan aksesibilitas L-CORE Auto Clicker agar bisa berjalan.");
        svcBtn.setVisibility(conn ? View.GONE : View.VISIBLE);
        svcBtn2.setVisibility(conn ? View.GONE : View.VISIBLE);
        cntText.setText(String.valueOf(ClickerConfig.count(a)));
        boolean sim = ClickerConfig.simultaneous(a);
        pillSim.setBackground(sim ? gradBox(new int[]{BLUE, BLUE2}, 0xFF7DB2FF, 14)
            : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 14));
        pillSim.setTextColor(sim ? TXT : TXT2);
        pillSeq.setBackground(!sim ? gradBox(new int[]{BLUE, BLUE2}, 0xFF7DB2FF, 14)
            : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 14));
        pillSeq.setTextColor(!sim ? TXT : TXT2);
    }
}
