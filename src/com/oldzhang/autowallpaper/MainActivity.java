package com.oldzhang.autowallpaper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * AutoWP 主界面：纯代码构建，支持明暗主题自适应
 */
public class MainActivity extends Activity {
    private static final int REQ_FOLDER = 2001;
    private static final int REQ_NOTIF = 2002;
    private static final String SIGNATURE = "Design & Build · Tencent Hunyuan Hy";

    private Prefs prefs;
    private Scheduler scheduler;

    // 主题调色板（启动时按当前明暗模式填充）
    private int cBg, cCard, cText, cTextSec, cTextTer;
    private int cBlue, cBlueLight, cBlueDark, cBlueSoft;
    private int cBorder, cDivider, cChipOff;
    private int cGreen, cGreenBg, cAmber, cAmberBg, cAmberIconBg, cRed;
    private boolean dark;

    private final List<View> modeRows = new ArrayList<>();
    private final List<View> targetBtns = new ArrayList<>();
    private final List<View> themeBtns = new ArrayList<>();
    private TextView folderCount;
    private TextView permissionStatus;
    private TextView heroFolder;
    private LinearLayout intervalSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        scheduler = new Scheduler(this);
        applyPalette();

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(cBg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, dp(12), pad, dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        root.addView(titleBar());
        root.addView(heroCard());
        root.addView(statusCard());
        root.addView(permissionCard());

        root.addView(sectionLabel("更换方式"));
        root.addView(modeCard());
        intervalSection = intervalSection();
        root.addView(intervalSection);
        updateIntervalVisibility();

        root.addView(sectionLabel("闲置防抖"));
        root.addView(debounceCard());

        root.addView(sectionLabel("壁纸来源与目标"));
        root.addView(sourceTargetCard());

        root.addView(sectionLabel("外观"));
        root.addView(themeCard());

        TextView sign = new TextView(this);
        sign.setText(SIGNATURE);
        sign.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        sign.setTextColor(cTextTer);
        sign.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(18);
        sign.setLayoutParams(sp);
        root.addView(sign);

        requestPermissionsIfNeeded();
        startServiceIfNeeded();
    }

    /** 按当前主题模式决定明暗，并填充调色板 */
    private void applyPalette() {
        int mode = prefs.getThemeMode();
        if (mode == Prefs.THEME_SYSTEM) {
            int ui = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            dark = (ui == Configuration.UI_MODE_NIGHT_YES);
        } else {
            dark = (mode == Prefs.THEME_DARK);
        }
        if (dark) {
            cBg = 0xFF141821; cCard = 0xFF1E2430; cText = 0xFFF3F4F6; cTextSec = 0xFF9CA3AF; cTextTer = 0xFF6B7280;
            cBlue = 0xFF3B82F6; cBlueLight = 0xFF17293F; cBlueDark = 0xFFBFDBFE; cBlueSoft = 0xFF60A5FA;
            cBorder = 0xFF2A3140; cDivider = 0xFF2A3140; cChipOff = 0xFF252B38;
            cGreen = 0xFF34D399; cGreenBg = 0xFF0F3D33; cAmber = 0xFFFBBF24; cAmberBg = 0xFF2A2118;
            cAmberIconBg = 0xFF4A3A22; cRed = 0xFFF87171;
        } else {
            cBg = 0xFFF5F7FA; cCard = 0xFFFFFFFF; cText = 0xFF1F2937; cTextSec = 0xFF6B7280; cTextTer = 0xFF9CA3AF;
            cBlue = 0xFF185FA5; cBlueLight = 0xFFE6F1FB; cBlueDark = 0xFF0C447C; cBlueSoft = 0xFF378ADD;
            cBorder = 0xFFE5E7EB; cDivider = 0xFFF0F1F3; cChipOff = 0xFFF3F4F6;
            cGreen = 0xFF1D9E75; cGreenBg = 0xFFE1F5EE; cAmber = 0xFFB45309; cAmberBg = 0xFFFFFBF2;
            cAmberIconBg = 0xFFFAEEDA; cRed = 0xFFB91C1C;
        }
    }

    private View titleBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(0, dp(8), 0, dp(14));

        TextView icon = new TextView(this);
        icon.setText("▣");
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        icon.setTextColor(Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{cBlue, cBlueDark});
        gd.setCornerRadius(dp(9));
        icon.setBackground(gd);
        bar.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(10), 0, 0, 0);
        TextView t = new TextView(this);
        t.setText("AutoWP");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        t.setTextColor(cText);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView sub = new TextView(this);
        sub.setText("V2.5 · 解锁 / 定时自动更换");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        sub.setTextColor(cTextTer);
        texts.addView(t);
        texts.addView(sub);
        bar.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Switch master = new Switch(this);
        master.setChecked(prefs.isEnabled());
        master.setOnCheckedChangeListener((b, on) -> {
            prefs.setEnabled(on);
            scheduler.reschedule();
            if (on) startServiceIfNeeded();
            toast(on ? "已开启" : "已暂停");
        });
        bar.addView(master);
        return bar;
    }

    /** 顶部精简壁纸预览条：显示当前壁纸缩略图 + 换一张 */
    private View heroCard() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{cBlue, cBlueDark});
        gd.setCornerRadius(dp(11));
        hero.setBackground(gd);
        hero.setPadding(dp(12), dp(11), dp(12), dp(11));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        hp.bottomMargin = dp(10);
        hero.setLayoutParams(hp);

        ImageView thumb = new ImageView(this);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable tg = new GradientDrawable();
        tg.setCornerRadius(dp(8));
        tg.setColor(cBlueLight);
        thumb.setBackground(tg);
        try {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            Drawable wall = wm.getDrawable();
            if (wall != null) thumb.setImageDrawable(wall);
        } catch (Exception ignored) {}
        hero.addView(thumb, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        tc.setPadding(dp(11), 0, dp(8), 0);
        TextView lbl = new TextView(this);
        lbl.setText("当前壁纸");
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        lbl.setTextColor(dark ? 0xFFB5D4F4 : 0xFFB5D4F4);
        heroFolder = new TextView(this);
        heroFolder.setText(describeFolderShort());
        heroFolder.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        heroFolder.setTextColor(Color.WHITE);
        heroFolder.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tc.addView(lbl); tc.addView(heroFolder);
        hero.addView(tc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView change = new TextView(this);
        change.setText("换一张");
        change.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        change.setTextColor(cBlue);
        change.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        change.setGravity(Gravity.CENTER);
        change.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable cg = new GradientDrawable();
        cg.setCornerRadius(dp(8));
        cg.setColor(Color.WHITE);
        change.setBackground(cg);
        change.setOnClickListener(v -> {
            if (new WallpaperEngine(this).changeNow()) { toast("已更换壁纸"); refreshHero(); }
            else toast("换壁纸失败，请检查壁纸文件夹");
        });
        hero.addView(change);
        return hero;
    }

    private String describeFolderShort() {
        int type = prefs.getSourceType();
        if (type == 2) return prefs.getFolderPath() + " · " + countImages() + " 张";
        if (type == 1) {
            try {
                Uri u = Uri.parse(prefs.getFolderUri());
                String docId = DocumentsContract.getTreeDocumentId(u);
                String name = docId.contains(":") ? docId.substring(docId.indexOf(':') + 1) : docId;
                return name + " · " + countImages() + " 张";
            } catch (Exception e) { return "已授权文件夹"; }
        }
        return "未选择壁纸文件夹";
    }

    private void refreshHero() {
        if (heroFolder != null) heroFolder.setText(describeFolderShort());
    }

    private View statusCard() {
        LinearLayout card = card();
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = new TextView(this);
        badge.setText("✓");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        badge.setTextColor(cGreen);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(cGreenBg);
        badge.setBackground(bg);
        line.addView(badge, new LinearLayout.LayoutParams(dp(24), dp(24)));
        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        tc.setPadding(dp(9), 0, 0, 0);
        TextView txt = new TextView(this);
        txt.setText("服务运行中");
        txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        txt.setTextColor(cText);
        txt.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView sub = new TextView(this);
        sub.setText("解锁或定时自动更换");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        sub.setTextColor(cTextSec);
        tc.addView(txt); tc.addView(sub);
        line.addView(tc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView ok = new TextView(this);
        ok.setText("正常");
        ok.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        ok.setTextColor(cGreen);
        ok.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable og = new GradientDrawable();
        og.setCornerRadius(dp(20));
        og.setColor(cGreenBg);
        ok.setBackground(og);
        line.addView(ok);
        card.addView(line);
        return card;
    }

    private View permissionCard() {
        LinearLayout c = card();
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        permissionStatus = new TextView(this);
        permissionStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        tc.addView(permissionStatus);
        TextView hint = new TextView(this);
        hint.setText("点击管理权限");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        hint.setTextColor(cTextTer);
        tc.addView(hint);
        c.addView(tc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        arrow.setTextColor(cTextTer);
        c.addView(arrow);
        c.setOnClickListener(v -> openAppSettings());
        refreshPermissionStatus();
        return c;
    }

    private void refreshPermissionStatus() {
        if (permissionStatus == null) return;
        boolean hasImages;
        if (Build.VERSION.SDK_INT >= 33) {
            hasImages = checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasImages = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        boolean partial = false;
        if (Build.VERSION.SDK_INT >= 34) {
            partial = checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == PackageManager.PERMISSION_GRANTED;
        }
        boolean hasNotif = true;
        if (Build.VERSION.SDK_INT >= 33) {
            hasNotif = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        if (partial && Build.VERSION.SDK_INT >= 34) {
            permissionStatus.setText("图片权限：仅选中照片 · 点此开启完整访问");
            permissionStatus.setTextColor(cAmber);
        } else if (!hasImages) {
            permissionStatus.setText("图片权限：未授予 · 点此开启");
            permissionStatus.setTextColor(cRed);
        } else {
            permissionStatus.setText("图片权限：已授予" + (hasNotif ? " · 通知已开" : " · 通知未开"));
            permissionStatus.setTextColor(cGreen);
        }
    }

    private void openAppSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private View sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(cTextSec);
        tv.setPadding(dp(2), dp(16), 0, dp(7));
        return tv;
    }

    private View modeCard() {
        LinearLayout card = card();
        card.setPadding(dp(5), dp(5), dp(5), dp(5));
        String[] labels = {"解锁更换", "每天更换", "每周更换", "自定义间隔"};
        String[] subs = {"解锁时换一张", "每天 08:00 换一张", "每周一 08:00 换一张", "按设定分钟数换"};
        int[] icons = {0};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(9));
            row.setBackground(bg);

            TextView radio = new TextView(this);
            radio.setText("●");
            radio.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            radio.setGravity(Gravity.CENTER);
            row.addView(radio, new LinearLayout.LayoutParams(dp(18), dp(18)));

            LinearLayout tc = new LinearLayout(this);
            tc.setOrientation(LinearLayout.VERTICAL);
            tc.setPadding(dp(11), 0, 0, 0);
            TextView a = new TextView(this);
            a.setText(labels[i]);
            a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            a.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            TextView b = new TextView(this);
            b.setText(subs[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            b.setTextColor(cTextSec);
            tc.addView(a); tc.addView(b);
            row.addView(tc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            row.setOnClickListener(v -> {
                prefs.setMode(idx);
                scheduler.reschedule();
                updateModeUI();
                updateIntervalVisibility();
            });
            modeRows.add(row);
            card.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        updateModeUI();
        return card;
    }

    private void updateModeUI() {
        for (int i = 0; i < modeRows.size(); i++) {
            LinearLayout row = (LinearLayout) modeRows.get(i);
            GradientDrawable bg = (GradientDrawable) row.getBackground();
            TextView title = (TextView) ((LinearLayout) row.getChildAt(1)).getChildAt(0);
            TextView radio = (TextView) row.getChildAt(0);
            boolean sel = (i == prefs.getMode());
            bg.setColor(sel ? cBlueLight : Color.TRANSPARENT);
            if (sel) bg.setStroke(0, Color.TRANSPARENT);
            radio.setText(sel ? "●" : "○");
            radio.setTextColor(sel ? cBlue : cTextTer);
            title.setTextColor(sel ? cBlueDark : cText);
        }
    }

    private LinearLayout intervalSection() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(sectionLabel("更换间隔"));
        LinearLayout card = card();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView lbl = new TextView(this);
        lbl.setText("每隔");
        lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        lbl.setTextColor(cText);
        final EditText et = new EditText(this);
        et.setText(String.valueOf(prefs.getIntervalMinutes()));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        et.setTextColor(cBlue);
        et.setBackgroundResource(android.R.drawable.editbox_background);
        et.setPadding(dp(8), dp(4), dp(8), dp(4));
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) {
                try { int v = Integer.parseInt(e.toString()); if (v > 0) { prefs.setIntervalMinutes(v); scheduler.reschedule(); } } catch (Exception ignored) {}
            }
        });
        TextView unit = new TextView(this);
        unit.setText("分钟换一张");
        unit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        unit.setTextColor(cText);
        row.addView(lbl);
        row.addView(et, new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(unit);
        card.addView(row);
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setPadding(0, dp(10), 0, 0);
        int[] mins = {5, 10, 60};
        String[] names = {"5 分钟", "10 分钟", "1 小时"};
        for (int i = 0; i < mins.length; i++) {
            final int v = mins[i];
            TextView chip = new TextView(this);
            chip.setText(names[i]);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setTextColor(cBlue);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(8));
            g.setColor(cBlueLight);
            chip.setBackground(g);
            chip.setOnClickListener(x -> { et.setText(String.valueOf(v)); prefs.setIntervalMinutes(v); scheduler.reschedule(); });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            cp.setMargins(i == 0 ? 0 : dp(6), 0, 0, 0);
            presets.addView(chip, cp);
        }
        card.addView(presets);
        box.addView(card);
        return box;
    }

    private void updateIntervalVisibility() {
        intervalSection.setVisibility(prefs.getMode() == Prefs.MODE_INTERVAL ? View.VISIBLE : View.GONE);
    }

    private View debounceCard() {
        LinearLayout card = card();
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(this);
        t.setText("闲置才换（锁屏超时判断）");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setTextColor(cText);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        Switch sw = new Switch(this);
        sw.setChecked(prefs.isDebounceEnabled());
        sw.setOnCheckedChangeListener((b, on) -> { prefs.setDebounceEnabled(on); refreshDebounceHint(); });
        head.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        head.addView(sw);
        card.addView(head);

        TextView desc = new TextView(this);
        desc.setText("锁屏超过设定时长，才视为「手机放一边了」，下次解锁才更换；短时间内拿起解锁不换。");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        desc.setTextColor(cTextSec);
        desc.setPadding(0, dp(6), 0, dp(10));
        card.addView(desc);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView l1 = new TextView(this);
        l1.setText("锁屏超过");
        l1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        l1.setTextColor(cText);
        final EditText et = new EditText(this);
        et.setText(String.valueOf(prefs.getDebounceMinutes()));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        et.setTextColor(cBlue);
        et.setPadding(dp(8), dp(4), dp(8), dp(4));
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable e) {
                try { int v = Integer.parseInt(e.toString()); if (v >= 0) prefs.setDebounceMinutes(v); } catch (Exception ignored) {}
            }
        });
        TextView l2 = new TextView(this);
        l2.setText("分钟，下次解锁才换");
        l2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        l2.setTextColor(cText);
        row.addView(l1);
        row.addView(et, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(l2);
        card.addView(row);
        return card;
    }

    private void refreshDebounceHint() {}

    /** 壁纸来源 + 更换目标 合并为一张设置组卡 */
    private View sourceTargetCard() {
        LinearLayout card = card();
        card.setPadding(dp(2), dp(2), dp(2), dp(2));

        LinearLayout srcRow = new LinearLayout(this);
        srcRow.setOrientation(LinearLayout.HORIZONTAL);
        srcRow.setGravity(Gravity.CENTER_VERTICAL);
        srcRow.setPadding(dp(14), dp(13), dp(14), dp(13));
        srcRow.setOnClickListener(v -> showSourceDialog());
        TextView si = new TextView(this);
        si.setText("▤");
        si.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        si.setTextColor(cBlue);
        srcRow.addView(si, new LinearLayout.LayoutParams(dp(26), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout stc = new LinearLayout(this);
        stc.setOrientation(LinearLayout.VERTICAL);
        stc.setPadding(dp(8), 0, 0, 0);
        TextView sa = new TextView(this);
        sa.setText("壁纸文件夹");
        sa.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sa.setTextColor(cText);
        folderCount = new TextView(this);
        folderCount.setText(describeFolder());
        folderCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        folderCount.setTextColor(cTextTer);
        stc.addView(sa); stc.addView(folderCount);
        srcRow.addView(stc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView sarrow = new TextView(this);
        sarrow.setText("›");
        sarrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        sarrow.setTextColor(cTextTer);
        srcRow.addView(sarrow);
        card.addView(srcRow);
        addDivider(card);

        LinearLayout tgtRow = new LinearLayout(this);
        tgtRow.setOrientation(LinearLayout.HORIZONTAL);
        tgtRow.setGravity(Gravity.CENTER_VERTICAL);
        tgtRow.setPadding(dp(14), dp(13), dp(14), dp(13));
        TextView ti = new TextView(this);
        ti.setText("▣");
        ti.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        ti.setTextColor(cBlue);
        tgtRow.addView(ti, new LinearLayout.LayoutParams(dp(26), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView ta = new TextView(this);
        ta.setText("更换目标");
        ta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        ta.setTextColor(cText);
        tgtRow.addView(ta, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        String[] names = {"桌面+锁屏", "仅桌面", "仅锁屏"};
        int[] vals = {Prefs.TARGET_BOTH, Prefs.TARGET_HOME, Prefs.TARGET_LOCK};
        for (int i = 0; i < 3; i++) {
            final int v = vals[i];
            TextView b = new TextView(this);
            b.setText(names[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(6), dp(7), dp(6), dp(7));
            b.setOnClickListener(x -> { prefs.setTarget(v); updateTargetUI(); });
            targetBtns.add(b);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) bp.setMargins(dp(5), 0, 0, 0);
            tgtRow.addView(b, bp);
        }
        card.addView(tgtRow);
        updateTargetUI();
        return card;
    }

    private void updateTargetUI() {
        for (int i = 0; i < targetBtns.size(); i++) {
            TextView b = (TextView) targetBtns.get(i);
            boolean sel = (i == prefs.getTarget());
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(dp(9));
            if (sel) { gd.setColor(cBlueLight); gd.setStroke(dp(2), cBlue); b.setTextColor(cBlue); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); }
            else { gd.setColor(cChipOff); gd.setStroke(0, Color.TRANSPARENT); b.setTextColor(cTextSec); b.setTypeface(Typeface.DEFAULT); }
            b.setBackground(gd);
        }
    }

    /** 外观切换：浅色 / 跟随系统 / 深色 */
    private View themeCard() {
        LinearLayout card = card();
        card.setPadding(dp(6), dp(6), dp(6), dp(6));
        card.setOrientation(LinearLayout.HORIZONTAL);
        String[] names = {"浅色", "跟随系统", "深色"};
        int[] vals = {Prefs.THEME_LIGHT, Prefs.THEME_SYSTEM, Prefs.THEME_DARK};
        for (int i = 0; i < 3; i++) {
            final int v = vals[i];
            TextView b = new TextView(this);
            b.setText(names[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(6), dp(9), dp(6), dp(9));
            b.setOnClickListener(x -> {
                if (prefs.getThemeMode() == v) return;
                prefs.setThemeMode(v);
                recreate();
            });
            themeBtns.add(b);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (i > 0) bp.setMargins(dp(5), 0, 0, 0);
            card.addView(b, bp);
        }
        updateThemeUI();
        return card;
    }

    private void updateThemeUI() {
        for (int i = 0; i < themeBtns.size(); i++) {
            TextView b = (TextView) themeBtns.get(i);
            int[] vals = {Prefs.THEME_LIGHT, Prefs.THEME_SYSTEM, Prefs.THEME_DARK};
            boolean sel = (vals[i] == prefs.getThemeMode());
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(dp(9));
            if (sel) { gd.setColor(cBlueLight); b.setTextColor(cBlue); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); }
            else { gd.setColor(Color.TRANSPARENT); b.setTextColor(cTextSec); b.setTypeface(Typeface.DEFAULT); }
            b.setBackground(gd);
        }
    }

    private String describeFolder() {
        int type = prefs.getSourceType();
        if (type == 2) return "路径 " + prefs.getFolderPath() + " · " + countImages() + " 张图片";
        if (type == 1) {
            try {
                Uri u = Uri.parse(prefs.getFolderUri());
                String docId = DocumentsContract.getTreeDocumentId(u);
                String name = docId.contains(":") ? docId.substring(docId.indexOf(':') + 1) : docId;
                return name + " · " + countImages() + " 张图片";
            } catch (Exception e) { return "已授权文件夹"; }
        }
        return "未选择 · 点此选择壁纸文件夹";
    }

    private int countImages() {
        try { return new WallpaperEngine(this).listImages().size(); }
        catch (Exception ex) { return -1; }
    }

    private void showSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("壁纸来源方式")
                .setItems(new String[]{"手动输入文件夹路径（推荐，兼容性好）", "系统文件夹授权（SAF）"}, (d, which) -> {
                    if (which == 0) showPathDialog();
                    else openFolderPicker();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showPathDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("例如 Pictures/Wallpapers");
        input.setText(prefs.getFolderPath().isEmpty() ? "Pictures/Wallpapers" : prefs.getFolderPath());
        input.setTextColor(cText);
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        new AlertDialog.Builder(this)
                .setTitle("输入壁纸文件夹路径")
                .setMessage("填写相对于手机内部存储的路径。把壁纸图片放进这个文件夹即可。")
                .setView(input)
                .setPositiveButton("确定", (d, w) -> {
                    String p = input.getText().toString().trim();
                    if (p.isEmpty()) { toast("路径不能为空"); return; }
                    prefs.setFolderPath(p);
                    prefs.setSourceType(2);
                    prefs.setCurrentIndex(-1);
                    folderCount.setText(describeFolder());
                    refreshHero();
                    toast("已设置：" + p);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openFolderPicker() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_FOLDER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (folderCount != null) folderCount.setText(describeFolder());
        refreshHero();
        refreshPermissionStatus();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_NOTIF && folderCount != null) folderCount.setText(describeFolder());
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_FOLDER && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            prefs.setFolderUri(uri.toString());
            prefs.setSourceType(1);
            prefs.setCurrentIndex(-1);
            folderCount.setText(describeFolder());
            refreshHero();
            toast("壁纸文件夹已设置");
        }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(cCard);
        gd.setCornerRadius(dp(12));
        gd.setStroke(1, cBorder);
        c.setBackground(gd);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(9);
        c.setLayoutParams(lp);
        return c;
    }

    private void addDivider(LinearLayout parent) {
        View d = new View(this);
        d.setBackgroundColor(cDivider);
        parent.addView(d, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }

    private void requestPermissionsIfNeeded() {
        java.util.List<String> needed = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (!needed.isEmpty()) requestPermissions(needed.toArray(new String[0]), REQ_NOTIF);
    }

    private void startServiceIfNeeded() {
        if (!prefs.isEnabled()) return;
        Intent svc = new Intent(this, WallpaperService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
        else startService(svc);
    }
}
