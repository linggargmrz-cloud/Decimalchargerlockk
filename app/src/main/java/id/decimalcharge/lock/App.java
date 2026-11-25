package id.decimalcharge.lock;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;

public class App extends Application {
    private final Handler h = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { tick(c); }
    };

    // Mengecek tiap 15 detik saat dicas (untuk desimal 0,1%), tiap 30 detik saat tidak dicas.
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            tick(App.this);
            long next = 30000;
            try {
                Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (i != null && i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0) next = 15000;
            } catch (Throwable ignored) { }
            h.postDelayed(this, next);
        }
    };

    static void tick(Context c) {
        try { BatteryReader.read(c); } catch (Throwable ignored) { }
    }

    @Override public void onCreate() {
        super.onCreate();
        try { registerReceiver(rx, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); } catch (Throwable ignored) { }
        h.post(poll);
    }
}
