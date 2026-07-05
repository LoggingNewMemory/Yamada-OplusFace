package ax.nd.faceunlock.hal;

import android.content.Context;
import android.os.HwBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.app.ActivityThread;
import android.os.NativeHandle;

import java.util.ArrayList;

import android.hardware.biometrics.face.V1_0.IBiometricsFace;
import android.hardware.biometrics.face.V1_0.IBiometricsFaceClientCallback;
import android.hardware.biometrics.face.V1_0.OptionalBool;
import android.hardware.biometrics.face.V1_0.OptionalUint64;
import android.hidl.base.V1_0.DebugInfo;

import ax.nd.faceunlock.FaceUnlockService;

public class FaceHalDaemon extends IBiometricsFace.Stub {
    private static final String TAG = "FaceHalDaemon";
    
    private IBiometricsFaceClientCallback mCallback;
    private long mDeviceId = 1;
    private int mUserId = 0;
    private FaceUnlockService mFaceService;
    
    public FaceHalDaemon(Context context) {
        mFaceService = new FaceUnlockService(context);
        mFaceService.initNoSocket();
    }
    
    public static void main(String[] args) {
        Log.i(TAG, "Starting Java Face HAL Daemon...");
        Looper.prepareMainLooper();
        
        try {
            ActivityThread at = ActivityThread.systemMain();
            Context context = at.getSystemContext();
            
            FaceUnlockService service = new FaceUnlockService(context);
            service.run(); // Start the LocalSocket client/server
            
            Log.i(TAG, "Java Daemon is running in Socket Mode. Waiting for patched services.jar to connect...");
            
            // Keep the process alive
            Looper.loop();
        } catch (Exception e) {
            Log.e(TAG, "Daemon error", e);
        }
        
        Looper.loop();
    }

    @Override
    public OptionalUint64 setCallback(IBiometricsFaceClientCallback clientCallback) throws RemoteException {
        mCallback = clientCallback;
        OptionalUint64 result = new OptionalUint64();
        result.status = 0; // Status.OK
        result.value = mDeviceId;
        return result;
    }

    @Override
    public int setActiveUser(int userId, String storePath) throws RemoteException {
        mUserId = userId;
        return 0; // Status.OK
    }

    @Override
    public OptionalUint64 generateChallenge(int challengeTimeoutSec) throws RemoteException {
        OptionalUint64 result = new OptionalUint64();
        result.status = 0;
        result.value = 12345;
        return result;
    }

    @Override
    public int enroll(ArrayList<Byte> hat, int timeoutSec, ArrayList<Integer> disabledFeatures) throws RemoteException {
        new Thread(() -> {
            boolean success = mFaceService.runEnroll();
            try {
                if (mCallback != null) {
                    if (success) {
                        mCallback.onEnrollResult(mDeviceId, 1, mUserId, 0);
                    } else {
                        mCallback.onError(mDeviceId, mUserId, 5, 0); // ERROR_CANCELED = 5
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to notify enroll result", e);
            }
        }).start();
        return 0;
    }

    @Override
    public int revokeChallenge() throws RemoteException {
        return 0;
    }

    @Override
    public int setFeature(int feature, boolean enabled, ArrayList<Byte> hat, int faceId) throws RemoteException {
        return 0;
    }

    @Override
    public OptionalBool getFeature(int feature, int faceId) throws RemoteException {
        OptionalBool result = new OptionalBool();
        result.status = 0;
        result.value = false;
        return result;
    }

    @Override
    public OptionalUint64 getAuthenticatorId() throws RemoteException {
        OptionalUint64 result = new OptionalUint64();
        result.status = 0;
        result.value = 0;
        return result;
    }

    @Override
    public int cancel() throws RemoteException {
        mFaceService.doCancel();
        try {
            if (mCallback != null) {
                mCallback.onError(mDeviceId, mUserId, 5, 0); // ERROR_CANCELED = 5
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to notify cancel", e);
        }
        return 0;
    }

    @Override
    public int enumerate() throws RemoteException {
        try {
            if (mCallback != null) {
                ArrayList<Integer> faces = new ArrayList<>();
                faces.add(1);
                mCallback.onEnumerate(mDeviceId, faces, mUserId);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to notify enumerate", e);
        }
        return 0;
    }

    @Override
    public int remove(int faceId) throws RemoteException {
        mFaceService.doRemove();
        try {
            if (mCallback != null) {
                ArrayList<Integer> faces = new ArrayList<>();
                mCallback.onRemoved(mDeviceId, faces, mUserId);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to notify remove", e);
        }
        return 0;
    }

    @Override
    public int authenticate(long operationId) throws RemoteException {
        new Thread(() -> {
            boolean success = mFaceService.runAuth();
            try {
                if (mCallback != null) {
                    if (success) {
                        ArrayList<Byte> token = new ArrayList<>();
                        mCallback.onAuthenticated(mDeviceId, 1, mUserId, token);
                    } else {
                        mCallback.onAuthenticated(mDeviceId, 0, mUserId, new ArrayList<>());
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to notify auth result", e);
            }
        }).start();
        return 0;
    }

    @Override
    public int userActivity() throws RemoteException {
        return 0;
    }

    @Override
    public int resetLockout(ArrayList<Byte> hat) throws RemoteException {
        return 0;
    }
}
