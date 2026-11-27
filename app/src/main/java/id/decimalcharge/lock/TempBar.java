package id.decimalcharge.lock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/** Batang gradasi suhu dengan penanda suhu saat ini (skala 20 sampai 50 derajat Celsius). */
class TempBar extends View {
    private double val = Double.NaN;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rc = new RectF();
    private final float d;

    TempBar(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
    }

    void setValue(double v) {
        val = v;
        invalidate();
    }

    @Override protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float bh = 10 * d;
        float top = (h - bh) / 2f;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, w, 0,
            new int[]{0xFF2D7BFF, 0xFF22D3EE, 0xFF2ED573, 0xFFFFC107, 0xFFFF9800, 0xFFFF4D3D},
            new float[]{0f, 0.2f, 0.45f, 0.65f, 0.8f, 1f}, Shader.TileMode.CLAMP));
        rc.set(0, top, w, top + bh);
        cv.drawRoundRect(rc, bh / 2f, bh / 2f, p);
        p.setShader(null);
        if (!Double.isNaN(val)) {
            double f = Math.max(0.0, Math.min(1.0, (val - 20.0) / 30.0));
            float x = 9 * d + (float) f * (w - 18 * d);
            p.setColor(0xFFFFFFFF);
            cv.drawCircle(x, h / 2f, 9 * d, p);
            p.setColor(0xFF0A1128);
            cv.drawCircle(x, h / 2f, 5 * d, p);
        }
    }
}
