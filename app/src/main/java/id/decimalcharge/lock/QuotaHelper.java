package id.decimalcharge.lock;

import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Process;
import java.util.Calendar;
import java.util.Locale;

final class QuotaHelper {
    static boolean hasAccess(Context c) {
        try {
            AppOpsManager ops = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            int mode;
            if (Build.VERSION.SDK_INT >= 29) {
                mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName());
            } else {
                mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.getPackageName());
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) {
            return false;
        }
    }

    private static long startOfDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // Total byte (unduh + unggah) sejak 00:00 hari ini, atau -1 jika gagal.
    private static long bytes(Context c, int type) {
        try {
            NetworkStatsManager nsm = (NetworkStatsManager) c.getSystemService(Context.NETWORK_STATS_SERVICE);
            NetworkStats.Bucket b = nsm.querySummaryForDevice(type, null, startOfDay(), System.currentTimeMillis());
            return b.getRxBytes() + b.getTxBytes();
        } catch (Throwable t) {
            return -1;
        }
    }

    static long mobileToday(Context c) { return bytes(c, ConnectivityManager.TYPE_MOBILE); }

    static long wifiToday(Context c) { return bytes(c, ConnectivityManager.TYPE_WIFI); }

    static String fmt(long b) {
        if (b < 0) return "-";
        double mb = b / 1048576.0;
        Locale id = new Locale("id", "ID");
        if (mb < 10) return String.format(id, "%.1f MB", mb);
        if (mb < 1000) return String.format(id, "%.0f MB", mb);
        return String.format(id, "%.2f GB", mb / 1024.0);
    }
            }
