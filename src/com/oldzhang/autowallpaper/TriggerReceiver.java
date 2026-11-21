package com.oldzhang.autowallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 触发器：锁屏记录时间 / 解锁判断是否换 / 定时 TICK 直接换
 */
public class TriggerReceiver extends BroadcastReceiver {
    private static final String TAG = "Trigger";
    private static final String ACTION_TICK = "com.oldzhang.autowallpaper.TICK";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        Prefs prefs = new Prefs(ctx);
        if (!prefs.isEnabled()) return;

        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            prefs.setLastScreenOff(System.currentTimeMillis());
            Log.i(TAG, "SCREEN_OFF recorded");

        } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            Log.i(TAG, "USER_PRESENT received, mode=" + prefs.getMode() + " enabled=" + prefs.isEnabled());
            if (prefs.getMode() != Prefs.MODE_UNLOCK) {
                Log.i(TAG, "skip: mode not unlock");
                return;
            }
            boolean shouldChange = true;
            if (prefs.isDebounceEnabled()) {
                long offAt = prefs.getLastScreenOff();
                long idle = offAt > 0 ? System.currentTimeMillis() - offAt : 0;
                long threshold = prefs.getDebounceMinutes() * 60L * 1000;
                shouldChange = idle >= threshold;
                Log.i(TAG, "debounce idle=" + (idle/60000) + "min thresh=" + prefs.getDebounceMinutes() + "min change=" + shouldChange);
            } else {
                Log.i(TAG, "debounce OFF -> change every unlock");
            }
            if (shouldChange) {
                boolean ok = new WallpaperEngine(ctx).changeNow();
                Log.i(TAG, "unlock changeNow result=" + ok);
            }

        } else if (ACTION_TICK.equals(action)) {
            if (prefs.getMode() == Prefs.MODE_UNLOCK) return;
            boolean ok = new WallpaperEngine(ctx).changeNow();
            Log.i(TAG, "tick changeNow result=" + ok);
        }
    }
}
