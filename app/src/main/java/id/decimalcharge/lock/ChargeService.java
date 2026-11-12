package id.decimalcharge.lock;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

public class ChargeService extends Service {
    private static final String CH_STATUS = "status_quiet", CH_ALERT = "alert";
    private static final int ID_FG = 1, ID_ALERT = 2;
    private static final long MANUAL_OFF_MS = 8000;

    private final Handler h = new Handler(Looper.getMainLooper());
    private int tries = 0;

    private boolean isCharging() {
        Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return i != null && i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
    }

    // Menunggu HP benar-benar terkunci setelah layar mati, baru buka layar pengisian (redup).
    private final Runnable waitLock = new Runnable() {
        @Override public void run() {
            PowerManager pm = getSystemService(PowerManager.class);
            KeyguardManager km = getSystemService(KeyguardManager.class);
            if (pm.isInteractive() || !isCharging()) return;
            if (ChargeActivity.showing) return;
            if (km.isKeyguardLocked() || ++tries >= 16) { launch(true); return; }
            h.postDelayed(this, 500);
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            String a = i.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(a)) {
                h.removeCallbacks(waitLock);
                if (getSystemService(PowerManager.class).isInteractive()) {
                    launch(false);
                } else {
                    tries = 0;
                    h.postDelayed(waitLock, 600);
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                if (!isCharging()) return;
                long now = System.currentTimeMillis();
                boolean was = ChargeActivity.showing || now - ChargeActivity.lastDestroy < 2000;
                if (was) {
                    // Layar pengisian redup: layar mati hanya bisa karena tombol power.
                    if (ChargeActivity.dimMode) return;
                    // Layar mati cepat setelah sentuhan terakhir: dianggap tombol power.
                    if (now - ChargeActivity.lastInteraction < MANUAL_OFF_MS) return;
                }
                tries = 0;
                h.removeCallbacks(waitLock);
                h.postDelayed(waitLock, 600);
            } else {
                h.removeCallbacks(waitLock);
                getSystemService(NotificationManager.class).cancel(ID_ALERT);
            }
        }
    };

    private Notification statusNotification() {
        Intent set = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
            .putExtra(Settings.EXTRA_CHANNEL_ID, CH_STATUS);
        PendingIntent open = PendingIntent.getActivity(this, 1, set,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CH_STATUS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Decimal Charge Lock aktif")
            .setContentText("Ketuk untuk menyembunyikan notifikasi ini")
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setContentIntent(open)
            .build();
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel quiet = new NotificationChannel(CH_STATUS, "Status layanan", NotificationManager.IMPORTANCE_MIN);
        quiet.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        quiet.setShowBadge(false);
        nm.createNotificationChannel(quiet);
        nm.createNotificationChannel(new NotificationChannel(CH_ALERT, "Layar pengisian", NotificationManager.IMPORTANCE_HIGH));
        startForeground(ID_FG, statusNotification());
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, f);
    }

    private void launch(boolean dim) {
        Intent i = new Intent(this, ChargeActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("dim", dim);
        PendingIntent pi = PendingIntent.getActivity(this, dim ? 10 : 11, i,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Sedang mengisi daya")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true).setContentIntent(pi).setAutoCancel(true).build();
        getSystemService(NotificationManager.class).notify(ID_ALERT, n);
        try { startActivity(i); } catch (Exception ignored) { }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

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
        h.removeCallbacks(waitLock);
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
                    }
