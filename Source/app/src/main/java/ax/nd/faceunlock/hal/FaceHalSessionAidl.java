package ax.nd.faceunlock.hal;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.hardware.common.NativeHandle;
import android.hardware.keymaster.HardwareAuthToken;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.biometrics.common.OperationContext;
import android.hardware.biometrics.face.ISession;
import android.hardware.biometrics.face.ISessionCallback;
import android.hardware.biometrics.face.FaceEnrollOptions;
import android.hardware.biometrics.face.EnrollmentStageConfig;

import ax.nd.faceunlock.FaceUnlockService;

public class FaceHalSessionAidl extends ISession.Stub {
    private static final String TAG = "FaceHalSessionAidl";
    private ISessionCallback mCallback;
    private FaceUnlockService mFaceService;

    public FaceHalSessionAidl(Context context, ISessionCallback cb) {
        mCallback = cb;
        mFaceService = new FaceUnlockService(context);
        mFaceService.initNoSocket();
    }

    private ICancellationSignal createCancellationSignal() {
        return new ICancellationSignal.Stub() {
            @Override
            public void cancel() throws RemoteException {
                Log.i(TAG, "CancellationSignal.cancel() called");
                mFaceService.doCancel();
                if (mCallback != null) {
                    mCallback.onError((byte) 5, 0); // ERROR_CANCELED = 5
                }
            }
            @Override
            public int getInterfaceVersion() { return 3; }
            @Override
            public String getInterfaceHash() { return "notfrozen"; }
        };
    }

    @Override
    public ICancellationSignal authenticate(long operationId) throws RemoteException {
        Log.i(TAG, "authenticate called");
        new Thread(() -> {
            boolean success = mFaceService.runAuth();
            try {
                if (mCallback != null) {
                    if (success) {
                        mCallback.onAuthenticationSucceeded(0, new HardwareAuthToken());
                    } else {
                        mCallback.onAuthenticationFailed();
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Callback error", e);
            }
        }).start();
        return createCancellationSignal();
    }

    @Override
    public ICancellationSignal authenticateWithContext(long operationId, OperationContext context) throws RemoteException {
        return authenticate(operationId);
    }

    @Override
    public ICancellationSignal enroll(HardwareAuthToken hat, byte type, byte[] features, NativeHandle previewSurface) throws RemoteException {
        Log.i(TAG, "enroll called");
        new Thread(() -> {
            boolean success = mFaceService.runEnroll();
            try {
                if (mCallback != null) {
                    if (success) {
                        mCallback.onEnrollmentProgress(1, 0);
                    } else {
                        mCallback.onError((byte) 5, 0);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Callback error", e);
            }
        }).start();
        return createCancellationSignal();
    }

    @Override
    public ICancellationSignal enrollWithContext(HardwareAuthToken hat, byte type, byte[] features, NativeHandle previewSurface, OperationContext context) throws RemoteException {
        return enroll(hat, type, features, previewSurface);
    }

    @Override
    public ICancellationSignal enrollWithOptions(FaceEnrollOptions options) throws RemoteException {
        return enroll(options.hardwareAuthToken, options.enrollmentType, options.features, options.nativeHandlePreview);
    }

    @Override
    public void close() throws RemoteException {
        Log.i(TAG, "close called");
    }

    @Override
    public ICancellationSignal detectInteraction() throws RemoteException {
        return createCancellationSignal();
    }

    @Override
    public ICancellationSignal detectInteractionWithContext(OperationContext context) throws RemoteException {
        return createCancellationSignal();
    }

    @Override
    public void enumerateEnrollments() throws RemoteException {
        if (mCallback != null) {
            mCallback.onEnrollmentsEnumerated(new int[]{1});
        }
    }

    @Override
    public void generateChallenge() throws RemoteException {
        if (mCallback != null) {
            mCallback.onChallengeGenerated(123456789L);
        }
    }

    @Override
    public void getAuthenticatorId() throws RemoteException {
        if (mCallback != null) {
            mCallback.onAuthenticatorIdRetrieved(0L);
        }
    }

    @Override
    public EnrollmentStageConfig[] getEnrollmentConfig(byte enrollmentType) throws RemoteException {
        return new EnrollmentStageConfig[0];
    }

    @Override
    public void getFeatures() throws RemoteException {
        if (mCallback != null) {
            mCallback.onFeaturesRetrieved(new byte[0]);
        }
    }

    @Override
    public void invalidateAuthenticatorId() throws RemoteException {
        if (mCallback != null) {
            mCallback.onAuthenticatorIdInvalidated(0L);
        }
    }

    @Override
    public void onContextChanged(OperationContext context) throws RemoteException {}

    @Override
    public void removeEnrollments(int[] enrollmentIds) throws RemoteException {
        mFaceService.doRemove();
        if (mCallback != null) {
            mCallback.onEnrollmentsRemoved(enrollmentIds);
        }
    }

    @Override
    public void resetLockout(HardwareAuthToken hat) throws RemoteException {
        if (mCallback != null) {
            mCallback.onLockoutCleared();
        }
    }

    @Override
    public void revokeChallenge(long challenge) throws RemoteException {
        if (mCallback != null) {
            mCallback.onChallengeRevoked(challenge);
        }
    }

    @Override
    public void setFeature(HardwareAuthToken hat, byte feature, boolean enabled) throws RemoteException {
        if (mCallback != null) {
            mCallback.onFeatureSet(feature);
        }
    }

    @Override
    public int getInterfaceVersion() throws RemoteException {
        return 3;
    }

    @Override
    public String getInterfaceHash() throws RemoteException {
        return "notfrozen";
    }
}
