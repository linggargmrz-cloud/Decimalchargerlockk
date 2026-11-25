package id.decimalcharge.lock;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
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
    private Row rowNotif, rowOverlay, rowBattery, rowDns, rowQuota;
    private TextView hStatus, hTemp, hVolt, hTech, hCycle, hDesign, hCap, hHealth, hHint;
    private TextView dnsMode, quotaText;
    private Switch bubbleSwitch, quotaSwitch;
    private CompoundButton.OnCheckedChangeListener bubbleListener, quotaListener;
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

        // Kartu DNS pribadi (bubble)
        LinearLayout dns = card();
        dns.addView(text("DNS PRIBADI (BUBBLE)", 0xff9aa5ab, 11, true));
        dnsMode = text("", 0xFFFFFFFF, 14, true);
        dnsMode.setPadding(0, dp(10), 0, 0);
        dns.addView(dnsMode);

        EditText et = new EditText(this);
        et.setHint("Hostname DNS, contoh: dns.google");
        et.setHintTextColor(0xFF59646A);
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        et.setBackground(box(0xFF1B2227, 0xFF2C353C, 10));
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setText(DnsHelper.host(this));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int cn, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int bf, int cn) { }
            @Override public void afterTextChanged(Editable s) { DnsHelper.saveHost(MainActivity.this, s.toString()); }
        });
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        elp.topMargin = dp(10);
        et.setLayoutParams(elp);
        dns.addView(et);

        LinearLayout bl = new LinearLayout(this);
        bl.setOrientation(LinearLayout.HORIZONTAL);
        bl.setGravity(Gravity.CENTER_VERTICAL);
        bl.setPadding(0, dp(14), 0, 0);
        bl.addView(text("Tampilkan bubble DNS", 0xFFFFFFFF, 15, true),
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bubbleSwitch = new Switch(this);
        bl.addView(bubbleSwitch);
        dns.addView(bl);
        TextView hintDns = text("Ketuk bubble: ON = DNS pribadi (hostname di atas), ketuk lagi = DNS Otomatis. Geser untuk memindahkan.", 0xff8a979d, 12, false);
        hintDns.setPadding(0, dp(6), 0, 0);
        dns.addView(hintDns);
        dns.addView(btn("Panduan memasang izin DNS (lengkap)", v -> showDnsGuide()));

        bubbleListener = (v, on) -> {
            if (on) {
                if (!Settings.canDrawOverlays(this)) {
                    open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                    handler.postDelayed(this::refresh, 500);
                    return;
                }
                startForegroundService(new Intent(this, DnsBubbleService.class));
            } else {
                stopService(new Intent(this, DnsBubbleService.class));
            }
            handler.postDelayed(this::refresh, 600);
        };

        rowDns = row(dns, "Izin ubah DNS", "Diberikan sekali lewat ADB. Ikuti panduan di atas, atau salin perintahnya.", "Salin perintah ADB", v -> copyAdb());
        root.addView(dns);

        // Kartu kuota harian
        LinearLayout quota = card();
        quota.addView(text("KUOTA HARI INI", 0xff9aa5ab, 11, true));
        quotaText = text("", 0xFFFFFFFF, 14, true);
        quotaText.setPadding(0, dp(10), 0, 0);
        quota.addView(quotaText);

        LinearLayout ql = new LinearLayout(this);
        ql.setOrientation(LinearLayout.HORIZONTAL);
        ql.setGravity(Gravity.CENTER_VERTICAL);
        ql.setPadding(0, dp(14), 0, 0);
        ql.addView(text("Tampilkan di panel notifikasi", 0xFFFFFFFF, 15, true),
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        quotaSwitch = new Switch(this);
        ql.addView(quotaSwitch);
        quota.addView(ql);

        quotaListener = (v, on) -> {
            if (on) {
                if (!QuotaHelper.hasAccess(this)) {
                    openUsageAccess();
                    handler.postDelayed(this::refresh, 500);
                    return;
                }
                prefs.edit().putBoolean("quota", true).apply();
                startForegroundService(new Intent(this, QuotaService.class));
            } else {
                prefs.edit().putBoolean("quota", false).apply();
                stopService(new Intent(this, QuotaService.class));
            }
            handler.postDelayed(this::refresh, 600);
        };

        rowQuota = row(quota, "Akses penggunaan", "Izin khusus agar aplikasi bisa membaca pemakaian data.", "Atur", v -> openUsageAccess());
        TextView qn = text("Angka dihitung oleh HP sejak 00:00 dan bisa berbeda sedikit dari hitungan provider. Data seluler dari semua SIM digabung.", 0xff8a979d, 11, false);
        qn.setPadding(0, dp(10), 0, 0);
        quota.addView(qn);
        root.addView(quota);

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

    private void showDnsGuide() {
        String html =
            "<b>Syarat:</b> Android 11 ke atas dan HP tersambung Wi-Fi.<br><br>"
            + "<b>1. Pasang aShell You (gratis)</b><br>"
            + "F-Droid: <a href=\"https://f-droid.org/en/packages/in.hridayan.ashell/\">f-droid.org/en/packages/in.hridayan.ashell</a><br>"
            + "GitHub: <a href=\"https://github.com/DP-Hridayan/aShellYou\">github.com/DP-Hridayan/aShellYou</a><br>"
            + "Situs: <a href=\"https://ashellyou.github.io/\">ashellyou.github.io</a><br>"
            + "Pastikan namanya <b>aShell You</b> (pembuat DP Hridayan).<br><br>"
            + "<b>2. Nyalakan Opsi pengembang</b><br>"
            + "Pengaturan > Tentang ponsel > ketuk <b>Versi MIUI</b> 7 kali. Lalu buka Pengaturan tambahan > Opsi pengembang dan nyalakan <b>Debugging USB</b>, <b>Debugging USB (setelan Keamanan)</b> (bisa meminta akun Mi dan SIM), dan <b>Debugging nirkabel</b>.<br><br>"
            + "<b>3. Pairing di aShell You</b><br>"
            + "Buka aShell You > kartu <b>ADB via Wireless Debugging</b> > <b>This Device</b> > <b>Pair</b>. Di Opsi pengembang > Debugging nirkabel, ketuk <b>Pasangkan perangkat dengan kode penyambungan</b>. Ketik kode 6 digit di notifikasi aShell You. Kembali ke aShell You, ketuk <b>Start</b> sampai tersambung (bukan \"Basic shell\").<br><br>"
            + "<b>4. Jalankan perintah</b><br>"
            + "Di terminal aShell You, ketik lalu kirim (tombol di bawah menyalinnya):<br>"
            + "<tt>pm grant id.decimalcharge.lock android.permission.WRITE_SECURE_SETTINGS</tt><br>"
            + "Berhasil jika tidak ada teks error.<br><br>"
            + "<b>5. Cek</b><br>"
            + "Kembali ke aplikasi ini: <b>Izin ubah DNS</b> harus <b>Aktif</b>.<br><br>"
            + "<b>6. Pakai</b><br>"
            + "Isi hostname DNS, nyalakan <b>Tampilkan bubble DNS</b>. Ketuk bubble: ON = DNS pribadi, ketuk lagi = Otomatis.<br><br>"
            + "<b>Catatan</b><br>"
            + "&#8226; Setelah izin masuk, aShell You, Wi-Fi, dan debugging boleh dimatikan.<br>"
            + "&#8226; Error \"user 10xxx\" berarti aShell You masih di Basic shell: ulangi Pair dan Start.<br>"
            + "&#8226; Izin hilang jika aplikasi ini dihapus lalu dipasang ulang.";
        TextView tv = new TextView(this);
        tv.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setTextColor(0xFFDDE3E7);
        tv.setLinkTextColor(0xFF6FB1FF);
        tv.setTextSize(14);
        tv.setPadding(dp(20), dp(12), dp(20), dp(4));
        ScrollView gsv = new ScrollView(this);
        gsv.addView(tv);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Panduan memasang izin DNS")
            .setView(gsv)
            .setPositiveButton("Salin perintah ADB", (d, w) -> copyAdb())
            .setNegativeButton("Tutup", null)
            .show();
    }

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

        refreshDns();
        refreshQuota();
        refreshHealth();
    }

    private void refreshDns() {
        bubbleSwitch.setOnCheckedChangeListener(null);
        bubbleSwitch.setChecked(DnsBubbleService.running);
        bubbleSwitch.setOnCheckedChangeListener(bubbleListener);
        String m = DnsHelper.mode(this);
        String s;
        if ("hostname".equals(m)) {
            String sp = DnsHelper.spec(this);
            s = "Mode DNS saat ini: Pribadi" + (sp.isEmpty() ? "" : " (" + sp + ")");
        } else if ("off".equals(m)) {
            s = "Mode DNS saat ini: Mati";
        } else {
            s = "Mode DNS saat ini: Otomatis";
        }
        dnsMode.setText(s);
        state(rowDns, DnsHelper.canWrite(this));
    }

    private void refreshQuota() {
        quotaSwitch.setOnCheckedChangeListener(null);
        quotaSwitch.setChecked(QuotaService.running);
        quotaSwitch.setOnCheckedChangeListener(quotaListener);
        boolean has = QuotaHelper.hasAccess(this);
        state(rowQuota, has);
        if (has) {
            quotaText.setText("Seluler: " + QuotaHelper.fmt(QuotaHelper.mobileToday(this))
                + "   Wi-Fi: " + QuotaHelper.fmt(QuotaHelper.wifiToday(this)));
        } else {
            quotaText.setText("Aktifkan izin Akses penggunaan dulu");
        }
    }

    private void openUsageAccess() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            catch (Exception e2) { openAppInfo(); }
        }
    }

    private void copyAdb() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("adb", DnsHelper.ADB_CMD));
        Toast.makeText(this, "Perintah disalin", Toast.LENGTH_SHORT).show();
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
