package id.decimalcharge.lock;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static id.decimalcharge.lock.Ui.*;

/** Halaman Riwayat: grafik, informasi detail, kondisi suhu, dan informasi baterai. */
class HistoryPage extends Page {
    TempChartView chartHist;
    final TextView[] pHist = new TextView[4];
    private TextView tTrend, tStats, tRises, tAvgT, tAvgB, tEndure, tCondSub, chipCond;
    private TempBar tempBar;
    private TextView hRemain, hStatus, hTemp, hVolt, hTech, hCycle, hDesign, hCap, hHealth, hHint;
    private int lastRev = -1, lastPeriod = -1;
    private long lastLoad = 0;
    private final SimpleDateFormat fmtHm = new SimpleDateFormat("HH.mm", new Locale("id", "ID"));
    private final SimpleDateFormat fmtDm = new SimpleDateFormat("d MMM HH.mm", new Locale("id", "ID"));

    HistoryPage(MainActivity a) { super(a); }

    void styleTabs() { styleArr(pHist); }

    @Override View build() {
        ScrollView sv = pageScroll();
        LinearLayout root = column();
        sv.addView(root);

        LinearLayout g = card();
        g.addView(header(IconView.CHART, "Grafik Penggunaan"));
        g.addView(periodRow(pHist));
        TextView legend = text("", TXT2, 11, false);
        legend.setText(Html.fromHtml(
            "<font color='#7FDBFF'>&#9679; Suhu di bawah 45 &deg;C</font> &nbsp; <font color='#FF4D3D'>&#9679; Suhu 45 &deg;C ke atas</font><br>"
            + "<font color='#3DDC84'>&#9679; Baterai mengisi</font> &nbsp; <font color='#3D8BFF'>&#9679; Baterai normal</font> &nbsp; <font color='#FFA726'>&#9679; Baterai di bawah 20%</font>",
            Html.FROM_HTML_MODE_LEGACY));
        legend.setPadding(0, dp(10), 0, 0);
        g.addView(legend);
        chartHist = new TempChartView(a);
        LinearLayout.LayoutParams clp = lp(MATCH, dp(260));
        clp.topMargin = dp(8);
        g.addView(chartHist, clp);
        TextView tap = text("Sentuh dan geser jari di grafik untuk detail. Sumbu kiri: suhu, sumbu kanan: baterai.", TXT3, 11, false);
        tap.setPadding(0, dp(4), 0, 0);
        g.addView(tap);
        root.addView(g);

        LinearLayout d = card();
        d.addView(header(IconView.THERMO, "Informasi Detail"));
        TextView sub = text("10 menit terakhir", TXT2, 13, true);
        sub.setPadding(0, dp(12), 0, 0);
        d.addView(sub);
        tAvgT = kv(d, "Suhu rata-rata");
        tAvgB = kv(d, "Baterai rata-rata");
        tTrend = text("", TXT, 13, true);
        tTrend.setPadding(0, dp(12), 0, 0);
        d.addView(tTrend);
        LinearLayout er = new LinearLayout(a);
        er.setOrientation(LinearLayout.HORIZONTAL);
        er.setGravity(Gravity.CENTER_VERTICAL);
        er.setPadding(0, dp(14), 0, 0);
        er.addView(new IconView(a, IconView.CLOCK, ACCENT), lp(dp(20), dp(20)));
        TextView el = text("Perkiraan daya tahan", TXT2, 13, false);
        el.setPadding(dp(8), 0, 0, 0);
        er.addView(el, lpw(0, WRAP, 1f));
        tEndure = text("-", TXT, 13, true);
        er.addView(tEndure);
        d.addView(er);
        TextView en = text("Perkiraan kasar dari laju baterai turun satu jam terakhir. Tidak dihitung saat mengisi daya.", TXT3, 11, false);
        en.setPadding(0, dp(6), 0, 0);
        d.addView(en);
        root.addView(d);

        LinearLayout c = card();
        LinearLayout ch = header(IconView.THERMO, "Kondisi Suhu");
        chipCond = text("", GRAY, 12, true);
        chipCond.setPadding(dp(10), dp(3), dp(10), dp(3));
        ch.addView(chipCond);
        c.addView(ch);
        tempBar = new TempBar(a);
        LinearLayout.LayoutParams blp = lp(MATCH, dp(34));
        blp.topMargin = dp(14);
        c.addView(tempBar, blp);
        LinearLayout sc = new LinearLayout(a);
        sc.setOrientation(LinearLayout.HORIZONTAL);
        sc.addView(text("20 °C", TXT3, 11, false), lpw(0, WRAP, 1f));
        TextView s50 = text("50 °C", TXT3, 11, false);
        s50.setGravity(Gravity.END);
        sc.addView(s50);
        c.addView(sc);
        tCondSub = text("", GREEN, 13, false);
        tCondSub.setGravity(Gravity.CENTER);
        tCondSub.setPadding(0, dp(8), 0, 0);
        c.addView(tCondSub);
        root.addView(c);

        LinearLayout s = card();
        s.addView(header(IconView.CLOCK, "Ringkasan Periode"));
        tStats = text("", TXT, 12, false);
        tStats.setPadding(0, dp(10), 0, 0);
        s.addView(tStats);
        TextView rt = text("Kenaikan suhu (naik 0,5 °C atau lebih)", TXT, 13, true);
        rt.setPadding(0, dp(14), 0, 0);
        s.addView(rt);
        tRises = text("", TXT2, 12, false);
        tRises.setPadding(0, dp(4), 0, 0);
        s.addView(tRises);
        root.addView(s);

        LinearLayout h = card();
        h.addView(header(IconView.BATTERY, "Informasi Baterai"));
        hRemain = kv(h, "Sisa baterai");
        hStatus = kv(h, "Status sistem");
        hTemp = kv(h, "Suhu");
        hVolt = kv(h, "Tegangan");
        hTech = kv(h, "Teknologi");
        hCycle = kv(h, "Siklus pengisian");
        hDesign = kv(h, "Kapasitas desain");
        hCap = kv(h, "Estimasi kapasitas sisa");
        hHealth = kv(h, "Estimasi kesehatan");
        hHint = text("", TXT3, 11, false);
        hHint.setPadding(0, dp(10), 0, 0);
        h.addView(hHint);
        root.addView(h);

        LinearLayout tip = card();
        tip.addView(header(IconView.INFO, "Tips"));
        TextView tt = text("Suhu baterai yang sering di atas 45 °C bisa mempercepat penuaan baterai. Lepas casing atau kurangi pemakaian berat saat mengisi daya.", TXT2, 12, false);
        tt.setPadding(0, dp(8), 0, 0);
        tip.addView(tt);
        root.addView(tip);
        return sv;
    }

    void refresh() {
        loadTemp(false);
        refreshHealth();
    }

    void setCondition(double tv) {
        tempBar.setValue(tv);
        if (Double.isNaN(tv)) {
            chip(chipCond, "-", GRAY);
            tCondSub.setText("Suhu tidak tersedia");
            tCondSub.setTextColor(TXT3);
        } else if (tv >= 45.0) {
            chip(chipCond, "Panas", RED);
            tCondSub.setText("Suhu tinggi. Hentikan pemakaian berat atau pengisian sebentar.");
            tCondSub.setTextColor(RED);
        } else if (tv >= 40.0) {
            chip(chipCond, "Hangat", YELLOW);
            tCondSub.setText("Suhu cukup hangat. Kurangi pemakaian berat.");
            tCondSub.setTextColor(YELLOW);
        } else {
            chip(chipCond, "Normal", GREEN);
            tCondSub.setText("Suhu masih dalam batas aman.");
            tCondSub.setTextColor(GREEN);
        }
    }

    // Perkiraan kasar dari laju baterai turun selama satu jam terakhir (hanya saat tidak mengisi daya).
    private String endurance() {
        long now = System.currentTimeMillis();
        List<TempLog.Sample> l = TempLog.load(a, now - 3600000L);
        if (l.size() < 2) return "data belum cukup";
        TempLog.Sample last = l.get(l.size() - 1);
        if (last.chg) return "sedang mengisi";
        int idx = l.size() - 1;
        while (idx > 0 && !l.get(idx - 1).chg) idx--;
        TempLog.Sample first = l.get(idx);
        double mins = (last.t - first.t) / 60000.0;
        int drop = first.level10 - last.level10;
        if (mins < 10 || drop < 10) return "data belum cukup";
        double rate = (drop / 10.0) / (mins / 60.0);
        double hours = (last.level10 / 10.0) / rate;
        if (hours > 99) return "lebih dari 99 jam";
        int totalMin = (int) Math.round(hours * 60);
        return "± " + (totalMin / 60) + " jam " + (totalMin % 60) + " menit";
    }

    void loadTemp(boolean force) {
        long now = System.currentTimeMillis();
        int rev = TempLog.rev;
        if (!force && rev == lastRev && a.period == lastPeriod && now - lastLoad < 15000) return;
        lastRev = rev;
        lastPeriod = a.period;
        lastLoad = now;

        // 10 menit terakhir
        List<TempLog.Sample> l10 = TempLog.load(a, now - 600000L);
        if (l10.isEmpty()) {
            tAvgT.setText("-");
            tAvgB.setText("-");
        } else {
            double st = 0, sbt = 0;
            for (TempLog.Sample s : l10) {
                st += s.temp / 10.0;
                sbt += s.level10 / 10.0;
            }
            tAvgT.setText(f1(st / l10.size()) + " °C");
            tAvgB.setText(String.format(idl, "%.1f%%", sbt / l10.size()));
        }
        tEndure.setText(endurance());

        long from = now - SPANS[a.period];
        List<TempLog.Sample> list = TempLog.load(a, from);
        a.home.chartHome.setData(list, from, now);
        chartHist.setData(list, from, now);

        SimpleDateFormat tf = a.period >= 2 ? fmtDm : fmtHm;
        double mx = -1e9, mn = 1e9, sum = 0;
        int cnt = 0;
        TempLog.Sample smx = null, smn = null;
        double base = Double.NaN;
        List<String> ev = new ArrayList<>();
        for (TempLog.Sample s : list) {
            double v = s.temp / 10.0;
            cnt++;
            sum += v;
            if (v > mx) { mx = v; smx = s; }
            if (v < mn) { mn = v; smn = s; }
            if (Double.isNaN(base)) { base = v; continue; }
            if (v >= base + 0.5) {
                ev.add(tf.format(new Date(s.t)) + "   naik ke " + f1(v) + " °C (+" + f1(v - base)
                    + ")   baterai " + TempLog.pct(s.level10) + (s.chg ? "   mengisi" : ""));
                base = v;
            } else if (v <= base - 0.5) {
                base = v;
            }
        }
        if (cnt == 0 || smx == null || smn == null) {
            tTrend.setText("Tren: belum ada data");
            tStats.setText("Belum ada data. Pastikan Layar Decimal aktif, lalu tunggu beberapa menit.");
            tRises.setText("-");
            return;
        }

        TempLog.Sample lastS = list.get(list.size() - 1);
        TempLog.Sample refS = list.get(0);
        for (TempLog.Sample s : list) {
            if (s.t <= lastS.t - 600000L) refS = s; else break;
        }
        long mins = Math.max(0L, (lastS.t - refS.t) / 60000L);
        double dT = (lastS.temp - refS.temp) / 10.0;
        int dL = lastS.level10 - refS.level10;
        String tsr = Math.abs(dT) < 0.05 ? "stabil" : ((dT > 0 ? "naik " : "turun ") + f1(Math.abs(dT)) + " °C");
        String bsr = dL == 0 ? "tetap" : ((dL > 0 ? "naik " : "turun ") + f1(Math.abs(dL) / 10.0) + "%");
        tTrend.setText(mins < 1 ? "Tren: data masih sedikit"
            : "Tren " + mins + " menit terakhir: suhu " + tsr + ", baterai " + bsr);

        tStats.setText("Tertinggi " + f1(mx) + " °C (" + tf.format(new Date(smx.t)) + ", baterai " + TempLog.pct(smx.level10) + ")\n"
            + "Terendah " + f1(mn) + " °C (" + tf.format(new Date(smn.t)) + ", baterai " + TempLog.pct(smn.level10) + ")\n"
            + "Rata-rata " + f1(sum / cnt) + " °C dari " + cnt + " catatan");
        if (ev.isEmpty()) {
            tRises.setText("Belum ada kenaikan 0,5 °C atau lebih pada periode ini.");
        } else {
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (int k = ev.size() - 1; k >= 0 && shown < 10; k--, shown++) {
                if (shown > 0) sb.append("\n");
                sb.append(ev.get(k));
            }
            tRises.setText(sb.toString());
        }
    }

    private void refreshHealth() {
        try {
            Intent bi = a.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bi != null) {
                int lv = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int sc = bi.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                hRemain.setText(lv >= 0 && sc > 0 ? Math.round(100f * lv / sc) + "%" : "-");
                int hs = bi.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
                String s;
                int col;
                switch (hs) {
                    case BatteryManager.BATTERY_HEALTH_GOOD: s = "Baik"; col = GREEN; break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT: s = "Terlalu panas"; col = RED; break;
                    case BatteryManager.BATTERY_HEALTH_DEAD: s = "Rusak"; col = RED; break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: s = "Tegangan berlebih"; col = RED; break;
                    case BatteryManager.BATTERY_HEALTH_COLD: s = "Terlalu dingin"; col = YELLOW; break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: s = "Gangguan"; col = RED; break;
                    default: s = "Tidak diketahui"; col = GRAY;
                }
                hStatus.setText(s);
                hStatus.setTextColor(col);
                int tm = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                hTemp.setText(tm == Integer.MIN_VALUE ? "Tidak tersedia" : String.format(idl, "%.1f °C", tm / 10.0));
                int mv = bi.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                hVolt.setText(mv > 0 ? String.format(idl, "%.2f V", mv / 1000.0) : "Tidak tersedia");
                String tech = bi.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                hTech.setText(tech == null || tech.isEmpty() ? "Tidak tersedia" : tech);
                int cycles = -1;
                if (Build.VERSION.SDK_INT >= 34) cycles = bi.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
                if (cycles < 0) cycles = (int) readLong("/sys/class/power_supply/battery/cycle_count");
                hCycle.setText(cycles >= 0 ? cycles + " kali" : "Tidak tersedia di HP ini");
            }
        } catch (Throwable ignored) { }

        double designMah = DESIGN_MAH;
        long sysDesign = readLong("/sys/class/power_supply/battery/charge_full_design");
        boolean designExact = sysDesign > 1000000;
        if (designExact) designMah = sysDesign / 1000.0;
        hDesign.setText(String.format(Locale.US, "%.0f mAh%s", designMah, designExact ? "" : " (spesifikasi)"));

        double learned;
        long sysFull = readLong("/sys/class/power_supply/battery/charge_full");
        boolean exact = sysFull > 1000000;
        if (exact) learned = sysFull / 1000.0;
        else learned = a.getSharedPreferences("cal", android.content.Context.MODE_PRIVATE).getFloat("full", 0f) / 1000.0;

        if (learned > 500) {
            hCap.setText(String.format(Locale.US, "%.0f mAh%s", learned, exact ? "" : " (estimasi)"));
            double pct = Math.min(100.0, learned / designMah * 100.0);
            hHealth.setText(String.format(Locale.US, "%.0f%%%s", pct, exact ? "" : " (estimasi kasar)"));
            hHealth.setTextColor(pct >= 80 ? GREEN : pct >= 60 ? YELLOW : RED);
            hHint.setText(exact ? "Kapasitas dibaca dari sistem."
                : "Estimasi dari kalibrasi charge counter, bisa meleset 10% atau lebih. Jangan dipakai untuk memutuskan ganti baterai.");
        } else {
            hCap.setText("Belum ada data");
            hHealth.setText("Belum ada data");
            hHealth.setTextColor(GRAY);
            hHint.setText("Colokkan charger dan biarkan layar pengisian terbuka sampai level naik 1%. Estimasi muncul setelah itu.");
        }
    }

    private static long readLong(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return Long.parseLong(br.readLine().trim());
        } catch (Throwable t) {
            return -1;
        }
    }
}
