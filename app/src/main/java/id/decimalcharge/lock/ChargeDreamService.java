package id.decimalcharge.lock;

import android.service.dreams.DreamService;

public class ChargeDreamService extends DreamService {
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        setContentView(new ChargeView(this));
    }
}
