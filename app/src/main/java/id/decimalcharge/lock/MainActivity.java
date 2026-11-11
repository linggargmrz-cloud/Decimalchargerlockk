package id.decimalcharge.lock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xff000000);
        getWindow().setNavigationBarColor(0xff000000);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xff000000);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 64, 40, 40);
        sv.addView(box);

        TextView title = new TextView(this);
        title.setText("Decimal Charge Lock"); title.setTextColor(0xffffffff); title.setTextSize(24);
        TextView info = new TextView(this);
        info.setText("\nScreensaver pengisian daya dengan persentase desimal.\n\n1. Tekan \"Buka pengaturan Screensaver\".\n2. Pilih Decimal Charge Lock.\n3. Atur mulai saat mengisi daya, dengan waktu tunggu terpendek.\n\nTombol Pratinjau menampilkan layarnya sekarang. Ketuk layar untuk menutup.\n\nCatatan: Android hanya melaporkan level bulat. Desimal dihitung dari charge counter asli baterai dan diberi label estimasi; jika tidak tersedia, yang tampil persentase bulat. Screensaver di MIUI biasanya muncul setelah layar idle.");
        info.setTextColor(0xffb8c4ca); info.setTextSize(15);

        Button settings = new Button(this); settings.setText("Buka pengaturan Screensaver");
        settings.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
            catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("Buka Setelan Android lalu cari 'Screensaver' atau 'Screen saver'.")
                    .setPositiveButton("OK", null).show();
            }
        });
        Button preview = new Button(this); preview.setText("Pratinjau tampilan");
        preview.setOnClickListener(v -> startActivity(new Intent(this, PreviewActivity.class)));

        box.addView(title); box.addView(info); box.addView(settings); box.addView(preview);
        setContentView(sv);
    }
}
