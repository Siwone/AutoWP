package com.oldzhang.autowallpaper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;
import android.app.WallpaperManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 换壁纸核心引擎：扫描文件夹、按顺序轮播、设置到桌面/锁屏
 */
public class WallpaperEngine {
    private static final String TAG = "WPEngine";
    private final Context ctx;
    private final Prefs prefs;

    public WallpaperEngine(Context c) {
        this.ctx = c.getApplicationContext();
        this.prefs = new Prefs(ctx);
    }

    /**
     * 执行一次换壁纸。返回是否成功。
     */
    public boolean changeNow() {
        List<Uri> images = listImages();
        if (images.isEmpty()) {
            Log.w(TAG, "no images found in folder");
            return false;
        }
        int idx = prefs.getCurrentIndex();
        idx = (idx + 1) % images.size();
        Uri chosen = images.get(idx);

        try {
            Bitmap bmp = decodeBitmap(chosen);
            if (bmp == null) return false;
            WallpaperManager wm = WallpaperManager.getInstance(ctx);
            int target = prefs.getTarget();
            if (Build.VERSION.SDK_INT >= 24) {
                if (target == Prefs.TARGET_HOME) {
                    wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM);
                } else if (target == Prefs.TARGET_LOCK) {
                    wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK);
                } else {
                    wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                }
            } else {
                wm.setBitmap(bmp);
            }
            prefs.setCurrentIndex(idx);
            prefs.setLastChange(System.currentTimeMillis());
            Log.i(TAG, "wallpaper changed to index " + idx + " / " + images.size());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "change failed: " + e.getMessage(), e);
            return false;
        }
    }

    /** 列出壁纸图片：支持 SAF 授权文件夹 或 手动路径(MediaStore) */
    public List<Uri> listImages() {
        int type = prefs.getSourceType();
        if (type == 2) return listByPath();
        return listBySaf();
    }

    /** 方式一：SAF 授权的文件夹树 */
    private List<Uri> listBySaf() {
        List<Uri> result = new ArrayList<>();
        String uriStr = prefs.getFolderUri();
        if (uriStr == null || uriStr.isEmpty()) return result;
        try {
            Uri treeUri = Uri.parse(uriStr);
            Uri docUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            String[] proj = new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE};
            android.content.ContentResolver cr = ctx.getContentResolver();
            android.database.Cursor cur = cr.query(docUri, proj, null, null, null);
            if (cur != null) {
                while (cur.moveToNext()) {
                    String id = cur.getString(0);
                    String mime = cur.getString(1);
                    if (mime != null && mime.startsWith("image/")) {
                        result.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, id));
                    }
                }
                cur.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "list SAF error: " + e.getMessage(), e);
        }
        Collections.sort(result);
        return result;
    }

    /** 方式二：按相对路径扫描文件夹。优先直接遍历文件，失败再退回 MediaStore。 */
    private List<Uri> listByPath() {
        List<Uri> result = new ArrayList<>();
        String path = prefs.getFolderPath();
        if (path == null || path.isEmpty()) return result;
        String rel = path.replace("\\", "/");
        while (rel.startsWith("/")) rel = rel.substring(1);
        while (rel.endsWith("/")) rel = rel.substring(0, rel.length() - 1);

        // 主路径：直接遍历公共存储目录下的图片文件（不依赖媒体库索引，最可靠）
        try {
            java.io.File dir = new java.io.File(
                    android.os.Environment.getExternalStorageDirectory(), rel);
            Log.d(TAG, "scan dir=" + dir.getAbsolutePath() + " exists=" + dir.exists());
            if (dir.isDirectory()) {
                java.io.File[] files = dir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        String n = f.getName().toLowerCase();
                        if (f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg")
                                || n.endsWith(".png") || n.endsWith(".webp"))) {
                            result.add(Uri.fromFile(f));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "file scan error: " + e.getMessage(), e);
        }

        // 兜底：目录遍历没结果时，再查 MediaStore
        if (result.isEmpty()) {
            String relWithSlash = rel + "/";
            try {
                String[] proj = new String[]{android.provider.MediaStore.Images.Media._ID};
                String sel = android.provider.MediaStore.Images.Media.RELATIVE_PATH + " = ?";
                String[] args = new String[]{relWithSlash};
                android.database.Cursor cur = ctx.getContentResolver().query(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        proj, sel, args, null);
                if (cur != null) {
                    int idCol = cur.getColumnIndex(android.provider.MediaStore.Images.Media._ID);
                    while (cur.moveToNext()) {
                        result.add(android.content.ContentUris.withAppendedId(
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                cur.getLong(idCol)));
                    }
                    cur.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "media scan error: " + e.getMessage(), e);
            }
        }
        Collections.sort(result);
        Log.i(TAG, "listByPath found " + result.size() + " images in " + rel);
        return result;
    }

    private InputStream openStream(Uri uri) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new java.io.FileInputStream(uri.getPath());
        }
        return ctx.getContentResolver().openInputStream(uri);
    }

    private Bitmap decodeBitmap(Uri uri) {
        try {
            InputStream is = openStream(uri);
            if (is == null) return null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, opts);
            is.close();

            is = openStream(uri);
            opts.inJustDecodeBounds = false;
            int maxEdge = Math.max(opts.outWidth, opts.outHeight);
            int scale = maxEdge > 2048 ? (int) Math.ceil(maxEdge / 2048.0) : 1;
            opts.inSampleSize = scale;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "decode error: " + e.getMessage(), e);
            return null;
        }
    }
}
