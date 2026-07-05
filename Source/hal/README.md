# Open-Source Face HAL Daemon

This directory contains the open-source implementation of the custom Face HAL (`android.hardware.biometrics.face@1.0-service`). 
It replaces the closed-source precompiled daemon from previous projects.

## Architecture
The HAL implements the standard Android `IBiometricsFace` HIDL interface. Instead of doing the heavy biometric processing itself, it acts as a lightweight proxy.
When the Android OS requests biometric operations (like `authenticate` or `enroll`), this HAL forwards the request as simple text strings (`AUTH`, `ENROLL`, `CANCEL`, `REMOVE`) to a local filesystem socket (`/dev/socket/vendor_face_hal`).

The Java System Service (`FaceUnlockService`) listens on the other end of this socket, performs the actual camera preview and Megvii facial recognition, and writes the result (`1` for success, `-1` for failure) back to the socket. The HAL then triggers the appropriate HIDL callback (`onAuthenticated`, `onEnrollResult`, etc.) to inform the Android OS.

## Files
- `BiometricsFace.h` & `BiometricsFace.cpp`: The core implementation of the HIDL interface and the socket client.
- `service.cpp`: Registers the HAL service.
- `Android.bp`: Soong build script.
- `*.rc` & `*.xml`: Init script and VINTF manifest.
