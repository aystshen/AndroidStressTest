package com.ayst.stresstest.util;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;

/**
 * Created by Habo Shen on 2017/10/27.
 */

public class InjectEvent {
    private static Instrumentation mInstrumentation = new Instrumentation();

    public static void sendKeyDownUpSync(int keyCode) {
        mInstrumentation.sendKeyDownUpSync(keyCode);
    }

    public static void sendPointerSync(final float x, final float y) {
        mInstrumentation.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),MotionEvent.ACTION_DOWN, x, y, 0));
        mInstrumentation.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),MotionEvent.ACTION_UP, x, y, 0));
    }
}
