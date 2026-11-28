package id.decimalcharge.lock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/**
 * Auto clicker multi-sentuhan (1 sampai 10 titik). Memakai layanan aksesibilitas hanya untuk
 * mengirim sentuhan yang diatur pengguna. Layanan ini tidak membaca isi layar.
 */
public class ClickerService extends AccessibilityService {
    static volatile ClickerService instance;
    static final int IDLE = 0, RUNNING = 1, PAUSED = 2;
    static volatile int state = IDLE;
    static volatile int cycles = 0;
    static volatile String lastError = "";

    private static final int BASE_FLAGS = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

    private final Handler h = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private final List<Marker> markers = new ArrayList<>();
    private Control control;
    private WindowManager.LayoutParams ctlLp;
    private boolean overlayShown = false;
    private boolean editMode = true;
    private int seqIdx = 0, failures = 0;

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    // ---- siklus layanan ----
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        state = IDLE;
        cycles = 0;
        lastError = "";
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override public void onInterrupt() { }

    @Override public boolean onUnbind(android.content.Intent intent) {
        shutdown();
        instance = null;
        return super.onUnbind(intent);
    }

    @Override public void onDestroy() {
        shutdown();
        instance = null;
        super.onDestroy();
    }

    private void shutdown() {
        state = IDLE;
        h.removeCallbacksAndMessages(null);
        hideOverlay();
    }

    // ---- penampil titik dan kontrol ----
    private int flags() {
        return BASE_FLAGS | (editMode ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    void showOverlay() {
        if (wm == null || overlayShown) return;
        overlayShown = true;
        editMode = state != RUNNING;
        int n = ClickerConfig.count(this);
        for (int i = 0; i < n; i++) addMarker(i);
        addControl();
        updateControl();
    }

    void hideOverlay() {
        overlayShown = false;
        for (Marker m : markers) {
            try { wm.removeView(m); } catch (Throwable ignored) { }
        }
        markers.clear();
        if (control != null) {
            try { wm.removeView(control); } catch (Throwable ignored) { }
            control = null;
        }
    }

    boolean isOverlayShown() { return overlayShown; }

    private void addMarker(int i) {
        try {
            Marker m = new Marker(this, i);
            float[] p = ClickerConfig.pos(this, i);
            m.lp = new WindowManager.LayoutParams(m.size, m.size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, flags(), PixelFormat.TRANSLUCENT);
            m.lp.gravity = Gravity.TOP | Gravity.START;
            m.lp.x = Math.round(p[0] - m.size / 2f);
            m.lp.y = Math.round(p[1] - m.size / 2f);
            wm.addView(m, m.lp);
            markers.add(m);
        } catch (Throwable t) {
            lastError = "Gagal menampilkan titik";
        }
    }

    private void addControl() {
        try {
            control = new Control(this);
            ctlLp = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                BASE_FLAGS, PixelFormat.TRANSLUCENT);
            ctlLp.gravity = Gravity.TOP | Gravity.START;
            SharedPreferences s = getSharedPreferences("clk", Context.MODE_PRIVATE);
            ctlLp.x = s.getInt("cx", dp(8));
            ctlLp.y = s.getInt("cy", dp(140));
            wm.addView(control, ctlLp);
        } catch (Throwable t) {
            control = null;
            lastError = "Gagal menampilkan kontrol";
        }
    }

    private void refreshFlags() {
        for (Marker m : markers) {
            if (m.lp == null) continue;
            m.lp.flags = flags();
            try { wm.updateViewLayout(m, m.lp); } catch (Throwable ignored) { }
            m.invalidate();
        }
    }

    void syncCount() {
        if (!overlayShown) return;
        int n = ClickerConfig.count(this);
        while (markers.size() > n) {
            Marker m = markers.remove(markers.size() - 1);
            try { wm.removeView(m); } catch (Throwable ignored) { }
        }
        while (markers.size() < n) {
            int before = markers.size();
            addMarker(before);
            if (markers.size() == before) break;
        }
        updateControl();
    }

    void relayout() {
        for (int i = 0; i < markers.size(); i++) {
            Marker m = markers.get(i);
            float[] p = ClickerConfig.pos(this, i);
            m.lp.x = Math.round(p[0] - m.size / 2f);
            m.lp.y = Math.round(p[1] - m.size / 2f);
            try { wm.updateViewLayout(m, m.lp); } catch (Throwable ignored) { }
        }
    }

    void updateControl() {
        if (control != null) control.refresh();
        for (Marker m : markers) m.invalidate();
    }

    // ---- mesin klik ----
    void start() {
        if (state == RUNNING) return;
        if (state == IDLE) {
            cycles = 0;
            seqIdx = 0;
        }
        failures = 0;
        lastError = "";
        state = RUNNING;
        editMode = false;
        refreshFlags();
        updateControl();
        h.removeCallbacksAndMessages(null);
        h.post(tick);
    }

    void pause() {
        if (state != RUNNING) return;
        state = PAUSED;
        h.removeCallbacksAndMessages(null);
        updateControl();
    }

    void stop() {
        state = IDLE;
        h.removeCallbacksAndMessages(null);
        editMode = true;
        refreshFlags();
        updateControl();
    }

    private void fail(String msg) {
        stop();
        lastError = msg;
        toast(msg);
    }

    private GestureDescription.StrokeDescription stroke(int i, int hold) {
        float[] p = ClickerConfig.pos(this, i);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float x = Math.max(1f, Math.min(dm.widthPixels - 1f, p[0]));
        float y = Math.max(1f, Math.min(dm.heightPixels - 1f, p[1]));
        Path path = new Path();
        path.moveTo(x, y);
        return new GestureDescription.StrokeDescription(path, 0, Math.max(1, hold));
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (state != RUNNING) return;
            final int n = ClickerConfig.count(ClickerService.this);
            final int hold = ClickerConfig.hold(ClickerService.this);
            final int interval = ClickerConfig.interval(ClickerService.this);
            final int repeat = ClickerConfig.repeat(ClickerService.this);
            final boolean sim = ClickerConfig.simultaneous(ClickerService.this);
            if (repeat > 0 && cycles >= repeat) {
                stop();
                return;
            }
            GestureDescription.Builder b = new GestureDescription.Builder();
            final int pulseIdx;
            try {
                if (sim) {
                    for (int i = 0; i < n; i++) b.addStroke(stroke(i, hold));
                    pulseIdx = -1;
                } else {
                    if (seqIdx >= n) seqIdx = 0;
                    b.addStroke(stroke(seqIdx, hold));
                    pulseIdx = seqIdx;
                }
            } catch (Throwable t) {
                fail("Gagal membuat sentuhan: " + t.getMessage());
                return;
            }
            boolean ok = ClickerService.this.dispatchGesture(b.build(), new AccessibilityService.GestureResultCallback() {
                @Override public void onCompleted(GestureDescription g) {
                    failures = 0;
                    if (state != RUNNING) return;
                    if (sim) {
                        cycles++;
                    } else {
                        seqIdx++;
                        if (seqIdx >= n) {
                            seqIdx = 0;
                            cycles++;
                        }
                    }
                    updateControl();
                    h.postDelayed(tick, Math.max(1, interval));
                }

                @Override public void onCancelled(GestureDescription g) {
                    failures++;
                    if (failures >= 5) fail("Sentuhan dibatalkan berulang kali");
                    else if (state == RUNNING) h.postDelayed(tick, Math.max(interval, 100));
                }
            }, null);
            if (!ok) {
                failures++;
                if (failures >= 5) fail("Sistem menolak sentuhan");
                else h.postDelayed(tick, Math.max(interval, 100));
            } else {
                for (Marker m : markers) {
                    if (pulseIdx < 0 || m.idx == pulseIdx) m.pulse();
                }
            }
        }
    };

    // ---- titik sentuh ----
    private class Marker extends View {
        final int idx;
        final int size;
        WindowManager.LayoutParams lp;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float sx, sy;
        private int slx, sly;

        Marker(Context c, int idx) {
            super(c);
            this.idx = idx;
            this.size = dp(46);
        }

        void pulse() {
            animate().scaleX(1.35f).scaleY(1.35f).setDuration(70)
                .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(110).start()).start();
        }

        @Override protected void onDraw(Canvas cv) {
            float r = getWidth() / 2f;
            int col = state == RUNNING ? 0xFF2ED573 : (editMode ? 0xFF4D9BFF : 0xFF22D3EE);
            p.setStyle(Paint.Style.FILL);
            p.setColor((col & 0x00FFFFFF) | 0x55000000);
            cv.drawCircle(r, r, r - dp(2), p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            p.setColor(col);
            cv.drawCircle(r, r, r - dp(2), p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFFFFF);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(dp(15));
            p.setFakeBoldText(true);
            cv.drawText(String.valueOf(idx + 1), r, r + dp(5), p);
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (!editMode || lp == null) return false;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    sx = e.getRawX();
                    sy = e.getRawY();
                    slx = lp.x;
                    sly = lp.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = slx + Math.round(e.getRawX() - sx);
                    lp.y = sly + Math.round(e.getRawY() - sy);
                    try { wm.updateViewLayout(this, lp); } catch (Throwable ignored) { }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    ClickerConfig.setPos(ClickerService.this, idx, lp.x + size / 2f, lp.y + size / 2f);
                    return true;
                default:
                    return true;
            }
        }
    }

    // ---- panel kontrol melayang ----
    private class Control extends LinearLayout {
        private final IconView play, pause, edit;
        private final TextView status;

        Control(Context c) {
            super(c);
            setOrientation(VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(0xEE0C1430);
            bg.setStroke(dp(1), 0xFF1C2B58);
            setBackground(bg);
            setPadding(dp(8), dp(6), dp(8), dp(6));

            LinearLayout row = new LinearLayout(c);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            IconView grip = new IconView(c, IconView.GRIP, 0xFF9AA5AB);
            row.addView(grip, new LinearLayout.LayoutParams(dp(26), dp(40)));

            play = new IconView(c, IconView.PLAY, 0xFF2ED573);
            pause = new IconView(c, IconView.PAUSE, 0xFFFFC107);
            FrameLayout pb = circle();
            pb.addView(play, center(dp(20)));
            pb.addView(pause, center(dp(20)));
            pb.setOnClickListener(v -> { if (state == RUNNING) pause(); else start(); });
            row.addView(pb, cp());

            FrameLayout sb = circle();
            sb.addView(new IconView(c, IconView.STOP, 0xFFFF5252), center(dp(20)));
            sb.setOnClickListener(v -> stop());
            row.addView(sb, cp());

            edit = new IconView(c, IconView.EDIT, 0xFF4D9BFF);
            FrameLayout eb = circle();
            eb.addView(edit, center(dp(20)));
            eb.setOnClickListener(v -> {
                if (state == RUNNING) return;
                editMode = !editMode;
                refreshFlags();
                updateControl();
            });
            row.addView(eb, cp());

            FrameLayout ab = circle();
            ab.addView(new IconView(c, IconView.PLUS, 0xFFFFFFFF), center(dp(20)));
            ab.setOnClickListener(v -> {
                if (state == RUNNING) return;
                int n = ClickerConfig.count(ClickerService.this);
                if (n < ClickerConfig.MAX) {
                    ClickerConfig.setCount(ClickerService.this, n + 1);
                    syncCount();
                }
            });
            row.addView(ab, cp());

            FrameLayout mb = circle();
            mb.addView(new IconView(c, IconView.MINUS, 0xFFFFFFFF), center(dp(20)));
            mb.setOnClickListener(v -> {
                if (state == RUNNING) return;
                int n = ClickerConfig.count(ClickerService.this);
                if (n > 1) {
                    ClickerConfig.setCount(ClickerService.this, n - 1);
                    syncCount();
                }
            });
            row.addView(mb, cp());

            FrameLayout xb = circle();
            xb.addView(new IconView(c, IconView.CLOSE, 0xFF9AA5AB), center(dp(18)));
            xb.setOnClickListener(v -> {
                stop();
                hideOverlay();
            });
            row.addView(xb, cp());

            addView(row);
            status = new TextView(c);
            status.setTextSize(11);
            status.setTextColor(0xFFA9B8D8);
            status.setPadding(dp(4), dp(2), 0, 0);
            addView(status);

            final float[] st = new float[4];
            grip.setOnTouchListener((v, e) -> {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        st[0] = e.getRawX();
                        st[1] = e.getRawY();
                        st[2] = ctlLp.x;
                        st[3] = ctlLp.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        ctlLp.x = (int) (st[2] + e.getRawX() - st[0]);
                        ctlLp.y = (int) (st[3] + e.getRawY() - st[1]);
                        try { wm.updateViewLayout(Control.this, ctlLp); } catch (Throwable ignored) { }
                        return true;
                    case MotionEvent.ACTION_UP:
                        getSharedPreferences("clk", Context.MODE_PRIVATE).edit()
                            .putInt("cx", ctlLp.x).putInt("cy", ctlLp.y).apply();
                        return true;
                    default:
                        return true;
                }
            });
            refresh();
        }

        private FrameLayout circle() {
            FrameLayout f = new FrameLayout(getContext());
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(0xFF141E3E);
            f.setBackground(g);
            return f;
        }

        private LinearLayout.LayoutParams cp() {
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(38), dp(38));
            l.setMargins(dp(3), 0, dp(3), 0);
            return l;
        }

        private FrameLayout.LayoutParams center(int s) {
            return new FrameLayout.LayoutParams(s, s, Gravity.CENTER);
        }

        void refresh() {
            play.setVisibility(state == RUNNING ? View.GONE : View.VISIBLE);
            pause.setVisibility(state == RUNNING ? View.VISIBLE : View.GONE);
            edit.setColor(editMode ? 0xFF4D9BFF : 0xFF9AA5AB);
            String s = state == RUNNING ? "BERJALAN" : (state == PAUSED ? "JEDA" : "SIAP");
            status.setText(s + " · siklus " + cycles + " · " + ClickerConfig.count(ClickerService.this) + " titik"
                + (editMode && state != RUNNING ? " · atur posisi" : ""));
        }
    }
}
