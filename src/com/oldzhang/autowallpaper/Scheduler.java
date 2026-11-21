package com.oldzhang.autowallpaper;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * 定时调度：每天 / 每周 / 自定义间隔
 */
public class Scheduler {
    private final Context ctx;
    private final Prefs prefs;
    private final AlarmManager am;

    private static final int REQ_TICK = 1001;

    public Scheduler(Context c) {
        this.ctx = c.getApplicationContext();
        this.prefs = new Prefs(ctx);
        this.am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
    }

    private PendingIntent tickIntent() {
        Intent i = new Intent("com.oldzhang.autowallpaper.TICK");
        i.setPackage(ctx.getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQ_TICK, i, flags);
    }

    /** 根据当前配置重新排定下一次触发 */
    public void reschedule() {
        am.cancel(tickIntent());
        if (!prefs.isEnabled()) return;
        int mode = prefs.getMode();
        long now = System.currentTimeMillis();
        long trigger;
        long period;

        switch (mode) {
            case Prefs.MODE_DAILY: {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 8);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                trigger = c.getTimeInMillis();
                if (trigger <= now) trigger += 24L * 60 * 60 * 1000;
                period = 24L * 60 * 60 * 1000;
                setRepeating(trigger, period);
                break;
            }
            case Prefs.MODE_WEEKLY: {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                c.set(Calendar.HOUR_OF_DAY, 8);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                trigger = c.getTimeInMillis();
                if (trigger <= now) trigger += 7L * 24 * 60 * 60 * 1000;
                period = 7L * 24 * 60 * 60 * 1000;
                setRepeating(trigger, period);
                break;
            }
            case Prefs.MODE_INTERVAL: {
                period = prefs.getIntervalMinutes() * 60L * 1000;
                if (period < 60 * 1000) period = 60 * 1000;
                trigger = now + period;
                setRepeating(trigger, period);
                break;
            }
            case Prefs.MODE_UNLOCK:
            default:
                // 解锁模式不依赖闹钟，由广播驱动
                break;
        }
    }

    private void setRepeating(long trigger, long period) {
        if (Build.VERSION.SDK_INT >= 23) {
            am.setRepeating(AlarmManager.RTC_WAKEUP, trigger, period, tickIntent());
        } else {
            am.setRepeating(AlarmManager.RTC_WAKEUP, trigger, period, tickIntent());
        }
    }

    /** 立即触发一次（测试用） */
    public void fireNow() {
        Intent i = new Intent("com.oldzhang.autowallpaper.TICK");
        i.setPackage(ctx.getPackageName());
        ctx.sendBroadcast(i);
    }
}
