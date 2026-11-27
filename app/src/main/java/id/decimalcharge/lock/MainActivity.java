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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import static id.decimalcharge.lock.Ui.*;

/** Kerangka aplikasi: header, menu bawah, dan perpindahan antar halaman. */
public class MainActivity extends Activity {
    Ui ui;
    SharedPreferences prefs;
    final Handler handler = new Handler(Looper.getMainLooper());
    int period = 1;

    HomePage home;
    HistoryPage history;
    SettingsPage settings;
    AboutPage about;

    private final Runnable ticker = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 2000); }
    };

    private FrameLayout content;
    private final View[] pages = new View[4];
    private final LinearLayout[] navItems = new LinearLayout[4];
    private final IconView[] navIcons = new IconView[4];
    private final TextView[] navLabels = new TextView[4];
    private int nav = 0;
    private int notifAsked = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF050A18);
        getWindow().setNavigationBarColor(0xFF091024);
        ui = new Ui(this);
        prefs = getSharedPreferences("app", MODE_PRIVATE);
        period = prefs.getInt("tperiod2", 1);
        if (period < 0 || period > 3) period = 1;

        home = new HomePage(this);
        history = new HistoryPage(this);
        settings = new SettingsPage(this);
        about = new AboutPage(this);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF050A18, 0xFF081634}));
        main.addView(buildHeader());
        content = new FrameLayout(this);
        main.addView(content, ui.lpw(MATCH, 0, 1f));
        main.addView(buildNav());

        pages[0] = home.build();
        pages[1] = history.build();
        pages[2] = settings.build();
        pages[3] = about.build();
        for (int i = 0; i < 4; i++) content.addView(pages[i], new FrameLayout.LayoutParams(MATCH, MATCH));
        home.styleTabs();
        history.styleTabs();
        selectNav(0);
        setContentView(main);
    }

    @Override public void onBackPressed() {
        if (nav != 0) selectNav(0);
        else if (home.pager != null && home.pager.getPage() == 1) home.pager.setPage(0, true);
        else super.onBackPressed();
    }

    @Override protected void onResume() { super.onResume(); handler.post(ticker); }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }

    String versionString() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
            return "Versi " + pi.versionName + " (build " + code + ")";
        } catch (Throwable t) {
            return "";
        }
    }

    private View buildHeader() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(ui.dp(20), ui.dp(30), ui.dp(16), ui.dp(10));
        h.addView(new IconView(this, IconView.SHIELD, BLUE), ui.lp(ui.dp(40), ui.dp(40)));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(ui.dp(12), 0, 0, 0);
        t.addView(ui.text("Decimal Charge Lock", TXT, 20, true));
        t.addView(ui.text(versionString(), TXT3, 12, false));
        h.addView(t, ui.lpw(0, WRAP, 1f));
        IconView gear = new IconView(this, IconView.GEAR, TXT2);
        gear.setOnClickListener(v -> selectNav(2));
        h.addView(gear, ui.lp(ui.dp(30), ui.dp(30)));
        return h;
    }

    private View buildNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(ui.dp(10), ui.dp(8), ui.dp(10), ui.dp(10));
        bar.setBackground(ui.gradBox(new int[]{0xFF0C1530, 0xFF091024}, 0, 0));
        int[] icons = {IconView.HOME, IconView.CHART, IconView.GEAR, IconView.INFO};
        String[] names = {"Beranda", "Riwayat", "Pengaturan", "Tentang"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout it = new LinearLayout(this);
            it.setOrientation(LinearLayout.VERTICAL);
            it.setGravity(Gravity.CENTER);
            it.setPadding(0, ui.dp(6), 0, ui.dp(6));
            IconView ic = new IconView(this, icons[i], TXT3);
            it.addView(ic, ui.lp(ui.dp(24), ui.dp(24)));
            TextView lb = ui.text(names[i], TXT3, 11, true);
            lb.setGravity(Gravity.CENTER);
            it.addView(lb);
            it.setOnClickListener(v -> selectNav(idx));
            LinearLayout.LayoutParams nlp = ui.lpw(0, WRAP, 1f);
            nlp.setMargins(ui.dp(3), 0, ui.dp(3), 0);
            bar.addView(it, nlp);
            navItems[i] = it;
            navIcons[i] = ic;
            navLabels[i] = lb;
        }
        return bar;
    }

    void selectNav(int i) {
        nav = i;
        for (int k = 0; k < 4; k++) {
            boolean sel = k == i;
            if (pages[k] != null) pages[k].setVisibility(sel ? View.VISIBLE : View.GONE);
            navItems[k].setBackground(sel ? ui.gradBox(new int[]{0xFF1E4FB8, 0xFF143A8C}, BLUE, 16) : null);
            navIcons[k].setColor(sel ? TXT : TXT3);
            navLabels[k].setTextColor(sel ? TXT : TXT3);
        }
    }

    void setPeriod(int k) {
        period = k;
        prefs.edit().putInt("tperiod2", k).apply();
        home.styleTabs();
        history.styleTabs();
        history.loadTemp(true);
    }

    // ===================== PEMBARUAN =====================
    void refresh() {
        boolean on = running();
        boolean n = Build.VERSION.SDK_INT < 33
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean o = Settings.canDrawOverlays(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean bt = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        boolean[] perm = {n, o, bt};

        home.refresh(on, perm);
        settings.refresh(perm);
        try {
            BatteryReader r = BatteryReader.read(this);
            Intent bi = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int tm = bi == null ? Integer.MIN_VALUE : bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            double tv = tm == Integer.MIN_VALUE ? Double.NaN : tm / 10.0;
            home.setNow(tv, r);
            history.setCondition(tv);
        } catch (Throwable ignored) { }
        history.refresh();
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

    // ===================== AKSI BERSAMA =====================
    void showDnsGuide() {
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
            + "Isi hostname DNS, nyalakan <b>Bubble DNS</b>. Ketuk bubble: ON = DNS pribadi, ketuk lagi = Otomatis.<br><br>"
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
        tv.setPadding(ui.dp(20), ui.dp(12), ui.dp(20), ui.dp(4));
        ScrollView gsv = new ScrollView(this);
        gsv.addView(tv);
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Panduan memasang izin DNS")
            .setView(gsv)
            .setPositiveButton("Salin perintah ADB", (d, w) -> copyAdb())
            .setNegativeButton("Tutup", null)
            .show();
    }

    void openUsageAccess() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            catch (Exception e2) { openAppInfo(); }
        }
    }

    void copyAdb() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("adb", DnsHelper.ADB_CMD));
        Toast.makeText(this, "Perintah disalin", Toast.LENGTH_SHORT).show();
    }

    void askNotif() {
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

    void open(Intent i) { try { startActivity(i); } catch (Exception e) { openAppInfo(); } }

    void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }
}
