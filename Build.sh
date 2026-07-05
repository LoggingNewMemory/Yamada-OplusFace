#!/bin/bash

# Define paths
BASE_DIR=$(pwd)
SOURCE_DIR="$BASE_DIR/Source"
PRECOMPILED_DIR="$BASE_DIR/PreCompiled"
OUT_DIR="$BASE_DIR/OUT"

echo "=================================================="
echo "    Yamada-OplusFace Build & Packaging Script     "
echo "=================================================="

# Clean previous build
echo "[*] Cleaning old OUT directory..."
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/system"
mkdir -p "$OUT_DIR/system/system/framework"
mkdir -p "$OUT_DIR/system/system/etc/init"
mkdir -p "$OUT_DIR/vendor/bin/hw"
mkdir -p "$OUT_DIR/vendor/etc/init"
mkdir -p "$OUT_DIR/vendor/etc/vintf/manifest"

# 1. Copy PreCompiled System files (Megvii libs, etc)
echo "[*] Copying PreCompiled Megvii libraries and configs..."
if [ -d "$PRECOMPILED_DIR/system" ]; then
    cp -rf "$PRECOMPILED_DIR/system/"* "$OUT_DIR/system/"
else
    echo "[!] Warning: PreCompiled/system not found!"
fi

# 2. Compile Java Service into APK
echo "[*] Compiling Java System Service..."
cd "$SOURCE_DIR"
chmod +x gradlew
./gradlew assembleRelease

APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
if [ -f "$APK_PATH" ]; then
    echo "[*] Copying standalone Java AIDL Daemon..."
    cp "$APK_PATH" "$OUT_DIR/system/system/framework/yamada-face-hal.jar"
else
    echo "[!] Error: APK compilation failed!"
    exit 1
fi
cd "$BASE_DIR"

# 3. Create generic HAL wrapper script for app_process
echo "[*] Creating standalone Java daemon script..."
cat << 'EOF' > "$OUT_DIR/vendor/bin/hw/yamada-oplusface"
#!/system/bin/sh
export CLASSPATH=/system/framework/yamada-face-hal.jar:/system/framework/framework.jar
exec /system/bin/app_process /system/bin ax.nd.faceunlock.hal.FaceHalDaemonAidl
EOF
chmod +x "$OUT_DIR/vendor/bin/hw/yamada-oplusface"

# 4. Copy HAL configs (RC, XML)
echo "[*] Copying HAL init script and manifest..."
cp "$SOURCE_DIR/hal/yamada-oplusface.rc" "$OUT_DIR/vendor/etc/init/"
cp "$SOURCE_DIR/hal/yamada-oplusface.xml" "$OUT_DIR/vendor/etc/vintf/manifest/"

echo "=================================================="
echo "[*] Structure preparation complete!"
echo "[*] We are using a 100% STANDALONE Java Daemon!"
echo "[*] No services.jar patching and no closed-source C++ HAL required."
echo "[*] Don't forget to manually add the following to your device's file_contexts:"
echo "    /vendor/bin/hw/yamada-oplusface u:object_r:hal_face_default_exec:s0"
echo "[*] OUT folder is ready for MIO Kitchen!"
echo "=================================================="
