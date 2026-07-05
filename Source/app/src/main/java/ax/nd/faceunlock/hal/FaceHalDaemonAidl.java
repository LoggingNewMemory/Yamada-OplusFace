package ax.nd.faceunlock.hal;

import android.content.Context;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.app.ActivityThread;
import java.lang.reflect.Method;

import android.hardware.biometrics.face.IFace;
import android.hardware.biometrics.face.ISession;
import android.hardware.biometrics.face.ISessionCallback;
import android.hardware.biometrics.face.SensorProps;

public class FaceHalDaemonAidl extends IFace.Stub {
    private static final String TAG = "FaceHalDaemonAidl";
    private Context mContext;

    public FaceHalDaemonAidl(Context context) {
        mContext = context;
    }

    public static void main(String[] args) {
        Log.i(TAG, "Starting Java Face HAL Daemon (AIDL)...");
        Looper.prepareMainLooper();
        
        try {
            ActivityThread at = ActivityThread.systemMain();
            Context context = at.getSystemContext();
            
            FaceHalDaemonAidl daemon = new FaceHalDaemonAidl(context);
            
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            Method addService = smClass.getMethod("addService", String.class, IBinder.class);
            addService.invoke(null, "android.hardware.biometrics.face.IFace/default", daemon);
            
            Log.i(TAG, "Successfully registered android.hardware.biometrics.face.IFace/default");
        } catch (Exception e) {
            Log.e(TAG, "Daemon error", e);
        }
        
        Looper.loop();
    }

    @Override
    public ISession createSession(int sensorId, int userId, ISessionCallback cb) throws RemoteException {
        Log.i(TAG, "createSession called for sensorId: " + sensorId + ", userId: " + userId);
        return new FaceHalSessionAidl(mContext, cb);
    }

    @Override
    public SensorProps[] getSensorProps() throws RemoteException {
        SensorProps props = new SensorProps();
        props.commonProps = new android.hardware.biometrics.common.CommonProps();
        props.commonProps.sensorId = 0;
        props.commonProps.sensorStrength = android.hardware.biometrics.common.SensorStrength.STRONG;
        props.commonProps.maxEnrollmentsPerUser = 1;
        props.sensorType = android.hardware.biometrics.face.FaceSensorType.RGB;
        props.supportsDetectInteraction = false;
        props.halControlsPreview = false;
        return new SensorProps[] { props };
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
