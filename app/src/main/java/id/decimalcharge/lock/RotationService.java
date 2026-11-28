package id.decimalcharge.lock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Kontrol rotasi: memasang jendela transparan 1 piksel yang meminta orientasi tertentu,
 * sehingga sistem memutar layar walau aplikasi di bawahnya mengunci orientasinya.
 */
public class RotationService extends Service {
    static volatile boolean running = false;
    static final String ACTION_APPLY = "id.decimalcharge.lock.ROT_APPLY";
    static final String ACTION_OFF = "id.decimalcharge.lock.ROT_OFF";
    static final String ACTION_BUBBLE = "id.decimalcharge.lock.ROT_BUBBLE";
    static final int OFF = -2;
    private static final String CH = "rotation";
    private static final int ID_FG = 9;
    private static final int[] BTN_SO = {
        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR, ActivityInfo.SCREEN_ORIENTATION_LOCKED,
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, OFF};
    private static final String[] BTN_TXT = {"AUTO", "KUNCI", "0°", "90°", "180°", "270°", "MATI"};

    private WindowManager wm;
    private View orientView;
    private WindowManager.LayoutParams olp;
    private LinearLayout bubble;
    private WindowManager.LayoutParams blp;
    private LinearLayout panel;
    private final TextView[] btns = new TextView[7];

    // ---- API untuk halaman ----
    static void apply(Context c, int so) {
        c.getSharedPreferences("rot", Context.MODE_PRIVATE).edit().putBoolean("on", true).putInt("so", so).apply();
        c.startForegroundService(new Intent(c, RotationService.class).setAction(ACTION_APPLY).putExtra("so", so));
    }

    static void off(Context c) {
        c.getSharedPreferences("rot", Context.MODE_PRIVATE).edit().putBoolean("on", false).apply();
        c.startForegroundService(new Intent(c, RotationService.class).setAction(ACTION_OFF));
    }

    static void bubble(Context c, boolean show) {
        c.getSharedPreferences("rot", Context.MODE_PRIVATE).edit().putBoolean("bubble", show).apply();
        c.startForegroundService(new Intent(c, RotationService.class).setAction(ACTION_BUBBLE).putExtra("on", show));
    }

    static int current(Context c) {
        SharedPreferences sp = c.getSharedPreferences("rot", Context.MODE_PRIVATE);
        return sp.getBoolean("on", false) ? sp.getInt("so", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) : OFF;
    }

    static String label(int so) {
        switch (so) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT: return "Potret (0°)";
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE: return "Lanskap kiri (90°)";
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT: return "Potret terbalik (180°)";
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE: return "Lanskap kanan (270°)";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT: return "Sensor potret";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE: return "Sensor lanskap";
            case ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR: return "Auto rotasi (semua arah)";
            case ActivityInfo.SCREEN_ORIENTATION_LOCKED: return "Kunci rotasi";
            default: return "Bawaan sistem";
        }
    }

    // ---- siklus layanan ----
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private SharedPreferences prefs() { return getSharedPreferences("rot", Context.MODE_PRIVATE); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH, "Kontrol rotasi", NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        nm.createNotificationChannel(ch);
        startForeground(ID_FG, new Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentTitle("L-CORE: Kontrol rotasi aktif")
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build());
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        running = true;
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sp = prefs();
        String act = intent == null ? null : intent.getAction();
        if (!Settings.canDrawOverlays(this)) {
            toast("Izin tampil di atas aplikasi lain belum aktif");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_OFF.equals(act)) {
            removeOrientation();
            updateBubble();
            if (!sp.getBoolean("bubble", false)) {
                stopSelf();
                return START_NOT_STICKY;
            }
            return START_STICKY;
        }
        if (ACTION_APPLY.equals(act)) {
            setOrientation(intent.getIntExtra("so", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
            updateBubble();
        } else if (ACTION_BUBBLE.equals(act)) {
            if (intent.getBooleanExtra("on", false)) {
                showBubble();
            } else {
                hideBubble();
                if (!sp.getBoolean("on", false)) {
                    stopSelf();
                    return START_NOT_STICKY;
                }
            }
        } else {
            if (sp.getBoolean("on", false)) setOrientation(sp.getInt("so", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
            if (sp.getBoolean("bubble", false)) showBubble();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        running = false;
        removeOrientation();
        hideBubble();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    // ---- orientasi ----
    private void setOrientation(int so) {
        try {
            if (orientView == null) {
                orientView = new View(this);
                olp = new WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
                olp.gravity = Gravity.TOP | Gravity.START;
                olp.screenOrientation = so;
                wm.addView(orientView, olp);
            } else {
                olp.screenOrientation = so;
                wm.updateViewLayout(orientView, olp);
            }
        } catch (Throwable t) {
            toast("Gagal menerapkan orientasi: " + t.getMessage());
        }
    }

    private void removeOrientation() {
        if (orientView != null) {
            try { wm.removeView(orientView); } catch (Throwable ignored) { }
            orientView = null;
            olp = null;
        }
    }

    // ---- kontrol melayang ----
    private void choose(int so) {
        if (so == OFF) {
            prefs().edit().putBoolean("on", false).apply();
            removeOrientation();
        } else {
            prefs().edit().putBoolean("on", true).putInt("so", so).apply();
            setOrientation(so);
        }
        updateBubble();
    }

    private void updateBubble() {
        if (bubble == null) return;
        int cur = current(this);
        for (int i = 0; i < 7; i++) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(10));
            g.setColor(BTN_SO[i] == cur ? 0xFF2D7BFF : 0xFF141E3E);
            btns[i].setBackground(g);
            btns[i].setTextColor(0xFFFFFFFF);
        }
    }

    private void showBubble() {
        if (bubble != null) {
            updateBubble();
            return;
        }
        bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);

        FrameLayout head = new FrameLayout(this);
        GradientDrawable hb = new GradientDrawable();
        hb.setShape(GradientDrawable.OVAL);
        hb.setColor(0xEE101A3A);
        hb.setStroke(dp(2), 0xFF4D9BFF);
        head.setBackground(hb);
        head.addView(new IconView(this, IconView.ROTATE, 0xFFFFFFFF),
            new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        bubble.addView(head, new LinearLayout.LayoutParams(dp(48), dp(48)));

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable pb = new GradientDrawable();
        pb.setCornerRadius(dp(14));
        pb.setColor(0xEE0C1430);
        pb.setStroke(dp(1), 0xFF1C2B58);
        panel.setBackground(pb);
        panel.setPadding(dp(6), dp(6), dp(6), dp(6));
        panel.setVisibility(View.GONE);
        LinearLayout row1 = new LinearLayout(this);
        LinearLayout row2 = new LinearLayout(this);
        for (int i = 0; i < 7; i++) {
            final int so = BTN_SO[i];
            TextView t = new TextView(this);
            t.setText(BTN_TXT[i]);
            t.setTextSize(12);
            t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(10), dp(8), dp(10), dp(8));
            t.setOnClickListener(v -> choose(so));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            (i < 4 ? row1 : row2).addView(t, lp);
            btns[i] = t;
        }
        panel.addView(row1);
        panel.addView(row2);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        plp.topMargin = dp(6);
        bubble.addView(panel, plp);

        blp = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        blp.gravity = Gravity.TOP | Gravity.START;
        SharedPreferences sp = prefs();
        blp.x = sp.getInt("bx", dp(8));
        blp.y = sp.getInt("by", dp(220));

        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        final float[] st = new float[4];
        final boolean[] moved = {false};
        head.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    st[0] = e.getRawX();
                    st[1] = e.getRawY();
                    st[2] = blp.x;
                    st[3] = blp.y;
                    moved[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - st[0], dy = e.getRawY() - st[1];
                    if (!moved[0] && Math.abs(dx) + Math.abs(dy) > slop) moved[0] = true;
                    if (moved[0]) {
                        blp.x = (int) (st[2] + dx);
                        blp.y = (int) (st[3] + dy);
                        try { wm.updateViewLayout(bubble, blp); } catch (Throwable ignored) { }
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                    if (!moved[0]) {
                        panel.setVisibility(panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                        bubble.requestLayout();
                    } else {
                        prefs().edit().putInt("bx", blp.x).putInt("by", blp.y).apply();
                    }
                    return true;
                default:
                    return true;
            }
        });
        try {
            wm.addView(bubble, blp);
        } catch (Throwable t) {
            bubble = null;
            toast("Gagal menampilkan kontrol melayang");
            return;
        }
        updateBubble();
    }

    private void hideBubble() {
        if (bubble != null) {
            try { wm.removeView(bubble); } catch (Throwable ignored) { }
            bubble = null;
            panel = null;
        }
    }
}
