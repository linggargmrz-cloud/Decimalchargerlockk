package id.decimalcharge.lock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ChargeView extends View {
    private static final int SPARKS = 160, DUST = 80, UP = 100;

    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF arcRect = new RectF();
    private final Typeface thin = Typeface.create("sans-serif-thin", Typeface.NORMAL);
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("H:mm:ss", Locale.US);
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
    private BatteryReader info = new BatteryReader();
    private double tempC = Double.NaN;
    private long fullMs = -1;
    private boolean fullEst = false;
    private long lastRead = 0;
    private final long t0 = System.currentTimeMillis();
    private long lastTouch = System.currentTimeMillis();
    private long dimDelay = 30000;
    private boolean dimmed = false;
    private float shown = 0f;

    private final float[] sa = new float[SPARKS], sr = new float[SPARKS], ss = new float[SPARKS],
        sp = new float[SPARKS], sz = new float[SPARKS];
    private final float[] da = new float[DUST], dr = new float[DUST], ds = new float[DUST],
        dp = new float[DUST], dz = new float[DUST];
    private final float[] ux = new float[UP], uph = new float[UP], uz = new float[UP], uw = new float[UP];

    private final Runnable frame = new Runnable() {
        @Override public void run() { invalidate(); postDelayed(this, 33); }
    };

    public ChargeView(Context c) {
        super(c);
        setBackgroundColor(Color.BLACK);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        dot.setStyle(Paint.Style.FILL);
        pill.setStyle(Paint.Style.STROKE);
        pill.setColor(0xCC9CC8FF);
        t.setTextAlign(Paint.Align.LEFT);
        Random rnd = new Random(7);
        for (int i = 0; i < SPARKS; i++) {
            sa[i] = (float) (rnd.nextFloat() * Math.PI * 2);
            sr[i] = (rnd.nextFloat() * 2f - 1f);
            ss[i] = (rnd.nextFloat() * 0.08f + 0.01f) * (rnd.nextBoolean() ? 1 : -1);
            sp[i] = rnd.nextFloat() * 6.28f;
            sz[i] = 0.5f + rnd.nextFloat() * 1.6f;
        }
        for (int i = 0; i < DUST; i++) {
            da[i] = (float) (rnd.nextFloat() * Math.PI * 2);
            dr[i] = (rnd.nextFloat() * 2f - 1f);
            ds[i] = (rnd.nextFloat() * 0.04f + 0.005f) * (rnd.nextBoolean() ? 1 : -1);
            dp[i] = rnd.nextFloat() * 6.28f;
            dz[i] = 0.5f + rnd.nextFloat() * 1.5f;
        }
        for (int i = 0; i < UP; i++) {
            ux[i] = rnd.nextFloat() * 2f - 1f;
            uph[i] = rnd.nextFloat();
            uz[i] = 0.6f + rnd.nextFloat() * 1.3f;
            uw[i] = rnd.nextFloat() * 6.28f;
        }
        try {
            long v = Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 30000);
            dimDelay = Math.max(5000, Math.min(120000, v));
        } catch (Throwable ignored) { }
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        float cx = w / 2f, cy = h * 0.54f;
        float R = Math.min(w * 0.43f, h * 0.30f);
        bg.setShader(new RadialGradient(cx, cy, R * 1.7f,
            new int[]{0x66123C7A, 0x30081C3C, 0x00000000}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastTouch = System.currentTimeMillis();
        post(frame);
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(frame);
        super.onDetachedFromWindow();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            lastTouch = System.currentTimeMillis();
            if (dimmed) { dimmed = false; applyDim(false); }
        }
        return super.onTouchEvent(e);
    }

    private void applyDim(boolean dim) {
        try {
            Context c = getContext();
            if (c instanceof Activity) {
                Window w = ((Activity) c).getWindow();
                WindowManager.LayoutParams lp = w.getAttributes();
                lp.screenBrightness = dim ? 0.03f : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                w.setAttributes(lp);
            } else if (c instanceof DreamService) {
                ((DreamService) c).setScreenBright(!dim);
            }
        } catch (Throwable ignored) { }
    }

    private void text(Canvas cv, String s, float cx, float y, float size, Typeface tf, int color, float spacing) {
        t.setTypeface(tf); t.setTextSize(size); t.setColor(color); t.setLetterSpacing(spacing);
        cv.drawText(s, cx - t.measureText(s) / 2f, y, t);
    }

    private void fitText(Canvas cv, String s, float cx, float y, float size, float maxW, Typeface tf, int color) {
        t.setTypeface(tf); t.setLetterSpacing(0.02f); t.setTextSize(size);
        float tw = t.measureText(s);
        if (tw > maxW) { size = size * maxW / tw; t.setTextSize(size); tw = t.measureText(s); }
        t.setColor(color);
        cv.drawText(s, cx - tw / 2f, y, t);
    }

    private int clamp(int a) { return Math.max(0, Math.min(255, a)); }

    private void readExtra() {
        try {
            Context c = getContext();
            Intent bi = c.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bi != null) {
                int tm = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                tempC = tm == Integer.MIN_VALUE ? Double.NaN : tm / 10.0;
            }
            fullMs = -1; fullEst = false;
            if (info.charging && info.whole >= 0) {
                double pct = info.whole + (info.hundredths >= 0 ? info.hundredths / 100.0 : 0);
                if (pct < 99.9) {
                    BatteryManager bm = (BatteryManager) c.getSystemService(Context.BATTERY_SERVICE);
                    if (Build.VERSION.SDK_INT >= 28) {
                        long ms = bm.computeChargeTimeRemaining();
                        if (ms > 0) fullMs = ms;
                    }
                    if (fullMs < 0) {
                        long cur = Math.abs((long) bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
                        if (cur > 0 && cur < 20000) cur *= 1000;
                        if (cur > 50000) {
                            double remainMah = (100.0 - pct) / 100.0 * 5000.0;
                            fullMs = (long) (remainMah / (cur / 1000.0) * 3600000.0);
                            fullEst = true;
                        }
                    }
                    if (fullMs > 24L * 3600000L) fullMs = -1;
                }
            }
        } catch (Throwable ignored) { }
    }

    private String fullText() {
        if (fullMs <= 0) return null;
        long mins = Math.max(1, Math.round(fullMs / 60000.0));
        String p = fullEst ? "Penuh dalam ~" : "Penuh dalam ";
        if (mins < 60) return p + mins + " menit";
        return p + (mins / 60) + " jam " + (mins % 60) + " menit";
    }

    @Override protected void onDraw(Canvas cv) {
        long now = System.currentTimeMillis();
        if (now - lastRead > 1000) {
            info = BatteryReader.read(getContext());
            lastRead = now;
            readExtra();
        }
        if (!dimmed && now - lastTouch > dimDelay) { dimmed = true; applyDim(true); }

        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        cv.drawColor(Color.BLACK);

        float cx = w / 2f, cy = h * 0.54f;
        float R = Math.min(w * 0.43f, h * 0.30f);
        float ts = (now - t0) / 1000f;
        Date d = new Date(now);

        cv.drawCircle(cx, cy, R * 1.7f, bg);

        text(cv, clockFmt.format(d), cx, h * 0.12f, w * 0.14f, thin, 0xFFEDEDED, 0f);
        text(cv, dateFmt.format(d), cx, h * 0.12f + w * 0.085f, w * 0.04f, light, 0xFFB5B5B5, 0.05f);

        float target = 0f;
        if (info.whole >= 0) target = (float) ((info.whole + (info.hundredths >= 0 ? info.hundredths / 100.0 : 0)) / 100.0);
        target = Math.max(0f, Math.min(1f, target));
        shown += (target - shown) * 0.06f;
        if (Math.abs(target - shown) < 0.0005f) shown = target;

        arcRect.set(cx - R, cy - R, cx + R, cy + R);

        // Cincin dasar redup
        arc.setStrokeWidth(Math.max(1.5f, w * 0.003f));
        arc.setColor(0x446FA8FF);
        cv.drawArc(arcRect, 0, 360, false, arc);

        // Busur level: tebal rata, kedua ujung bulat, 5 lapis cahaya
        float sweep = 360f * shown;
        if (sweep > 0.4f) {
            float[] widths = {w * 0.085f, w * 0.055f, w * 0.034f, w * 0.018f, w * 0.0075f};
            int[] cols = {0x124A96FF, 0x2A4A96FF, 0x543F8CFF, 0xA078BEFF, 0xFFD7EEFF};
            for (int pass = 0; pass < 5; pass++) {
                arc.setStrokeWidth(widths[pass]);
                arc.setColor(cols[pass]);
                cv.drawArc(arcRect, -90f, sweep, false, arc);
            }
        }

        // Cahaya di ujung depan dan ujung awal
        double headA = -Math.PI / 2 + 2 * Math.PI * shown;
        float hx = (float) (cx + R * Math.cos(headA)), hy = (float) (cy + R * Math.sin(headA));
        float pulse = 0.88f + 0.12f * (float) Math.sin(ts * 4f);
        if (shown > 0.003f) {
            dot.setColor(0x104FA3FF); cv.drawCircle(hx, hy, w * 0.07f * pulse, dot);
            dot.setColor(0x224FA3FF); cv.drawCircle(hx, hy, w * 0.045f * pulse, dot);
            dot.setColor(0x484FA3FF); cv.drawCircle(hx, hy, w * 0.028f * pulse, dot);
            dot.setColor(0x90BFDFFF); cv.drawCircle(hx, hy, w * 0.014f, dot);
            dot.setColor(0xFFFFFFFF); cv.drawCircle(hx, hy, w * 0.006f, dot);
            float sx = cx, sy = cy - R;
            dot.setColor(0x184FA3FF); cv.drawCircle(sx, sy, w * 0.04f, dot);
            dot.setColor(0x404FA3FF); cv.drawCircle(sx, sy, w * 0.022f, dot);
            dot.setColor(0xB0D7EEFF); cv.drawCircle(sx, sy, w * 0.008f, dot);
        }

        // Debu halus melayang di sekitar cincin
        for (int i = 0; i < DUST; i++) {
            double a = da[i] + ds[i] * ts;
            float rr = R + dr[i] * w * 0.06f;
            float x = (float) (cx + rr * Math.cos(a)), y = (float) (cy + rr * Math.sin(a));
            double ang = (a + Math.PI / 2) % (2 * Math.PI);
            if (ang < 0) ang += 2 * Math.PI;
            float inArc = ang <= 2 * Math.PI * shown ? 1f : 0.3f;
            float tw = 0.5f + 0.5f * (float) Math.sin(ts * 1.5f + dp[i]);
            dot.setColor(Color.argb(clamp((int) ((14 + 34 * tw) * inArc)), 120, 180, 255));
            cv.drawCircle(x, y, w * 0.007f * dz[i], dot);
        }

        // Butiran berkelap-kelip di sekeliling cincin
        for (int i = 0; i < SPARKS; i++) {
            double a = sa[i] + ss[i] * ts;
            float rr = R + sr[i] * w * 0.04f;
            float x = (float) (cx + rr * Math.cos(a)), y = (float) (cy + rr * Math.sin(a));
            double ang = (a + Math.PI / 2) % (2 * Math.PI);
            if (ang < 0) ang += 2 * Math.PI;
            float inArc = ang <= 2 * Math.PI * shown ? 1f : 0.2f;
            float tw = 0.5f + 0.5f * (float) Math.sin(ts * 3f + sp[i]);
            dot.setColor(Color.argb(clamp((int) ((50 + 170 * tw) * inArc)), 190, 225, 255));
            cv.drawCircle(x, y, w * 0.0025f * sz[i], dot);
        }

        // Partikel naik dari tengah bawah, menyempit, lalu terhisap ke dasar cincin
        float yStart = h * 0.99f, yEnd = cy + R;
        for (int i = 0; i < UP; i++) {
            float f = (uph[i] + ts * 0.22f) % 1f;
            float e = (float) Math.pow(f, 1.7);
            float spread = w * 0.17f * (float) Math.pow(1f - e, 1.2);
            float x = cx + ux[i] * spread + (float) Math.sin(ts * 2.5f + uw[i]) * (1f - e) * w * 0.01f;
            float y = yStart + (yEnd - yStart) * e;
            float fadeIn = Math.min(1f, f * 6f);
            float fadeOut = 1f - Math.max(0f, (f - 0.8f) / 0.2f);
            int al = clamp((int) (230 * fadeIn * fadeOut));
            dot.setColor(Color.argb(al, 150, 205, 255));
            cv.drawCircle(x, y, w * 0.0032f * uz[i] * (1f - 0.5f * e), dot);
        }

        // Teks
        if (!Double.isNaN(tempC)) {
            text(cv, String.format(new Locale("id", "ID"), "%.1f °C", tempC), cx, cy - R * 0.50f, w * 0.036f, light, 0xFFD0D0D0, 0.1f);
        }
        text(cv, info.charging ? "MENGISI DAYA" : "ON BATTERY", cx, cy - R * 0.32f, w * 0.036f, light, 0xFFB5B5B5, 0.28f);

        String whole = info.whole >= 0 ? String.valueOf(info.whole) : "--";
        String frac = info.hundredths >= 0
            ? String.format(new Locale("id", "ID"), ",%02d%%", info.hundredths) : "%";
        float bigSize = w * 0.1265f, smallSize = w * 0.0575f;
        t.setLetterSpacing(0f);
        t.setTypeface(thin); t.setTextSize(bigSize);
        float wBig = t.measureText(whole);
        t.setTypeface(light); t.setTextSize(smallSize);
        float wSmall = t.measureText(frac);
        float x0 = cx - (wBig + wSmall) / 2f;
        float base = cy + R * 0.06f;
        t.setTypeface(thin); t.setTextSize(bigSize); t.setColor(0xFFFFFFFF);
        cv.drawText(whole, x0, base, t);
        t.setTypeface(light); t.setTextSize(smallSize); t.setColor(0xFFE6E6E6);
        cv.drawText(frac, x0 + wBig, base, t);

        if (info.charging && !Double.isNaN(info.watts)) {
            text(cv, String.format(Locale.US, "%.1f W", info.watts), cx, cy + R * 0.34f, w * 0.058f, medium, 0xFF8EC5FF, 0f);
            text(cv, "MASUK BATERAI", cx, cy + R * 0.50f, w * 0.03f, light, 0xFFB5B5B5, 0.2f);
            t.setTypeface(medium); t.setTextSize(w * 0.027f); t.setLetterSpacing(0.15f);
            float tw = t.measureText(info.tier);
            float pad = w * 0.035f, ph = w * 0.05f, py = cy + R * 0.70f;
            pill.setStrokeWidth(Math.max(1.5f, w * 0.003f));
            rect.set(cx - tw / 2 - pad, py - ph / 2, cx + tw / 2 + pad, py + ph / 2);
            cv.drawRoundRect(rect, ph / 2, ph / 2, pill);
            t.setColor(0xFFBFDFFF);
            cv.drawText(info.tier, cx - tw / 2, py + w * 0.01f, t);
        }

        fitText(cv, info.note, cx, h * 0.925f, w * 0.026f, w * 0.92f, light, 0xFF7A7A7A);
        String full = fullText();
        if (full != null) {
            fitText(cv, full, cx, h * 0.962f, w * 0.036f, w * 0.92f, medium, 0xFFD0D6DC);
        }
    }
        } k * segDeg, segDeg + 0.8f, false, arc);
            }
        }

        double headA = -Math.PI / 2 + 2 * Math.PI * shown;
        float hx = (float) (cx + R * Math.cos(headA)), hy = (float) (cy + R * Math.sin(headA));
        float pulse = 0.85f + 0.15f * (float) Math.sin(ts * 4f);
        if (shown > 0.003f) {
            dot.setColor(0x144FA3FF); cv.drawCircle(hx, hy, w * 0.045f * pulse, dot);
            dot.setColor(0x304FA3FF); cv.drawCircle(hx, hy, w * 0.028f * pulse, dot);
            dot.setColor(0x80BFDFFF); cv.drawCircle(hx, hy, w * 0.014f, dot);
            dot.setColor(0xFFFFFFFF); cv.drawCircle(hx, hy, w * 0.006f, dot);
        }

        for (int i = 0; i < SPARKS; i++) {
            double a = sa[i] + ss[i] * ts;
            float rr = R + sr[i] * w * 0.035f;
            float x = (float) (cx + rr * Math.cos(a)), y = (float) (cy + rr * Math.sin(a));
            double ang = (a + Math.PI / 2) % (2 * Math.PI);
            if (ang < 0) ang += 2 * Math.PI;
            float inArc = ang <= 2 * Math.PI * shown ? 1f : 0.2f;
            float tw = 0.5f + 0.5f * (float) Math.sin(ts * 3f + sp[i]);
            int al = (int) ((50 + 170 * tw) * inArc);
            dot.setColor(Color.argb(Math.max(0, Math.min(255, al)), 190, 225, 255));
            cv.drawCircle(x, y, w * 0.0025f * sz[i], dot);
        }

        float len = h * 0.10f;
        for (int i = 0; i < STREAM; i++) {
            float f = (pph[i] + ts * 0.25f) % 1f;
            float x = cx + px[i] * w * (0.004f + 0.012f * f) + (float) Math.sin(ts * 2f + pw[i]) * w * 0.002f;
            float y = cy + R + f * len;
            int al = (int) ((1f - f) * 210 * (0.6f + 0.4f * (float) Math.sin(ts * 5f + pw[i])));
            dot.setColor(Color.argb(Math.max(0, Math.min(255, al)), 130, 190, 255));
            cv.drawCircle(x, y, w * 0.0028f * pz[i] * (1f - f * 0.4f), dot);
        }

        if (!Double.isNaN(tempC)) {
            text(cv, String.format(new Locale("id", "ID"), "%.1f °C", tempC), cx, cy - R * 0.50f, w * 0.036f, light, 0xFFD0D0D0, 0.1f);
        }
        text(cv, info.charging ? "MENGISI DAYA" : "ON BATTERY", cx, cy - R * 0.32f, w * 0.036f, light, 0xFFB5B5B5, 0.28f);

        String whole = info.whole >= 0 ? String.valueOf(info.whole) : "--";
        String frac = info.hundredths >= 0
            ? String.format(new Locale("id", "ID"), ",%02d%%", info.hundredths) : "%";
        float smallSize = w * 0.115f, bigSize = smallSize * 1.1f;
        t.setLetterSpacing(0f);
        t.setTypeface(thin); t.setTextSize(bigSize);
        float wBig = t.measureText(whole);
        t.setTypeface(light); t.setTextSize(smallSize);
        float wSmall = t.measureText(frac);
        float x0 = cx - (wBig + wSmall) / 2f;
        float base = cy + R * 0.06f;
        t.setTypeface(thin); t.setTextSize(bigSize); t.setColor(0xFFFFFFFF);
        cv.drawText(whole, x0, base, t);
        t.setTypeface(light); t.setTextSize(smallSize); t.setColor(0xFFE6E6E6);
        cv.drawText(frac, x0 + wBig, base, t);

        if (info.charging && !Double.isNaN(info.watts)) {
            text(cv, String.format(Locale.US, "%.1f W", info.watts), cx, cy + R * 0.34f, w * 0.058f, medium, 0xFF8EC5FF, 0f);
            text(cv, "MASUK BATERAI", cx, cy + R * 0.50f, w * 0.03f, light, 0xFFB5B5B5, 0.2f);
            t.setTypeface(medium); t.setTextSize(w * 0.027f); t.setLetterSpacing(0.15f);
            float tw = t.measureText(info.tier);
            float pad = w * 0.035f, ph = w * 0.05f, py = cy + R * 0.70f;
            pill.setStrokeWidth(Math.max(1.5f, w * 0.003f));
            rect.set(cx - tw / 2 - pad, py - ph / 2, cx + tw / 2 + pad, py + ph / 2);
            cv.drawRoundRect(rect, ph / 2, ph / 2, pill);
            t.setColor(0xFFBFDFFF);
            cv.drawText(info.tier, cx - tw / 2, py + w * 0.01f, t);
        }

        // Peringatan hangat (kuning) di bawah lingkaran
        if (!Double.isNaN(tempC) && tempC >= WARM_C) {
            fitText(cv, "Baterai hangat, jadi pengisian melambat untuk melindunginya",
                cx, cy + R + h * 0.035f, w * 0.03f, w * 0.92f, medium, 0xFFFFD54F);
        }

        // Dua baris paling bawah: keterangan, lalu waktu penuh
        fitText(cv, info.note, cx, h * 0.925f, w * 0.026f, w * 0.92f, light, 0xFF7A7A7A);
        String full = fullText();
        if (full != null) {
            fitText(cv, full, cx, h * 0.962f, w * 0.036f, w * 0.92f, medium, 0xFFD0D6DC);
        }
    }
                                                         }
