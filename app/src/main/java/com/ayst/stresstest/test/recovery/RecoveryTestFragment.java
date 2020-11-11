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

package com.ayst.stresstest.test.recovery;

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

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseCountTestFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.util.AppUtils;
import com.ayst.stresstest.util.FileUtils;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RecoveryTestFragment extends BaseCountTestFragment {

    private static File RECOVERY_DIR = new File("/cache/recovery");
    private static File COMMAND_FILE = new File(RECOVERY_DIR, "command");

    /* Used to save the current state.
     * Must be prefixed with last, otherwise the restart file will be cleared.
     */
    public static File STATE_FILE = new File(RECOVERY_DIR, "last_recovery_test_state");

    public static final String EXTRA_RECOVERY_FLAG = "recovery_flag";
    public static final String EXTRA_RECOVERY_COUNT = "recovery_count";
    public static final String EXTRA_RECOVERY_MAX = "recovery_max";
    public static final String EXTRA_RECOVERY_WIPE_ALL = "recovery_wipe_all";
    public static final String EXTRA_RECOVERY_ERASE_FLASH = "recovery_erase_flash";
    public static final String EXTRA_RECOVERY_DELAY = "recovery_delay";

    private final static int MSG_RECOVERY_COUNTDOWN = 1001;
    private final static int DELAY_DEFAULT = 10; // Default 10s

    @BindView(R.id.chbox_erase_flash)
    CheckBox mEraseFlashCheckbox;
    @BindView(R.id.chbox_wipe_all)
    CheckBox mWipeAllCheckbox;
    @BindView(R.id.edt_delay)
    EditText mDelayEdt;
    @BindView(R.id.tv_countdown)
    TextView mCountdownTv;
    Unbinder unbinder;

    private int mDelayTime;
    private int mCountDownTime;
    private boolean mIsWipeAll = false;
    private boolean mIsEraseFlash = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mState = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_FLAG, State.STOP);
        mCurrentCount = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_COUNT, 0);
        mTargetCount = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_MAX, 0);
        mIsWipeAll = mActivity.getIntent().getBooleanExtra(EXTRA_RECOVERY_WIPE_ALL, false);
        mIsEraseFlash = mActivity.getIntent().getBooleanExtra(EXTRA_RECOVERY_ERASE_FLASH, false);
        mDelayTime = mActivity.getIntent().getIntExtra(EXTRA_RECOVERY_DELAY, DELAY_DEFAULT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.recovery_test);
        setType(TestType.TYPE_RECOVERY_TEST);

        View contentView = inflater.inflate(R.layout.fragment_recovery_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onStart() {
        super.onStart();

        check();
    }

    private void check() {
        if (isRunning()) {
            if (next()) {
                mCountDownTime = mDelayTime;
                mHandler.sendEmptyMessage(MSG_RECOVERY_COUNTDOWN);
            }
        }
    }

    @Override
    public void onStartClicked() {
        String delayStr = mDelayEdt.getText().toString();
        if (TextUtils.isEmpty(delayStr)) {
            showToast(R.string.reboot_test_delay_empty);
            return;
        }
        mDelayTime = Integer.valueOf(mDelayEdt.getText().toString());

        super.onStartClicked();
    }

    @Override
    public void start() {
        new AlertDialog.Builder(mActivity)
                .setMessage(String.format(getString(R.string.recovery_test_recovery_tips), mDelayTime))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
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
                                stop();
                                dialog.cancel();
                            }
                        }).show();

        super.start();
    }

    @Override
    public void stop() {
        mHandler.removeMessages(MSG_RECOVERY_COUNTDOWN);
        mCountdownTv.setVisibility(View.GONE);

        super.stop();

        /*
         * Must be placed after super.stop because you want to
         * save the new state to the file.
         */
        saveState();
    }

    @Override
    public boolean isSupport() {
        return true;
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
        if (saveState()) {
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
            showErrorDialog(R.string.recovery_test_save_state_failed);
        }
    }

    private boolean saveState() {
        StringBuilder sb = new StringBuilder();
        sb.append(EXTRA_RECOVERY_FLAG).append(":").append(mState).append("\n");
        sb.append(EXTRA_RECOVERY_COUNT).append(":").append(mCurrentCount).append("\n");
        sb.append(EXTRA_RECOVERY_MAX).append(":").append(mTargetCount).append("\n");
        sb.append(EXTRA_RECOVERY_WIPE_ALL).append(":").append(mIsWipeAll ? "1" : "0").append("\n");
        sb.append(EXTRA_RECOVERY_ERASE_FLASH).append(":").append(mIsEraseFlash ? "1" : "0").append("\n");
        sb.append(EXTRA_RECOVERY_DELAY).append(":").append(mDelayTime).append("\n");
        String content = sb.toString();

        try {
            RECOVERY_DIR.mkdirs();
            STATE_FILE.delete();
            FileUtils.writeFile(STATE_FILE, content);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Reboot into the recovery system with the supplied argument.
     *
     * @param arg to pass to the recovery utility.
     * @throws IOException if something goes wrong.
     */
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
