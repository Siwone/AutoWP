# AutoWP

一个轻量、无广告、纯本地运行的安卓自动换壁纸 App。按解锁、每天、每周或自定义间隔自动更换桌面/锁屏壁纸。

> Design & Build · Tencent Hunyuan Hy

## 功能

- **四种更换方式**：解锁更换 / 每天 08:00 / 每周一 08:00 / 自定义间隔（5 分钟、10 分钟、1 小时或任意分钟）
- **闲置防抖**：锁屏超过设定时长（默认 10 分钟）才算「手机放一边了」，下次解锁才换；短时间拿起解锁不换，避免一天换几十次
- **壁纸来源**：指定一个本地文件夹，按稳定顺序轮播。支持两种授权方式：
  - 手动输入文件夹路径（推荐，兼容性好，直接读文件不依赖媒体库索引）
  - 系统文件夹授权（SAF）
- **更换目标**：桌面 + 锁屏 / 仅桌面 / 仅锁屏
- **明暗主题**：浅色 / 深色 / 跟随系统
- 开机自启、前台服务保活、权限状态可视化

## 下载安装

直接下载 [最新版 APK](AutoWP-v2.5.apk) 安装即可（Android 6.0 / API 23 及以上）。

## 首次使用

1. 打开 App，总开关默认已开
2. 点「壁纸文件夹」→ 选「手动输入文件夹路径」→ 填 `Pictures/Wallpapers`（把壁纸图片放进这个文件夹）
3. 选更换方式和闲置阈值
4. 允许通知权限和图片读取权限

## 从源码构建

不用 Gradle，直接调用 Android SDK 工具链编译（JDK 17 + Android SDK 34）：

```bash
bash build.sh <版本号> <versionCode>
# 例：bash build.sh 2.5 10
```

`build.sh` 里 `TC` 变量指向 Android SDK 与 JDK 的路径，按本机实际位置修改即可。构建脚本会完成：资源编译链接 → javac → d8 转 DEX → 打包 → zipalign → apksigner 签名 → 验签。

> **注意**：`release.jks` 签名密钥不在仓库里（已被 .gitignore 排除）。首次构建需在 `build.sh` 里配置你自己的 keystore，或自行生成：
> ```
> keytool -genkeypair -keystore release.jks -alias autowallpaper -keyalg RSA -keysize 2048 -validity 10000
> ```

## 技术实现

原生 Java 开发，无第三方依赖，纯代码构建 UI（无 XML 布局文件）。

| 类 | 职责 |
|----|------|
| `MainActivity` | 设置界面，纯代码 UI，明暗双主题调色板 |
| `WallpaperEngine` | 扫描文件夹 / MediaStore 兜底、顺序轮播、设置桌面/锁屏壁纸 |
| `TriggerReceiver` | 处理解锁（`USER_PRESENT`）、锁屏（`SCREEN_OFF`）、定时 `TICK` 广播 |
| `Scheduler` | AlarmManager 定时：每天 / 每周 / 自定义间隔 |
| `WallpaperService` | 前台服务，动态注册系统广播保证后台存活 |
| `BootReceiver` | 开机 / 应用更新后拉起服务 |
| `Prefs` | SharedPreferences 配置持久化 |

### 安卓兼容性要点（踩过的坑）

- Android 14+ 的 `specialUse` 前台服务必须声明 `FOREGROUND_SERVICE_SPECIAL_USE` 权限 + 用途 property
- Android 14+ 动态注册广播必须显式声明 `RECEIVER_EXPORTED`/`NOT_EXPORTED`；监听系统解锁广播要用 `RECEIVER_EXPORTED`
- 部分机型 SAF 文件夹选择器无法导航，故提供「手动路径」备用方案
- 「仅选中照片」部分权限下 MediaStore 读不全，改用直接文件遍历优先

## 版本历史

| 版本 | versionCode | 变更 |
|------|-------------|------|
| V1.0 | 1 | 初始版本 |
| V1.1 | 2 | 修复安卓14+ 前台服务权限 |
| V1.2 | 3 | 修复动态广播注册标志 |
| V1.4 | 4 | 修复 SAF 选择器初始 URI |
| V2.0 | 5 | 新增手动路径壁纸来源 |
| V2.1 | 6 | 新增文件遍历兜底 |
| V2.2 | 7 | 修复扫描计数，文件遍历优先 |
| V2.3 | 8 | 权限状态可视化 |
| V2.4 | 9 | 修复解锁换壁纸失效（广播 EXPORTED） |
| V2.5 | 10 | 全新 UI：AutoWP 命名、壁纸预览条、明暗主题、新图标、开发者署名 |

## License

[MIT](LICENSE) — 自由使用、修改、商用，保留署名即可。
