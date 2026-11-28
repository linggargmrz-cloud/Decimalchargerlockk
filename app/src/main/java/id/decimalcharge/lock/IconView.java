package id.decimalcharge.lock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Ikon garis sederhana yang digambar sendiri (tanpa file gambar). */
public class IconView extends View {
    static final int HOME = 0, CHART = 1, GEAR = 2, INFO = 3, SHIELD = 4, BATTERY = 5, THERMO = 6,
        BELL = 7, GLOBE = 8, TERMINAL = 9, DOC = 10, CLOCK = 11, CHECK = 12, ARROW = 13,
        PLAY = 14, PAUSE = 15, STOP = 16, PLUS = 17, MINUS = 18, TOUCH = 19, ROTATE = 20,
        LOCK = 21, CLOSE = 22, GRIP = 23, EDIT = 24, PHONE = 25;

    private final int type;
    private int color;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    public IconView(Context c, int type, int color) {
        super(c);
        this.type = type;
        this.color = color;
    }

    public void setColor(int c) {
        if (c != color) {
            color = c;
            invalidate();
        }
    }

    private void bolt(Canvas cv, float cx, float cy, float sc) {
        path.reset();
        path.moveTo(cx + 1.2f * sc, cy - 5f * sc);
        path.lineTo(cx - 3f * sc, cy + 1f * sc);
        path.lineTo(cx - 0.2f * sc, cy + 1f * sc);
        path.lineTo(cx - 1.2f * sc, cy + 5f * sc);
        path.lineTo(cx + 3f * sc, cy - 1.2f * sc);
        path.lineTo(cx + 0.2f * sc, cy - 1.2f * sc);
        path.close();
        p.setStyle(Paint.Style.FILL);
        cv.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h);
        if (s <= 0) return;
        cv.save();
        cv.translate((w - s) / 2f, (h - s) / 2f);
        float u = s / 24f;
        cv.scale(u, u);
        p.setColor(color);
        p.setStrokeWidth(2f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStyle(Paint.Style.STROKE);
        path.reset();
        switch (type) {
            case HOME:
                path.moveTo(3, 11);
                path.lineTo(12, 3);
                path.lineTo(21, 11);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(5, 9.5f);
                path.lineTo(5, 20);
                path.lineTo(19, 20);
                path.lineTo(19, 9.5f);
                cv.drawPath(path, p);
                path.reset();
                path.moveTo(10, 20);
                path.lineTo(10, 14);
                path.lineTo(14, 14);
                path.lineTo(14, 20);
                cv.drawPath(path, p);
                break;
            case CHART:
                p.setStyle(Paint.Style.FILL);
                r.set(4, 12, 8, 20);
                cv.drawRoundRect(r, 1.2f, 1.2f, p);
                r.set(10, 5, 14, 20);
                cv.drawRoundRect(r, 1.2f, 1.2f, p);
                r.set(16, 9, 20, 20);
                cv.drawRoundRect(r, 1.2f, 1.2f, p);
                break;
            case GEAR:
                cv.drawCircle(12, 12, 3.2f, p);
                cv.drawCircle(12, 12, 7f, p);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
                    cv.drawLine(12 + 7f * cs, 12 + 7f * sn, 12 + 9.8f * cs, 12 + 9.8f * sn, p);
                }
                break;
            case INFO:
                cv.drawCircle(12, 12, 9, p);
                cv.drawLine(12, 11, 12, 17, p);
                p.setStyle(Paint.Style.FILL);
                cv.drawCircle(12, 7.6f, 1.2f, p);
                break;
            case SHIELD:
                path.moveTo(12, 3);
                path.lineTo(20, 6);
                path.lineTo(20, 12);
                path.cubicTo(20, 17, 16.5f, 20.5f, 12, 22);
                path.cubicTo(7.5f, 20.5f, 4, 17, 4, 12);
                path.lineTo(4, 6);
                path.close();
                cv.drawPath(path, p);
                bolt(cv, 12, 12.5f, 1f);
                break;
            case BATTERY:
                r.set(6, 5, 18, 21);
                cv.drawRoundRect(r, 2.5f, 2.5f, p);
                p.setStyle(Paint.Style.FILL);
                r.set(9.5f, 2.5f, 14.5f, 5);
                cv.drawRoundRect(r, 1f, 1f, p);
                bolt(cv, 12, 13.2f, 0.8f);
                break;
            case THERMO:
                r.set(9.5f, 3, 14.5f, 15);
                cv.drawRoundRect(r, 2.5f, 2.5f, p);
                cv.drawCircle(12, 17.5f, 3.6f, p);
                p.setStyle(Paint.Style.FILL);
                cv.drawCircle(12, 17.5f, 1.8f, p);
                p.setStyle(Paint.Style.STROKE);
                cv.drawLine(12, 8, 12, 16, p);
                break;
            case BELL:
                path.moveTo(6, 17);
                path.lineTo(6, 11);
                path.cubicTo(6, 7.7f, 8.7f, 5, 12, 5);
                path.cubicTo(15.3f, 5, 18, 7.7f, 18, 11);
                path.lineTo(18, 17);
                path.lineTo(20, 19);
                path.lineTo(4, 19);
                path.close();
                cv.drawPath(path, p);
                cv.drawLine(10.5f, 21.5f, 13.5f, 21.5f, p);
                cv.drawLine(12, 3, 12, 5, p);
                break;
            case GLOBE:
                cv.drawCircle(12, 12, 9, p);
                r.set(8, 3, 16, 21);
                cv.drawOval(r, p);
                cv.drawLine(3, 12, 21, 12, p);
                break;
            case TERMINAL:
                path.moveTo(5, 7);
                path.lineTo(11, 12);
                path.lineTo(5, 17);
                cv.drawPath(path, p);
                cv.drawLine(13, 17, 19, 17, p);
                break;
            case DOC:
                r.set(6, 3, 18, 21);
                cv.drawRoundRect(r, 2f, 2f, p);
                cv.drawLine(9, 9, 15, 9, p);
                cv.drawLine(9, 13, 15, 13, p);
                cv.drawLine(9, 17, 13, 17, p);
                break;
            case CLOCK:
                cv.drawCircle(12, 12, 9, p);
                path.moveTo(12, 7);
                path.lineTo(12, 12);
                path.lineTo(15.5f, 14);
                cv.drawPath(path, p);
                break;
            case CHECK:
                path.moveTo(5, 12.5f);
                path.lineTo(10, 17);
                path.lineTo(19, 7);
                cv.drawPath(path, p);
                break;
            case ARROW:
                path.moveTo(9, 5);
                path.lineTo(16, 12);
                path.lineTo(9, 19);
                cv.drawPath(path, p);
                break;
            case PLAY:
                path.moveTo(8, 5);
                path.lineTo(19, 12);
                path.lineTo(8, 19);
                path.close();
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(path, p);
                break;
            case PAUSE:
                p.setStyle(Paint.Style.FILL);
                r.set(7, 5, 10.5f, 19);
                cv.drawRoundRect(r, 1.2f, 1.2f, p);
                r.set(13.5f, 5, 17, 19);
                cv.drawRoundRect(r, 1.2f, 1.2f, p);
                break;
            case STOP:
                p.setStyle(Paint.Style.FILL);
                r.set(6, 6, 18, 18);
                cv.drawRoundRect(r, 2f, 2f, p);
                break;
            case PLUS:
                cv.drawLine(12, 5, 12, 19, p);
                cv.drawLine(5, 12, 19, 12, p);
                break;
            case MINUS:
                cv.drawLine(5, 12, 19, 12, p);
                break;
            case TOUCH:
                cv.drawCircle(12, 12, 8.5f, p);
                p.setStyle(Paint.Style.FILL);
                cv.drawCircle(12, 12, 3f, p);
                break;
            case ROTATE:
                r.set(5, 5, 19, 19);
                cv.drawArc(r, 40, 260, false, p);
                path.moveTo(18.1f, 7.4f);
                path.lineTo(16.6f, 4.0f);
                path.lineTo(14.4f, 7.8f);
                path.close();
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(path, p);
                break;
            case LOCK:
                r.set(6, 11, 18, 21);
                cv.drawRoundRect(r, 2.5f, 2.5f, p);
                r.set(8.5f, 4.5f, 15.5f, 11.5f);
                cv.drawArc(r, 180, 180, false, p);
                cv.drawLine(8.5f, 8, 8.5f, 11, p);
                cv.drawLine(15.5f, 8, 15.5f, 11, p);
                break;
            case CLOSE:
                cv.drawLine(6, 6, 18, 18, p);
                cv.drawLine(18, 6, 6, 18, p);
                break;
            case GRIP:
                p.setStyle(Paint.Style.FILL);
                cv.drawCircle(9, 7, 1.4f, p);
                cv.drawCircle(15, 7, 1.4f, p);
                cv.drawCircle(9, 12, 1.4f, p);
                cv.drawCircle(15, 12, 1.4f, p);
                cv.drawCircle(9, 17, 1.4f, p);
                cv.drawCircle(15, 17, 1.4f, p);
                break;
            case EDIT:
                path.moveTo(4, 20);
                path.lineTo(5, 15);
                path.lineTo(15.5f, 4.5f);
                path.lineTo(19.5f, 8.5f);
                path.lineTo(9, 19);
                path.close();
                cv.drawPath(path, p);
                break;
            case PHONE:
                r.set(7, 2.5f, 17, 21.5f);
                cv.drawRoundRect(r, 2.5f, 2.5f, p);
                cv.drawLine(10.5f, 18.5f, 13.5f, 18.5f, p);
                break;
            default:
                break;
        }
        cv.restore();
    }
}
