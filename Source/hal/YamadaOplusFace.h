#pragma once

#include <android/hardware/biometrics/face/1.0/IBiometricsFace.h>
#include <hidl/MQDescriptor.h>
#include <hidl/Status.h>
#include <mutex>
#include <thread>
#include <string>
#include <cutils/sockets.h>

namespace android {
namespace hardware {
namespace biometrics {
namespace face {
namespace V1_0 {
namespace implementation {

using ::android::hardware::hidl_array;
using ::android::hardware::hidl_memory;
using ::android::hardware::hidl_string;
using ::android::hardware::hidl_vec;
using ::android::hardware::Return;
using ::android::hardware::Void;
using ::android::sp;
using ::android::hardware::biometrics::face::V1_0::IBiometricsFace;
using ::android::hardware::biometrics::face::V1_0::IBiometricsFaceClientCallback;
using ::android::hardware::biometrics::face::V1_0::Feature;

class YamadaOplusFace : public IBiometricsFace {
public:
    YamadaOplusFace();
    ~YamadaOplusFace();

    // Methods from ::android::hardware::biometrics::face::V1_0::IBiometricsFace follow.
    Return<::android::hardware::biometrics::face::V1_0::OptionalUint64> setCallback(const sp<IBiometricsFaceClientCallback>& clientCallback) override;
    Return<::android::hardware::biometrics::face::V1_0::Status> setActiveUser(int32_t userId, const hidl_string& storePath) override;
    Return<::android::hardware::biometrics::face::V1_0::OptionalUint64> generateChallenge(uint32_t challengeTimeoutSec) override;
    Return<::android::hardware::biometrics::face::V1_0::Status> enroll(const hidl_array<uint8_t, 69>& hat, uint32_t timeoutSec, const hidl_vec<int32_t>& disabledFeatures) override;
    Return<::android::hardware::biometrics::face::V1_0::Status> revokeChallenge() override;
    Return<::android::hardware::biometrics::face::V1_0::Status> setFeature(Feature feature, bool enabled, const hidl_array<uint8_t, 69>& hat, uint32_t faceId) override;
    Return<::android::hardware::biometrics::face::V1_0::OptionalBool> getFeature(Feature feature, uint32_t faceId) override;
    Return<::android::hardware::biometrics::face::V1_0::OptionalUint64> getAuthenticatorId() override;
    Return<::android::hardware::biometrics::face::V1_0::Status> cancel() override;
    Return<::android::hardware::biometrics::face::V1_0::Status> enumerate() override;
    Return<::android::hardware::biometrics::face::V1_0::Status> remove(uint32_t faceId) override;
    Return<::android::hardware::biometrics::face::V1_0::Status> authenticate(uint64_t operationId) override;
    Return<::android::hardware::biometrics::face::V1_0::Status> userActivity() override;
    Return<::android::hardware::biometrics::face::V1_0::Status> resetLockout(const hidl_array<uint8_t, 69>& hat) override;

private:
    std::mutex mClientCallbackMutex;
    sp<IBiometricsFaceClientCallback> mClientCallback;
    int mSocketFd;
    
    uint64_t mDeviceId;
    uint64_t mChallenge;
    uint32_t mUserId;
    
    bool connectToSocket();
    void sendCommand(const std::string& cmd);
    void readSocketLoop();
    std::thread mSocketThread;
};

}  // namespace implementation
}  // namespace V1_0
}  // namespace face
}  // namespace biometrics
}  // namespace hardware
}  // namespace android
