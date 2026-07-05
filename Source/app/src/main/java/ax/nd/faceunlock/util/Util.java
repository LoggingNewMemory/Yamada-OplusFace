package ax.nd.faceunlock.util;

import android.content.Context;
import ax.nd.faceunlock.FaceUnlockService.*;

public class Util {
    public static void setSystemProperty(String key, String value) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method set = c.getMethod("set", String.class, String.class);
            set.invoke(null, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getIntSystemProperty(String key, int defaultValue) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method getIntMethod = c.getMethod("getInt", String.class, int.class);
            return (Integer) getIntMethod.invoke(null, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static boolean getBooleanSystemProperty(String key, boolean defaultValue) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method getBooleanMethod = c.getMethod("getBoolean", String.class, boolean.class);
            return (Boolean) getBooleanMethod.invoke(null, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public static Context getSystemContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Object systemContext = activityThreadClass.getMethod("getSystemContext").invoke(activityThread);
            return (Context) systemContext;
        } catch (Exception e) {
            return null;
        }
    }
}