package id.decimalcharge.lock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TempChartView extends View {
    private static final long GAP_MS = 25L * 60000L;
    private static final long[] STEPS = {60000L, 120000L, 300000L, 600000L, 900000L, 1800000L,
        3600000L, 7200000L, 10800000L, 21600000L, 43200000L, 86400000L, 172800000L};
    private static final int GREEN = 0xFF4CD08A, RED = 0xFFFF5A5A, BLUE = 0xFF4DA3FF;
    private static final double LIMIT = 40.0, CUT = 39.95;

    private List<TempLog.Sample> data = new ArrayList<>();
    private long reqFrom = 0, reqTo = 1, t0 = 0, t1 = 1;
    private float touchX = -1, downX, downY;
    private final int slop;
    private final Locale idl = new Locale("id", "ID");

    private float pl, pt, pw, ph;
    private double yMinT, yMaxT, stepT;
    private int bMin, bMax, nGrid;

    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ref = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("d MMM", idl);
    private final SimpleDateFormat hmFmt = new SimpleDateFormat("HH.mm", idl);
    private final SimpleDateFormat hmsFmt = new SimpleDateFormat("HH.mm.ss", idl);
    private final SimpleDateFormat dhmFmt = new SimpleDateFormat("d MMM HH.mm", idl);

    public TempChartView(Context c) {
        super(c);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        grid.setStyle(Paint.Style.STROKE);
        grid.setColor(0xFF2A3036);
        grid.setStrokeWidth(dp(1));
        ref.setStyle(Paint.Style.STROKE);
        ref.setColor(0x66FF5A5A);
        ref.setStrokeWidth(dp(1));
        ref.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(5)}, 0));
        lineP.setStyle(Paint.Style.STROKE);
        lineP.setStrokeWidth(dp(2.2f));
        lineP.setStrokeJoin(Paint.Join.ROUND);
        lineP.setStrokeCap(Paint.Cap.ROUND);
        batP.setStyle(Paint.Style.STROKE);
        batP.setColor(BLUE);
        batP.setStrokeWidth(dp(1.8f));
        batP.setStrokeJoin(Paint.Join.ROUND);
        batP.setStrokeCap(Paint.Cap.ROUND);
        fillP.setStyle(Paint.Style.FILL);
        dotP.setStyle(Paint.Style.FILL);
        tipBg.setStyle(Paint.Style.FILL);
        tipBg.setColor(0xFF1B2227);
        txt.setTextSize(dp(11));
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setData(List<TempLog.Sample> d, long from, long to) {
        data = d;
        reqFrom = from;
        reqTo = Math.max(to, from + 1);
        invalidate();
    }

    private float xOf(long t) { return pl + (float) ((t - t0) / (double) (t1 - t0)) * pw; }

    private float yT(double v) { return pt + (float) ((yMaxT - v) / (yMaxT - yMinT)) * ph; }

    private float yB(int lv) { return pt + (float) ((bMax - lv) / (double) (bMax - bMin)) * ph; }

    private Path build(boolean bat, Path fill, float[] end) {
        Path p = new Path();
        boolean open = false;
        long prev = 0;
        float lastX = 0, lastY = 0, firstX = 0, bottom = pt + ph;
        end[0] = -1;
        end[1] = -1;
        for (TempLog.Sample s : data) {
            if (s.t < t0 || s.t > t1) continue;
            float x = xOf(s.t);
            float y = bat ? yB(s.level) : yT(s.temp / 10.0);
            if (!open || s.t - prev > GAP_MS) {
                if (open && fill != null) { fill.lineTo(lastX, bottom); fill.lineTo(firstX, bottom); fill.close(); }
                p.moveTo(x, y);
                if (fill != null) fill.moveTo(x, y);
                firstX = x;
                open = true;
            } else {
                p.lineTo(x, y);
                if (fill != null) fill.lineTo(x, y);
            }
            lastX = x;
            lastY = y;
            prev = s.t;
        }
        if (open) {
            if (t1 > prev && t1 - prev <= GAP_MS) {
                float ex = xOf(t1);
                p.lineTo(ex, lastY);
                if (fill != null) fill.lineTo(ex, lastY);
                lastX = ex;
            }
            if (fill != null) { fill.lineTo(lastX, bottom); fill.lineTo(firstX, bottom); fill.close(); }
            end[0] = lastX;
            end[1] = lastY;
        }
        return p;
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        pl = dp(44);
        float pr = dp(40);
        pt = dp(34);
        float pb = dp(26);
        pw = w - pl - pr;
        ph = h - pt - pb;
        if (pw <= 0 || ph <= 0) return;

        // Jendela waktu menyesuaikan data supaya grafik lebar
        long a = reqFrom;
        if (!data.isEmpty()) {
            long f = data.get(0).t;
            if (f > a) a = f;
        }
        long b = reqTo;
        if (b - a < 180000L) a = b - 180000L;
        t0 = a;
        t1 = b;

        int n = 0;
        double mnT = 1e9, mxT = -1e9;
        int mnL = 101, mxL = -1;
        for (TempLog.Sample s : data) {
            if (s.t < t0 || s.t > t1) continue;
            n++;
            double v = s.temp / 10.0;
            if (v < mnT) mnT = v;
            if (v > mxT) mxT = v;
            if (s.level < mnL) mnL = s.level;
            if (s.level > mxL) mxL = s.level;
        }
        if (n == 0) { mnT = 30; mxT = 40; mnL = 0; mxL = 100; }

        // Skala suhu
        double lowT = mnT - 0.2, highT = mxT + 0.2;
        double raw = Math.max(highT - lowT, 1.0);
        double[] steps = {0.5, 1, 2, 5, 10, 20};
        stepT = steps[steps.length - 1];
        for (double s : steps) {
            if (raw / s <= 3.0) { stepT = s; break; }
        }
        yMinT = Math.floor(lowT / stepT) * stepT;
        yMaxT = Math.ceil(highT / stepT) * stepT;
        while ((yMaxT - yMinT) / stepT < 2.0 - 1e-9) yMaxT += stepT;
        nGrid = (int) Math.round((yMaxT - yMinT) / stepT);

        // Skala baterai
        int bLow = Math.max(0, mnL - 1), bHigh = Math.min(100, mxL + 1);
        int bStep = Math.max(1, (int) Math.ceil((bHigh - bLow) / (double) nGrid));
        bMin = bLow;
        bMax = bMin + bStep * nGrid;
        if (bMax > 100) {
            int sh = bMax - 100;
            bMin = Math.max(0, bMin - sh);
            bMax = Math.min(100, bMin + bStep * nGrid);
        }
        if (bMax <= bMin) bMax = bMin + 1;

        // Garis bantu dan label kiri (suhu), kanan (baterai)
        for (int k = 0; k <= nGrid; k++) {
            double val = yMinT + stepT * k;
            float y = yT(val);
            cv.drawLine(pl, y, w - pr, y, grid);
            txt.setTextAlign(Paint.Align.RIGHT);
            txt.setColor(0xFF9AA5AB);
            cv.drawText(stepT < 1 ? String.format(idl, "%.1f", val) : String.format(idl, "%.0f", val),
                pl - dp(6), y + dp(4), txt);
            if (n > 0) {
                int bv = Math.round(bMin + (bMax - bMin) * (float) k / nGrid);
                txt.setTextAlign(Paint.Align.LEFT);
                txt.setColor(BLUE);
                cv.drawText(bv + "%", w - pr + dp(6), y + dp(4), txt);
            }
        }

        // Label waktu
        long span = t1 - t0;
        long step = STEPS[STEPS.length - 1];
        for (long s : STEPS) {
            if (span / s <= 5) { step = s; break; }
        }
        long off = TimeZone.getDefault().getOffset(t0);
        long first = ((t0 + off + step - 1) / step) * step - off;
        txt.setTextAlign(Paint.Align.LEFT);
        txt.setColor(0xFF9AA5AB);
        long tk = first;
        for (int g = 0; tk <= t1 && g < 12; g++) {
            long local = tk + off;
            String lb = (step >= 86400000L || local % 86400000L == 0)
                ? dayFmt.format(new Date(tk)) : hmFmt.format(new Date(tk));
            float tw = txt.measureText(lb);
            float lx = Math.max(pl, Math.min(xOf(tk) - tw / 2f, w - pr - tw));
            cv.drawText(lb, lx, h - dp(6), txt);
            tk += step;
        }

        if (n == 0) {
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setTextSize(dp(13));
            txt.setColor(0xFF59646A);
            cv.drawText("Belum ada data", pl + pw / 2f, pt + ph / 2f, txt);
            txt.setTextSize(dp(11));
            return;
        }

        // Garis batas 40 °C
        if (yMinT < LIMIT && LIMIT < yMaxT) {
            float y = yT(LIMIT);
            cv.drawLine(pl, y, w - pr, y, ref);
            txt.setTextAlign(Paint.Align.RIGHT);
            txt.setColor(0xAAFF5A5A);
            cv.drawText("40 °C", w - pr - dp(4), y - dp(4), txt);
        }

        // Baterai (biru)
        float[] eb = new float[2];
        cv.drawPath(build(true, null, eb), batP);

        // Suhu: hijau di bawah 40 °C, merah mulai 40 °C
        float[] et = new float[2];
        Path fill = new Path();
        Path tp = build(false, fill, et);
        if (yMaxT <= CUT) {
            fillP.setShader(new LinearGradient(0, pt, 0, pt + ph,
                new int[]{0x554CD08A, 0x084CD08A}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            lineP.setShader(null);
            lineP.setColor(GREEN);
        } else if (yMinT >= CUT) {
            fillP.setShader(new LinearGradient(0, pt, 0, pt + ph,
                new int[]{0x66FF5A5A, 0x08FF5A5A}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
            lineP.setShader(null);
            lineP.setColor(RED);
        } else {
            float p = (yT(CUT) - pt) / ph;
            fillP.setShader(new LinearGradient(0, pt, 0, pt + ph,
                new int[]{0x66FF5A5A, 0x40FF5A5A, 0x404CD08A, 0x084CD08A},
                new float[]{0f, p, p, 1f}, Shader.TileMode.CLAMP));
            lineP.setShader(new LinearGradient(0, pt, 0, pt + ph,
                new int[]{RED, RED, GREEN, GREEN}, new float[]{0f, p, p, 1f}, Shader.TileMode.CLAMP));
        }
        cv.drawPath(fill, fillP);
        cv.drawPath(tp, lineP);

        // Titik di ujung kanan
        TempLog.Sample last = null;
        for (TempLog.Sample s : data) {
            if (s.t >= t0 && s.t <= t1) last = s;
        }
        if (et[0] >= 0 && last != null) {
            dotP.setColor(last.temp / 10.0 >= LIMIT ? RED : GREEN);
            cv.drawCircle(et[0], et[1], dp(4.5f), dotP);
        }
        if (eb[0] >= 0) {
            dotP.setColor(BLUE);
            cv.drawCircle(eb[0], eb[1], dp(3.5f), dotP);
        }

        // Sentuhan: jam, suhu, persen baterai
        if (touchX >= 0) {
            TempLog.Sample near = null;
            float nx = 0, best = 1e9f;
            for (TempLog.Sample s : data) {
                if (s.t < t0 || s.t > t1) continue;
                float x = xOf(s.t);
                float d = Math.abs(x - touchX);
                if (d < best) { best = d; near = s; nx = x; }
            }
            if (near != null) {
                float ny = yT(near.temp / 10.0), nyb = yB(near.level);
                grid.setColor(0xFF9AA5AB);
                cv.drawLine(nx, pt, nx, pt + ph, grid);
                grid.setColor(0xFF2A3036);
                dotP.setColor(BLUE);
                cv.drawCircle(nx, nyb, dp(4), dotP);
                dotP.setColor(0xFFFFFFFF);
                cv.drawCircle(nx, ny, dp(5.5f), dotP);
                dotP.setColor(near.temp / 10.0 >= LIMIT ? RED : GREEN);
                cv.drawCircle(nx, ny, dp(3.5f), dotP);

                String time = (span > 20L * 3600000L ? dhmFmt : hmsFmt).format(new Date(near.t));
                String tip = time + "  ·  " + String.format(idl, "%.1f °C", near.temp / 10.0)
                    + "  ·  " + near.level + "%" + (near.chg ? "  ·  mengisi" : "");
                txt.setTextAlign(Paint.Align.LEFT);
                txt.setTextSize(dp(12));
                float tw = txt.measureText(tip);
                float bx = Math.max(dp(8), Math.min(nx - tw / 2f, w - tw - dp(8)));
                rect.set(bx - dp(8), dp(2), bx + tw + dp(8), dp(26));
                cv.drawRoundRect(rect, dp(8), dp(8), tipBg);
                txt.setColor(0xFFFFFFFF);
                cv.drawText(tip, bx, dp(18), txt);
                txt.setTextSize(dp(11));
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                touchX = e.getX();
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = Math.abs(e.getX() - downX), dy = Math.abs(e.getY() - downY);
                if (dx > dy && dx > slop && getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                touchX = e.getX();
                invalidate();
                return true;
            }
            default:
                touchX = -1;
                invalidate();
                return true;
        }
    }
        }
