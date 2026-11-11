package id.decimalcharge.lock;

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

public class ChargeService extends Service {
    private static final String CH_STATUS = "status", CH_ALERT = "alert";
    private static final int ID_FG = 1, ID_ALERT = 2;

    private final BroadcastReceiver power = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_POWER_CONNECTED.equals(i.getAction())) launch();
            else getSystemService(NotificationManager.class).cancel(ID_ALERT);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CH_STATUS, "Status layanan", NotificationManager.IMPORTANCE_MIN));
        nm.createNotificationChannel(new NotificationChannel(CH_ALERT, "Layar pengisian", NotificationManager.IMPORTANCE_HIGH));
        Notification n = new Notification.Builder(this, CH_STATUS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("Decimal Charge Lock aktif")
            .setContentText("Menunggu charger dicolokkan").setOngoing(true).build();
        startForeground(ID_FG, n);
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

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override public void onDestroy() {
        try { unregisterReceiver(power); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
  }
