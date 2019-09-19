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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileInputStream;
import com.ayst.stresstest.test.BaseTestFragment;
import com.ayst.stresstest.test.RecoveryTestFragment;
import com.ayst.stresstest.ui.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Administrator on 2018/5/21.
 */

public class WorkService extends Service {
    private final static String TAG = "WorkService";

    public static final int COMMAND_NULL = 0;
    public static final int COMMAND_CHECK_RECOVERY_STATE = 1;

    private Handler mMainHandler;
    private WorkHandler mWorkHandler;
    private static volatile boolean sWorkHandleLocked = false;
    private static UsbFile sUsbRootFile = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mMainHandler = new Handler(getMainLooper());

        HandlerThread workThread = new HandlerThread("WorkService: workThread");
        workThread.start();
        mWorkHandler = new WorkHandler(workThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
            String path = "";

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

    private UsbFile createUsbRootFile(Context context) {
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(context);

        for (UsbMassStorageDevice device : devices) {

            // before interacting with a device you need to call init()!
            try {
                device.init();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            // Only uses the first partition on the device
            FileSystem currentFs = device.getPartitions().get(0).getFileSystem();
            Log.d(TAG, "Capacity: " + currentFs.getCapacity());
            Log.d(TAG, "Occupied Space: " + currentFs.getOccupiedSpace());
            Log.d(TAG, "Free Space: " + currentFs.getFreeSpace());
            Log.d(TAG, "Chunk size: " + currentFs.getChunkSize());

            return currentFs.getRootDirectory();
        }

        return null;
    }

    private boolean checkRecoveryState() {
        if (null == sUsbRootFile) {
            sUsbRootFile = createUsbRootFile(this);
            if (null == sUsbRootFile) {
                return false;
            }
        }

        BufferedReader reader = null;
        InputStream is = null;
        InputStreamReader isr = null;
        try {
            UsbFile stateFile = null;
            UsbFile[] files = sUsbRootFile.listFiles();

            for (UsbFile file : files) {
                if (TextUtils.equals("recovery_state", file.getName())) {
                    Log.d(TAG, "checkRecoveryState, found recovery_state file");
                    stateFile = file;
                    break;
                }
            }
            if (null == stateFile) {
                Log.d(TAG, "checkRecoveryState, not found recovery_state file");
                return false;
            }

            is = new UsbFileInputStream(stateFile);
            isr = new InputStreamReader(is);
            reader = new BufferedReader(isr);

            String tempString = null;
            int state = BaseTestFragment.STATE_STOP;
            int curCount = 0;
            int maxCount = 0;
            boolean isWipeAll = false;
            boolean isEraseFlash = false;
            int delayTime = 0;
            while ((tempString = reader.readLine()) != null) {
                Log.i(TAG, "checkRecoveryState, read line: " + tempString);
                String[] temp = tempString.split(":");
                if (temp.length < 2) {
                    Log.e(TAG, "checkRecoveryState, Recovery state file parse error.");
                    return false;
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
            reader.close();
            is.close();
            isr.close();

            if (state == BaseTestFragment.STATE_RUNNING) {
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
            return true;
        } catch (IOException e) {
            Log.e(TAG, "readState, error: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    Log.e(TAG, "readState, error: " + e1.getMessage());
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e1) {
                    Log.e(TAG, "readState, error: " + e1.getMessage());
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e1) {
                    Log.e(TAG, "readState, error: " + e1.getMessage());
                }
            }
        }
        return false;
    }

    private void showToast(final String text) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }
}
