#!/bin/bash
set -e

echo "Building Yamada OplusFace Java Daemon..."
cd Source
./gradlew :app:assembleRelease
cd ..

echo "Creating OUT folder structure..."
rm -rf OUT
mkdir -p OUT/system/system/framework
mkdir -p OUT/system/system/etc/init
mkdir -p OUT/system/vendor/bin/hw

echo "Copying Java HAL Daemon..."
cp Source/app/build/outputs/apk/release/app-release-unsigned.apk OUT/system/system/framework/yamada-face-hal.jar

echo "Creating startup script..."
cat << 'EOF' > OUT/system/vendor/bin/hw/yamada-oplusface
#!/system/bin/sh
export CLASSPATH=/system/framework/yamada-face-hal.jar
exec /system/bin/app_process /system/bin ax.nd.faceunlock.hal.FaceHalDaemon
EOF
chmod +x OUT/system/vendor/bin/hw/yamada-oplusface

echo "Creating init.rc file..."
cat << 'EOF' > OUT/system/system/etc/init/yamada-face-daemon.rc
service vendor.face-hal-1-0 /vendor/bin/hw/yamada-oplusface
    class hal
    user system
    group system input camera
    seclabel u:r:hal_face_default:s0
EOF

# Assuming the Megvii library is already in the ROM or provided separately
# mkdir -p OUT/system/system/lib64
# cp Source/app/src/main/jniLibs/arm64-v8a/libMegviiFacepp-0.5.2.so OUT/system/system/lib64/

echo "---------------------------------------------------"
echo "Build complete! The OUT folder is ready for MIO Kitchen."
echo "Since this is the clean Java Daemon approach, you DO NOT need to patch services.jar!"
echo "Just copy OUT to your kitchen and flash."
echo "Don't forget to add this to your vendor_file_contexts:"
echo "/vendor/bin/hw/yamada-oplusface u:object_r:hal_face_default_exec:s0"
echo "---------------------------------------------------"
