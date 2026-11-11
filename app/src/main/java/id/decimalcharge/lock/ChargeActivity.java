package id.decimalcharge.lock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class ChargeActivity extends Activity {
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { finishAndRemoveTask(); }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!BatteryReader.read(this).charging) { finishAndRemoveTask(); return; }
        ChargeView v = new ChargeView(this);
        v.setOnLongClickListener(x -> { finishAndRemoveTask(); return true; });
        setContentView(v);
        hideUi();
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
    }

    private void hideUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override public void onWindowFocusChanged(boolean f) { super.onWindowFocusChanged(f); if (f) hideUi(); }

    @Override protected void onDestroy() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
