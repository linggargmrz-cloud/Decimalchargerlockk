package id.decimalcharge.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        SharedPreferences sp = c.getSharedPreferences("app", Context.MODE_PRIVATE);
        if (sp.getBoolean("auto", true)) {
            try { c.startForegroundService(new Intent(c, ChargeService.class)); } catch (Exception ignored) { }
        }
        if (sp.getBoolean("quota", false)) {
            try { c.startForegroundService(new Intent(c, QuotaService.class)); } catch (Exception ignored) { }
        }
        if (sp.getBoolean("bubble", false) && Settings.canDrawOverlays(c)) {
            try { c.startForegroundService(new Intent(c, DnsBubbleService.class)); } catch (Exception ignored) { }
        }
        SharedPreferences rot = c.getSharedPreferences("rot", Context.MODE_PRIVATE);
        if ((rot.getBoolean("on", false) || rot.getBoolean("bubble", false)) && Settings.canDrawOverlays(c)) {
            try { c.startForegroundService(new Intent(c, RotationService.class)); } catch (Exception ignored) { }
        }
    }
}
