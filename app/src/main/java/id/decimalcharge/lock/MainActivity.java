package id.decimalcharge.lock;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private LinearLayout box;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xff000000);
        getWindow().setNavigationBarColor(0xff000000);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xff000000);
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 64, 40, 40);
        sv.addView(box);

        label("Decimal Charge Lock", 0xffffffff, 24);
        label("\nMode otomatis: layar pengisian terbuka sendiri saat charger dicolok. Lakukan langkah 1 sampai 4.", 0xffb8c4ca, 15);
        button("1. Aktifkan mode otomatis", v -> startForegroundService(new Intent(this, ChargeService.class)));
        button("2. Izin notifikasi", v -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1));
        button("3. Izin tampil di atas aplikasi lain", v -> startActivity(new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        button("4. Pengaturan aplikasi (MIUI)", v -> startActivity(new Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))));
        label("\nDi MIUI 14: buka Izin lain, aktifkan \"Tampilkan di layar kunci\" dan \"Tampilkan jendela pop-up saat berjalan di latar belakang\". Aktifkan Autostart, dan Hemat baterai pilih \"Tanpa pembatasan\".", 0xff8a979d, 13);

        label("\nCadangan: Screensaver", 0xffffffff, 16);
        button("Buka pengaturan Screensaver", v -> {
            try { startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
            catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("Buka Setelan Android lalu cari 'Screensaver'.")
                    .setPositiveButton("OK", null).show();
            }
        });
        button("Pratinjau tampilan", v -> startActivity(new Intent(this, PreviewActivity.class)));
        label("\nAndroid hanya melaporkan level bulat. Desimal dihitung dari charge counter asli baterai dan diberi label estimasi; jika tidak tersedia, yang tampil persentase bulat. Tekan lama layar pengisian untuk menutupnya.", 0xff8a979d, 13);
        setContentView(sv);
    }

    private void label(String t, int color, int sp) {
        TextView v = new TextView(this); v.setText(t); v.setTextColor(color); v.setTextSize(sp); box.addView(v);
    }

    private void button(String t, android.view.View.OnClickListener l) {
        Button bt = new Button(this); bt.setText(t); bt.setOnClickListener(l); box.addView(bt);
    }
}
