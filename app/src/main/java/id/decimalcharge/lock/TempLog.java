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
    private static boolean inited = false;
    private static int lastTemp = Integer.MIN_VALUE, lastLevel = -1, appended = 0;
    private static long lastTime = 0;

    private static File file(Context c) {
        return new File(c.getApplicationContext().getFilesDir(), "templog.csv");
    }

    private static Sample parse(String line) {
        try {
            String[] p = line.split(",");
            if (p.length < 4) return null;
            return new Sample(Long.parseLong(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), "1".equals(p[3]));
        } catch (Throwable t) {
            return null;
        }
    }

    private static List<Sample> readAll(Context c) {
        List<Sample> out = new ArrayList<>();
        File f = file(c);
        if (!f.exists()) return out;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String ln;
            while ((ln = br.readLine()) != null) {
                Sample s = parse(ln);
                if (s != null) out.add(s);
            }
        } catch (Throwable ignored) { }
        return out;
    }

    // Suhu dalam persepuluh derajat (352 = 35,2 C). Dicatat hanya jika suhu atau level berubah,
    // atau sudah lebih dari 10 menit sejak catatan terakhir.
    static synchronized void record(Context c, int level, int temp, boolean charging) {
        if (temp == Integer.MIN_VALUE || temp < -300 || temp > 1000 || level < 0) return;
        long now = System.currentTimeMillis();
        if (!inited) {
            inited = true;
            List<Sample> all = readAll(c);
            if (!all.isEmpty()) {
                Sample s = all.get(all.size() - 1);
                lastTemp = s.temp; lastLevel = s.level; lastTime = s.t;
            }
        }
        boolean changed = temp != lastTemp || level != lastLevel;
        if (!changed && now - lastTime < 600000L) return;
        try (FileWriter w = new FileWriter(file(c), true)) {
            w.write(now + "," + level + "," + temp + "," + (charging ? "1" : "0") + "\n");
        } catch (Throwable t) {
            return;
        }
        lastTemp = temp; lastLevel = level; lastTime = now;
        rev++;
        if (++appended >= 300) { appended = 0; trim(c, now); }
    }

    private static void trim(Context c, long now) {
        List<Sample> all = readAll(c);
        try (FileWriter w = new FileWriter(file(c), false)) {
            for (Sample s : all) {
                if (now - s.t <= KEEP_MS) {
                    w.write(s.t + "," + s.level + "," + s.temp + "," + (s.chg ? "1" : "0") + "\n");
                }
            }
        } catch (Throwable ignored) { }
    }

    static synchronized List<Sample> load(Context c, long fromMs) {
        List<Sample> out = new ArrayList<>();
        for (Sample s : readAll(c)) {
            if (s.t >= fromMs) out.add(s);
        }
        return out;
    }

    static synchronized void clear(Context c) {
        file(c).delete();
        inited = true;
        lastTemp = Integer.MIN_VALUE; lastLevel = -1; lastTime = 0;
        rev++;
    }
                              }
