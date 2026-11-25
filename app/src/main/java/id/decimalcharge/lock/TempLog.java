package id.decimalcharge.lock;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

final class TempLog {
    static final class Sample {
        final long t;
        final int level;
        final int temp;
        final boolean chg;
        Sample(long t, int level, int temp, boolean chg) {
            this.t = t; this.level = level; this.temp = temp; this.chg = chg;
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
        return s.t + "," + s.level + "," + s.temp + "," + (s.chg ? "1" : "0") + "\n";
    }

    private static Sample parse(String ln) {
        try {
            String[] p = ln.split(",");
            if (p.length < 4) return null;
            return new Sample(Long.parseLong(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), "1".equals(p[3]));
        } catch (Throwable t) {
            return null;
        }
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

    // Suhu dalam persepuluh derajat (352 = 35,2 C). Dicatat setiap suhu, level, atau status charger berubah,
    // atau sudah 5 menit sejak catatan terakhir.
    static synchronized void record(Context c, int level, int temp, boolean charging) {
        if (temp == Integer.MIN_VALUE || temp < -300 || temp > 1000 || level < 0) return;
        ensure(c);
        long now = System.currentTimeMillis();
        Sample last = cache.isEmpty() ? null : cache.get(cache.size() - 1);
        if (last != null) {
            if (now <= last.t) return;
            boolean changed = temp != last.temp || level != last.level || charging != last.chg;
            if (!changed && now - last.t < KEEPALIVE_MS) return;
        }
        Sample s = new Sample(now, level, temp, charging);
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
