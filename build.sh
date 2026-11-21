#!/usr/bin/env bash
# AutoWP APK 一键构建脚本（无需 Gradle）
# 用法: bash build.sh <版本号> <versionCode>
# 可用环境变量覆盖: TC(工具链根目录) KEYSTORE KEY_ALIAS STORE_PASS KEY_PASS
set -e
PROJ="$(cd "$(dirname "$0")" && pwd)"
TC="${TC:-$PROJ/.toolchain}"
BT="$TC/android-sdk/build-tools/34.0.0"
JDK_DIR=$(ls -d "$TC"/jdk-* 2>/dev/null | head -1)
JAVAC="${JAVAC:-$JDK_DIR/bin/javac}"
JAVA="${JAVA:-$JDK_DIR/bin/java}"
AJAR="$TC/android-sdk/platforms/android-34/android.jar"
AAPT2="$BT/aapt2.exe"
AAPT="$BT/aapt.exe"
KEYSTORE="${KEYSTORE:-$PROJ/release.jks}"
KEY_ALIAS="${KEY_ALIAS:-autowallpaper}"
STORE_PASS="${STORE_PASS:-123456}"
KEY_PASS="${KEY_PASS:-123456}"
VER="${1:-2.5}"
VCODE="${2:-10}"
OUT="$PROJ/AutoWP-v${VER}.apk"

cd "$PROJ"
rm -rf build gen obj
mkdir -p build gen obj

echo "[1/7] 编译资源"
"$AAPT2" compile --dir res -o build/res.zip

echo "[2/7] 链接资源 + Manifest (v${VER})"
"$AAPT2" link -o build/base.apk -I "$AJAR" \
  --manifest AndroidManifest.xml -R build/res.zip --java gen \
  --min-sdk-version 23 --target-sdk-version 34 \
  --version-code "$VCODE" --version-name "$VER" --auto-add-overlay

echo "[3/7] 编译 Java"
find src gen -name "*.java" > build/sources.txt
"$JAVAC" -encoding UTF-8 -source 11 -target 11 -classpath "$AJAR" -d obj @build/sources.txt 2>/dev/null

echo "[4/7] 生成 DEX"
find obj -name "*.class" > build/classes.txt
"$JAVA" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --lib "$AJAR" --min-api 23 --output build @build/classes.txt

echo "[5/7] DEX 入包"
(cd build && "$AAPT" add base.apk classes.dex >/dev/null 2>&1 || true)

echo "[6/7] 对齐 + 签名"
"$BT/zipalign.exe" -f -p 4 build/base.apk build/aligned.apk
"$JAVA" -cp "$BT/lib/apksigner.jar" com.android.apksigner.ApkSignerTool sign \
  --ks "$PROJ/release.jks" --ks-key-alias autowallpaper \
  --ks-pass pass:123456 --key-pass pass:123456 \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT" build/aligned.apk

echo "[7/7] 验签"
"$JAVA" -cp "$BT/lib/apksigner.jar" com.android.apksigner.ApkSignerTool verify "$OUT"

echo ""
echo "构建完成: $OUT"
ls -lh "$OUT"
echo "MD5: $(md5sum "$OUT" | cut -d' ' -f1)"
