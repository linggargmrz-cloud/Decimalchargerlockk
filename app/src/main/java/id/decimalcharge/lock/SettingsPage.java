package id.decimalcharge.lock;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static id.decimalcharge.lock.Ui.*;

/** Halaman Pengaturan: izin, tampilan, dan data. */
class SettingsPage extends Page {
    private Row rowNotif, rowOverlay, rowBattery;

    SettingsPage(MainActivity a) { super(a); }

    @Override View build() {
        ScrollView sv = pageScroll();
        LinearLayout root = column();
        sv.addView(root);

        LinearLayout perm = card();
        perm.addView(header(IconView.BELL, "Izin"));
        rowNotif = row(perm, "Notifikasi", "Dibutuhkan agar layanan tampil dan berjalan stabil.", "Atur", -1, false, v -> a.askNotif());
        rowOverlay = row(perm, "Tampil di atas aplikasi lain", "Agar layar pengisian bisa terbuka sendiri.", "Atur", -1, false, v ->
            a.open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.getPackageName()))));
        rowBattery = row(perm, "Hemat baterai: tanpa batas", "Agar MIUI tidak mematikan layanan.", "Atur", -1, false, v -> {
            try { a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
            catch (Exception e) { a.openAppInfo(); }
        });
        Row miui = row(perm, "MIUI: layar kunci dan pop-up", "Aktifkan \"Tampilkan di layar kunci\", \"Tampilkan jendela pop-up saat berjalan di latar belakang\", dan Autostart.", "Buka Pengaturan Aplikasi", IconView.GEAR, false, v -> a.openAppInfo());
        chip(miui.chip, "Cek manual", GRAY);
        root.addView(perm);

        LinearLayout look = card();
        look.addView(header(IconView.BATTERY, "Tampilan"));
        look.addView(button(IconView.BATTERY, "Pratinjau Tampilan", true, v -> a.startActivity(new Intent(a, PreviewActivity.class))));
        look.addView(button(IconView.INFO, "Screensaver (Cadangan)", false, v -> {
            try { a.startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
            catch (Exception e) {
                new AlertDialog.Builder(a).setMessage("Buka Setelan Android lalu cari 'Screensaver'.")
                    .setPositiveButton("OK", null).show();
            }
        }));
        root.addView(look);

        LinearLayout data = card();
        data.addView(header(IconView.CHART, "Data"));
        TextView dt = text("Riwayat suhu dan baterai disimpan di HP selama 7 hari.", TXT3, 12, false);
        dt.setPadding(0, dp(8), 0, 0);
        data.addView(dt);
        data.addView(button(-1, "Hapus Riwayat Suhu & Baterai", false, v ->
            new AlertDialog.Builder(a)
                .setMessage("Hapus semua riwayat suhu dan baterai?")
                .setPositiveButton("Hapus", (dd, w) -> { TempLog.clear(a); a.history.loadTemp(true); })
                .setNegativeButton("Batal", null)
                .show()));
        root.addView(data);
        return sv;
    }

    void refresh(boolean[] perm) {
        state(rowNotif, perm[0]);
        state(rowOverlay, perm[1]);
        state(rowBattery, perm[2]);
    }
}
