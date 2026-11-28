package id.decimalcharge.lock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;

/** Pengaturan auto clicker yang disimpan di HP. */
final class ClickerConfig {
    static final int MAX = 10;

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("clk", Context.MODE_PRIVATE);
    }

    static int count(Context c) { return Math.max(1, Math.min(MAX, sp(c).getInt("n", 3))); }

    static void setCount(Context c, int n) { sp(c).edit().putInt("n", Math.max(1, Math.min(MAX, n))).apply(); }

    static int hold(Context c) { return Math.max(1, Math.min(5000, sp(c).getInt("hold", 50))); }

    static void setHold(Context c, int v) { sp(c).edit().putInt("hold", Math.max(1, Math.min(5000, v))).apply(); }

    static int interval(Context c) { return Math.max(10, Math.min(600000, sp(c).getInt("interval", 500))); }

    static void setInterval(Context c, int v) { sp(c).edit().putInt("interval", Math.max(10, Math.min(600000, v))).apply(); }

    // 0 = tanpa batas
    static int repeat(Context c) { return Math.max(0, Math.min(999999, sp(c).getInt("repeat", 0))); }

    static void setRepeat(Context c, int v) { sp(c).edit().putInt("repeat", Math.max(0, Math.min(999999, v))).apply(); }

    static boolean simultaneous(Context c) { return sp(c).getBoolean("sim", true); }

    static void setSimultaneous(Context c, boolean v) { sp(c).edit().putBoolean("sim", v).apply(); }

    // Posisi pusat titik ke-i dalam piksel layar.
    static float[] pos(Context c, int i) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        SharedPreferences s = sp(c);
        float x = s.getFloat("x" + i, dm.widthPixels / 2f + ((i % 5) - 2) * 64f * dm.density);
        float y = s.getFloat("y" + i, dm.heightPixels / 2f + (i / 5) * 80f * dm.density);
        return new float[]{x, y};
    }

    static void setPos(Context c, int i, float x, float y) {
        sp(c).edit().putFloat("x" + i, x).putFloat("y" + i, y).apply();
    }

    static void resetPositions(Context c) {
        SharedPreferences.Editor e = sp(c).edit();
        for (int i = 0; i < MAX; i++) e.remove("x" + i).remove("y" + i);
        e.apply();
    }
}
