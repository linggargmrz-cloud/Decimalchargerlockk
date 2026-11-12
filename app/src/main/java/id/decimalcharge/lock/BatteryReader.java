package id.decimalcharge.lock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
    String note = "";

    private static boolean loaded;
    private static int anchorLevel = -1, lastLevel = -1;
    private static long anchorCounter = -1, anchorTime = 0, lastTime = 0;
    private static double fullUah = 0;

    private static void load(Context c) {
        if (loaded) return;
        loaded = true;
        SharedPreferences sp = c.getApplicationContext().getSharedPreferences("cal", Context.MODE_PRIVATE);
        fullUah = sp.getFloat("full", 0f);
    }

    private static void save(Context c) {
        c.getApplicationContext().getSharedPreferences("cal", Context.MODE_PRIVATE)
            .edit().putFloat("full", (float) fullUah).apply();
    }

    static synchronized BatteryReader read(Context c) {
        BatteryReader r = new BatteryReader();
        load(c);
        Intent i = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return r;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int voltageMv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        r.charging = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        if (level < 0 || scale <= 0) return r;
        double base = 100.0 * level / scale;
        r.whole = (int) Math.round(base);
        int lv = r.whole;
        long now = System.currentTimeMillis();

        try {
            BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
            long counter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            if (counter > 0 && counter < 20000) counter *= 1000;
            long sys = sysfsFull();
            r.exact = sys > 0;

            if (!r.charging) {
                anchorLevel = -1;
                r.note = "desimal hanya dihitung saat mengisi daya";
            } else if (lv >= 100) {
                anchorLevel = -1;
                r.whole = 100;
                r.hundredths = -1;
                r.note = "baterai penuh";
            } else if (counter <= 0) {
                anchorLevel = -1;
                r.note = "charge counter tidak tersedia di HP ini";
            } else {
                if (now - lastTime > 120000) lastLevel = -1;
                if (lastLevel != -1 && lv != lastLevel) {
                    if (lv == lastLevel + 1) {
                        if (anchorLevel == lv - 1 && counter > anchorCounter) {
                            double sample = 100.0 * (counter - anchorCounter);
                            if (sample > 1.5e6 && sample < 8e6) {
                                fullUah = fullUah == 0 ? sample : 0.7 * fullUah + 0.3 * sample;
                                save(c);
                            }
                        }
                        anchorLevel = lv;
                        anchorCounter = counter;
                        anchorTime = now;
                    } else {
                        anchorLevel = -1;
                    }
                }
                double full = sys > 0 ? sys : (fullUah > 0 ? fullUah : 5000000.0);
                if (anchorLevel == lv && anchorLevel != -1) {
                    if (counter == anchorCounter && now - anchorTime > 45000) {
                        r.note = "charge counter tidak bergerak, desimal tidak bisa dihitung";
                    } else {
                        double p = anchorLevel + 100.0 * (counter - anchorCounter) / full;
                        if (p < anchorLevel) p = anchorLevel;
                        if (p > anchorLevel + 0.99) p = anchorLevel + 0.99;
                        if (p > 99.99) p = 99.99;
                        int h = (int) Math.floor(p * 100.0 + 1e-6);
                        r.whole = h / 100;
                        r.hundredths = h % 100;
                        r.note = r.exact ? "desimal dari charge counter baterai"
                            : (fullUah > 0 ? "estimasi, dikalibrasi dari charge counter (" + (int) (fullUah / 1000) + " mAh)"
                                : "estimasi awal, kapasitas 5000 mAh");
                    }
                } else {
                    r.note = "mengkalibrasi: menunggu level naik 1% (sekitar 1-3 menit)";
                }
            }

            long cur = Math.abs((long) bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
            if (cur > 0 && cur < 20000) cur *= 1000;
            if (cur > 0 && voltageMv > 0) r.watts = (cur / 1e6) * (voltageMv / 1e3);
        } catch (Throwable ignored) {
            r.note = "gagal membaca data baterai";
        }

        lastLevel = lv;
        lastTime = now;
        double w = Double.isNaN(r.watts) ? 0 : r.watts;
        r.tier = w < 2.5 ? "SLOW" : w < 12 ? "NORMAL" : w < 25 ? "FAST" : "SUPER FAST";
        return r;
    }

    private static long sysfsFull() {
        String[] p = {"/sys/class/power_supply/battery/charge_full", "/sys/class/power_supply/bms/charge_full"};
        for (String s : p) {
            long v = readLong(s);
            if (v > 1000000) return v;
        }
        return -1;
    }

    private static long readLong(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return Long.parseLong(br.readLine().trim());
        } catch (Throwable t) { return -1; }
    }
                    }
