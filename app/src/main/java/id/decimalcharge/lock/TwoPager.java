package id.decimalcharge.lock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

/** Dua halaman yang bisa digeser ke samping (tanpa library tambahan). */
public class TwoPager extends FrameLayout {
    interface Listener {
        void onScroll(float pos);
        void onSelected(int page);
    }

    private float pos = 0f;
    private int page = 0;
    private float downX, downY, lastX;
    private boolean dragging;
    private VelocityTracker vt;
    private ValueAnimator anim;
    private Listener listener;
    private final int slop;

    public TwoPager(Context c) {
        super(c);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
    }

    void setListener(Listener l) { listener = l; }
    float getPos() { return pos; }
    int getPage() { return page; }

    void setPage(int p, boolean animate) {
        page = p;
        if (anim != null) anim.cancel();
        if (!animate || getWidth() == 0) {
            pos = p;
            apply();
            if (listener != null) listener.onSelected(p);
            return;
        }
        anim = ValueAnimator.ofFloat(pos, p);
        anim.setDuration(240);
        anim.addUpdateListener(a -> {
            pos = (Float) a.getAnimatedValue();
            apply();
        });
        anim.start();
        if (listener != null) listener.onSelected(p);
    }

    private void apply() {
        int w = getWidth();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).setTranslationX((i - pos) * w);
        }
        if (listener != null) listener.onScroll(pos);
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        apply();
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if (vt == null) vt = VelocityTracker.obtain();
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) vt.clear();
        vt.addMovement(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                lastX = e.getX();
                downY = e.getY();
                dragging = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.4f) {
                    dragging = true;
                    lastX = e.getX();
                    if (anim != null) anim.cancel();
                    return true;
                }
                break;
            }
            default:
                break;
        }
        return false;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (vt == null) vt = VelocityTracker.obtain();
        vt.addMovement(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                lastX = e.getX();
                downY = e.getY();
                return true;
            case MotionEvent.ACTION_MOVE: {
                if (!dragging) {
                    float dx = e.getX() - downX, dy = e.getY() - downY;
                    if (Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy)) {
                        dragging = true;
                        lastX = e.getX();
                        if (anim != null) anim.cancel();
                    } else {
                        return true;
                    }
                }
                float step = e.getX() - lastX;
                lastX = e.getX();
                float w = Math.max(1, getWidth());
                pos = Math.max(0f, Math.min(1f, pos - step / w));
                apply();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (dragging) {
                    vt.computeCurrentVelocity(1000);
                    float v = vt.getXVelocity();
                    int target = pos > 0.5f ? 1 : 0;
                    if (Math.abs(v) > 900f) target = v < 0 ? 1 : 0;
                    dragging = false;
                    setPage(target, true);
                }
                vt.recycle();
                vt = null;
                return true;
            }
            default:
                return true;
        }
    }
    }
