package id.decimalcharge.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        boolean on = c.getSharedPreferences("app", Context.MODE_PRIVATE).getBoolean("auto", true);
        if (!on) return;
        try { c.startForegroundService(new Intent(c, ChargeService.class)); } catch (Exception ignored) { }
    }
}
