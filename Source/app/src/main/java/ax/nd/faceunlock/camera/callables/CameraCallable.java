package ax.nd.faceunlock.camera.callables;

import java.lang.ref.WeakReference;
import android.os.Handler;
import android.os.Looper;
import ax.nd.faceunlock.camera.CameraRepository;
import ax.nd.faceunlock.camera.listeners.CameraListener;

public abstract class CameraCallable implements Runnable {
    protected WeakReference<CameraListener> mCameraListener;

    public CameraCallable(CameraListener cameraListener) {
        this.mCameraListener = new WeakReference<>(cameraListener);
    }

    public CameraRepository.CameraData getCameraData() {
        return CameraRepository.getInstance().getCameraData();
    }

    public CameraListener getCameraListener() {
        return this.mCameraListener.get();
    }

    public static void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
    
    @Override
    public abstract void run();
}