package net.timelegend.ayesha;

public class Log {
    private final static String TAG = "Ayesha";

    public final static <T> void i(T v) {
        android.util.Log.i(TAG, v.toString());
    }

    public final static <T> void d(T v) {
        android.util.Log.d(TAG, v.toString());
    }

    public final static <T> void e(T v) {
        android.util.Log.e(TAG, v.toString());
    }

    public final static <T> void w(T v) {
        android.util.Log.w(TAG, v.toString());
    }
}
