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
mkdir -p "$OUT_DIR/vendor/bin/hw"
mkdir -p "$OUT_DIR/vendor/etc/init"
mkdir -p "$OUT_DIR/vendor/etc/vintf/manifest"
mkdir -p "$OUT_DIR/AUTOPATCH/faceunlock"
mkdir -p "$OUT_DIR/AUTOPATCH/PUT_JARS_HERE"
mkdir -p "$OUT_DIR/AUTOPATCH/PATCHED_JARS"
cp -r "/home/yamada/CODE/FaceUnlock-camera2/AUTOPATCH/tool" "$OUT_DIR/AUTOPATCH/"
cp "/home/yamada/CODE/FaceUnlock-camera2/AUTOPATCH/faceunlock/patch" "$OUT_DIR/AUTOPATCH/faceunlock/"
cp "/home/yamada/CODE/FaceUnlock-camera2/AUTOPATCH/patchFU" "$OUT_DIR/AUTOPATCH/"

cat << 'EOF' > "$OUT_DIR/AUTOPATCH/auto_patch.sh"
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
JARS_DIR="$DIR/PUT_JARS_HERE"
OUT_JARS="$DIR/../system/system/framework"

echo "=================================================="
echo "    Yamada-OplusFace Auto-Patcher for Jars        "
echo "=================================================="

if [ ! -f "$JARS_DIR/services.jar" ]; then
    echo "[!] Error: services.jar not found in PUT_JARS_HERE/"
    echo "Please copy your ROM's services.jar into PUT_JARS_HERE/ and run this script again."
    exit 1
fi

mkdir -p "$OUT_JARS"

echo "[*] Found services.jar, starting patch process..."
cp "$JARS_DIR/services.jar" "$DIR/services.jar"

cd "$DIR"
chmod +x patchFU
./patchFU services.jar

if [ -f "$DIR/services.jar" ]; then
    mv "$DIR/services.jar" "$OUT_JARS/services.jar"
    echo "[*] Success! Patched services.jar is ready in OUT/system/system/framework/"
else
    echo "[!] Error: Patching failed!"
    exit 1
fi

if [ -f "$JARS_DIR/oplus-services.jar" ]; then
    echo "[*] Found oplus-services.jar. No patches required for this jar for Yamada-OplusFace!"
fi

echo "=================================================="
echo " All done! The patched jar has been automatically "
echo " moved to system/system/framework/ in the OUT     "
echo " folder. It's fully ready for MIO Kitchen!        "
echo "=================================================="
EOF
chmod +x "$OUT_DIR/AUTOPATCH/auto_patch.sh"

# 1. Copy PreCompiled System files (Megvii libs, etc)
echo "[*] Copying PreCompiled Megvii libraries and configs..."
if [ -d "$PRECOMPILED_DIR/system" ]; then
    cp -rf "$PRECOMPILED_DIR/system/"* "$OUT_DIR/system/"
else
    echo "[!] Warning: PreCompiled/system not found!"
fi

# 2. Compile Java Service into classes.dex
echo "[*] Compiling Java System Service..."
cd "$SOURCE_DIR"
chmod +x gradlew
./gradlew assembleRelease

APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
if [ -f "$APK_PATH" ]; then
    echo "[*] Extracting classes.dex from APK..."
    unzip -j -q "$APK_PATH" classes.dex -d "$OUT_DIR/AUTOPATCH/faceunlock/"
else
    echo "[!] Error: APK compilation failed! classes.dex not generated."
fi
cd "$BASE_DIR"

# 3. Copy HAL configs (RC, XML)
echo "[*] Copying HAL init script and manifest..."
cp "$SOURCE_DIR/hal/yamada-oplusface.rc" "$OUT_DIR/vendor/etc/init/"
cp "$SOURCE_DIR/hal/yamada-oplusface.xml" "$OUT_DIR/vendor/etc/vintf/manifest/"

# 4. Handle HAL Binary
echo "[*] Copying and renaming generic HAL binary..."
# Since this is for a ColorOS port via MIO Kitchen (no AOSP tree), we will reuse
# the generic pre-compiled proxy daemon from FaceUnlock-camera2. Our open-source 
# Java service uses the exact same socket protocol, making them perfectly compatible!
cp "/home/yamada/CODE/FaceUnlock-camera2/ROM/vendor/bin/hw/android.hardware.biometrics.face-service" "$OUT_DIR/vendor/bin/hw/yamada-oplusface"
chmod +x "$OUT_DIR/vendor/bin/hw/yamada-oplusface"

echo "=================================================="
echo "[*] Structure preparation complete!"
echo "[*] Don't forget to manually add the following to your device's file_contexts:"
echo "    /vendor/bin/hw/yamada-oplusface u:object_r:hal_face_default_exec:s0"
echo "[*] OUT folder is ready for MIO Kitchen!"
