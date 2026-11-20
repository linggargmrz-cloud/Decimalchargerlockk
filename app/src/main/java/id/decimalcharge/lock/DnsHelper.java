package id.decimalcharge.lock;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

final class DnsHelper {
    static final String KEY_MODE = "private_dns_mode", KEY_SPEC = "private_dns_specifier";
    static final String ADB_CMD = "pm grant id.decimalcharge.lock android.permission.WRITE_SECURE_SETTINGS";

    static boolean canWrite(Context c) {
        return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    static String mode(Context c) {
        String m = Settings.Global.getString(c.getContentResolver(), KEY_MODE);
        return m == null ? "opportunistic" : m;
    }

    static String spec(Context c) {
        String s = Settings.Global.getString(c.getContentResolver(), KEY_SPEC);
        return s == null ? "" : s;
    }

    static boolean isPrivate(Context c) { return "hostname".equals(mode(c)); }

    static String host(Context c) {
        return c.getSharedPreferences("app", Context.MODE_PRIVATE).getString("dns_host", "").trim();
    }

    static void saveHost(Context c, String h) {
        c.getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("dns_host", h.trim()).apply();
    }

    // 0 = berhasil, 1 = izin belum ada, 2 = hostname kosong, 3 = gagal
    static int set(Context c, boolean priv) {
        if (!canWrite(c)) return 1;
        try {
            ContentResolver cr = c.getContentResolver();
            if (priv) {
                String h = host(c);
                if (h.isEmpty()) return 2;
                Settings.Global.putString(cr, KEY_SPEC, h);
                Settings.Global.putString(cr, KEY_MODE, "hostname");
            } else {
                Settings.Global.putString(cr, KEY_MODE, "opportunistic");
            }
            return 0;
        } catch (Throwable t) { return 3; }
    }
}
