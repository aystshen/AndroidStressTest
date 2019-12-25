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

package com.ayst.stresstest.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;

import android.util.Log;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseTestFragment;
import com.ayst.stresstest.test.RecoveryTestFragment;
import com.ayst.stresstest.ui.MainActivity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by Bob Shen on 2018/5/21.
 */

public class WorkService extends Service {
    private final static String TAG = "WorkService";

    public static final String ID = "com.ayst.stresstest.WorkService";
    public static final String NAME = "AndroidStressTest";

    public static final int COMMAND_NULL = 0;
    public static final int COMMAND_CHECK_RECOVERY_STATE = 1;

    private WorkHandler mWorkHandler;
    private static volatile boolean sWorkHandleLocked = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread workThread = new HandlerThread("WorkService: workThread");
        workThread.start();
        mWorkHandler = new WorkHandler(workThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        startForeground();
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }

        int command = intent.getIntExtra("command", COMMAND_NULL);
        int delayTime = intent.getIntExtra("delay", 1000);

        Log.d(TAG, "onStartCommand, command=" + command + " delayTime=" + delayTime);
        if (command == COMMAND_NULL) {
            return Service.START_NOT_STICKY;
        }

        Message msg = new Message();
        msg.what = command;
        mWorkHandler.sendMessageDelayed(msg, delayTime);
        return Service.START_REDELIVER_INTENT;
    }

    /**
     * WorkHandler
     */
    private class WorkHandler extends Handler {
        WorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COMMAND_CHECK_RECOVERY_STATE:
                    Log.d(TAG, "WorkHandler, COMMAND_CHECK_RECOVERY_STATE");
                    if (sWorkHandleLocked) {
                        Log.w(TAG, "WorkHandler, locked!!!");
                        return;
                    }

                    sWorkHandleLocked = true;
                    checkRecoveryState();
                    sWorkHandleLocked = false;
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Check recovery test state
     */
    private void checkRecoveryState() {
        try {
            FileReader fRead = new FileReader(RecoveryTestFragment.STATE_FILE);
            try {
                BufferedReader buffer = new BufferedReader(fRead);

                int state = BaseTestFragment.State.STOP;
                int curCount = 0;
                int maxCount = 0;
                boolean isWipeAll = false;
                boolean isEraseFlash = false;
                int delayTime = 0;

                String line;
                while ((line = buffer.readLine()) != null) {
                    Log.i(TAG, "checkRecoveryState, read line: " + line);
                    String[] temp = line.split(":");
                    if (temp.length < 2) {
                        Log.e(TAG, "checkRecoveryState, Recovery state file parse error.");
                        return;
                    }

                    if (temp[0].equals(RecoveryTestFragment.EXTRA_RECOVERY_FLAG)) {
                        state = Integer.valueOf(temp[1]);
                    } else if (temp[0].equals(RecoveryTestFragment.EXTRA_RECOVERY_COUNT)) {
                        curCount = Integer.valueOf(temp[1]);
                    } else if (temp[0].equals(RecoveryTestFragment.EXTRA_RECOVERY_MAX)) {
                        maxCount = Integer.valueOf(temp[1]);
                    } else if (temp[0].equals(RecoveryTestFragment.EXTRA_RECOVERY_WIPE_ALL)) {
                        isWipeAll = (Integer.valueOf(temp[1]) == 1);
                    } else if (temp[0].equals(RecoveryTestFragment.EXTRA_RECOVERY_ERASE_FLASH)) {
                        isEraseFlash = (Integer.valueOf(temp[1]) == 1);
                    } else if (temp[0].equals(RecoveryTestFragment.EXTRA_RECOVERY_DELAY)) {
                        delayTime = Integer.valueOf(temp[1]);
                    }
                }

                if (state == BaseTestFragment.State.RUNNING) {
                    Log.d(TAG, "checkRecoveryState, start Recovery Test");
                    Intent startIntent = new Intent(this, MainActivity.class);
                    startIntent.putExtra(RecoveryTestFragment.EXTRA_RECOVERY_FLAG, state);
                    startIntent.putExtra(RecoveryTestFragment.EXTRA_RECOVERY_COUNT, curCount);
                    startIntent.putExtra(RecoveryTestFragment.EXTRA_RECOVERY_MAX, maxCount);
                    startIntent.putExtra(RecoveryTestFragment.EXTRA_RECOVERY_WIPE_ALL, isWipeAll);
                    startIntent.putExtra(RecoveryTestFragment.EXTRA_RECOVERY_ERASE_FLASH, isEraseFlash);
                    startIntent.putExtra(RecoveryTestFragment.EXTRA_RECOVERY_DELAY, delayTime);
                    startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startIntent);
                } else {
                    Log.d(TAG, "checkRecoveryState, state stop");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fRead.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "No recovery state file found");
        }
    }

    /**
     * From Android O (8.1), the startup service does not allow the use of context.startService(intent),
     * but instead uses context.startForegroundService(intent), and the startForeground() interface
     * must be called after starting the service, otherwise the Service will be forcibly terminated
     * by the system.
     */
    private void startForeground() {
        Notification.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(ID, NAME, NotificationManager.IMPORTANCE_HIGH);
            chan.enableLights(true);
            chan.setLightColor(Color.RED);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(chan);
            }
            builder = new Notification.Builder(this, ID);
        } else {
            builder = new Notification.Builder(this.getApplicationContext());
        }
        Notification notification = builder.setContentTitle("Android Stress Test")
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .build();
        startForeground(1, notification);
    }
}
