package id.decimalcharge.lock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.io.BufferedReader;
import java.io.FileReader;

final class BatteryReader {
    boolean charging;
    int whole = -1;
    int hundredths = -1;
    boolean exact;
    double watts = Double.NaN;
    String tier = "NORMAL";
    long counterMah = -1;

    static BatteryReader read(Context c) {
        BatteryReader r = new BatteryReader();
        Intent i = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return r;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int voltageMv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        r.charging = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        if (level < 0 || scale <= 0) return r;
        double base = 100.0 * level / scale;
        r.whole = (int) Math.round(base);

        try {
            BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
            long counter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            if (counter > 0 && counter < 20000) counter *= 1000;
            long full = readLong("/sys/class/power_supply/battery/charge_full");
            r.exact = full > 0;
            if (!r.exact) full = 5000000L;
            if (counter > 0) {
                double p = 100.0 * counter / full;
                if (p <= 100.0 && Math.abs(p - base) <= 2.0) {
                    int h = (int) Math.floor(p * 100.0);
                    r.whole = h / 100;
                    r.hundredths = h % 100;
                    r.counterMah = counter / 1000;
                }
            }

            long cur = Math.abs((long) bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
            if (cur > 0 && cur < 20000) cur *= 1000;
            if (cur > 0 && voltageMv > 0) r.watts = (cur / 1e6) * (voltageMv / 1e3);
        } catch (Throwable ignored) { }

        double w = Double.isNaN(r.watts) ? 0 : r.watts;
        r.tier = w < 2.5 ? "SLOW" : w < 12 ? "NORMAL" : w < 25 ? "FAST" : "SUPER FAST";
        return r;
    }

    private static long readLong(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return Long.parseLong(br.readLine().trim());
        } catch (Throwable t) { return -1; }
    }
                      }
