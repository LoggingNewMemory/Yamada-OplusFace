#define LOG_TAG "android.hardware.biometrics.face@1.0-service"

#include "YamadaOplusFace.h"
#include <android-base/logging.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <vector>

namespace android {
namespace hardware {
namespace biometrics {
namespace face {
namespace V1_0 {
namespace implementation {

using ::android::hardware::biometrics::face::V1_0::Status;

YamadaOplusFace::YamadaOplusFace() : mSocketFd(-1), mDeviceId(1), mChallenge(0), mUserId(0) {
    LOG(INFO) << "YamadaOplusFace HAL constructed";
}

YamadaOplusFace::~YamadaOplusFace() {
    if (mSocketFd >= 0) {
        close(mSocketFd);
    }
}

bool YamadaOplusFace::connectToSocket() {
    if (mSocketFd >= 0) return true;

    mSocketFd = socket(AF_LOCAL, SOCK_STREAM, 0);
    if (mSocketFd < 0) {
        LOG(ERROR) << "Failed to create socket";
        return false;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_LOCAL;
    strncpy(addr.sun_path, "/dev/socket/vendor_face_hal", sizeof(addr.sun_path) - 1);

    if (connect(mSocketFd, (struct sockaddr*)&addr, sizeof(addr)) < 0) {
        LOG(ERROR) << "Failed to connect to /dev/socket/vendor_face_hal";
        close(mSocketFd);
        mSocketFd = -1;
        return false;
    }

    LOG(INFO) << "Connected to /dev/socket/vendor_face_hal";
    
    mSocketThread = std::thread(&YamadaOplusFace::readSocketLoop, this);
    mSocketThread.detach();

    return true;
}

void YamadaOplusFace::sendCommand(const std::string& cmd) {
    if (!connectToSocket()) return;
    std::string toSend = cmd + "\n";
    if (write(mSocketFd, toSend.c_str(), toSend.length()) < 0) {
        LOG(ERROR) << "Failed to send command to socket: " << cmd;
        close(mSocketFd);
        mSocketFd = -1;
    }
}

void YamadaOplusFace::readSocketLoop() {
    char buffer[256];
    while (mSocketFd >= 0) {
        ssize_t n = read(mSocketFd, buffer, sizeof(buffer) - 1);
        if (n <= 0) {
            LOG(ERROR) << "Socket read failed or closed";
            close(mSocketFd);
            mSocketFd = -1;
            break;
        }
        buffer[n] = '\0';
        std::string reply(buffer);
        // Clean up newlines
        reply.erase(std::remove(reply.begin(), reply.end(), '\n'), reply.end());
        reply.erase(std::remove(reply.begin(), reply.end(), '\r'), reply.end());

        std::lock_guard<std::mutex> lock(mClientCallbackMutex);
        if (mClientCallback != nullptr) {
            if (reply == "1") {
                // Success
                hidl_vec<uint8_t> token; // empty token
                mClientCallback->onAuthenticated(mDeviceId, 0, mUserId, token);
                mClientCallback->onEnrollResult(mDeviceId, 1, mUserId, 0);
            } else if (reply == "-1") {
                // Failure
                mClientCallback->onAuthenticated(mDeviceId, 0, mUserId, hidl_vec<uint8_t>());
            }
        }
    }
}

Return<OptionalUint64> YamadaOplusFace::setCallback(const sp<IBiometricsFaceClientCallback>& clientCallback) {
    std::lock_guard<std::mutex> lock(mClientCallbackMutex);
    mClientCallback = clientCallback;
    OptionalUint64 result;
    result.status = Status::OK;
    result.value = mDeviceId;
    return result;
}

Return<Status> YamadaOplusFace::setActiveUser(int32_t userId, const hidl_string& storePath) {
    mUserId = userId;
    return Status::OK;
}

Return<OptionalUint64> YamadaOplusFace::generateChallenge(uint32_t challengeTimeoutSec) {
    OptionalUint64 result;
    result.status = Status::OK;
    mChallenge = 12345;
    result.value = mChallenge;
    return result;
}

Return<Status> YamadaOplusFace::enroll(const hidl_array<uint8_t, 69>& hat, uint32_t timeoutSec, const hidl_vec<int32_t>& disabledFeatures) {
    sendCommand("ENROLL");
    return Status::OK;
}

Return<Status> YamadaOplusFace::revokeChallenge() {
    mChallenge = 0;
    return Status::OK;
}

Return<Status> YamadaOplusFace::setFeature(Feature feature, bool enabled, const hidl_array<uint8_t, 69>& hat, uint32_t faceId) {
    return Status::OK;
}

Return<OptionalBool> YamadaOplusFace::getFeature(Feature feature, uint32_t faceId) {
    OptionalBool result;
    result.status = Status::OK;
    result.value = false;
    return result;
}

Return<OptionalUint64> YamadaOplusFace::getAuthenticatorId() {
    OptionalUint64 result;
    result.status = Status::OK;
    result.value = 0;
    return result;
}

Return<Status> YamadaOplusFace::cancel() {
    sendCommand("CANCEL");
    std::lock_guard<std::mutex> lock(mClientCallbackMutex);
    if (mClientCallback != nullptr) {
        mClientCallback->onError(mDeviceId, mUserId, 5, 0); // ERROR_CANCELED = 5
    }
    return Status::OK;
}

Return<Status> YamadaOplusFace::enumerate() {
    std::lock_guard<std::mutex> lock(mClientCallbackMutex);
    if (mClientCallback != nullptr) {
        hidl_vec<uint32_t> faces; // dummy faces list
        faces.resize(1);
        faces[0] = 1; 
        // For Oplus, usually if there's a feature, enumerate should return it.
        // The daemon can also just send dummy values.
    }
    return Status::OK;
}

Return<Status> BiometricsFace::remove(uint32_t faceId) {
    sendCommand("REMOVE");
    std::lock_guard<std::mutex> lock(mClientCallbackMutex);
    if (mClientCallback != nullptr) {
        hidl_vec<uint32_t> faces; // dummy faces list
        mClientCallback->onRemoved(mDeviceId, faceId, mUserId, faces);
    }
    return Status::OK;
}

Return<Status> BiometricsFace::authenticate(uint64_t operationId) {
    sendCommand("AUTH");
    return Status::OK;
}

Return<Status> BiometricsFace::userActivity() {
    return Status::OK;
}

Return<Status> BiometricsFace::resetLockout(const hidl_array<uint8_t, 69>& hat) {
    return Status::OK;
}

}  // namespace implementation
}  // namespace V1_0
}  // namespace face
}  // namespace biometrics
}  // namespace hardware
}  // namespace android
