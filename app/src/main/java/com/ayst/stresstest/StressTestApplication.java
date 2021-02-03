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

package com.ayst.stresstest;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

//import com.ms_square.debugoverlay.DebugOverlay;
//import com.ms_square.debugoverlay.Position;
//import com.ms_square.debugoverlay.modules.CpuFreqModule;
//import com.ms_square.debugoverlay.modules.CpuUsageModule;
//import com.ms_square.debugoverlay.modules.FpsModule;
//import com.ms_square.debugoverlay.modules.LogcatLine;
//import com.ms_square.debugoverlay.modules.LogcatLineFilter;
//import com.ms_square.debugoverlay.modules.LogcatModule;
//import com.ms_square.debugoverlay.modules.MemInfoModule;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;


/**
 * Created by Bob Shen on 2017/11/3.
 */
public class StressTestApplication extends Application {
    private static final String TAG = "StressTestApplication";

    public static Typeface sMIUIBoldTextType = null;
    private ActivityManager mAm;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();

        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(0)         // (Optional) How many method line to show. Default 2
                .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
                .tag("StressTest")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));

        // 加载自定义字体
        try{
            sMIUIBoldTextType = Typeface.createFromAsset(getAssets(), "fonts/dincond_medium.otf");
        }catch(Exception e){
            Log.e(TAG, "onCreate, loading fronts/MIUI-Bold.ttf failed!") ;
        }

        // 只展示error信息
//        LogcatModule logcatModule = new LogcatModule(3,
//                new LogcatLineFilter.SimpleLogcatLineFilter(LogcatLine.Priority.ERROR));
//
//        new DebugOverlay.Builder(this)
//                .modules(new CpuFreqModule(),
//                        new CpuUsageModule(),
//                        new MemInfoModule(this),
//                        new FpsModule())
//                .position(Position.BOTTOM_START)
//                .build()
//                .install();

        mAm = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        ActivityManager.MemoryInfo systemMemInfo = new ActivityManager.MemoryInfo();
        mAm.getMemoryInfo(systemMemInfo);
        Log.w(TAG, "onTrimMemory, level=" + level
                + " availMem=" + systemMemInfo.availMem/1024 + "kB");
    }
}
