#define LOG_TAG "android.hardware.biometrics.face@1.0-service"

#include <android/hardware/biometrics/face/1.0/IBiometricsFace.h>
#include <hidl/HidlSupport.h>
#include <hidl/HidlTransportSupport.h>
#include "YamadaOplusFace.h"
#include <android-base/logging.h>

using android::hardware::biometrics::face::V1_0::IBiometricsFace;
using android::hardware::biometrics::face::V1_0::implementation::YamadaOplusFace;
using android::hardware::configureRpcThreadpool;
using android::hardware::joinRpcThreadpool;
using android::sp;

int main() {
    LOG(INFO) << "Starting custom Oplus Face HAL service";

    configureRpcThreadpool(1, true /*callerWillJoin*/);

    sp<IBiometricsFace> biometricsFace = new YamadaOplusFace();
    
    // Check if Oplus uses a custom instance name or standard "default"
    // Usually they just register "default".
    if (biometricsFace->registerAsService() != android::OK) {
        LOG(ERROR) << "Failed to register IBiometricsFace as service";
        return 1;
    }

    LOG(INFO) << "IBiometricsFace registered successfully. Ready to join threadpool";

    joinRpcThreadpool();

    return 0; // should never get here
}
