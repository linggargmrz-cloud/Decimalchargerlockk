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
        t.addView(text("Decimal Charge Lock", TXT, 18, true));
        t.addView(text(a.versionString(), TXT3, 12, false));
        top.addView(t);
        box.addView(top);
        TextView d = text("Layar pengisian dengan persen baterai desimal, pemantau suhu dan baterai, serta pengganti DNS pribadi lewat bubble.", TXT2, 13, false);
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
            + "• Pengaturan dan riwayat tidak hilang saat aplikasi diperbarui, selama aplikasi tidak dihapus.\n\n"
            + "• Tekan lama layar pengisian untuk menutupnya.", TXT2, 12, false);
        lt.setPadding(0, dp(10), 0, 0);
        lim.addView(lt);
        root.addView(lim);

        LinearLayout g = card();
        g.addView(header(IconView.DOC, "Bantuan"));
        g.addView(button(IconView.DOC, "Panduan Memasang Izin DNS (Lengkap)", false, v -> a.showDnsGuide()));
        root.addView(g);
        return sv;
    }
}
