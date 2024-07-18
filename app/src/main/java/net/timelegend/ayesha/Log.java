package net.timelegend.ayesha;

public class Log {
    private final static String TAG = "net.timelegend.ayesha";

    public static int i(String msg) {
        return android.util.Log.i(TAG, msg);
    }

    public static int d(String msg) {
        return android.util.Log.d(TAG, msg);
    }

    public static int e(String msg) {
        return android.util.Log.e(TAG, msg);
    }

    public static int w(String msg) {
        return android.util.Log.w(TAG, msg);
    }
}
