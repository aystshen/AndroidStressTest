/*
 * Copyright(c) 2018 Bob Shen <ayst.shen@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ayst.stresstest.util;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.MotionEvent;

/**
 * Created by Bob Shen on 2017/10/27.
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
