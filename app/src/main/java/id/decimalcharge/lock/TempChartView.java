package id.decimalcharge.lock;

import android.content.Context;
import android.graphics.Canvas;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TempChartView extends View {
    private static final long GAP_MS = 3L * 3600000L;
    private static final int SALMON = 0xFFF28B82, BLUE = 0xFF4DA3FF;

    private List<TempLog.Sample> data = new ArrayList<>();
    private long t0 = 0, t1 = 1;
    private boolean longRange = false;
    private float touchX = -1, downX, downY;
    private final int slop;
    private final Locale idl = new Locale("id", "ID");

    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lineP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint batP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tipBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("d MMM", idl);
    private final SimpleDateFormat tipShort = new SimpleDateFormat("HH.mm", idl);
    private final SimpleDateFormat tipLong = new SimpleDateFormat("d MMM HH.mm", idl);

    public TempChartView(Context c) {
        super(c);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        grid.setStyle(Paint.Style.STROKE);
        grid.setColor(0xFF2A3036);
        grid.setStrokeWidth(dp(1));
        lineP.setStyle(Paint.Style.STROKE);
        lineP.setColor(SALMON);
        lineP.setStrokeWidth(dp(1.8f));
        lineP.setStrokeJoin(Paint.Join.ROUND);
        batP.setStyle(Paint.Style.STROKE);
        batP.setColor(0x884DA3FF);
        batP.setStrokeWidth(dp(1.2f));
        batP.setStrokeJoin(Paint.Join.ROUND);
        fillP.setStyle(Paint.Style.FILL);
        dotP.setStyle(Paint.Style.FILL);
        tipBg.setStyle(Paint.Style.FILL);
        tipBg.setColor(0xFF1B2227);
        txt.setTextSize(dp(11));
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setData(List<TempLog.Sample> d, long from, long to) {
        data = d;
        t0 = from;
        t1 = Math.max(to, from + 1);
        longRange = (t1 - t0) > 36L * 3600000L;
        invalidate();
    }

    private float xOf(long t, float L, float pw) {
        return L + (float) ((t - t0) / (double) (t1 - t0)) * pw;
    }

    private Path build(boolean bat, double mn, double mx, float L, float T, float pw, float ph, Path fill) {
        Path p = new Path();
        boolean open = false;
        long prev = 0;
        float lastX = 0, firstX = 0, bottom = T + ph;
        for (TempLog.Sample s : data) {
            if (s.t < t0 || s.t > t1) continue;
            float x = xOf(s.t, L, pw);
            float y = bat ? T + (float) ((100.0 - s.level) / 100.0) * ph
                : T + (float) ((mx - s.temp / 10.0) / (mx - mn)) * ph;
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
            prev = s.t;
        }
        if (open && fill != null) { fill.lineTo(lastX, bottom); fill.lineTo(firstX, bottom); fill.close(); }
        return p;
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float L = dp(46), R = dp(14), T = dp(30), B = dp(26);
        float pw = w - L - R, ph = h - T - B;
        if (pw <= 0 || ph <= 0) return;

        int n = 0;
        double mn = 1e9, mx = -1e9;
        for (TempLog.Sample s : data) {
            if (s.t < t0 || s.t > t1) continue;
            n++;
            double v = s.temp / 10.0;
            if (v < mn) mn = v;
            if (v > mx) mx = v;
        }
        if (n == 0) { mn = 30; mx = 40; }
        if (mx - mn < 2) {
            double mid = (mx + mn) / 2;
            mn = mid - 1;
            mx = mid + 1;
        } else {
            mn = Math.floor(mn - 0.5);
            mx = Math.ceil(mx + 0.5);
        }

        // Garis bantu dan label suhu
        for (int k = 0; k < 3; k++) {
            float y = T + ph * k / 2f;
            cv.drawLine(L, y, w - R, y, grid);
            double val = mx - (mx - mn) * k / 2.0;
            txt.setColor(0xFF9AA5AB);
            txt.setTextAlign(Paint.Align.RIGHT);
            cv.drawText(String.format(idl, "%.1f", val), L - dp(6), y + dp(4), txt);
        }

        // Label jam atau tanggal
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(t0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (longRange) cal.set(Calendar.HOUR_OF_DAY, 0);
        if (cal.getTimeInMillis() < t0) cal.add(longRange ? Calendar.DAY_OF_MONTH : Calendar.HOUR_OF_DAY, 1);
        txt.setTextAlign(Paint.Align.CENTER);
        txt.setColor(0xFF9AA5AB);
        int guard = 0;
        while (cal.getTimeInMillis() <= t1 && guard++ < 40) {
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (longRange || hour % 6 == 0) {
                float x = xOf(cal.getTimeInMillis(), L, pw);
                String lb = longRange ? dayFmt.format(cal.getTime()) : String.format(Locale.US, "%02d.00", hour);
                cv.drawText(lb, x, h - dp(6), txt);
            }
            cal.add(longRange ? Calendar.DAY_OF_MONTH : Calendar.HOUR_OF_DAY, 1);
        }

        if (n == 0) {
            txt.setTextAlign(Paint.Align.CENTER);
            txt.setTextSize(dp(13));
            txt.setColor(0xFF59646A);
            cv.drawText("Belum ada data", L + pw / 2f, T + ph / 2f, txt);
            txt.setTextSize(dp(11));
            return;
        }

        // Garis baterai (tipis), lalu area dan garis suhu
        cv.drawPath(build(true, mn, mx, L, T, pw, ph, null), batP);
        Path fill = new Path();
        Path line = build(false, mn, mx, L, T, pw, ph, fill);
        fillP.setShader(new LinearGradient(0, T, 0, T + ph, 0x55F28B82, 0x00F28B82, Shader.TileMode.CLAMP));
        cv.drawPath(fill, fillP);
        cv.drawPath(line, lineP);

        // Titik di ujung kanan (data terakhir)
        TempLog.Sample last = null;
        for (TempLog.Sample s : data) {
            if (s.t >= t0 && s.t <= t1) last = s;
        }
        if (last != null) {
            float lx = xOf(last.t, L, pw);
            float ly = T + (float) ((mx - last.temp / 10.0) / (mx - mn)) * ph;
            dotP.setColor(SALMON);
            cv.drawCircle(lx, ly, dp(4), dotP);
        }

        // Sentuhan: tampilkan jam, suhu, dan persen baterai
        if (touchX >= 0) {
            TempLog.Sample near = null;
            float nx = 0, best = 1e9f;
            for (TempLog.Sample s : data) {
                if (s.t < t0 || s.t > t1) continue;
                float x = xOf(s.t, L, pw);
                float d = Math.abs(x - touchX);
                if (d < best) { best = d; near = s; nx = x; }
            }
            if (near != null) {
                float ny = T + (float) ((mx - near.temp / 10.0) / (mx - mn)) * ph;
                grid.setColor(0xFF9AA5AB);
                cv.drawLine(nx, T, nx, T + ph, grid);
                grid.setColor(0xFF2A3036);
                dotP.setColor(0xFFFFFFFF);
                cv.drawCircle(nx, ny, dp(5), dotP);
                dotP.setColor(SALMON);
                cv.drawCircle(nx, ny, dp(3), dotP);

                String time = (longRange ? tipLong : tipShort).format(new Date(near.t));
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
