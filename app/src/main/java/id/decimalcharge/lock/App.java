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
        @Override public void onReceive(Context c, Intent i) { log(c, i); }
    };

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            try {
                log(App.this, registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
            } catch (Throwable ignored) { }
            h.postDelayed(this, 30000);
        }
    };

    static void log(Context c, Intent i) {
        if (i == null) return;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        boolean chg = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        if (level < 0 || scale <= 0) return;
        TempLog.record(c, Math.round(100f * level / scale), temp, chg);
    }

    @Override public void onCreate() {
        super.onCreate();
        try { registerReceiver(rx, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); } catch (Throwable ignored) { }
        h.post(poll);
    }
}
