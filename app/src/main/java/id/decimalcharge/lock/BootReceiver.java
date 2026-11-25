package id.decimalcharge.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        SharedPreferences sp = c.getSharedPreferences("app", Context.MODE_PRIVATE);
        if (sp.getBoolean("auto", true)) {
            try { c.startForegroundService(new Intent(c, ChargeService.class)); } catch (Exception ignored) { }
        }
        if (sp.getBoolean("quota", false)) {
            try { c.startForegroundService(new Intent(c, QuotaService.class)); } catch (Exception ignored) { }
        }
    }
}
