package com.oldzhang.autowallpaper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * 前台服务：保持进程存活，并动态注册解锁/锁屏广播
 */
public class WallpaperService extends Service {
    private static final String TAG = "WPService";
    private static final String CHANNEL_ID = "wallpaper_service";
    private static final int NOTIF_ID = 2026;
    private TriggerReceiver receiver;
    private boolean registered = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIF_ID, buildNotification());
        registerDynamicReceiver();
        new Scheduler(this).reschedule();
        Log.i(TAG, "service started");
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("自动换壁纸后台服务");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, flags);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        return b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("自动换壁纸运行中")
                .setContentText("解锁或定时自动更换")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void registerDynamicReceiver() {
        if (registered) return;
        receiver = new TriggerReceiver();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_USER_PRESENT);
        f.addAction("com.oldzhang.autowallpaper.TICK");
        f.setPriority(1000);
        if (Build.VERSION.SDK_INT >= 33) {
            // USER_PRESENT / SCREEN_OFF 是系统广播，必须用 EXPORTED 才能在国内 ROM/部分机型收到。
            // IntentFilter 已限定 action 白名单，不会收到无关广播。
            registerReceiver(receiver, f, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, f);
        }
        registered = true;
        Log.i(TAG, "dynamic receiver registered (EXPORTED)");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!registered) registerDynamicReceiver();
        new Scheduler(this).reschedule();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (registered && receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception e) {}
            registered = false;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
