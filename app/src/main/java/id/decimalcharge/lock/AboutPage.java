package id.decimalcharge.lock;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static id.decimalcharge.lock.Ui.*;

/** Halaman Tentang. */
class AboutPage extends Page {
    AboutPage(MainActivity a) { super(a); }

    private TextView center(String s, int color, int sp, boolean bold, int topDp) {
        TextView t = text(s, color, sp, bold);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(topDp), 0, 0);
        return t;
    }

    @Override View build() {
        ScrollView sv = pageScroll();
        LinearLayout root = column();
        sv.addView(root);

        LinearLayout box = card();
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(new IconView(a, IconView.SHIELD, BLUE), lp(dp(46), dp(46)));
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(14), 0, 0, 0);
        t.addView(text("L-CORE", TXT, 18, true));
        t.addView(text(a.versionString(), TXT3, 12, false));
        top.addView(t);
        box.addView(top);
        TextView d = text("Kumpulan alat Android: layar pengisian dengan persen desimal, pemantau suhu dan baterai, DNS pribadi lewat bubble, kontrol rotasi layar, dan auto clicker multi-sentuhan.", TXT2, 13, false);
        d.setPadding(0, dp(12), 0, 0);
        box.addView(d);
        root.addView(box);

        LinearLayout lim = card();
        lim.addView(header(IconView.INFO, "Hal yang perlu diketahui"));
        TextView lt = text(
            "• Android hanya melaporkan level baterai bulat. Desimal dihitung dari charge counter baterai dan merupakan estimasi.\n\n"
            + "• Suhu berasal dari sensor baterai, bisa berbeda dari suhu prosesor.\n\n"
            + "• Pencatatan riwayat berhenti saat HP tidur nyenyak, sehingga garis bisa putus.\n\n"
            + "• Kuota dihitung oleh HP dan bisa berbeda dari hitungan provider.\n\n"
            + "• Izin ubah DNS hilang jika aplikasi dihapus lalu dipasang ulang.\n\n"
            + "• Kontrol rotasi memaksa orientasi lewat jendela transparan. Sebagian aplikasi atau ROM bisa mengabaikannya.\n\n"
            + "• Auto clicker butuh layanan aksesibilitas, hanya mengirim sentuhan yang Anda atur, dan tidak membaca isi layar.\n\n"
            + "• Pengaturan dan riwayat tidak hilang saat aplikasi diperbarui, selama aplikasi tidak dihapus.\n\n"
            + "• Tekan lama layar pengisian untuk menutupnya.", TXT2, 12, false);
        lt.setPadding(0, dp(10), 0, 0);
        lim.addView(lt);
        root.addView(lim);

        LinearLayout g = card();
        g.addView(header(IconView.DOC, "Bantuan"));
        g.addView(button(IconView.DOC, "Panduan Memasang Izin DNS (Lengkap)", false, v -> a.showDnsGuide()));
        root.addView(g);

        // Kartu identitas, paling bawah
        LinearLayout brand = card();
        brand.setGravity(Gravity.CENTER_HORIZONTAL);
        brand.setPadding(dp(20), dp(24), dp(20), dp(24));
        LinearLayout.LayoutParams ilp = lp(dp(44), dp(44));
        ilp.gravity = Gravity.CENTER_HORIZONTAL;
        brand.addView(new IconView(a, IconView.SHIELD, BLUE), ilp);
        brand.addView(center("L-CORE", TXT, 26, true, 10));
        brand.addView(center("Multi-Tools Ecosystem", ACCENT, 14, true, 2));
        brand.addView(center("A unified collection of intelligent Android utilities designed for performance, control, and convenience.", TXT2, 13, false, 14));
        brand.addView(center("Created & Developed by Linggar", TXT, 13, true, 18));
        brand.addView(center("Version 1.0.0", TXT3, 12, false, 6));
        brand.addView(center("© 2026 Linggar. All rights reserved.", TXT3, 12, false, 2));
        brand.addView(center("Made with passion. Built for innovation.", TXT2, 12, false, 14));
        root.addView(brand);
        return sv;
    }
}
