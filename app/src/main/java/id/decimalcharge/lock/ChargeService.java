package id.decimalcharge.lock;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;

public class ChargeService extends Service {
    private static final String CH_STATUS = "status_quiet", CH_ALERT = "alert";
    private static final String ACTION_REPOST = "id.decimalcharge.lock.REPOST";
    private static final int ID_FG = 1, ID_ALERT = 2;

    private final BroadcastReceiver power = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_POWER_CONNECTED.equals(i.getAction())) launch();
            else getSystemService(NotificationManager.class).cancel(ID_ALERT);
        }
    };

    private Notification statusNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent del = PendingIntent.getService(this, 2,
            new Intent(this, ChargeService.class).setAction(ACTION_REPOST),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CH_STATUS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Decimal Charge Lock aktif")
            .setContentText("Menunggu charger dicolokkan")
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setContentIntent(open)
            .setDeleteIntent(del)
            .build();
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.deleteNotificationChannel("status");
        NotificationChannel quiet = new NotificationChannel(CH_STATUS, "Status layanan", NotificationManager.IMPORTANCE_MIN);
        quiet.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        quiet.setShowBadge(false);
        nm.createNotificationChannel(quiet);
        nm.createNotificationChannel(new NotificationChannel(CH_ALERT, "Layar pengisian", NotificationManager.IMPORTANCE_HIGH));
        startForeground(ID_FG, statusNotification());
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(power, f);
    }

    private void launch() {
        Intent i = new Intent(this, ChargeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Sedang mengisi daya")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true).setContentIntent(pi).setAutoCancel(true).build();
        getSystemService(NotificationManager.class).notify(ID_ALERT, n);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_REPOST.equals(intent.getAction())) {
            try { startForeground(ID_FG, statusNotification()); } catch (Exception ignored) { }
        }
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        try {
            PendingIntent pi = PendingIntent.getForegroundService(getApplicationContext(), 3,
                new Intent(getApplicationContext(), ChargeService.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
            getSystemService(AlarmManager.class).set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1500, pi);
        } catch (Exception ignored) { }
        super.onTaskRemoved(rootIntent);
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(power); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
