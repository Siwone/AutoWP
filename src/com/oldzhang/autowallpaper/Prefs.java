package com.oldzhang.autowallpaper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置存储：所有设置项 + 运行状态持久化
 */
public class Prefs {
    private static final String NAME = "wp_prefs";
    private final SharedPreferences sp;

    // 更换方式
    public static final int MODE_UNLOCK = 0;   // 解锁更换
    public static final int MODE_DAILY = 1;    // 每天
    public static final int MODE_WEEKLY = 2;   // 每周
    public static final int MODE_INTERVAL = 3; // 自定义间隔

    // 更换目标
    public static final int TARGET_BOTH = 0;   // 桌面+锁屏
    public static final int TARGET_HOME = 1;   // 仅桌面
    public static final int TARGET_LOCK = 2;   // 仅锁屏

    public Prefs(Context c) {
        sp = c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() { return sp.getBoolean("enabled", true); }
    public void setEnabled(boolean v) { sp.edit().putBoolean("enabled", v).apply(); }

    public int getMode() { return sp.getInt("mode", MODE_UNLOCK); }
    public void setMode(int v) { sp.edit().putInt("mode", v).apply(); }

    public int getIntervalMinutes() { return sp.getInt("interval_min", 30); }
    public void setIntervalMinutes(int v) { sp.edit().putInt("interval_min", v).apply(); }

    public boolean isDebounceEnabled() { return sp.getBoolean("debounce", true); }
    public void setDebounceEnabled(boolean v) { sp.edit().putBoolean("debounce", v).apply(); }

    public int getDebounceMinutes() { return sp.getInt("debounce_min", 10); }
    public void setDebounceMinutes(int v) { sp.edit().putInt("debounce_min", v).apply(); }

    public int getTarget() { return sp.getInt("target", TARGET_BOTH); }
    public void setTarget(int v) { sp.edit().putInt("target", v).apply(); }

    public String getFolderUri() { return sp.getString("folder_uri", ""); }
    public void setFolderUri(String v) { sp.edit().putString("folder_uri", v).apply(); }

    /** 手动输入的文件夹相对路径（如 Pictures/Wallpapers），作为 SAF 的备用来源 */
    public String getFolderPath() { return sp.getString("folder_path", ""); }
    public void setFolderPath(String v) { sp.edit().putString("folder_path", v).apply(); }

    /** 来源类型：0=未设置 1=SAF授权文件夹 2=手动路径(MediaStore) */
    public int getSourceType() { return sp.getInt("source_type", 0); }
    public void setSourceType(int v) { sp.edit().putInt("source_type", v).apply(); }

    /** 外观模式：0=跟随系统 1=浅色 2=深色 */
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    public int getThemeMode() { return sp.getInt("theme_mode", THEME_SYSTEM); }
    public void setThemeMode(int v) { sp.edit().putInt("theme_mode", v).apply(); }

    /** 上次屏幕点亮的时刻（用于计算闲置时长） */
    public long getLastScreenOff() { return sp.getLong("last_screen_off", 0L); }
    public void setLastScreenOff(long v) { sp.edit().putLong("last_screen_off", v).apply(); }

    /** 上次实际更换壁纸的时刻 */
    public long getLastChange() { return sp.getLong("last_change", 0L); }
    public void setLastChange(long v) { sp.edit().putLong("last_change", v).apply(); }

    /** 当前壁纸索引（用于按顺序轮播，避免随机重复） */
    public int getCurrentIndex() { return sp.getInt("current_index", -1); }
    public void setCurrentIndex(int v) { sp.edit().putInt("current_index", v).apply(); }
}
