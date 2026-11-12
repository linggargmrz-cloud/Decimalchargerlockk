package id.decimalcharge.lock;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class ChargeActivity extends Activity {
    static volatile boolean showing = false;
    static volatile boolean dimMode = false;
    static volatile long lastDestroy = 0;
    static volatile long lastInteraction = 0;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { finishAndRemoveTask(); }
    };

    private void setDim(boolean dim) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = dim ? 0.03f : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        showing = true;
        lastInteraction = System.currentTimeMillis();
        boolean dim = getIntent().getBooleanExtra("dim", false);
        dimMode = dim;
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        if (dim) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!BatteryReader.read(this).charging) { finishAndRemoveTask(); return; }
        try { getSystemService(NotificationManager.class).cancel(2); } catch (Exception ignored) { }
        ChargeView v = new ChargeView(this);
        v.setOnLongClickListener(x -> { finishAndRemoveTask(); return true; });
        v.setOnTouchListener((x, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN && dimMode) setDim(false);
            return false;
        });
        setContentView(v);
        if (dim) setDim(true);
        hideUi();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, f);
    }

    @Override public void onUserInteraction() {
        super.onUserInteraction();
        lastInteraction = System.currentTimeMillis();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        if (i.getBooleanExtra("dim", false)) {
            dimMode = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setDim(true);
        }
    }

    private void hideUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) hideUi(); }

    @Override protected void onDestroy() {
        showing = false;
        lastDestroy = System.currentTimeMillis();
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        super.onDestroy();
    }
            }
