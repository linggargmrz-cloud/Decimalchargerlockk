package id.decimalcharge.lock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class DnsBubbleService extends Service {
    static volatile boolean running = false;
    private static final String CH = "dns_bubble";
    private static final int ID_FG = 5;

    private WindowManager wm;
    private BubbleView bubble;
    private WindowManager.LayoutParams lp;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (bubble != null) bubble.invalidate();
            h.postDelayed(this, 2000);
        }
    };

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH, "Bubble DNS", NotificationManager.IMPORTANCE_MIN));
        PendingIntent open = PendingIntent.getActivity(this, 5, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(ID_FG, new Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("Bubble DNS aktif")
            .setContentText("Ketuk bubble untuk mengganti DNS")
            .setOngoing(true).setContentIntent(open).build());

        if (!Settings.canDrawOverlays(this)) {
            toast("Izin tampil di atas aplikasi lain belum aktif");
            stopSelf();
            return;
        }
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        SharedPreferences sp = getSharedPreferences("app", MODE_PRIVATE);
        int size = dp(52);
        lp = new WindowManager.LayoutParams(size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = sp.getInt("bx", dp(8));
        lp.y = sp.getInt("by", dp(240));
        bubble = new BubbleView(this);
        try {
            wm.addView(bubble, lp);
            running = true;
            h.postDelayed(poll, 2000);
        } catch (Throwable t) {
            toast("Gagal menampilkan bubble");
            stopSelf();
        }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private void toggle() {
        int r = DnsHelper.set(this, !DnsHelper.isPrivate(this));
        if (r == 1) toast("Izin WRITE_SECURE_SETTINGS belum diberikan. Buka aplikasi untuk panduannya.");
        else if (r == 2) toast("Isi hostname DNS di aplikasi dulu.");
        else if (r == 3) toast("Gagal mengganti DNS.");
        if (bubble != null) bubble.invalidate();
    }

    private void snap() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = lp.width;
        lp.x = (lp.x + size / 2 < dm.widthPixels / 2) ? dp(4) : dm.widthPixels - size - dp(4);
        lp.y = Math.max(dp(24), Math.min(dm.heightPixels - size - dp(48), lp.y));
        try { wm.updateViewLayout(bubble, lp); } catch (Throwable ignored) { }
        getSharedPreferences("app", MODE_PRIVATE).edit().putInt("bx", lp.x).putInt("by", lp.y).apply();
    }

    private class BubbleView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tx = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float downX, downY;
        private int startX, startY;
        private boolean dragging;

        BubbleView(Context c) {
            super(c);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(0xE6101316);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeWidth(dp(3));
            tx.setTextAlign(Paint.Align.CENTER);
            tx.setTypeface(Typeface.DEFAULT_BOLD);
        }

        @Override protected void onDraw(Canvas cv) {
            String m = DnsHelper.mode(getContext());
            boolean on = "hostname".equals(m);
            int col = on ? 0xFF2ECC71 : "off".equals(m) ? 0xFF7D8890 : 0xFF3D8BFF;
            String label = on ? "ON" : "off".equals(m) ? "MATI" : "AUTO";
            float r = getWidth() / 2f;
            cv.drawCircle(r, r, r - dp(1), fill);
            ring.setColor(col);
            cv.drawCircle(r, r, r - dp(3), ring);
            tx.setTextSize(dp(13));
            tx.setColor(0xFFFFFFFF);
            cv.drawText("DNS", r, r - dp(1), tx);
            tx.setTextSize(dp(9));
            tx.setColor(col);
            cv.drawText(label, r, r + dp(11), tx);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = lp.x;
                    startY = lp.y;
                    dragging = false;
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                    if (!dragging && Math.abs(dx) + Math.abs(dy) > dp(10)) dragging = true;
                    if (dragging) {
                        lp.x = startX + (int) dx;
                        lp.y = startY + (int) dy;
                        try { wm.updateViewLayout(this, lp); } catch (Throwable ignored) { }
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP:
                    if (dragging) {
                        snap();
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                        toggle();
                    }
                    return true;
                default:
                    return true;
            }
        }
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }

    @Override public void onDestroy() {
        running = false;
        h.removeCallbacksAndMessages(null);
        try { if (wm != null && bubble != null) wm.removeView(bubble); } catch (Throwable ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
                                                       }
