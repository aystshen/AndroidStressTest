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

package com.ayst.stresstest.test;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;
import com.ayst.stresstest.R;
import com.ayst.stresstest.util.AppUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

public class RecoveryTestFragment extends BaseTestFragment {
    public static final String EXTRA_RECOVERY_FLAG = "recovery_flag";
    public static final String EXTRA_RECOVERY_COUNT = "recovery_count";
    public static final String EXTRA_RECOVERY_MAX = "recovery_max";
    public static final String EXTRA_RECOVERY_WIPE_ALL = "recovery_wipe_all";
    public static final String EXTRA_RECOVERY_ERASE_FLASH = "recovery_erase_flash";
    public static final String EXTRA_RECOVERY_DELAY = "recovery_delay";

    private final static int MSG_RECOVERY_COUNTDOWN = 1001;

    private CheckBox mEraseFlashCheckbox;
    private CheckBox mWipeAllCheckbox;
    private EditText mDelayEdt;
    private TextView mCountdownTv;

    private int mDelayTime;
    private int mCountDownTime;
    private boolean mIsWipeAll = false;
    private boolean mIsEraseFlash = false;
    private UsbFile mUsbRootFile = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mState = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_FLAG, STATE_STOP);
        mCurrentCount = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_COUNT, 0);
        mMaxTestCount = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_MAX, 0);
        mIsWipeAll = mActivity.getIntent().getBooleanExtra(EXTRA_RECOVERY_WIPE_ALL, false);
        mIsEraseFlash = mActivity.getIntent().getBooleanExtra(EXTRA_RECOVERY_ERASE_FLASH, false);
        mDelayTime = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_DELAY, 5);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.recovery_test);
        setCountType(COUNT_TYPE_COUNT);
        setType(TestType.TYPE_RECOVERY_TEST);

        View contentView = inflater.inflate(R.layout.fragment_recovery_test, container, false);
        setContentView(contentView);

        initView(contentView);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        check();
    }

    private void initView(View view) {
        mEraseFlashCheckbox = (CheckBox) view.findViewById(R.id.chbox_erase_flash);
        mWipeAllCheckbox = (CheckBox) view.findViewById(R.id.chbox_wipe_all);
        mDelayEdt = (EditText) view.findViewById(R.id.edt_delay);
        mCountdownTv = (TextView) view.findViewById(R.id.tv_countdown);

        mEraseFlashCheckbox.setChecked(mIsEraseFlash);
        mWipeAllCheckbox.setChecked(mIsWipeAll);
        mDelayEdt.setText(mDelayTime + "");

        mEraseFlashCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!AppUtils.isExternalStorageMounted()) {
                        Toast.makeText(mActivity, R.string.recovery_test_insert_sdcard_tips, Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                }
            }
        });

        mWipeAllCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!AppUtils.isExternalStorageMounted()) {
                        Toast.makeText(mActivity, R.string.recovery_test_insert_sdcard_tips, Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                }
            }
        });
    }

    private void check() {
        if (isRunning()) {
            if (mMaxTestCount != 0 && mMaxTestCount <= mCurrentCount) {
                stop();
            } else {
                mCountDownTime = mDelayTime; // DELAY_TIME/1000;
                mHandler.sendEmptyMessage(MSG_RECOVERY_COUNTDOWN);
            }
        }
    }

    @Override
    public void onStartClicked() {
        if (null == mUsbRootFile) {
            mUsbRootFile = createUsbRootFile();
            if (null == mUsbRootFile) {
                Toast.makeText(mActivity, R.string.recovery_test_insert_udisk_tips, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        super.onStartClicked();
    }

    @Override
    public void start() {
        super.start();

        mDelayTime = Integer.valueOf(mDelayEdt.getText().toString());
        new AlertDialog.Builder(mActivity)
                .setMessage(String.format(getString(R.string.recovery_test_recovery_tips), mDelayTime))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                mState = STATE_RUNNING;
                                mIsEraseFlash = mEraseFlashCheckbox.isChecked();
                                mIsWipeAll = mWipeAllCheckbox.isChecked();
                                mCountDownTime = mDelayTime;
                                mHandler.sendEmptyMessage(MSG_RECOVERY_COUNTDOWN);
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                mState = STATE_STOP;
                                dialog.cancel();
                            }
                        }).show();
    }

    @Override
    public void stop() {
        super.stop();

        mHandler.removeMessages(MSG_RECOVERY_COUNTDOWN);
        mCountdownTv.setVisibility(View.GONE);
        saveState();
    }

    @Override
    protected void handleMsg(Message msg) {
        super.handleMsg(msg);

        switch (msg.what) {
            case MSG_RECOVERY_COUNTDOWN:
                if (!isRunning()) {
                    return;
                }

                if (mCountDownTime > 0) {
                    mCountdownTv.setText(mCountDownTime + "");
                    mCountdownTv.setVisibility(View.VISIBLE);
                    mCountDownTime--;
                    mHandler.sendEmptyMessageDelayed(MSG_RECOVERY_COUNTDOWN, 1000);
                } else {
                    mCountdownTv.setVisibility(View.GONE);
                    recovery();
                }

                break;
        }
    }

    private void recovery() {
        // save state
        incCurrentCount();

        if (saveState()) {
            // 恢复出厂设置
            if (mIsWipeAll) {
                try {
                    bootCommand(mActivity, "--wipe_all");
                } catch (IOException e) {
                    Log.e(TAG, "recovery, bootCommand failed: " + e.getMessage());
                }
            } else if (mIsEraseFlash) {
                Intent intent = new Intent("com.android.internal.os.storage.FORMAT_AND_FACTORY_RESET");
                ComponentName componentName = new ComponentName("android", "com.android.internal.os.storage.ExternalStorageFormatter");
                intent.setComponent(componentName);
                intent.putExtra("android.intent.extra.REASON", "WipeAllFlash");
                mActivity.startService(intent);
            } else {
                Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
                intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                intent.putExtra("android.intent.extra.REASON", "MasterClearConfirm");
                mActivity.sendBroadcast(intent);
            }
            Log.d(TAG, "recovery");
        } else {
            stop();
        }
    }

    private UsbFile createUsbRootFile() {
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(mActivity);

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

    private boolean saveState() {
        StringBuilder sb = new StringBuilder();
        sb.append(EXTRA_RECOVERY_FLAG).append(":").append(mState).append("\n");
        sb.append(EXTRA_RECOVERY_COUNT).append(":").append(mCurrentCount).append("\n");
        sb.append(EXTRA_RECOVERY_MAX).append(":").append(mMaxTestCount).append("\n");
        sb.append(EXTRA_RECOVERY_WIPE_ALL).append(":").append(mIsWipeAll ? "1" : "0").append("\n");
        sb.append(EXTRA_RECOVERY_ERASE_FLASH).append(":").append(mIsEraseFlash ? "1" : "0").append("\n");
        sb.append(EXTRA_RECOVERY_DELAY).append(":").append(mDelayTime).append("\n");
        String content = sb.toString();

        if (null == mUsbRootFile) {
            mUsbRootFile = createUsbRootFile();
            if (null == mUsbRootFile) {
                return false;
            }
        }

        OutputStream fos = null;
        try {
            UsbFile stateFile = null;
            UsbFile[] files = mUsbRootFile.listFiles();

            for(UsbFile file: files) {
                if(TextUtils.equals("recovery_state", file.getName())) {
                    Log.d(TAG, "saveState, found recovery_state file");
                    stateFile = file;
                    break;
                }
            }
            if (null == stateFile) {
                stateFile = mUsbRootFile.createFile("recovery_state");
            }

            fos = new UsbFileOutputStream(stateFile);
            fos.write(content.getBytes());
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "saveState, error: " + e.getMessage());
        } catch (IOException ie) {
            Log.e(TAG, "saveState, error: " + ie.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "saveState, error: " + e.getMessage());
                }
            }
        }

        return false;
    }

    /**
     * Reboot into the recovery system with the supplied argument.
     *
     * @param arg to pass to the recovery utility.
     * @throws IOException if something goes wrong.
     */
    private static File RECOVERY_DIR = new File("/cache/recovery");
    private static File COMMAND_FILE = new File(RECOVERY_DIR, "command");

    private static void bootCommand(Context context, String arg) throws IOException {
        RECOVERY_DIR.mkdirs();  // In case we need it
        COMMAND_FILE.delete();  // In case it's not writable

        FileWriter command = new FileWriter(COMMAND_FILE);
        try {
            command.write(arg);
            command.write("\n");
        } finally {
            command.close();
        }

        // Having written the command file, go ahead and reboot
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pm.reboot("recovery");

        throw new IOException("Reboot failed (no permissions?)");
    }
}
