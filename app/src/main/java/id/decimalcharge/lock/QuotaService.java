package id.decimalcharge.lock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class QuotaService extends Service {
    static volatile boolean running = false;
    private static final String CH = "quota";
    private static final int ID = 7;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { getSystemService(NotificationManager.class).notify(ID, build()); } catch (Throwable ignored) { }
            h.postDelayed(this, 60000);
        }
    };

    private Notification build() {
        String title, text;
        if (!QuotaHelper.hasAccess(this)) {
            title = "Kuota hari ini";
            text = "Izin Akses penggunaan belum aktif. Ketuk untuk membuka aplikasi.";
        } else {
            title = "Kuota hari ini: " + QuotaHelper.fmt(QuotaHelper.mobileToday(this));
            text = "Seluler. Wi-Fi hari ini: " + QuotaHelper.fmt(QuotaHelper.wifiToday(this));
        }
        PendingIntent open = PendingIntent.getActivity(this, 7, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setContentIntent(open)
            .build();
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CH, "Kuota hari ini", NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        nm.createNotificationChannel(ch);
        startForeground(ID, build());
        running = true;
        h.postDelayed(tick, 60000);
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }

    @Override public void onDestroy() {
        running = false;
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
  }
