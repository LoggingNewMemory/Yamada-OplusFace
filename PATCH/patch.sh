#!/bin/bash

# Automation script for Oplus Face Unlock patching
# Created automatically based on instructions

set -e

# Directories
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_DIR="${BASE_DIR}/JAR"
SERVICES_JAR="${JAR_DIR}/services.jar"
LZY_PATCH_DIR="$(dirname "${BASE_DIR}")/LZY Patch"
OUTPUT_DIR="${BASE_DIR}/output"
WORK_DIR="${BASE_DIR}/workspace"

SMALI_VER="2.5.2"
BAKSMALI_JAR="${BASE_DIR}/baksmali-${SMALI_VER}.jar"
SMALI_JAR="${BASE_DIR}/smali-${SMALI_VER}.jar"

echo "[*] Cleaning up old workspace and output..."
rm -rf "${OUTPUT_DIR}" "${WORK_DIR}"
mkdir -p "${OUTPUT_DIR}" "${WORK_DIR}"

echo "[*] Downloading smali and baksmali if needed..."
if [ ! -f "${BAKSMALI_JAR}" ]; then
    wget -q "https://bitbucket.org/JesusFreke/smali/downloads/baksmali-${SMALI_VER}.jar" -O "${BAKSMALI_JAR}"
fi
if [ ! -f "${SMALI_JAR}" ]; then
    wget -q "https://bitbucket.org/JesusFreke/smali/downloads/smali-${SMALI_VER}.jar" -O "${SMALI_JAR}"
fi

echo "[*] Extracting services.jar..."
mkdir -p "${WORK_DIR}/services_unzip"
unzip -q "${SERVICES_JAR}" -d "${WORK_DIR}/services_unzip"

echo "[*] Decompiling classes.dex..."
java -jar "${BAKSMALI_JAR}" d "${WORK_DIR}/services_unzip/classes.dex" -o "${WORK_DIR}/smali_classes"

echo "[*] Deleting specified classes from smali..."
# -FaceProvider
# -FaceService
# -TestHal (Face AIDL directory)

TARGET_FACE_DIR="${WORK_DIR}/smali_classes/com/android/server/biometrics/sensors/face"

# FaceProvider and TestHal in AIDL directory
rm -f "${TARGET_FACE_DIR}/aidl/FaceProvider.smali"
rm -f "${TARGET_FACE_DIR}/aidl/TestHal.smali"

# FaceService in face directory
rm -f "${TARGET_FACE_DIR}/FaceService.smali"

echo "[*] Recompiling classes.dex..."
java -jar "${SMALI_JAR}" a "${WORK_DIR}/smali_classes" -o "${WORK_DIR}/services_unzip/classes.dex"

echo "[*] Importing classes5.dex into services.jar..."
if [ -f "${LZY_PATCH_DIR}/system/system/framework/classes5.dex" ]; then
    cp "${LZY_PATCH_DIR}/system/system/framework/classes5.dex" "${WORK_DIR}/services_unzip/classes5.dex"
else
    echo "[!] Error: classes5.dex not found in LZY Patch!"
    exit 1
fi

echo "[*] Repackaging services.jar..."
cd "${WORK_DIR}/services_unzip"
zip -qr "../../services_patched.jar" .
cd "${BASE_DIR}"

echo "[*] Preparing the final output structure..."
# Copy LZY Patch structure
cp -r "${LZY_PATCH_DIR}/system" "${OUTPUT_DIR}/"

# Remove the standalone classes5.dex as it is now inside services.jar
rm -f "${OUTPUT_DIR}/system/system/framework/classes5.dex"

# Put patched services.jar in the right place
mkdir -p "${OUTPUT_DIR}/system/system/framework"
cp "${BASE_DIR}/services_patched.jar" "${OUTPUT_DIR}/system/system/framework/services.jar"

echo "[*] Cleaning up workspace..."
rm -rf "${WORK_DIR}"
rm -f "${BASE_DIR}/services_patched.jar"

echo "[+] Patching complete! Ready copy-pasting structure is in ${OUTPUT_DIR}/system"
