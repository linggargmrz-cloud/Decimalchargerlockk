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
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int CARD_A = 0xFF0F1833, CARD_B = 0xFF0A1128, STROKE = 0xFF1C2B58;
    private static final int BLUE = 0xFF2D7BFF, BLUE2 = 0xFF1450D8, ACCENT = 0xFF4D9BFF;
    private static final int TXT = 0xFFFFFFFF, TXT2 = 0xFFA9B8D8, TXT3 = 0xFF6B7AA0;
    private static final int GREEN = 0xFF2ED573, RED = 0xFFFF5252, YELLOW = 0xFFFFC107, GRAY = 0xFF7D8890;
    private static final int ICE = 0xFF7FDBFF, FIRE = 0xFFFF4D3D;
    private static final int B_GREEN = 0xFF3DDC84, B_ORANGE = 0xFFFFA726, B_BLUE = 0xFF3D8BFF;
    private static final double DESIGN_MAH = 5000.0;
    private static final long[] SPANS = {3600000L, 21600000L, 86400000L, 604800000L};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 2000); }
    };

    private SharedPreferences prefs;
    private final Locale idl = new Locale("id", "ID");

    // Struktur halaman
    private FrameLayout content;
    private final View[] pages = new View[4];
    private final LinearLayout[] navItems = new LinearLayout[4];
    private final IconView[] navIcons = new IconView[4];
    private final TextView[] navLabels = new TextView[4];
    private TwoPager pager;
    private FrameLayout tabs;
    private View indicator;
    private TextView tabDecimal, tabDns;

    // Beranda: ruang Decimal
    private LinearLayout statusCard;
    private GradientDrawable ringDrawable;
    private IconView ringIcon;
    private TextView statusTitle, statusSub, permChip, hTempNow, hBatNow;
    private Switch toggle;
    private CompoundButton.OnCheckedChangeListener toggleListener;
    private final GradientDrawable[] permDot = new GradientDrawable[3];
    private final IconView[] permTick = new IconView[3];
    private TempChartView chartHome, chartHist;
    private final TextView[] pHome = new TextView[4];
    private final TextView[] pHist = new TextView[4];
    private int period = 1;
    private int lastRev = -1, lastPeriod = -1;
    private long lastLoad = 0;
    private final SimpleDateFormat fmtHm = new SimpleDateFormat("HH.mm", new Locale("id", "ID"));
    private final SimpleDateFormat fmtDm = new SimpleDateFormat("d MMM HH.mm", new Locale("id", "ID"));

    // Beranda: ruang DNS
    private LinearLayout dnsHero;
    private TextView dnsMode, dnsSub, dnsModeChip, quotaText;
    private Switch bubbleSwitch, quotaSwitch;
    private CompoundButton.OnCheckedChangeListener bubbleListener, quotaListener;
    private Row rowDns, rowQuota;

    // Riwayat
    private TextView tTrend, tStats, tRises, tAvgT, tAvgB, tEndure, tCondSub, chipCond;
    private TempBar tempBar;
    private TextView hRemain, hStatus, hTemp, hVolt, hTech, hCycle, hDesign, hCap, hHealth, hHint;

    // Pengaturan
    private Row rowNotif, rowOverlay, rowBattery;
    private int notifAsked = 0;
    private int nav = 0;

    private static class Row { TextView chip; View btn; }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF050A18);
        getWindow().setNavigationBarColor(0xFF091024);
        prefs = getSharedPreferences("app", MODE_PRIVATE);
        period = prefs.getInt("tperiod2", 1);
        if (period < 0 || period > 3) period = 1;

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xFF050A18, 0xFF081634}));
        main.addView(buildHeader());
        content = new FrameLayout(this);
        main.addView(content, lpw(MATCH, 0, 1f));
        main.addView(buildNav());

        pages[0] = buildHome();
        pages[1] = buildHistory();
        pages[2] = buildSettings();
        pages[3] = buildAbout();
        for (int i = 0; i < 4; i++) content.addView(pages[i], new FrameLayout.LayoutParams(MATCH, MATCH));
        styleTabs();
        selectNav(0);
        setContentView(main);
    }

    @Override public void onBackPressed() {
        if (nav != 0) selectNav(0);
        else if (pager != null && pager.getPage() == 1) pager.setPage(0, true);
        else super.onBackPressed();
    }

    @Override protected void onResume() { super.onResume(); handler.post(ticker); }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }

    // ===================== KERANGKA =====================
    private String versionString() {
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
        h.setPadding(dp(20), dp(30), dp(16), dp(10));
        h.addView(new IconView(this, IconView.SHIELD, BLUE), lp(dp(40), dp(40)));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(12), 0, 0, 0);
        t.addView(text("Decimal Charge Lock", TXT, 20, true));
        t.addView(text(versionString(), TXT3, 12, false));
        h.addView(t, lpw(0, WRAP, 1f));
        IconView gear = new IconView(this, IconView.GEAR, TXT2);
        gear.setOnClickListener(v -> selectNav(2));
        h.addView(gear, lp(dp(30), dp(30)));
        return h;
    }

    private View buildNav() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(dp(10), dp(8), dp(10), dp(10));
        bar.setBackground(gradBox(new int[]{0xFF0C1530, 0xFF091024}, 0, 0));
        int[] icons = {IconView.HOME, IconView.CHART, IconView.GEAR, IconView.INFO};
        String[] names = {"Beranda", "Riwayat", "Pengaturan", "Tentang"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout it = new LinearLayout(this);
            it.setOrientation(LinearLayout.VERTICAL);
            it.setGravity(Gravity.CENTER);
            it.setPadding(0, dp(6), 0, dp(6));
            IconView ic = new IconView(this, icons[i], TXT3);
            it.addView(ic, lp(dp(24), dp(24)));
            TextView lb = text(names[i], TXT3, 11, true);
            lb.setGravity(Gravity.CENTER);
            it.addView(lb);
            it.setOnClickListener(v -> selectNav(idx));
            LinearLayout.LayoutParams nlp = lpw(0, WRAP, 1f);
            nlp.setMargins(dp(3), 0, dp(3), 0);
            bar.addView(it, nlp);
            navItems[i] = it;
            navIcons[i] = ic;
            navLabels[i] = lb;
        }
        return bar;
    }

    private void selectNav(int i) {
        nav = i;
        for (int k = 0; k < 4; k++) {
            boolean sel = k == i;
            if (pages[k] != null) pages[k].setVisibility(sel ? View.VISIBLE : View.GONE);
            navItems[k].setBackground(sel ? gradBox(new int[]{0xFF1E4FB8, 0xFF143A8C}, BLUE, 16) : null);
            navIcons[k].setColor(sel ? TXT : TXT3);
            navLabels[k].setTextColor(sel ? TXT : TXT3);
        }
    }

    private ScrollView pageScroll() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        return sv;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(6), dp(18), dp(24));
        return l;
    }

    // ===================== BERANDA =====================
    private View buildHome() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        tabs = new FrameLayout(this);
        tabs.setBackground(gradBox(new int[]{0xFF0C1530, 0xFF0A1128}, STROKE, 26));
        indicator = new View(this);
        indicator.setBackground(gradBox(new int[]{BLUE, BLUE2}, 0xFF7DB2FF, 22));
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(dp(10), MATCH);
        ilp.setMargins(0, dp(4), 0, dp(4));
        tabs.addView(indicator, ilp);
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabDecimal = tab("Decimal", 0);
        tabDns = tab("DNS", 1);
        tabRow.addView(tabDecimal, lpw(0, MATCH, 1f));
        tabRow.addView(tabDns, lpw(0, MATCH, 1f));
        tabs.addView(tabRow, new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout.LayoutParams tlp = lp(MATCH, dp(48));
        tlp.setMargins(dp(18), dp(4), dp(18), dp(10));
        col.addView(tabs, tlp);

        pager = new TwoPager(this);
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
            @Override public void onSelected(int page) { prefs.edit().putInt("page", page).apply(); }
        });
        tabs.addOnLayoutChangeListener((v, l, tp, r, bt, ol, ot, orr, ob) -> updateIndicator(pager.getPos()));
        pager.setPage(prefs.getInt("page", 0) == 1 ? 1 : 0, false);
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

    private void buildDecimalRoom(LinearLayout root) {
        // Kartu utama: layar decimal
        statusCard = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout ringBox = new FrameLayout(this);
        ringDrawable = new GradientDrawable();
        ringDrawable.setShape(GradientDrawable.OVAL);
        ringDrawable.setColor(0xFF0A1530);
        ringDrawable.setStroke(dp(5), GREEN);
        ringBox.setBackground(ringDrawable);
        ringIcon = new IconView(this, IconView.BATTERY, GREEN);
        ringBox.addView(ringIcon, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        top.addView(ringBox, lp(dp(72), dp(72)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(16), 0, dp(8), 0);
        tx.addView(text("LAYAR DECIMAL", TXT3, 11, true));
        statusTitle = text("", TXT, 28, true);
        statusSub = text("", TXT2, 13, false);
        tx.addView(statusTitle);
        tx.addView(statusSub);
        top.addView(tx, lpw(0, WRAP, 1f));
        toggle = new Switch(this);
        tintSwitch(toggle);
        top.addView(toggle);
        statusCard.addView(top);
        TextView hint = text("Layar pengisian dengan persen desimal terbuka sendiri saat charger dicolok.", TXT3, 12, false);
        hint.setPadding(0, dp(12), 0, 0);
        statusCard.addView(hint);
        root.addView(statusCard);

        toggleListener = (v, on) -> {
            prefs.edit().putBoolean("auto", on).apply();
            Intent i = new Intent(this, ChargeService.class);
            if (on) startForegroundService(i); else stopService(i);
            handler.postDelayed(this::refresh, 500);
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
        perm.addView(button(IconView.GEAR, "Buka Pengaturan", true, v -> selectNav(2)));
        root.addView(perm);

        // Suhu dan baterai
        LinearLayout sb = card();
        sb.addView(header(IconView.CHART, "Suhu & Baterai"));
        LinearLayout vals = new LinearLayout(this);
        vals.setOrientation(LinearLayout.HORIZONTAL);
        vals.setPadding(0, dp(14), 0, 0);
        LinearLayout v1 = new LinearLayout(this);
        v1.setOrientation(LinearLayout.HORIZONTAL);
        v1.setGravity(Gravity.CENTER_VERTICAL);
        v1.addView(new IconView(this, IconView.THERMO, ICE), lp(dp(30), dp(30)));
        LinearLayout v1t = new LinearLayout(this);
        v1t.setOrientation(LinearLayout.VERTICAL);
        v1t.setPadding(dp(10), 0, 0, 0);
        hTempNow = text("-", TXT, 24, true);
        v1t.addView(hTempNow);
        v1t.addView(text("Suhu", TXT3, 12, false));
        v1.addView(v1t);
        LinearLayout v2 = new LinearLayout(this);
        v2.setOrientation(LinearLayout.HORIZONTAL);
        v2.setGravity(Gravity.CENTER_VERTICAL);
        v2.addView(new IconView(this, IconView.BATTERY, B_GREEN), lp(dp(30), dp(30)));
        LinearLayout v2t = new LinearLayout(this);
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
        chartHome = new TempChartView(this);
        LinearLayout.LayoutParams clp = lp(MATCH, dp(220));
        clp.topMargin = dp(10);
        sb.addView(chartHome, clp);
        TextView tap = text("Sentuh dan geser jari di grafik untuk detail.", TXT3, 11, false);
        tap.setPadding(0, dp(4), 0, 0);
        sb.addView(tap);
        sb.addView(button(IconView.CHART, "Lihat riwayat lengkap", false, v -> selectNav(1)));
        root.addView(sb);
    }

    private View permRow(int i, String name) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(12), 0, 0);
        FrameLayout dotBox = new FrameLayout(this);
        permDot[i] = new GradientDrawable();
        permDot[i].setShape(GradientDrawable.OVAL);
        dotBox.setBackground(permDot[i]);
        permTick[i] = new IconView(this, IconView.CHECK, TXT);
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

    private void buildDnsRoom(LinearLayout root) {
        // Kartu utama: bubble DNS
        dnsHero = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout gl = new FrameLayout(this);
        gl.setBackground(gradBox(new int[]{0xFF1E4FB8, 0xFF143A8C}, BLUE, 26));
        gl.addView(new IconView(this, IconView.GLOBE, TXT), new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        top.addView(gl, lp(dp(52), dp(52)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, dp(8), 0);
        tx.addView(text("Bubble DNS", TXT, 20, true));
        dnsSub = text("", TXT2, 13, false);
        tx.addView(dnsSub);
        top.addView(tx, lpw(0, WRAP, 1f));
        bubbleSwitch = new Switch(this);
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
                if (!Settings.canDrawOverlays(this)) {
                    open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                    handler.postDelayed(this::refresh, 500);
                    return;
                }
                prefs.edit().putBoolean("bubble", true).apply();
                startForegroundService(new Intent(this, DnsBubbleService.class));
            } else {
                prefs.edit().putBoolean("bubble", false).apply();
                stopService(new Intent(this, DnsBubbleService.class));
            }
            handler.postDelayed(this::refresh, 600);
        };

        // Pengaturan DNS
        LinearLayout dns = card();
        dns.addView(text("Pengaturan DNS", TXT, 16, true));
        LinearLayout field = new LinearLayout(this);
        field.setOrientation(LinearLayout.HORIZONTAL);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setBackground(gradBox(new int[]{0xFF0C1430, 0xFF0C1430}, STROKE, 14));
        field.setPadding(dp(14), 0, dp(10), 0);
        field.addView(new IconView(this, IconView.GLOBE, TXT3), lp(dp(22), dp(22)));
        EditText et = new EditText(this);
        et.setHint("Hostname DNS, contoh: dns.google");
        et.setHintTextColor(TXT3);
        et.setTextColor(TXT);
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        et.setBackground(null);
        et.setPadding(dp(12), dp(14), dp(8), dp(14));
        et.setText(DnsHelper.host(this));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int cn, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int bf, int cn) { }
            @Override public void afterTextChanged(Editable s) { DnsHelper.saveHost(MainActivity.this, s.toString()); }
        });
        field.addView(et, lpw(0, WRAP, 1f));
        LinearLayout.LayoutParams flp = lp(MATCH, dp(52));
        flp.topMargin = dp(12);
        dns.addView(field, flp);

        LinearLayout mr = new LinearLayout(this);
        mr.setOrientation(LinearLayout.HORIZONTAL);
        mr.setGravity(Gravity.CENTER_VERTICAL);
        mr.setPadding(0, dp(16), 0, 0);
        LinearLayout mt = new LinearLayout(this);
        mt.setOrientation(LinearLayout.VERTICAL);
        mt.addView(text("Mode DNS", TXT, 15, true));
        mt.addView(text("Ketuk bubble untuk berganti Pribadi dan Otomatis.", TXT3, 12, false));
        mr.addView(mt, lpw(0, WRAP, 1f));
        dnsModeChip = text("", GRAY, 12, true);
        dnsModeChip.setPadding(dp(10), dp(3), dp(10), dp(3));
        mr.addView(dnsModeChip);
        dns.addView(mr);

        rowDns = row(dns, "Izin ubah DNS", "Diberikan sekali lewat ADB.", "Salin Perintah ADB", IconView.TERMINAL, true, v -> copyAdb());
        dns.addView(button(IconView.DOC, "Panduan Memasang Izin DNS (Lengkap)", false, v -> showDnsGuide()));
        root.addView(dns);

        // Kuota hari ini
        LinearLayout quota = card();
        quota.addView(header(IconView.CHART, "Kuota Hari Ini"));
        quotaText = text("", TXT, 16, true);
        quotaText.setPadding(0, dp(12), 0, 0);
        quota.addView(quotaText);
        LinearLayout ql = new LinearLayout(this);
        ql.setOrientation(LinearLayout.HORIZONTAL);
        ql.setGravity(Gravity.CENTER_VERTICAL);
        ql.setPadding(0, dp(14), 0, 0);
        ql.addView(text("Tampilkan di panel notifikasi", TXT, 15, true), lpw(0, WRAP, 1f));
        quotaSwitch = new Switch(this);
        tintSwitch(quotaSwitch);
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
        rowQuota = row(quota, "Akses penggunaan", "Izinkan akses agar aplikasi bisa membaca pemakaian data.", "Atur", -1, false, v -> openUsageAccess());
        TextView qn = text("Angka dihitung oleh HP sejak 00:00 dan bisa berbeda sedikit dari hitungan provider. Data seluler dari semua SIM digabung.", TXT3, 11, false);
        qn.setPadding(0, dp(10), 0, 0);
        quota.addView(qn);
        root.addView(quota);

        // Catatan
        LinearLayout note = card();
        note.addView(header(IconView.SHIELD, "Catatan"));
        TextView nt = text("Aplikasi ini tidak mengubah kuota. Aplikasi hanya memantau pemakaian data dan mengganti mode DNS pribadi ketika Anda mengetuk bubble.", TXT2, 12, false);
        nt.setPadding(0, dp(8), 0, 0);
        note.addView(nt);
        root.addView(note);
    }

    // ===================== RIWAYAT =====================
    private View buildHistory() {
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
        chartHist = new TempChartView(this);
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
        LinearLayout er = new LinearLayout(this);
        er.setOrientation(LinearLayout.HORIZONTAL);
        er.setGravity(Gravity.CENTER_VERTICAL);
        er.setPadding(0, dp(14), 0, 0);
        er.addView(new IconView(this, IconView.CLOCK, ACCENT), lp(dp(20), dp(20)));
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
        tempBar = new TempBar(this);
        LinearLayout.LayoutParams blp = lp(MATCH, dp(34));
        blp.topMargin = dp(14);
        c.addView(tempBar, blp);
        LinearLayout sc = new LinearLayout(this);
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

    // ===================== PENGATURAN =====================
    private View buildSettings() {
        ScrollView sv = pageScroll();
        LinearLayout root = column();
        sv.addView(root);

        LinearLayout perm = card();
        perm.addView(header(IconView.BELL, "Izin"));
        rowNotif = row(perm, "Notifikasi", "Dibutuhkan agar layanan tampil dan berjalan stabil.", "Atur", -1, false, v -> askNotif());
        rowOverlay = row(perm, "Tampil di atas aplikasi lain", "Agar layar pengisian bisa terbuka sendiri.", "Atur", -1, false, v ->
            open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        rowBattery = row(perm, "Hemat baterai: tanpa batas", "Agar MIUI tidak mematikan layanan.", "Atur", -1, false, v -> {
            try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
            catch (Exception e) { openAppInfo(); }
        });
        Row miui = row(perm, "MIUI: layar kunci dan pop-up", "Aktifkan \"Tampilkan di layar kunci\", \"Tampilkan jendela pop-up saat berjalan di latar belakang\", dan Autostart.", "Buka Pengaturan Aplikasi", IconView.GEAR, false, v -> openAppInfo());
        chip(miui.chip, "Cek manual", GRAY);
        root.addView(perm);

        LinearLayout look = card();
        look.addView(header(IconView.BATTERY, "Tampilan"));
        look.addView(button(IconView.BATTERY, "Pratinjau Tampilan", true, v -> startActivity(new Intent(this, PreviewActivity.class))));
        look.addView(button(IconView.INFO, "Screensaver (Cadangan)", false, v -> {
            try { startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS)); }
            catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("Buka Setelan Android lalu cari 'Screensaver'.")
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
            new AlertDialog.Builder(this)
                .setMessage("Hapus semua riwayat suhu dan baterai?")
                .setPositiveButton("Hapus", (dd, w) -> { TempLog.clear(this); loadTemp(true); })
                .setNegativeButton("Batal", null)
                .show()));
        root.addView(data);
        return sv;
    }

    // ===================== TENTANG =====================
    private View buildAbout() {
        ScrollView sv = pageScroll();
        LinearLayout root = column();
        sv.addView(root);

        LinearLayout a = card();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(new IconView(this, IconView.SHIELD, BLUE), lp(dp(46), dp(46)));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(14), 0, 0, 0);
        t.addView(text("Decimal Charge Lock", TXT, 18, true));
        t.addView(text(versionString(), TXT3, 12, false));
        top.addView(t);
        a.addView(top);
        TextView d = text("Layar pengisian dengan persen baterai desimal, pemantau suhu dan baterai, serta pengganti DNS pribadi lewat bubble.", TXT2, 13, false);
        d.setPadding(0, dp(12), 0, 0);
        a.addView(d);
        root.addView(a);

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
        g.addView(button(IconView.DOC, "Panduan Memasang Izin DNS (Lengkap)", false, v -> showDnsGuide()));
        root.addView(g);
        return sv;
    }

    // ===================== PEMBARUAN DATA =====================
    private void refresh() {
        boolean on = running();
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(on);
        toggle.setOnCheckedChangeListener(toggleListener);
        int c = on ? GREEN : RED;
        statusCard.setBackground(gradBox(on ? new int[]{0xFF0C2A2E, 0xFF0A1830} : new int[]{0xFF2A1220, 0xFF150C1A}, c, 20));
        ringDrawable.setStroke(dp(5), c);
        ringIcon.setColor(c);
        statusTitle.setText(on ? "AKTIF" : "MATI");
        statusTitle.setTextColor(c);
        statusSub.setText(on ? "Layanan berjalan" : (prefs.getBoolean("auto", true)
            ? "Layanan tidak berjalan (HP restart?). Nyalakan dengan saklar." : "Dimatikan oleh Anda"));

        boolean n = Build.VERSION.SDK_INT < 33
            || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean o = Settings.canDrawOverlays(this);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean bt = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        setPerm(0, n);
        setPerm(1, o);
        setPerm(2, bt);
        boolean all = n && o && bt;
        chip(permChip, all ? "Aktif" : "Perlu diatur", all ? GREEN : RED);
        state(rowNotif, n);
        state(rowOverlay, o);
        state(rowBattery, bt);

        refreshDns();
        refreshQuota();
        refreshTemp();
        refreshHealth();
        refreshTabs(on);
    }

    private CharSequence tabLabel(String label, int dot) {
        SpannableStringBuilder sb = new SpannableStringBuilder("● ");
        sb.setSpan(new ForegroundColorSpan(dot), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(label);
        return sb;
    }

    private void refreshTabs(boolean decimalOn) {
        tabDecimal.setText(tabLabel("Decimal", decimalOn ? GREEN : RED));
        String m = DnsHelper.mode(this);
        int dd = "hostname".equals(m) ? GREEN : (DnsBubbleService.running ? B_BLUE : GRAY);
        tabDns.setText(tabLabel("DNS", dd));
    }

    private void refreshDns() {
        bubbleSwitch.setOnCheckedChangeListener(null);
        bubbleSwitch.setChecked(DnsBubbleService.running);
        bubbleSwitch.setOnCheckedChangeListener(bubbleListener);
        String m = DnsHelper.mode(this);
        String s;
        int col;
        if ("hostname".equals(m)) {
            String sp = DnsHelper.spec(this);
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

    private String f1(double v) { return String.format(idl, "%.1f", v); }

    private int batCol(boolean chg, double pct) {
        return chg ? B_GREEN : (pct < 20.0 ? B_ORANGE : B_BLUE);
    }

    private void styleTabs() {
        TextView[][] all = {pHome, pHist};
        for (TextView[] arr : all) {
            for (int k = 0; k < 4; k++) {
                if (arr[k] == null) continue;
                boolean sel = k == period;
                arr[k].setBackground(sel ? gradBox(new int[]{BLUE, BLUE2}, 0xFF7DB2FF, 12)
                    : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 12));
                arr[k].setTextColor(sel ? TXT : TXT2);
            }
        }
    }

    private LinearLayout periodRow(final TextView[] store) {
        LinearLayout pr = new LinearLayout(this);
        pr.setOrientation(LinearLayout.HORIZONTAL);
        pr.setPadding(0, dp(12), 0, 0);
        String[] names = {"1 jam", "6 jam", "24 jam", "7 hari"};
        for (int k = 0; k < 4; k++) {
            final int kk = k;
            TextView pb = text(names[k], TXT2, 12, true);
            pb.setGravity(Gravity.CENTER);
            pb.setOnClickListener(v -> {
                period = kk;
                prefs.edit().putInt("tperiod2", kk).apply();
                styleTabs();
                loadTemp(true);
            });
            LinearLayout.LayoutParams plp = lpw(0, dp(36), 1f);
            plp.rightMargin = k < 3 ? dp(6) : 0;
            pr.addView(pb, plp);
            store[k] = pb;
        }
        return pr;
    }

    private void refreshTemp() {
        try {
            BatteryReader r = BatteryReader.read(this);
            Intent bi = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int tm = bi == null ? Integer.MIN_VALUE : bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            double tv = tm == Integer.MIN_VALUE ? Double.NaN : tm / 10.0;
            hTempNow.setText(Double.isNaN(tv) ? "-" : f1(tv) + " °C");
            hTempNow.setTextColor(Double.isNaN(tv) ? TXT : (tv >= 45.0 ? FIRE : ICE));
            double pct = r.whole < 0 ? -1 : r.whole + (r.hundredths >= 0 ? r.hundredths / 100.0 : 0);
            String bt = r.whole < 0 ? "-" : (r.hundredths >= 0
                ? String.format(idl, "%d,%d%%", r.whole, r.hundredths / 10) : r.whole + "%");
            hBatNow.setText(bt);
            hBatNow.setTextColor(batCol(r.charging, pct));
            updateCondition(tv);
        } catch (Throwable ignored) { }
        loadTemp(false);
    }

    private void updateCondition(double tv) {
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
        List<TempLog.Sample> l = TempLog.load(this, now - 3600000L);
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

    private void loadTemp(boolean force) {
        long now = System.currentTimeMillis();
        int rev = TempLog.rev;
        if (!force && rev == lastRev && period == lastPeriod && now - lastLoad < 15000) return;
        lastRev = rev;
        lastPeriod = period;
        lastLoad = now;

        // 10 menit terakhir
        List<TempLog.Sample> l10 = TempLog.load(this, now - 600000L);
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

        long from = now - SPANS[period];
        List<TempLog.Sample> list = TempLog.load(this, from);
        chartHome.setData(list, from, now);
        chartHist.setData(list, from, now);

        SimpleDateFormat tf = period >= 2 ? fmtDm : fmtHm;
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
            Intent bi = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
        } catch (Throwable t) {
            return -1;
        }
    }

    // ===================== AKSI =====================
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

    // ===================== PEMBANTU TAMPILAN =====================
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    private LinearLayout.LayoutParams lpw(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h, weight); }

    private GradientDrawable gradBox(int[] colors, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    private TextView text(String s, int color, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(color);
        v.setTextSize(sp);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(16), dp(18), dp(16));
        l.setBackground(gradBox(new int[]{CARD_A, CARD_B}, STROKE, 20));
        LinearLayout.LayoutParams l2 = lp(MATCH, WRAP);
        l2.bottomMargin = dp(12);
        l.setLayoutParams(l2);
        return l;
    }

    private LinearLayout header(int icon, String title) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.addView(new IconView(this, icon, ACCENT), lp(dp(22), dp(22)));
        TextView t = text(title, TXT, 16, true);
        t.setPadding(dp(10), 0, 0, 0);
        h.addView(t, lpw(0, WRAP, 1f));
        return h;
    }

    private LinearLayout button(int icon, String label, boolean primary, View.OnClickListener l) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), 0, dp(14), 0);
        b.setBackground(primary ? gradBox(new int[]{BLUE, BLUE2}, 0xFF5B9BFF, 14)
            : gradBox(new int[]{0xFF141E3E, 0xFF0F1833}, STROKE, 14));
        if (icon >= 0) {
            LinearLayout.LayoutParams ilp = lp(dp(20), dp(20));
            ilp.rightMargin = dp(10);
            b.addView(new IconView(this, icon, TXT), ilp);
        }
        TextView t = text(label, TXT, 14, true);
        t.setGravity(Gravity.CENTER);
        b.addView(t);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams blp = lp(MATCH, dp(48));
        blp.topMargin = dp(10);
        b.setLayoutParams(blp);
        return b;
    }

    private void chip(TextView v, String s, int color) {
        v.setText(s);
        v.setTextColor(color);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor((color & 0x00FFFFFF) | 0x26000000);
        v.setBackground(g);
    }

    private Row row(LinearLayout parent, String title, String desc, String btnText, int icon, boolean primary, View.OnClickListener l) {
        Row r = new Row();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(14), 0, 0);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.addView(text(title, TXT, 15, true), lpw(0, WRAP, 1f));
        r.chip = text("", GRAY, 12, true);
        r.chip.setPadding(dp(10), dp(3), dp(10), dp(3));
        line.addView(r.chip);
        box.addView(line);
        box.addView(text(desc, TXT3, 12, false));
        r.btn = button(icon, btnText, primary, l);
        box.addView(r.btn);
        parent.addView(box);
        return r;
    }

    private void state(Row r, boolean ok) {
        chip(r.chip, ok ? "Aktif" : "Belum", ok ? GREEN : RED);
        r.btn.setVisibility(ok ? View.GONE : View.VISIBLE);
    }

    private TextView kv(LinearLayout parent, String label) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setPadding(0, dp(10), 0, 0);
        line.addView(text(label, TXT2, 13, false), lpw(0, WRAP, 1f));
        TextView v = text("-", TXT, 13, true);
        v.setGravity(Gravity.END);
        line.addView(v);
        parent.addView(line);
        return v;
    }

    private void tintSwitch(Switch sw) {
        int[][] st = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(st, new int[]{0xFFFFFFFF, 0xFFB7C3E0}));
        sw.setTrackTintList(new ColorStateList(st, new int[]{BLUE, 0xFF2A3558}));
    }

    // Batang gradasi suhu dengan penanda posisi suhu saat ini (skala 20 sampai 50 °C).
    private class TempBar extends View {
        private double val = Double.NaN;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rc = new RectF();

        TempBar(Context c) { super(c); }

        void setValue(double v) {
            val = v;
            invalidate();
        }

        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            float bh = dp(10);
            float top = (h - bh) / 2f;
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(0, 0, w, 0,
                new int[]{0xFF2D7BFF, 0xFF22D3EE, 0xFF2ED573, 0xFFFFC107, 0xFFFF9800, 0xFFFF4D3D},
                new float[]{0f, 0.2f, 0.45f, 0.65f, 0.8f, 1f}, Shader.TileMode.CLAMP));
            rc.set(0, top, w, top + bh);
            cv.drawRoundRect(rc, bh / 2f, bh / 2f, p);
            p.setShader(null);
            if (!Double.isNaN(val)) {
                double f = Math.max(0.0, Math.min(1.0, (val - 20.0) / 30.0));
                float x = dp(9) + (float) f * (w - dp(18));
                p.setColor(0xFFFFFFFF);
                cv.drawCircle(x, h / 2f, dp(9), p);
                p.setColor(0xFF0A1128);
                cv.drawCircle(x, h / 2f, dp(5), p);
            }
        }
    }
}
