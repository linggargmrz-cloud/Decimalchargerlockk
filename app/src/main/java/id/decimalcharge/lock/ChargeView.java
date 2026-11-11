package id.decimalcharge.lock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChargeView extends View {
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Typeface thin = Typeface.create("sans-serif-thin", Typeface.NORMAL);
    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("H:mm:ss", Locale.US);
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
    private BatteryReader info = new BatteryReader();
    private double tempC = Double.NaN;
    private long lastRead = 0;
    private final long t0 = System.currentTimeMillis();
    private long lastTouch = System.currentTimeMillis();
    private long dimDelay = 30000;
    private boolean dimmed = false;

    private final Runnable frame = new Runnable() {
        @Override public void run() { invalidate(); postDelayed(this, 33); }
    };

    public ChargeView(Context c) {
        super(c);
        setBackgroundColor(Color.BLACK);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(0xB0FFFFFF);
        pill.setStyle(Paint.Style.STROKE);
        pill.setColor(0xCCFFFFFF);
        dot.setStyle(Paint.Style.FILL);
        t.setTextAlign(Paint.Align.LEFT);
        try {
            long v = Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 30000);
            dimDelay = Math.max(5000, Math.min(120000, v));
        } catch (Throwable ignored) { }
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

    @Override protected void onDraw(Canvas cv) {
        long now = System.currentTimeMillis();
        if (now - lastRead > 1000) {
            info = BatteryReader.read(getContext());
            lastRead = now;
            try {
                Intent bi = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (bi != null) {
                    int tm = bi.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                    tempC = tm == Integer.MIN_VALUE ? Double.NaN : tm / 10.0;
                }
            } catch (Throwable ignored) { }
        }
        if (!dimmed && now - lastTouch > dimDelay) { dimmed = true; applyDim(true); }

        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        cv.drawColor(Color.BLACK);

        float cx = w / 2f, cy = h * 0.54f;
        float R = Math.min(w * 0.43f, h * 0.30f);
        Date d = new Date(now);

        text(cv, clockFmt.format(d), cx, h * 0.12f, w * 0.14f, thin, 0xFFEDEDED, 0f);
        text(cv, dateFmt.format(d), cx, h * 0.12f + w * 0.085f, w * 0.04f, light, 0xFFB5B5B5, 0.05f);

        ring.setStrokeWidth(Math.max(1.5f, w * 0.0035f));
        cv.drawCircle(cx, cy, R, ring);

        double period = 16000.0;
        double a0 = ((now - t0) % (long) period) / period * 2 * Math.PI - Math.PI * 0.75;
        for (int k = 14; k >= 0; k--) {
            double a = a0 - k * 0.035;
            float x = (float) (cx + R * Math.cos(a)), y = (float) (cy + R * Math.sin(a));
            int alpha = (int) (170 * (1 - k / 15f));
            dot.setColor(Color.argb(alpha, 255, 255, 255));
            cv.drawCircle(x, y, w * 0.004f * (1 - k / 22f), dot);
        }
        float hx = (float) (cx + R * Math.cos(a0)), hy = (float) (cy + R * Math.sin(a0));
        dot.setColor(0x14FFFFFF); cv.drawCircle(hx, hy, w * 0.026f, dot);
        dot.setColor(0x30FFFFFF); cv.drawCircle(hx, hy, w * 0.016f, dot);
        dot.setColor(0xFFFFFFFF); cv.drawCircle(hx, hy, w * 0.006f, dot);

        if (!Double.isNaN(tempC)) {
            text(cv, String.format(new Locale("id", "ID"), "%.1f °C", tempC), cx, cy - R * 0.50f, w * 0.036f, light, 0xFFD0D0D0, 0.1f);
        }
        text(cv, info.charging ? "CHARGING" : "ON BATTERY", cx, cy - R * 0.32f, w * 0.036f, light, 0xFFB5B5B5, 0.28f);

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
            text(cv, String.format(Locale.US, "%.1f W", info.watts), cx, cy + R * 0.34f, w * 0.058f, medium, 0xFFFFFFFF, 0f);
            text(cv, "INTO BATTERY", cx, cy + R * 0.50f, w * 0.03f, light, 0xFFB5B5B5, 0.2f);
            t.setTypeface(medium); t.setTextSize(w * 0.027f); t.setLetterSpacing(0.15f);
            float tw = t.measureText(info.tier);
            float pad = w * 0.035f, ph = w * 0.05f, py = cy + R * 0.70f;
            pill.setStrokeWidth(Math.max(1.5f, w * 0.003f));
            rect.set(cx - tw / 2 - pad, py - ph / 2, cx + tw / 2 + pad, py + ph / 2);
            cv.drawRoundRect(rect, ph / 2, ph / 2, pill);
            t.setColor(0xFFFFFFFF);
            cv.drawText(info.tier, cx - tw / 2, py + w * 0.01f, t);
        }

        text(cv, info.note, cx, h * 0.955f, w * 0.026f, light, 0xFF7A7A7A, 0.05f);
    }
            }
