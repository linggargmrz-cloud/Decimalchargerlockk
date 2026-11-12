package id.decimalcharge.lock;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = 0xFF2ECC71, RED = 0xFFE74C3C, GRAY = 0xFF7D8890, YELLOW = 0xFFF1C40F;
    private static final double DESIGN_MAH = 5000.0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 2000); }
    };

    private SharedPreferences prefs;
    private LinearLayout statusCard;
    private TextView statusTitle, statusSub;
    private Switch toggle;
    private CompoundButton.OnCheckedChangeListener toggleListener;
    private Row rowNotif, rowOverlay, rowBattery;
    private TextView hStatus, hTemp, hVolt, hTech, hCycle, hDesign, hCap, hHealth, hHint;
    private int notifAsked = 0;

    private static class Row { TextView chip; Button btn; }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xff000000);
        getWindow().setNavigationBarColor(0xff000000);
        prefs = getSharedPreferences("app", MODE_PRIVATE);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xff000000);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(36), dp(18), dp(28));
        sv.addView(root);

        TextView title = text("Decimal Charge Lock", 0xffffffff, 24, true);
        TextView sub = text("Layar pengisian dengan persen desimal", 0xff8a979d, 13, false);
        sub.setPadding(0, dp(2), 0, dp(16));
        root.addView(title);
        root.addView(sub);

        // Kartu status mode otomatis
        statusCard = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        statusTitle = text("", 0xffffffff, 20, true);
        statusSub = text("", 0xffb8c4ca, 13, false);
        tx.addView(text("MODE OTOMATIS", 0xff9aa5ab, 11, true));
        tx.addView(statusTitle);
        tx.addView(statusSub);
        top.addView(tx, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        toggle = new Switch(this);
        top.addView(toggle);
        statusCard.addView(top);
        statusCard.addView(text("Layar pengisian terbuka sendiri saat charger dicolok.", 0xff8a979d, 12, false));
        root.addView(statusCard);

        toggleListener = (v, on) -> {
            prefs.edit().putBoolean("auto", on).apply();
            Intent i = new Intent(this, ChargeService.class);
            if (on) startForegroundService(i); else stopService(i);
            handler.postDelayed(this::refresh, 500);
        };

        // Kartu izin
        LinearLayout perm = card();
        perm.addView(text("IZIN", 0xff9aa5ab, 11, true));
        rowNotif = row(perm, "Notifikasi", "Dibutuhkan agar layanan tampil dan berjalan stabil.", "Atur", v -> askNotif());
        rowOverlay = row(perm, "Tampil di atas aplikasi lain", "Agar layar pengisian bisa terbuka sendiri.", "Atur", v ->
            open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        rowBattery = row(perm, "Hemat baterai: tanpa batas", "Agar MIUI tidak mematikan layanan.", "Atur", v -> {
            try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
            catch (Exception e) { openAppInfo(); }
        });
        Row miui = row(perm, "MIUI: layar kunci dan pop-up", "Aktifkan \"Tampilkan di layar kunci\", \"Tampilkan jendela pop-up saat berjalan di latar belakang\", dan Autostart.", "Buka pengaturan", v -> openAppInfo());
        chip(miui.chip, "Cek manual", GRAY);
        root.addView(perm);

        // Kartu tampilan
        LinearLayout look = card();
        look.addView(text("TAMPILAN", 0xff9aa5ab, 11, true));
        look.addView(btn("Pratinjau tampilan", v -> startActivity(new Intent(this, PreviewActivity.class))));
        look.addView(btn("Screensaver (cadangan)", v -> {
            try { startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
            catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("Buka Setelan Android lalu cari 'Screensaver'.")
                    .setPositiveButton("OK", null).show();
            }
        }));
        root.addView(look);

        // Kartu kesehatan baterai (hanya di dalam aplikasi)
        LinearLayout health = card();
        health.addView(text("KESEHATAN BATERAI", 0xff9aa5ab, 11, true));
        hStatus = kv(health, "Status sistem");
        hTemp = kv(health, "Suhu");
        hVolt = kv(health, "Tegangan");
        hTech = kv(health, "Teknologi");
        hCycle = kv(health, "Siklus pengisian");
        hDesign = kv(health, "Kapasitas desain");
        hCap = kv(health, "Estimasi kapasitas saat ini");
        hHealth = kv(health, "Estimasi kesehatan");
        hHint = text("", 0xff8a979d, 11, false);
        hHint.setPadding(0, dp(10), 0, 0);
        health.addView(hHint);
        root.addView(health);

        TextView foot = text("Android hanya melaporkan level baterai bulat. Desimal dihitung dari charge counter asli baterai dan diberi label estimasi. Tekan lama layar pengisian untuk menutupnya.", 0xff59646a, 11, false);
        foot.setPadding(dp(4), dp(10), dp(4), 0);
        root.addView(foot);

        setContentView(sv);
    }

    @Override protected void onResume() { super.onResume(); handler.post(ticker); }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }

    private void refresh() {
        boolean on = running();
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(on);
        toggle.setOnCheckedChangeListener(toggleListener);
        int c = on ? GREEN : RED;
        statusCard.setBackground(box(on ? 0xFF0E2417 : 0xFF2A1210, c, 14));
        statusTitle.setText(on ? "AKTIF" : "MATI");
        statusTitle.setTextColor(c);
        statusSub.setText(on ? "Layanan berjalan" : (prefs.getBoolean("auto", true)
            ? "Layanan tidak berjalan (HP restart?). Nyalakan dengan tombol." : "Dimatikan oleh Anda"));

        boolean n = Build.VERSION.SDK_INT < 33
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        state(rowNotif, n);
        state(rowOverlay, Settings.canDrawOverlays(this));
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        state(rowBattery, pm != null && pm.isIgnoringBatteryOptimizations(getPackageName()));

        refreshHealth();
    }

    private void refreshHealth() {
        try {
            Intent bi = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bi != null) {
                int hs = bi.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
                String s; int col = 0xFFFFFFFF;
                switch (hs) {
                    case BatteryManager.BATTERY_HEALTH_GOOD: s = "Baik"; col = GREEN; break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT: s = "Terlalu panas"; col = RED; break;
                    case BatteryManager.BATTERY_HEALTH_DEAD: s = "Rusak"; col = RED; break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: s = "Tegangan berlebih"; col = RED; break;
                    case BatteryManager.BATTERY_HEALTH_COLD: s = "Terlalu dingin"; col = YELLOW; break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: s = "Gangguan"; col = RED; break;
                    default: s = "Tidak diketahui"; col = GRAY;
                }
                hStatus.setText(s); hStatus.setTextColor(col);
                int tm = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                hTemp.setText(tm == Integer.MIN_VALUE ? "Tidak tersedia"
                    : String.format(new Locale("id", "ID"), "%.1f °C", tm / 10.0));
                int mv = bi.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                hVolt.setText(mv > 0 ? String.format(new Locale("id", "ID"), "%.2f V", mv / 1000.0) : "Tidak tersedia");
                String tech = bi.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                hTech.setText(tech == null || tech.isEmpty() ? "Tidak tersedia" : tech);

                int cycles = -1;
                if (Build.VERSION.SDK_INT >= 34) cycles = bi.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
                if (cycles < 0) cycles = (int) readLong("/sys/class/power_supply/battery/cycle_count");
                hCycle.setText(cycles >= 0 ? String.valueOf(cycles) : "Tidak tersedia di HP ini");
            }
        } catch (Throwable ignored) { }

        double designMah = DESIGN_MAH;
        long sysDesign = readLong("/sys/class/power_supply/battery/charge_full_design");
        boolean designExact = sysDesign > 1000000;
        if (designExact) designMah = sysDesign / 1000.0;
        hDesign.setText(String.format(Locale.US, "%.0f mAh%s", designMah, designExact ? "" : " (spesifikasi)"));

        double learned = 0;
        long sysFull = readLong("/sys/class/power_supply/battery/charge_full");
        boolean exact = sysFull > 1000000;
        if (exact) learned = sysFull / 1000.0;
        else learned = getSharedPreferences("cal", MODE_PRIVATE).getFloat("full", 0f) / 1000.0;

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

    @SuppressWarnings("deprecation")
    private boolean running() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo s : am.getRunningServices(Integer.MAX_VALUE)) {
                if (ChargeService.class.getName().equals(s.service.getClassName())) return true;
            }
        } catch (Throwable ignored) { }
        return false;
    }

    private static long readLong(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return Long.parseLong(br.readLine().trim());
        } catch (Throwable t) { return -1; }
    }

    private void askNotif() {
        if (Build.VERSION.SDK_INT >= 33 && notifAsked == 0) {
            notifAsked++;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        } else {
            try {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            } catch (Exception e) { openAppInfo(); }
        }
    }

    private void open(Intent i) { try { startActivity(i); } catch (Exception e) { openAppInfo(); } }

    private void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    // ---- pembantu tampilan ----
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable box(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView text(String s, int color, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextColor(color); v.setTextSize(sp);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    private TextView kv(LinearLayout parent, String label) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(0, dp(10), 0, 0);
        TextView l = text(label, 0xFF9AA5AB, 13, false);
        line.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = text("-", 0xFFFFFFFF, 13, true);
        v.setGravity(Gravity.END);
        line.addView(v);
        parent.addView(line);
        return v;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(14), dp(16), dp(14));
        l.setBackground(box(0xFF101316, 0xFF20272C, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        l.setLayoutParams(lp);
        return l;
    }

    private Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s); b.setAllCaps(false); b.setTextColor(0xFFFFFFFF);
        b.setBackground(box(0xFF1B2227, 0xFF2C353C, 10));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private void chip(TextView v, String s, int color) {
        v.setText(s); v.setTextColor(color);
        v.setBackground(box((color & 0x00FFFFFF) | 0x26000000, 0, 12));
    }

    private Row row(LinearLayout parent, String title, String desc, String btnText, View.OnClickListener l) {
        Row r = new Row();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(12), 0, 0);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = text(title, 0xFFFFFFFF, 15, true);
        line.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.chip = text("", GRAY, 12, true);
        r.chip.setPadding(dp(10), dp(3), dp(10), dp(3));
        line.addView(r.chip);
        box.addView(line);
        box.addView(text(desc, 0xFF8A979D, 12, false));
        r.btn = btn(btnText, l);
        box.addView(r.btn);
        parent.addView(box);
        return r;
    }

    private void state(Row r, boolean ok) {
        chip(r.chip, ok ? "Aktif" : "Belum", ok ? GREEN : RED);
        r.btn.setVisibility(ok ? View.GONE : View.VISIBLE);
    }
    }
