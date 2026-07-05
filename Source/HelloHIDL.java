import android.os.HwBinder;
import android.os.Looper;
import android.hardware.biometrics.face.V1_0.IBiometricsFace;

public class HelloHIDL {
    public static void main(String[] args) {
        System.out.println("Starting...");
        Looper.prepareMainLooper();
        try {
            System.out.println("Registering...");
            // Fake register or just join threadpool
            HwBinder.joinRpcThreadpool();
            System.out.println("Joined.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
