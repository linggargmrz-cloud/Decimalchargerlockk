package id.decimalcharge.lock;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TempLog {
    static final class Sample {
        final long t;
        final int level10;   // persen baterai dalam persepuluh (823 = 82,3%)
        final int temp;      // suhu dalam persepuluh derajat (352 = 35,2 C)
        final boolean chg;
        Sample(long t, int level10, int temp, boolean chg) {
            this.t = t; this.level10 = level10; this.temp = temp; this.chg = chg;
        }
    }

    static volatile int rev = 0;
    private static final long KEEP_MS = 7L * 24 * 3600 * 1000;
    private static final long KEEPALIVE_MS = 300000L;
    private static final List<Sample> cache = new ArrayList<>();
    private static boolean loaded = false;
    private static int appended = 0;

    private static File file(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "templog.csv");
    }

    private static String line(Sample s) {
        return s.t + "," + Math.round(s.level10 / 10f) + "," + s.temp + "," + (s.chg ? "1" : "0") + "," + s.level10 + "\n";
    }

    private static Sample parse(String ln) {
        try {
            String[] p = ln.split(",");
            if (p.length < 4) return null;
            int l10 = p.length >= 5 ? Integer.parseInt(p[4]) : Integer.parseInt(p[1]) * 10;
            return new Sample(Long.parseLong(p[0]), l10, Integer.parseInt(p[2]), "1".equals(p[3]));
        } catch (Throwable t) {
            return null;
        }
    }

    static String pct(int level10) {
        return String.format(new Locale("id", "ID"), "%.1f%%", level10 / 10.0);
    }

    private static void ensure(Context c) {
        if (loaded) return;
        loaded = true;
        File f = file(c);
        if (!f.exists()) return;
        long cutoff = System.currentTimeMillis() - KEEP_MS;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String ln;
            while ((ln = br.readLine()) != null) {
                Sample s = parse(ln);
                if (s != null && s.t >= cutoff) cache.add(s);
            }
        } catch (Throwable ignored) { }
    }

    // Dicatat setiap suhu, level (0,1%), atau status charger berubah, atau 5 menit sejak catatan terakhir.
    static synchronized void record(Context c, int level10, int temp, boolean charging) {
        if (temp == Integer.MIN_VALUE || temp < -300 || temp > 1000 || level10 < 0 || level10 > 1000) return;
        ensure(c);
        long now = System.currentTimeMillis();
        Sample last = cache.isEmpty() ? null : cache.get(cache.size() - 1);
        if (last != null) {
            if (now <= last.t) return;
            boolean changed = temp != last.temp || level10 != last.level10 || charging != last.chg;
            if (!changed && now - last.t < KEEPALIVE_MS) return;
        }
        Sample s = new Sample(now, level10, temp, charging);
        try (FileWriter w = new FileWriter(file(c), true)) {
            w.write(line(s));
        } catch (Throwable t) {
            return;
        }
        cache.add(s);
        rev++;
        if (++appended >= 500) { appended = 0; trim(c, now); }
    }

    private static void trim(Context c, long now) {
        long cutoff = now - KEEP_MS;
        int k = 0;
        while (k < cache.size() && cache.get(k).t < cutoff) k++;
        if (k > 0) cache.subList(0, k).clear();
        try (FileWriter w = new FileWriter(file(c), false)) {
            for (Sample s : cache) w.write(line(s));
        } catch (Throwable ignored) { }
    }

    static synchronized List<Sample> load(Context c, long fromMs) {
        ensure(c);
        List<Sample> out = new ArrayList<>();
        for (Sample s : cache) {
            if (s.t >= fromMs) out.add(s);
        }
        return out;
    }

    static synchronized void clear(Context c) {
        ensure(c);
        cache.clear();
        file(c).delete();
        rev++;
    }
                    }
