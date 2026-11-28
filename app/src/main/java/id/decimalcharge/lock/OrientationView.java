package id.decimalcharge.lock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Indikator visual orientasi layar: ponsel kecil yang berputar mengikuti rotasi sebenarnya. */
class OrientationView extends View {
    private float shown = 0f, target = 0f;
    private int color = 0xFF4D9BFF;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final RectF r2 = new RectF();
    private ValueAnimator anim;
    private final float d;

    OrientationView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
    }

    void set(float degrees, int col) {
        color = col;
        if (Math.abs(degrees - target) < 0.1f) {
            invalidate();
            return;
        }
        float diff = ((degrees - shown + 540f) % 360f) - 180f;
        target = degrees;
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(shown, shown + diff);
        anim.setDuration(300);
        anim.addUpdateListener(a -> {
            shown = (Float) a.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h);
        if (s <= 0) return;
        float cx = w / 2f, cy = h / 2f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f * d);
        p.setColor(0xFF1C2B58);
        cv.drawCircle(cx, cy, s * 0.46f, p);
        cv.save();
        cv.rotate(shown, cx, cy);
        float pw = s * 0.30f, ph = s * 0.58f;
        r.set(cx - pw / 2f, cy - ph / 2f, cx + pw / 2f, cy + ph / 2f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF0C1430);
        cv.drawRoundRect(r, 8 * d, 8 * d, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3 * d);
        p.setColor(color);
        cv.drawRoundRect(r, 8 * d, 8 * d, p);
        // penanda sisi atas ponsel
        r2.set(cx - pw * 0.18f, r.top + 8 * d, cx + pw * 0.18f, r.top + 11 * d);
        p.setStyle(Paint.Style.FILL);
        cv.drawRoundRect(r2, 2 * d, 2 * d, p);
        cv.restore();
    }
}
