package id.decimalcharge.lock;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import static id.decimalcharge.lock.Ui.*;

/** Halaman Beranda: dua ruang yang bisa digeser, Decimal dan DNS. */
class HomePage extends Page {
    TwoPager pager;
    TempChartView chartHome;
    final TextView[] pHome = new TextView[4];

    private FrameLayout tabs;
    private View indicator;
    private TextView tabDecimal, tabDns;

    // Ruang Decimal
    private LinearLayout statusCard;
    private GradientDrawable ringDrawable;
    private IconView ringIcon;
    private TextView statusTitle, statusSub, permChip, hTempNow, hBatNow;
    private Switch toggle;
    private CompoundButton.OnCheckedChangeListener toggleListener;
    private final GradientDrawable[] permDot = new GradientDrawable[3];
    private final IconView[] permTick = new IconView[3];

    // Ruang DNS
    private LinearLayout dnsHero;
    private TextView dnsMode, dnsSub, dnsModeChip, quotaText;
    private Switch bubbleSwitch, quotaSwitch;
    private CompoundButton.OnCheckedChangeListener bubbleListener, quotaListener;
    private Row rowDns, rowQuota;

    HomePage(MainActivity a) { super(a); }

    void styleTabs() { styleArr(pHome); }

    @Override View build() {
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);

        tabs = new FrameLayout(a);
        tabs.setBackground(gradBox(new int[]{0xFF0C1530, 0xFF0A1128}, STROKE, 26));
        indicator = new View(a);
        indicator.setBackground(gradBox(new int[]{BLUE, BLUE2}, 0xFF7DB2FF, 22));
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(dp(10), MATCH);
        ilp.setMargins(0, dp(4), 0, dp(4));
        tabs.addView(indicator, ilp);
        LinearLayout tabRow = new LinearLayout(a);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabDecimal = tab("Decimal", 0);
        tabDns = tab("DNS", 1);
        tabRow.addView(tabDecimal, lpw(0, MATCH, 1f));
        tabRow.addView(tabDns, lpw(0, MATCH, 1f));
        tabs.addView(tabRow, new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout.LayoutParams tlp = lp(MATCH, dp(48));
        tlp.setMargins(dp(18), dp(4), dp(18), dp(10));
        col.addView(tabs, tlp);

        pager = new TwoPager(a);
        ScrollView sv0 = pageScroll();
        LinearLayout c0 = column();
        sv0.addView(c0);
        ScrollView sv1 = pageScroll();
        LinearLayout c1 = column();
        sv1.addView(c1);
        pager.addView(sv0, new FrameLayout.LayoutParams(MATCH, MATCH));
        pager.addView(sv1, new FrameLayout.LayoutParams(MATCH, MATCH));
        col.addView(pager, lpw(MATCH, 0, 1f));

        buildDecimalRoom(c0);
        buildDnsRoom(c1);

        pager.setListener(new TwoPager.Listener() {
            @Override public void onScroll(float pos) { updateIndicator(pos); }
            @Override public void onSelected(int page) { a.prefs.edit().putInt("page", page).apply(); }
        });
        tabs.addOnLayoutChangeListener((v, l, tp, r, bt, ol, ot, orr, ob) -> updateIndicator(pager.getPos()));
        pager.setPage(a.prefs.getInt("page", 0) == 1 ? 1 : 0, false);
        return col;
    }

    private TextView tab(String label, final int idx) {
        TextView t = text(label, TXT3, 14, true);
        t.setGravity(Gravity.CENTER);
        t.setOnClickListener(v -> pager.setPage(idx, true));
        return t;
    }

    private void updateIndicator(float pos) {
        if (tabs == null || indicator == null || tabDecimal == null) return;
        int w = tabs.getWidth();
        if (w <= 0) return;
        int iw = (w - dp(8)) / 2;
        FrameLayout.LayoutParams l = (FrameLayout.LayoutParams) indicator.getLayoutParams();
        if (l.width != iw) {
            l.width = iw;
            indicator.setLayoutParams(l);
        }
        indicator.setTranslationX(dp(4) + pos * iw);
        tabDecimal.setTextColor(pos < 0.5f ? TXT : TXT3);
        tabDns.setTextColor(pos >= 0.5f ? TXT : TXT3);
    }

    // ===================== RUANG DECIMAL =====================
    private void buildDecimalRoom(LinearLayout root) {
        statusCard = card();
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout ringBox = new FrameLayout(a);
        ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setColor(0xFF0A1530);
        ringDrawable.setStroke(dp(5), GREEN);
        ringBox.setBackground(ringDrawable);
        ringIcon = new IconView(a, IconView.BATTERY, GREEN);
        ringBox.addView(ringIcon, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        top.addView(ringBox, lp(dp(72), dp(72)));
        LinearLayout tx = new LinearLayout(a);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(16), 0, dp(8), 0);
        tx.addView(text("LAYAR DECIMAL", TXT3, 11, true));
        statusTitle = text("", TXT, 28, true);
        statusSub = text("", TXT2, 13, false);
        tx.addView(statusTitle);
        tx.addView(statusSub);
        top.addView(tx, lpw(0, WRAP, 1f));
        toggle = new Switch(a);
        tintSwitch(toggle);
        top.addView(toggle);
        statusCard.addView(top);
        TextView hint = text("Layar pengisian dengan persen desimal terbuka sendiri saat charger dicolok.", TXT3, 12, false);
        hint.setPadding(0, dp(12), 0, 0);
        statusCard.addView(hint);
        root.addView(statusCard);

        toggleListener = (v, on) -> {
            a.prefs.edit().putBoolean("auto", on).apply();
            Intent i = new Intent(a, ChargeService.class);
            if (on) a.startForegroundService(i); else a.stopService(i);
            a.handler.postDelayed(a::refresh, 500);
        };

        // Notifikasi dan izin
        LinearLayout perm = card();
        LinearLayout ph = header(IconView.BELL, "Notifikasi & izin");
        permChip = text("", GRAY, 12, true);
        permChip.setPadding(dp(10), dp(3), dp(10), dp(3));
        ph.addView(permChip);
        perm.addView(ph);
        String[] pn = {"Notifikasi", "Tampil di atas aplikasi lain", "Hemat baterai: tanpa batas"};
        for (int i = 0; i < 3; i++) perm.addView(permRow(i, pn[i]));
        perm.addView(button(IconView.GEAR, "Buka Pengaturan", true, v -> a.selectNav(2)));
        root.addView(perm);

        // Suhu dan baterai
        LinearLayout sb = card();
        sb.addView(header(IconView.CHART, "Suhu & Baterai"));
        LinearLayout vals = new LinearLayout(a);
        vals.setOrientation(LinearLayout.HORIZONTAL);
        vals.setPadding(0, dp(14), 0, 0);
        LinearLayout v1 = new LinearLayout(a);
        v1.setOrientation(LinearLayout.HORIZONTAL);
        v1.setGravity(Gravity.CENTER_VERTICAL);
        v1.addView(new IconView(a, IconView.THERMO, ICE), lp(dp(30), dp(30)));
        LinearLayout v1t = new LinearLayout(a);
        v1t.setOrientation(LinearLayout.VERTICAL);
        v1t.setPadding(dp(10), 0, 0, 0);
        hTempNow = text("-", TXT, 24, true);
        v1t.addView(hTempNow);
        v1t.addView(text("Suhu", TXT3, 12, false));
        v1.addView(v1t);
        LinearLayout v2 = new LinearLayout(a);
        v2.setOrientation(LinearLayout.HORIZONTAL);
        v2.setGravity(Gravity.CENTER_VERTICAL);
        v2.addView(new IconView(a, IconView.BATTERY, B_GREEN), lp(dp(30), dp(30)));
        LinearLayout v2t = new LinearLayout(a);
        v2t.setOrientation(LinearLayout.VERTICAL);
        v2t.setPadding(dp(10), 0, 0, 0);
        hBatNow = text("-", TXT, 24, true);
        v2t.addView(hBatNow);
        v2t.addView(text("Baterai", TXT3, 12, false));
        v2.addView(v2t);
        vals.addView(v1, lpw(0, WRAP, 1f));
        vals.addView(v2, lpw(0, WRAP, 1f));
        sb.addView(vals);
        sb.addView(periodRow(pHome));
        chartHome = new TempChartView(a);
        LinearLayout.LayoutParams clp = lp(MATCH, dp(220));
        clp.topMargin = dp(10);
        sb.addView(chartHome, clp);
        TextView tap = text("Sentuh dan geser jari di grafik untuk detail.", TXT3, 11, false);
        tap.setPadding(0, dp(4), 0, 0);
        sb.addView(tap);
        sb.addView(button(IconView.CHART, "Lihat riwayat lengkap", false, v -> a.selectNav(1)));
        root.addView(sb);
    }

    private View permRow(int i, String name) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(12), 0, 0);
        FrameLayout dotBox = new FrameLayout(a);
        permDot[i] = new GradientDrawable();
        permDot[i].setShape(GradientDrawable.OVAL);
        dotBox.setBackground(permDot[i]);
        permTick[i] = new IconView(a, IconView.CHECK, TXT);
        dotBox.addView(permTick[i], new FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER));
        r.addView(dotBox, lp(dp(24), dp(24)));
        TextView t = text(name, TXT, 15, false);
        t.setPadding(dp(14), 0, 0, 0);
        r.addView(t);
        return r;
    }

    private void setPerm(int i, boolean ok) {
        permDot[i].setColor(ok ? BLUE : 0x00000000);
        permDot[i].setStroke(dp(2), ok ? BLUE : RED);
        permTick[i].setVisibility(ok ? View.VISIBLE : View.INVISIBLE);
    }

    // ===================== RUANG DNS =====================
    private void buildDnsRoom(LinearLayout root) {
        dnsHero = card();
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout gl = new FrameLayout(a);
        gl.setBackground(gradBox(new int[]{0xFF1E4FB8, 0xFF143A8C}, BLUE, 26));
        gl.addView(new IconView(a, IconView.GLOBE, TXT), new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        top.addView(gl, lp(dp(52), dp(52)));
        LinearLayout tx = new LinearLayout(a);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, dp(8), 0);
        tx.addView(text("Bubble DNS", TXT, 20, true));
        dnsSub = text("", TXT2, 13, false);
        tx.addView(dnsSub);
        top.addView(tx, lpw(0, WRAP, 1f));
        bubbleSwitch = new Switch(a);
        tintSwitch(bubbleSwitch);
        top.addView(bubbleSwitch);
        dnsHero.addView(top);
        dnsMode = text("", TXT, 14, true);
        dnsMode.setPadding(0, dp(12), 0, 0);
        dnsHero.addView(dnsMode);
        TextView dh = text("Nyalakan saklar untuk menampilkan bubble di atas semua aplikasi. Ketuk bubble: ON = DNS pribadi (hostname di bawah), ketuk lagi = DNS Otomatis. Geser bubble untuk memindahkan.", TXT3, 12, false);
        dh.setPadding(0, dp(6), 0, 0);
        dnsHero.addView(dh);
        root.addView(dnsHero);

        bubbleListener = (v, on) -> {
            if (on) {
                if (!Settings.canDrawOverlays(a)) {
                    a.open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.getPackageName())));
                    a.handler.postDelayed(a::refresh, 500);
                    return;
                }
                a.prefs.edit().putBoolean("bubble", true).apply();
                a.startForegroundService(new Intent(a, DnsBubbleService.class));
            } else {
                a.prefs.edit().putBoolean("bubble", false).apply();
                a.stopService(new Intent(a, DnsBubbleService.class));
            }
            a.handler.postDelayed(a::refresh, 600);
        };

        LinearLayout dns = card();
        dns.addView(text("Pengaturan DNS", TXT, 16, true));
        LinearLayout field = new LinearLayout(a);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setBackground(gradBox(new int[]{0xFF0C1430, 0xFF0C1430}, STROKE, 14));
        field.setPadding(dp(14), 0, dp(10), 0);
        field.addView(new IconView(a, IconView.GLOBE, TXT3), lp(dp(22), dp(22)));
        EditText et = new EditText(a);
        et.setHint("Hostname DNS, contoh: dns.google");
        et.setHintTextColor(TXT3);
        et.setTextColor(TXT);
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        et.setBackground(null);
        et.setPadding(dp(12), dp(14), dp(8), dp(14));
        et.setText(DnsHelper.host(a));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int cn, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int bf, int cn) { }
            @Override public void afterTextChanged(Editable s) { DnsHelper.saveHost(a, s.toString()); }
        });
        field.addView(et, lpw(0, WRAP, 1f));
        LinearLayout.LayoutParams flp = lp(MATCH, dp(52));
        flp.topMargin = dp(12);
        dns.addView(field, flp);

        LinearLayout mr = new LinearLayout(a);
        mr.setOrientation(LinearLayout.HORIZONTAL);
        mr.setGravity(Gravity.CENTER_VERTICAL);
        mr.setPadding(0, dp(16), 0, 0);
        LinearLayout mt = new LinearLayout(a);
        mt.setOrientation(LinearLayout.VERTICAL);
        mt.addView(text("Mode DNS", TXT, 15, true));
        mt.addView(text("Ketuk bubble untuk berganti Pribadi dan Otomatis.", TXT3, 12, false));
        mr.addView(mt, lpw(0, WRAP, 1f));
        dnsModeChip = text("", GRAY, 12, true);
        dnsModeChip.setPadding(dp(10), dp(3), dp(10), dp(3));
        mr.addView(dnsModeChip);
        dns.addView(mr);

        rowDns = row(dns, "Izin ubah DNS", "Diberikan sekali lewat ADB.", "Salin Perintah ADB", IconView.TERMINAL, true, v -> a.copyAdb());
        dns.addView(button(IconView.DOC, "Panduan Memasang Izin DNS (Lengkap)", false, v -> a.showDnsGuide()));
        root.addView(dns);

        // Kuota hari ini
        LinearLayout quota = card();
        quota.addView(header(IconView.CHART, "Kuota Hari Ini"));
        quotaText = text("", TXT, 16, true);
        quotaText.setPadding(0, dp(12), 0, 0);
        quota.addView(quotaText);
        LinearLayout ql = new LinearLayout(a);
        ql.setOrientation(LinearLayout.HORIZONTAL);
        ql.setGravity(Gravity.CENTER_VERTICAL);
        ql.setPadding(0, dp(14), 0, 0);
        ql.addView(text("Tampilkan di panel notifikasi", TXT, 15, true), lpw(0, WRAP, 1f));
        quotaSwitch = new Switch(a);
        tintSwitch(quotaSwitch);
        ql.addView(quotaSwitch);
        quota.addView(ql);
        quotaListener = (v, on) -> {
            if (on) {
                if (!QuotaHelper.hasAccess(a)) {
                    a.openUsageAccess();
                    a.handler.postDelayed(a::refresh, 500);
                    return;
                }
                a.prefs.edit().putBoolean("quota", true).apply();
                a.startForegroundService(new Intent(a, QuotaService.class));
            } else {
                a.prefs.edit().putBoolean("quota", false).apply();
                a.stopService(new Intent(a, QuotaService.class));
            }
            a.handler.postDelayed(a::refresh, 600);
        };
        rowQuota = row(quota, "Akses penggunaan", "Izinkan akses agar aplikasi bisa membaca pemakaian data.", "Atur", -1, false, v -> a.openUsageAccess());
        TextView qn = text("Angka dihitung oleh HP sejak 00:00 dan bisa berbeda sedikit dari hitungan provider. Data seluler dari semua SIM digabung.", TXT3, 11, false);
        qn.setPadding(0, dp(10), 0, 0);
        quota.addView(qn);
        root.addView(quota);

        LinearLayout note = card();
        note.addView(header(IconView.SHIELD, "Catatan"));
        TextView nt = text("Aplikasi ini tidak mengubah kuota. Aplikasi hanya memantau pemakaian data dan mengganti mode DNS pribadi ketika Anda mengetuk bubble.", TXT2, 12, false);
        nt.setPadding(0, dp(8), 0, 0);
        note.addView(nt);
        root.addView(note);
    }

    // ===================== PEMBARUAN =====================
    void refresh(boolean on, boolean[] perm) {
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(on);
        toggle.setOnCheckedChangeListener(toggleListener);
        int c = on ? GREEN : RED;
        statusCard.setBackground(gradBox(on ? new int[]{0xFF0C2A2E, 0xFF0A1830} : new int[]{0xFF2A1220, 0xFF150C1A}, c, 20));
        ringDrawable.setStroke(dp(5), c);
        ringIcon.setColor(c);
        statusTitle.setText(on ? "AKTIF" : "MATI");
        statusTitle.setTextColor(c);
        statusSub.setText(on ? "Layanan berjalan" : (a.prefs.getBoolean("auto", true)
            ? "Layanan tidak berjalan (HP restart?). Nyalakan dengan saklar." : "Dimatikan oleh Anda"));

        setPerm(0, perm[0]);
        setPerm(1, perm[1]);
        setPerm(2, perm[2]);
        boolean all = perm[0] && perm[1] && perm[2];
        chip(permChip, all ? "Aktif" : "Perlu diatur", all ? GREEN : RED);

        refreshDns();
        refreshQuota();
        refreshTabs(on);
    }

    void setNow(double tv, BatteryReader r) {
        hTempNow.setText(Double.isNaN(tv) ? "-" : f1(tv) + " °C");
        hTempNow.setTextColor(Double.isNaN(tv) ? TXT : (tv >= 45.0 ? FIRE : ICE));
        double pct = r.whole < 0 ? -1 : r.whole + (r.hundredths >= 0 ? r.hundredths / 100.0 : 0);
        String bt = r.whole < 0 ? "-" : (r.hundredths >= 0
            ? String.format(idl, "%d,%d%%", r.whole, r.hundredths / 10) : r.whole + "%");
        hBatNow.setText(bt);
        hBatNow.setTextColor(r.charging ? B_GREEN : (pct < 20.0 ? B_ORANGE : B_BLUE));
    }

    private CharSequence tabLabel(String label, int dot) {
        SpannableStringBuilder sb = new SpannableStringBuilder("● ");
        sb.setSpan(new ForegroundColorSpan(dot), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(label);
        return sb;
    }

    private void refreshTabs(boolean decimalOn) {
        tabDecimal.setText(tabLabel("Decimal", decimalOn ? GREEN : RED));
        String m = DnsHelper.mode(a);
        int dd = "hostname".equals(m) ? GREEN : (DnsBubbleService.running ? B_BLUE : GRAY);
        tabDns.setText(tabLabel("DNS", dd));
    }

    private void refreshDns() {
        bubbleSwitch.setOnCheckedChangeListener(null);
        bubbleSwitch.setChecked(DnsBubbleService.running);
        bubbleSwitch.setOnCheckedChangeListener(bubbleListener);
        String m = DnsHelper.mode(a);
        String s;
        int col;
        if ("hostname".equals(m)) {
            String sp = DnsHelper.spec(a);
            s = "Pribadi" + (sp.isEmpty() ? "" : " · " + sp);
            col = GREEN;
        } else if ("off".equals(m)) {
            s = "Mati";
            col = GRAY;
        } else {
            s = "Otomatis";
            col = B_BLUE;
        }
        dnsMode.setText("DNS saat ini: " + s);
        dnsMode.setTextColor(col);
        dnsSub.setText(DnsBubbleService.running ? "Bubble tampil di layar" : "Bubble mati");
        chip(dnsModeChip, s, col);
        dnsHero.setBackground(gradBox(new int[]{CARD_A, CARD_B}, col, 20));
        state(rowDns, DnsHelper.canWrite(a));
    }

    private void refreshQuota() {
        quotaSwitch.setOnCheckedChangeListener(null);
        quotaSwitch.setChecked(QuotaService.running);
        quotaSwitch.setOnCheckedChangeListener(quotaListener);
        boolean has = QuotaHelper.hasAccess(a);
        state(rowQuota, has);
        if (has) {
            quotaText.setText("Seluler: " + QuotaHelper.fmt(QuotaHelper.mobileToday(a))
                + "   Wi-Fi: " + QuotaHelper.fmt(QuotaHelper.wifiToday(a)));
        } else {
            quotaText.setText("Aktifkan izin Akses penggunaan dulu");
        }
    }
}
